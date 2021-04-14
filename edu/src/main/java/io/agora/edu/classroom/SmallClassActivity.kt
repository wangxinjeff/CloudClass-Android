package io.agora.edu.classroom

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.FrameLayout
import com.google.gson.Gson
import com.herewhite.sdk.domain.MemberState
import com.herewhite.sdk.domain.SDKError
import com.herewhite.sdk.domain.SceneState
import io.agora.edu.launch.AgoraEduCourseware
import io.agora.edu.launch.AgoraEduSDK
import io.agora.education.api.EduCallback
import io.agora.education.api.base.EduError
import io.agora.education.api.room.EduRoom
import io.agora.education.api.user.EduStudent
import io.agora.education.api.user.EduUser
import io.agora.education.api.message.EduChatMsg
import io.agora.education.api.room.data.EduRoomChangeType
import io.agora.education.api.statistics.ConnectionState
import io.agora.education.api.statistics.NetworkQuality
import io.agora.education.api.stream.data.EduStreamEvent
import io.agora.education.api.stream.data.EduStreamInfo
import io.agora.education.api.stream.data.VideoSourceType
import io.agora.education.api.user.data.*
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcChannel
import io.agora.uikit.AgoraUIError
import io.agora.uikit.impl.chat.AgoraUIChatItem
import io.agora.uikit.impl.chat.AgoraUIChatItemType
import io.agora.uikit.impl.chat.AgoraUIChatSource
import io.agora.uikit.impl.chat.AgoraUIChatState
import io.agora.uikit.interfaces.protocols.AgoraUIContainer
import io.agora.uikit.impl.container.AgoraContainerType
import io.agora.uikit.impl.handsup.AgoraUIHandsUpState
import io.agora.uikit.impl.room.AgoraUIConnectionState
import io.agora.uikit.impl.room.ClassState
import io.agora.uikit.impl.room.AgoraUINetworkState
import io.agora.uikit.impl.users.AgoraUIUserDetailInfo
import io.agora.uikit.impl.users.AgoraUIUserInfo
import io.agora.uikit.interfaces.listeners.IAgoraUIDeviceListener
import io.agora.uikit.interfaces.protocols.AgoraUIDrawingConfig

class SmallClassActivity : BaseClassActivity() {
    private val tag = "SmallClassActivity"

    private var userListManager: UserListManager? = null

    private var handsUpManager: HandsUpManager? = null

    private val settingListener = object : IAgoraUIDeviceListener {
        override fun onCameraEnabled(enabled: Boolean) {
            Log.i(tag, "onCameraEnabled $enabled")
        }

        override fun onCameraFacingChanged(front: Boolean) {
            Log.i(tag, "onCameraFacingChanged $front")
        }

        override fun onMicEnabled(enabled: Boolean) {
            Log.i(tag, "onMicEnabled $enabled")
        }

        override fun onSpeakerEnabled(enabled: Boolean) {
            Log.i(tag, "onSpeakerEnabled $enabled")
        }
    }

    private val roomStatusManagerEventListener = object : RoomStatusManagerEventListener {
        override fun onClassName(name: String) {
            container?.room()?.setClassroomName(name)
        }

        override fun onClassState(state: ClassState) {
            container?.room()?.setClassState(state)
        }

        override fun onClassTime(timeStr: String) {
            container?.room()?.setClassTime(timeStr)
        }

        override fun onNetworkState(stateAgoraUI: AgoraUINetworkState) {
            container?.room()?.setNetworkState(stateAgoraUI)
        }

        override fun onConnectionState(state: AgoraUIConnectionState) {
            container?.room()?.setConnectionState(state)
        }

        override fun onClassTips(msg: String) {
            container?.room()?.showClassTips(msg)
        }

        override fun onForceLeave() {
            forceLeave(true)
        }

        override fun onAbortedByRemoteLogin() {
            forceLeave(true)
        }
    }

