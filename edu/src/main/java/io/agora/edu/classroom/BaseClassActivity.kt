package io.agora.edu.classroom

import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.view.WindowManager
import io.agora.agoraactionprocess.AgoraActionListener
import io.agora.agoraactionprocess.AgoraActionMsgRes
import io.agora.edu.common.bean.response.RoomPreCheckRes
import io.agora.edu.launch.AgoraEduEvent
import io.agora.edu.launch.AgoraEduLaunchConfig
import io.agora.edu.launch.AgoraEduSDK
import io.agora.edu.widget.EyeProtection
import io.agora.education.api.EduCallback
import io.agora.education.api.base.EduError
import io.agora.education.api.base.EduError.Companion.internalError
import io.agora.education.api.manager.EduManager
import io.agora.education.api.manager.listener.EduManagerEventListener
import io.agora.education.api.message.EduActionMessage
import io.agora.education.api.message.EduChatMsg
import io.agora.education.api.message.EduMsg
import io.agora.education.api.room.EduRoom
import io.agora.education.api.room.data.*
import io.agora.education.api.room.listener.EduRoomEventListener
import io.agora.education.api.statistics.ConnectionState
import io.agora.education.api.statistics.NetworkQuality
import io.agora.education.api.stream.data.EduStreamEvent
import io.agora.education.api.stream.data.EduStreamInfo
import io.agora.education.api.stream.data.VideoSourceType
import io.agora.education.api.user.EduStudent
import io.agora.education.api.user.EduUser
import io.agora.education.api.user.data.*
import io.agora.education.api.user.listener.EduUserEventListener
import io.agora.education.impl.Constants
import io.agora.report.ReportManager.getAPaasReporter
import io.agora.report.reporters.APaasReporter
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcChannel
import io.agora.rte.RteEngineImpl
import io.agora.rte.data.RteLocalVideoStats
import io.agora.rte.data.RteRemoteVideoStats
import io.agora.uikit.AgoraUIError
import io.agora.uikit.impl.chat.AgoraUIChatItem
import io.agora.uikit.interfaces.protocols.IAgoraUIContainer