    private val whiteBoardManagerEventListener = object : WhiteBoardManagerEventListener {
        override fun onWhiteBoardJoinSuccess(config: AgoraUIDrawingConfig) {
            getReporter().reportWhiteBoardResult("1", null, null)
            setWhiteboardJoinSuccess()
            checkProcessSuccess()
            container?.whiteBoard()?.setDrawingConfig(config)
        }

        override fun onWhiteBoardJoinFail(error: SDKError?) {
            if (error != null) {
                container?.showError(AgoraUIError(-1, error.toString()))
            }
            getReporter().reportWhiteBoardResult("0", "White board join room fail", null)
        }

        override fun onWhiteBoardLoadingStatus(loading: Boolean) {
            container?.whiteBoard()?.setLoadingVisible(loading)
        }

        override fun onDisableDeviceInput(disable: Boolean) {
            container?.whiteBoard()?.setDrawingEnabled(!disable)
            container?.whiteBoard()?.setPagingEnabled(!disable)
        }

        override fun onDisableCameraTransform(disable: Boolean) {
            container?.whiteBoard()?.setZoomEnable(!disable, !disable)
        }

        override fun onSceneStateChanged(state: SceneState) {
            Log.e(tag, "onSceneStateChanged->" + Gson().toJson(state))
            container?.whiteBoard()?.setPageNo(state.index, state.scenes.size)
        }

        override fun onMemberStateChanged(state: MemberState) {

        }

        override fun onBoardSizeChangedFollowBroadcaster(full: Boolean) {
            container?.whiteBoard()?.setIsFullScreen(full)
            container?.whiteBoard()?.setFullScreenEnable(!full)
        }

        override fun onRoster(anchor: View) {
            val roster = container?.roster(anchor)
            roster?.show()
        }

        // when granted,
        // progressView or failedView is showing, whiteBoard is disableInput
        // progressView or failedView is dismiss, whiteBoard is enableInput
        // but when not granted,
        // progressView or failedView is dismiss, whiteBoard is disableInput
        override fun onBoardInputEnable(enable: Boolean) {
            val granted = whiteBoardManager?.isGranted(launchConfig!!.userUuid) ?: false
            container?.whiteBoard()?.setDrawingEnabled(granted && enable)
            container?.whiteBoard()?.setInteractionEnabled(enable)
        }

        override fun onBoardResourceProgress(url: String, progress: Double) {
            container?.whiteBoard()?.setDownloadProgress(url, progress.toFloat())
        }

        override fun onBoardResourceLoadTimeout(url: String) {
            container?.whiteBoard()?.setDownloadTimeout(url)
        }

        override fun onBoardResourceReady(url: String) {
            container?.whiteBoard()?.setDownloadCompleted(url)
        }

        override fun onBoardResourceLoadFailed(url: String) {
            container?.whiteBoard()?.downloadError(url)
        }

        override fun onFullScreen(full: Boolean) {
            container?.whiteBoard()?.setIsFullScreen(full)
        }

        override fun onCancelCurDownload() {
            container?.whiteBoard()?.cancelCurDownload()
        }

        override fun onGrantedChanged(enabled: Boolean) {
            container?.whiteBoard()?.showPermissionTips(enabled)
            userListManager?.notifyUserList()
        }
    }

    private val teacherVideoManagerEventListener = object : TeacherVideoManagerEventListener {
        override fun onDetailInfoUpsert(info: AgoraUIUserDetailInfo) {
            container?.videoGroup()?.upsertUserDetailInfo(info)
        }

        override fun onAudioVolumeIndicationUpdate(value: Int, streamUuid: String) {
            container?.videoGroup()?.updateAudioVolumeIndication(value, streamUuid)
        }

        override fun onMediaMsgUpdate(msg: String) {
            container?.videoGroup()?.updateMediaMessage(msg)
        }

        override fun onGranted(userId: String): Boolean {
            return whiteBoardManager?.isGranted(userId) ?: false
        }
    }

    private val screenShareManagerEventListener = object : ScreenShareManagerEventListener {
        override fun onScreenShare(sharing: Boolean, streamUuid: String) {
            container?.screenShare()?.updateScreenShareState(sharing, streamUuid)
        }

        override fun onScreenShareTips(tip: String) {
            container?.screenShare()?.showScreenShareTips(tip)
        }
    }

    private val handsUpManagerEventListener = object : HandsUpManagerEventListener {
        override fun onHandsUpEnable(enable: Boolean) {
            container?.handsUp()?.setHandsUpEnable(enable)
        }

        override fun onHandsUpState(state: AgoraUIHandsUpState, coHost: Boolean) {
            container?.handsUp()?.updateHandsUpState(state, coHost)
        }

        override fun onHandsUpError(error: AgoraUIError?) {
            container?.handsUp()?.updateHandsUpStateResult(error)
        }

        override fun onHandsUpTips(msg: String) {
            container?.handsUp()?.showHandsUpTips(msg)
        }
    }

    private val userListManagerEventListener = object : UserListManagerEventListener {
        override fun onUpdateUserList(list: MutableList<AgoraUIUserDetailInfo>) {
            container?.userList()?.updateUserList(list)
        }

        override fun onUpdateCoHostList(list: MutableList<AgoraUIUserDetailInfo>) {
            container?.userList()?.updateCoHostList(list)
        }

        override fun showUserReward(userInfo: AgoraUIUserInfo) {
            container?.userList()?.showUserReward(userInfo)
        }

        override fun onAudioVolumeIndicationUpdated(value: Int, streamUuid: String) {
            container?.userList()?.updateAudioVolumeIndication(value, streamUuid)
        }

        override fun onGranted(userId: String): Boolean {
            return whiteBoardManager?.isGranted(userId) ?: false
        }

        override fun onKickOut() {
            forceLeave(false)
            container?.userList()?.kickedOut()
        }

        override fun onMediaMsgUpdate(msg: String) {
            container?.userList()?.showUserTips(msg)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = FrameLayout(this)
        setContentView(layout)

        layout.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (layout.width > 0 && layout.height > 0) {
                    layout.viewTreeObserver.removeOnGlobalLayoutListener(this)

                    container = AgoraUIContainer.create(layout,
                            0, 0, layout.width, layout.height,
                            AgoraContainerType.SmallClass)

                    container?.setDeviceListener(settingListener)
                    chatManager?.let {
                        container?.setChatListener(it)
                    }

                    container?.whiteBoard()?.getContainer().let {
                        whiteBoardContainer = it
                        whiteBoardManager = WhiteBoardManager(this@SmallClassActivity, launchConfig!!, it!!)
                    }

                    whiteBoardManager?.let {
                        launchConfig?.let { config ->
                            var ware: AgoraEduCourseware? = null
                            if (AgoraEduSDK.COURSEWARES.size > 0) {
                                ware = AgoraEduSDK.COURSEWARES[0]
                            }
                            it.initData(config.roomUuid, config.whiteBoardAppId, ware)
                        }

                        it.whiteBoardManagerEventListener = whiteBoardManagerEventListener
                        container?.setWhiteboardListener(it)
                    }
                }
            }
        })
    }

    override fun initData() {
        super.initData()
        joinRoomAsStudent(launchConfig!!.userName, launchConfig!!.userUuid, autoSubscribe = true,
                autoPublish = false, needUserListener = true, callback = object : EduCallback<EduStudent?> {
            override fun onSuccess(res: EduStudent?) {
                whiteBoardManager!!.initBoardWithRoomToken(preCheckData!!.board.boardId,
                        preCheckData!!.board.boardToken, launchConfig!!.userUuid)
                initVideoManager()
                initRoomStatusManager()
                chatManager?.initChat()
                teacherVideoManager?.notifyUserDetailInfo(EduUserRole.STUDENT)
                teacherVideoManager?.notifyUserDetailInfo(EduUserRole.TEACHER)
                initHandsUpManager()
                initUserListManager()
                setRoomJoinSuccess()
                checkProcessSuccess()
            }

            override fun onFailure(error: EduError) {
                joinFailed(error.type, error.msg)
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        teacherVideoManager?.dispose()
        screenShareManager?.dispose()
        userListManager?.dispose()
        handsUpManager?.dispose()
        roomStatusManager?.dispose()
    }

    private fun initVideoManager() {
        getLocalUser(object : EduCallback<EduUser?> {
            override fun onSuccess(user: EduUser?) {
                user?.let {
                    teacherVideoManager = TeacherVideoManager(applicationContext, launchConfig!!, eduRoom, it)
                    teacherVideoManager?.let { manager ->
                        manager.managerEventListener = teacherVideoManagerEventListener
                        container?.setVideoGroupListener(manager.videoGroupListener)
                        manager.screenShareStarted = object : () -> Boolean {
                            override fun invoke(): Boolean {
                                return screenShareManager?.isScreenSharing() ?: false
                            }
                        }
                    }

                    screenShareManager = ScreenShareManager(this@SmallClassActivity, launchConfig!!, eduRoom!!, it)
                    screenShareManager?.let { manager ->
                        manager.eventListener = screenShareManagerEventListener
                        container?.setScreenShareListener(manager.screenShareListener)
                        manager.screenShareStateChangedListener = object : (Boolean) -> Unit {
                            override fun invoke(p1: Boolean) {
                                teacherVideoManager?.container?.let { container ->
                                    teacherVideoManager?.videoGroupListener?.onRendererContainer(
                                            container, teacherVideoManager!!.teacherStreamUuid)
                                }
                            }
                        }
                    }
                }
            }

            override fun onFailure(error: EduError) {

            }
        })
    }

    private fun initRoomStatusManager() {
        if (launchConfig != null && preCheckData != null) {
            roomStatusManager = RoomStatusManager(this, launchConfig!!, preCheckData!!, eduRoom)
            roomStatusManager?.let {
                roomStatusManagerEventListener.onClassName(launchConfig!!.roomName)
                it.eventListener = roomStatusManagerEventListener
                container?.setRoomListener(it)
                it.initClassState()
            }
        }
    }

    private fun initHandsUpManager() {
        getLocalUser(object : EduCallback<EduUser?> {
            override fun onSuccess(user: EduUser?) {
                user?.let { user ->
                    launchConfig?.let {
                        handsUpManager = HandsUpManager(applicationContext, it, eduRoom, user)
                        container?.setHandsUpListener(handsUpManager!!)
                        handsUpManager!!.eventListener = handsUpManagerEventListener
                        handsUpManager!!.initHandsUpData()
                    }
                }
            }

            override fun onFailure(error: EduError) {

            }
        })
    }

    private fun initUserListManager() {
        getLocalUser(object : EduCallback<EduUser?> {
            override fun onSuccess(user: EduUser?) {
                user?.let { user1 ->
                    userListManager = UserListManager(applicationContext, launchConfig!!, eduRoom!!, user1)
                    userListManager?.let {
                        it.eventListener = userListManagerEventListener
                        container?.setUserListListener(it)
                        it.notifyUserList()
                    }
                }
            }

            override fun onFailure(error: EduError) {

            }
        })
    }

    override fun onRemoteUsersInitialized(users: List<EduUserInfo>, classRoom: EduRoom) {
        super.onRemoteUsersInitialized(users, classRoom)
        whiteBoardManager!!.initBoardWithRoomToken(preCheckData!!.board.boardId,
                preCheckData!!.board.boardToken, launchConfig!!.userUuid)
    }

    override fun onRemoteUsersJoined(users: List<EduUserInfo>, classRoom: EduRoom) {
        super.onRemoteUsersJoined(users, classRoom)
        userListManager?.notifyUserList()
    }

    override fun onRemoteUserUpdated(userEvent: EduUserEvent, type: EduUserStateChangeType, classRoom: EduRoom) {
        super.onRemoteUserUpdated(userEvent, type, classRoom)
    }

    override fun onRemoteUserLeft(userEvent: EduUserEvent, classRoom: EduRoom) {
        super.onRemoteUserLeft(userEvent, classRoom)
        userListManager?.notifyUserList()
    }

    override fun onRoomChatMessageReceived(chatMsg: EduChatMsg, classRoom: EduRoom) {
        super.onRoomChatMessageReceived(chatMsg, classRoom)
        val item = AgoraUIChatItem(chatMsg.fromUser.userName ?: "", chatMsg.fromUser.userUuid ?: "",
                chatMsg.message, chatMsg.messageId, AgoraUIChatItemType.Text, AgoraUIChatSource.Remote,
                AgoraUIChatState.Success, chatMsg.timestamp)
        container?.chat()?.addMessage(item)
    }

    override fun onRemoteStreamsInitialized(streams: List<EduStreamInfo>, classRoom: EduRoom) {
        super.onRemoteStreamsInitialized(streams, classRoom)
        screenShareManager?.restoreScreenShare()
    }

    override fun onRemoteStreamsAdded(streamEvents: MutableList<EduStreamEvent>, classRoom: EduRoom) {
        super.onRemoteStreamsAdded(streamEvents, classRoom)
        teacherVideoManager?.notifyUserDetailInfo(EduUserRole.TEACHER)
        screenShareManager?.upsertScreenShare(streamEvents)
        if (streamEvents.find { it.modifiedStream.publisher.role == EduUserRole.TEACHER } == null) {
            userListManager?.notifyUserList()
        }
    }

    override fun onRemoteStreamUpdated(streamEvents: MutableList<EduStreamEvent>, classRoom: EduRoom) {
        super.onRemoteStreamUpdated(streamEvents, classRoom)
        teacherVideoManager?.notifyUserDetailInfo(EduUserRole.TEACHER)
        screenShareManager?.upsertScreenShare(streamEvents)
        if (streamEvents.find { it.modifiedStream.publisher.role == EduUserRole.TEACHER } == null) {
            userListManager?.notifyUserList()
        }
    }

    override fun onRemoteStreamsRemoved(streamEvents: MutableList<EduStreamEvent>, classRoom: EduRoom) {
        super.onRemoteStreamsRemoved(streamEvents, classRoom)
        teacherVideoManager?.notifyUserDetailInfo(EduUserRole.TEACHER)
        screenShareManager?.removeScreenShare(streamEvents)
        if (streamEvents.find { it.modifiedStream.publisher.role == EduUserRole.TEACHER } == null) {
            userListManager?.notifyUserList()
        }
    }

    override fun onLocalStreamAdded(streamEvent: EduStreamEvent) {
        super.onLocalStreamAdded(streamEvent)
        userListManager?.addLocalStream(streamEvent)
    }

    override fun onLocalStreamUpdated(streamEvent: EduStreamEvent) {
        super.onLocalStreamUpdated(streamEvent)
        userListManager?.updateLocalStream(streamEvent)
        userListManager?.notifyUserList()
    }

    override fun onLocalStreamRemoved(streamEvent: EduStreamEvent) {
        super.onLocalStreamRemoved(streamEvent)
        userListManager?.removeLocalStream(streamEvent)
        userListManager?.notifyUserList()
    }

    override fun onLocalUserLeft(userEvent: EduUserEvent, leftType: EduUserLeftType) {
        super.onLocalUserLeft(userEvent, leftType)
        userListManager?.kickOut()
    }

    override fun onRoomStatusChanged(type: EduRoomChangeType, operatorUser: EduUserInfo?, classRoom: EduRoom) {
        super.onRoomStatusChanged(type, operatorUser, classRoom)
        roomStatusManager?.updateClassState(type)
    }

    override fun onRoomPropertiesChanged(classRoom: EduRoom, cause: MutableMap<String, Any>?) {
        super.onRoomPropertiesChanged(classRoom, cause)
        handsUpManager?.notifyHandsUpEnable(cause)
        handsUpManager?.notifyHandsUpState(cause)
        userListManager?.notifyListByPropertiesChanged(cause)
    }

    override fun onRemoteVideoStateChanged(rtcChannel: RtcChannel?, uid: Int, state: Int, reason: Int, elapsed: Int) {
        super.onRemoteVideoStateChanged(rtcChannel, uid, state, reason, elapsed)
        teacherVideoManager?.notifyUserDetailInfo(EduUserRole.TEACHER)
    }

    override fun onLocalVideoStateChanged(localVideoState: Int, error: Int) {
        super.onLocalVideoStateChanged(localVideoState, error)
        teacherVideoManager?.updateLocalCameraState(localVideoState)
        userListManager?.updateLocalCameraState(localVideoState)
    }

    override fun onAudioVolumeIndicationOfLocalSpeaker(speakers: Array<out IRtcEngineEventHandler.AudioVolumeInfo>?, totalVolume: Int) {
        super.onAudioVolumeIndicationOfLocalSpeaker(speakers, totalVolume)
        teacherVideoManager?.updateAudioVolume(speakers)
        userListManager?.updateAudioVolumeIndication(speakers)

    }

    override fun onAudioVolumeIndicationOfRemoteSpeaker(speakers: Array<out IRtcEngineEventHandler.AudioVolumeInfo>?, totalVolume: Int) {
        super.onAudioVolumeIndicationOfRemoteSpeaker(speakers, totalVolume)
        teacherVideoManager?.updateAudioVolume(speakers)
        userListManager?.updateAudioVolumeIndication(speakers)
    }

    override fun onRemoteUserPropertiesChanged(classRoom: EduRoom, userInfo: EduUserInfo, cause: MutableMap<String, Any>?) {
        super.onRemoteUserPropertiesChanged(classRoom, userInfo, cause)
        teacherVideoManager?.updateRemoteCameraState(userInfo, cause)
        teacherVideoManager?.notifyUserDetailInfo(EduUserRole.TEACHER)
        userListManager?.updateRemoteCameraState(userInfo, cause)
    }
}