abstract class BaseClassActivity : BaseActivity(),
        EduRoomEventListener, EduUserEventListener,
        EduManagerEventListener, AgoraActionListener {

    object Data {
        @kotlin.jvm.JvmField
        val LAUNCHCONFIG = "LAUNCHCONFIG"

        @kotlin.jvm.JvmField
        val PRECHECKDATA = "PRECHECKDATA"

        @kotlin.jvm.JvmField
        val RESULT_CODE = 808
    }

    private val tag = BaseClassActivity::javaClass.name

    protected var container: IAgoraUIContainer? = null
    protected var whiteBoardContainer: ViewGroup? = null

    protected var launchConfig: AgoraEduLaunchConfig? = null
    protected var preCheckData: RoomPreCheckRes? = null
    protected var eduRoom: EduRoom? = null
    protected var chatManager: ChatManager? = null
    protected var whiteBoardManager: WhiteBoardManager? = null
    protected var oneToOneVideoManager: OneToOneVideoManager? = null
    protected var screenShareManager: ScreenShareManager? = null
    protected var roomStatusManager: RoomStatusManager? = null
    protected var teacherVideoManager: TeacherVideoManager? = null

    @Volatile
    var isJoining = false

    @Volatile
    var joinSuccess = false

    // The entire room entry should be said complete when
    // both room and white board join successfully
    private var roomJoinSuccess = false
    private var whiteboardJoinSuccess = false

    @Synchronized
    protected fun processJoinSuccess(): Boolean {
        return roomJoinSuccess && whiteboardJoinSuccess
    }

    @Synchronized
    protected fun setRoomJoinSuccess() {
        roomJoinSuccess = true
    }

    @Synchronized
    protected fun setWhiteboardJoinSuccess() {
        whiteboardJoinSuccess = true
    }

    protected fun checkProcessSuccess() {
        if (processJoinSuccess()) {
            getReporter().reportRoomEntryEnd("1", null, null, null)
        }
    }

    companion object EduManagerDelegate {
        private var eduManager: EduManager? = null

        fun setEduManager(manager: EduManager) {
            eduManager = manager
        }

        fun getEduManager(): EduManager? {
            return eduManager
        }
    }

    private val chatManagerEventListener = object : ChatManagerEventListener {
        override fun onAddMsg(item: AgoraUIChatItem) {
            container?.chat()?.addMessage(item)
        }

        override fun onSendLocalMessageResult(message: String, uid: String, messageId: Int, timestamp: Long, success: Boolean) {
            container?.chat()?.sendLocalMessageResult(message, uid, messageId, timestamp, success)
        }

        override fun onFetchMessageHistoryResult(success: Boolean, list: MutableList<AgoraUIChatItem>?) {
            container?.chat()?.fetchMessageHistoryResult(success, list)
        }

        override fun onMuteChat(mute: Boolean) {
            container?.chat()?.allowChat(!mute)
        }

        override fun onShowChatTips(msg: String) {
            container?.chat()?.showChatTips(msg)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        window.setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onCreate(savedInstanceState)
    }

    override fun initData() {
        eduManager?.eduManagerEventListener = this
        launchConfig = intent.getParcelableExtra(Data.LAUNCHCONFIG)
        preCheckData = intent.getParcelableExtra(Data.PRECHECKDATA)

        launchConfig?.let {
            eduRoom = getEduManager()?.createClassroom(
                    RoomCreateOptions(it.roomUuid, it.roomName, it.roomType))
            eduRoom?.eventListener = this

            chatManager = ChatManager(this, eduRoom, it)
            chatManager?.eventListener = chatManagerEventListener
        }
    }

    override fun onStart() {
        super.onStart()
        launchConfig?.let { EyeProtection.setNeedShow(it.eyeCare == 1) }
    }

    override fun onDestroy() {
        super.onDestroy()
        roomStatusManager?.dispose()
        eduRoom = null
        getEduManager()?.let {
            it.eduManagerEventListener = null
            it.release()
        }

        AgoraEduSDK.agoraEduLaunchCallback.onCallback(AgoraEduEvent.AgoraEduEventDestroyed)
    }

    override fun onBackPressed() {
        container?.showLeave()
    }

    protected open fun joinRoomAsStudent(name: String?, uuid: String?,
                                         autoSubscribe: Boolean, autoPublish: Boolean,
                                         needUserListener: Boolean, callback: EduCallback<EduStudent?>) {
        if (isJoining) {
            Log.e(tag, "join fail because you are joining the classroom")
            return
        }

        if (launchConfig == null) {
            Log.e(tag, "join fail because no launch config info is found")
            return
        }

        isJoining = true
        val options = RoomJoinOptions(uuid!!, name, EduUserRole.STUDENT,
                RoomMediaOptions(autoSubscribe, autoPublish), launchConfig?.roomType)

        eduRoom?.joinClassroom(options, object : EduCallback<EduUser> {
            override fun onSuccess(res: EduUser?) {
                if (res != null) {
                    joinSuccess = true
                    isJoining = false
                    if (needUserListener) {
                        res.eventListener = this@BaseClassActivity
                    }
                    val student = res as EduStudent
                    launchConfig?.let {
                        if (!it.frontCamera) {
                            RteEngineImpl.switchCamera()
                        }
                    }
                    callback.onSuccess(student)
                    AgoraEduSDK.agoraEduLaunchCallback.onCallback(AgoraEduEvent.AgoraEduEventReady)
                } else {
                    val error = internalError("join failed: localUser is null")
                    callback.onFailure(error)
                    reportClassJoinSuccess("0", error.type.toString() + "", error.httpError.toString() + "")
                }
            }

            override fun onFailure(error: EduError) {
                isJoining = false
                callback.onFailure(error)
                reportClassJoinSuccess("0", error.type.toString() + "", error.httpError.toString() + "")
            }
        })
    }

    protected fun joinFailed(code: Int, reason: String) {
        val msg = "join classRoom failed->code:$code,reason:$reason"
        container?.showError(AgoraUIError(code, msg))
        Constants.AgoraLog.e(tag, msg)
        AgoraEduSDK.agoraEduLaunchCallback.onCallback(AgoraEduEvent.AgoraEduEventFailed)
        val intent = intent.putExtra(AgoraEduSDK.CODE, code).putExtra(AgoraEduSDK.REASON, reason)
        setResult(Data.RESULT_CODE, intent)
        finish()
    }

    protected fun getLocalUser(callback: EduCallback<EduUser?>) {
        if (eduRoom == null) {
            callback.onFailure(internalError("current eduRoom is null"))
            return
        }

        eduRoom?.getLocalUser(object : EduCallback<EduUser> {
            override fun onSuccess(res: EduUser?) {
                if (res == null) {
                    callback.onFailure(internalError("current eduRoom`s localUsr is null"))
                } else {
                    callback.onSuccess(res)
                }
            }

            override fun onFailure(error: EduError) {
                callback.onFailure(error)
            }
        })
    }

    protected fun forceLeave(finish: Boolean) {
        runOnUiThread {
            // exit whiteBoard
            whiteBoardManager?.releaseBoard()
            // exit room
            eduRoom?.leave(object : EduCallback<Unit> {
                override fun onSuccess(res: Unit?) {
                    if (finish) {
                        finish()
                    }
                }

                override fun onFailure(error: EduError) {
                    Constants.AgoraLog.e("$tag:leave EduRoom error->code:" + error.type + ",reason:" + error.msg)
                }
            })
        }
    }

    protected fun getLocalUserInfo(callback: EduCallback<EduUserInfo?>) {
        getLocalUser(object : EduCallback<EduUser?> {
            override fun onSuccess(res: EduUser?) {
                callback.onSuccess(res?.userInfo)
            }

            override fun onFailure(error: EduError) {
                callback.onFailure(error)
            }
        })
    }

    protected fun getCurFullUser(callback: EduCallback<MutableList<EduUserInfo>>) {
        eduRoom?.getFullUserList(callback)
                ?: callback.onFailure(internalError("current eduRoom is null"))
    }

    protected fun getCurFullStream(callback: EduCallback<MutableList<EduStreamInfo>>) {
        eduRoom?.getFullStreamList(callback)
                ?: callback.onFailure(internalError("current eduRoom is null"))
    }

    protected open fun getMainRoomStatus(callback: EduCallback<EduRoomStatus>) {
        eduRoom?.getRoomStatus(object : EduCallback<EduRoomStatus> {
            override fun onSuccess(res: EduRoomStatus?) {
                if (res == null) {
                    callback.onFailure(internalError("current eduRoom`s status is null"))
                } else {
                    callback.onSuccess(res)
                }
            }

            override fun onFailure(error: EduError) {
                callback.onFailure(error)
            }
        })
    }

    protected fun getMainRoomUuid(callback: EduCallback<String?>) {
        eduRoom?.getRoomInfo(object : EduCallback<EduRoomInfo> {
            override fun onSuccess(res: EduRoomInfo?) {
                callback.onSuccess(res!!.roomUuid)
            }

            override fun onFailure(error: EduError) {
                callback.onFailure(error)
            }
        }) ?: callback.onFailure(internalError("current eduRoom is null"))
    }

    fun getReporter(): APaasReporter {
        return getAPaasReporter()
    }

    private fun reportClassJoinSuccess(result: String, errorCode: String, httpCode: String) {
        getReporter().reportRoomEntryEnd(result, errorCode, httpCode, null)
    }

    override fun onRemoteUsersInitialized(users: List<EduUserInfo>, classRoom: EduRoom) {

    }

    override fun onRemoteUsersJoined(users: List<EduUserInfo>, classRoom: EduRoom) {
        Constants.AgoraLog.e("$tag:Receive a callback when the remote user joined")
    }

    override fun onRemoteUserLeft(userEvent: EduUserEvent, classRoom: EduRoom) {
        Constants.AgoraLog.e("$tag:Receive a callback when the remote user left")
    }

    override fun onRemoteUserUpdated(userEvent: EduUserEvent, type: EduUserStateChangeType, classRoom: EduRoom) {
        Constants.AgoraLog.e("$tag:Receive a callback when the remote user modified")
    }

    override fun onRoomMessageReceived(message: EduMsg, classRoom: EduRoom) {

    }

    override fun onRoomChatMessageReceived(chatMsg: EduChatMsg, classRoom: EduRoom) {

    }

    override fun onRemoteStreamsInitialized(streams: List<EduStreamInfo>, classRoom: EduRoom) {
        Constants.AgoraLog.e("$tag:onRemoteStreamsInitialized")
    }

    override fun onRemoteStreamsAdded(streamEvents: MutableList<EduStreamEvent>, classRoom: EduRoom) {
        Constants.AgoraLog.e("$tag:Receive callback to add remote stream")
    }

    override fun onRemoteStreamUpdated(streamEvents: MutableList<EduStreamEvent>, classRoom: EduRoom) {
        Constants.AgoraLog.e("$tag:Receive callback to update remote stream")
    }

    override fun onRemoteStreamsRemoved(streamEvents: MutableList<EduStreamEvent>, classRoom: EduRoom) {
        Constants.AgoraLog.e("$tag:Receive callback to remove remote stream")
    }

    override fun onRoomStatusChanged(type: EduRoomChangeType, operatorUser: EduUserInfo?, classRoom: EduRoom) {
        chatManager?.notifyMuteChatStatus(type)
    }

    override fun onRoomPropertiesChanged(classRoom: EduRoom, cause: MutableMap<String, Any>?) {
        Constants.AgoraLog.e("$tag:Received callback of roomProperty change")
    }

    override fun onNetworkQualityChanged(quality: NetworkQuality, user: EduBaseUserInfo, classRoom: EduRoom) {
        if (user.userUuid == launchConfig?.userUuid) {
            roomStatusManager?.updateNetworkState(quality)
        } else {
        }
    }

    override fun onConnectionStateChanged(state: ConnectionState, classRoom: EduRoom) {
        classRoom.getRoomInfo(object : EduCallback<EduRoomInfo> {
            override fun onSuccess(roomInfo: EduRoomInfo?) {
                Constants.AgoraLog.e("$tag:onConnectionStateChanged->" + state.value + ",room:"
                        + roomInfo!!.roomUuid)
            }

            override fun onFailure(error: EduError) {}
        })
        roomStatusManager?.updateConnectionState(state)
    }

    override fun onRemoteVideoStateChanged(rtcChannel: RtcChannel?, uid: Int, state: Int, reason: Int, elapsed: Int) {
    }

    override fun onRemoteVideoStats(stats: RteRemoteVideoStats) {

    }

    override fun onLocalVideoStateChanged(localVideoState: Int, error: Int) {

    }

    override fun onLocalVideoStats(stats: RteLocalVideoStats) {

    }

    override fun onAudioVolumeIndicationOfLocalSpeaker(speakers: Array<out IRtcEngineEventHandler.AudioVolumeInfo>?, totalVolume: Int) {

    }

    override fun onAudioVolumeIndicationOfRemoteSpeaker(speakers: Array<out IRtcEngineEventHandler.AudioVolumeInfo>?, totalVolume: Int) {

    }

    override fun onLocalUserUpdated(userEvent: EduUserEvent, type: EduUserStateChangeType) {

    }

    override fun onLocalStreamAdded(streamEvent: EduStreamEvent) {
        Constants.AgoraLog.e("$tag:Receive callback to add local stream")
        when (streamEvent.modifiedStream.videoSourceType) {
            VideoSourceType.CAMERA -> {
                Constants.AgoraLog.e("$tag:Receive callback to add local camera stream")
            }
            VideoSourceType.SCREEN -> {
            }
            else -> {
            }
        }
    }

    override fun onLocalStreamUpdated(streamEvent: EduStreamEvent) {
        Constants.AgoraLog.e("$tag:Receive callback to update local stream")
        when (streamEvent.modifiedStream.videoSourceType) {
            VideoSourceType.CAMERA -> {
            }
            VideoSourceType.SCREEN -> {
            }
            else -> {
            }
        }
    }

    override fun onLocalStreamRemoved(streamEvent: EduStreamEvent) {
        Constants.AgoraLog.e("$tag:Receive callback to remove local stream")
        when (streamEvent.modifiedStream.videoSourceType) {
            VideoSourceType.CAMERA -> {
            }
            VideoSourceType.SCREEN -> {
            }
            else -> {
            }
        }
    }

    override fun onLocalUserLeft(userEvent: EduUserEvent, leftType: EduUserLeftType) {

    }

    override fun onUserMessageReceived(message: EduMsg) {

    }

    override fun onUserChatMessageReceived(chatMsg: EduChatMsg) {

    }

    override fun onUserActionMessageReceived(actionMessage: EduActionMessage) {

    }

    override fun onApply(actionMsgRes: AgoraActionMsgRes) {

    }

    override fun onInvite(actionMsgRes: AgoraActionMsgRes) {

    }

    override fun onAccept(actionMsgRes: AgoraActionMsgRes) {

    }

    override fun onReject(actionMsgRes: AgoraActionMsgRes) {

    }

    override fun onCancel(actionMsgRes: AgoraActionMsgRes) {

    }

    override fun onRemoteUserPropertiesChanged(classRoom: EduRoom, userInfo: EduUserInfo, cause: MutableMap<String, Any>?) {

    }

    override fun onLocalUserPropertiesChanged(userInfo: EduUserInfo, cause: MutableMap<String, Any>?) {

    }
}