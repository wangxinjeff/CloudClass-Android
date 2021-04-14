package io.agora.edu.classroom

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import com.google.gson.Gson
import com.herewhite.sdk.domain.MemberState
import com.herewhite.sdk.domain.SDKError
import com.herewhite.sdk.domain.SceneState
import io.agora.edu.launch.AgoraEduCourseware
import io.agora.edu.launch.AgoraEduSDK
import io.agora.education.api.EduCallback
import io.agora.education.api.base.EduError
import io.agora.education.api.message.EduChatMsg
import io.agora.education.api.room.EduRoom
import io.agora.education.api.room.data.EduRoomChangeType
import io.agora.education.api.statistics.ConnectionState
import io.agora.education.api.statistics.NetworkQuality
import io.agora.education.api.stream.data.EduStreamEvent
import io.agora.education.api.stream.data.EduStreamInfo
import io.agora.education.api.user.EduStudent
import io.agora.education.api.user.EduUser
import io.agora.education.api.user.data.EduBaseUserInfo
import io.agora.education.api.user.data.EduUserInfo
import io.agora.education.api.user.data.EduUserRole
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcChannel
import io.agora.uikit.AgoraUIError
import io.agora.uikit.impl.chat.AgoraUIChatItem
import io.agora.uikit.impl.chat.AgoraUIChatItemType
import io.agora.uikit.impl.chat.AgoraUIChatSource
import io.agora.uikit.impl.chat.AgoraUIChatState
import io.agora.uikit.impl.container.AgoraContainerType
import io.agora.uikit.impl.room.AgoraUIConnectionState
import io.agora.uikit.impl.room.AgoraUINetworkState
import io.agora.uikit.impl.room.ClassState
import io.agora.uikit.impl.users.AgoraUIUserDetailInfo
import io.agora.uikit.interfaces.listeners.IAgoraUIDeviceListener
import io.agora.uikit.interfaces.protocols.AgoraUIContainer
import io.agora.uikit.interfaces.protocols.AgoraUIDrawingConfig

class OneToOneClassActivity : BaseClassActivity() {
    private val tag = "OneToOneClassActivity"

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
        }
    }

    private val oneToOneVideoManagerEventListener = object : OneToOneVideoManagerEventListener {
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = FrameLayout(this)
        setContentView(layout)

        layout.viewTreeObserver.addOnGlobalLayoutListener(
                object : ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        if (layout.width > 0 && layout.height > 0) {
                            layout.viewTreeObserver.removeOnGlobalLayoutListener(this)

                            container = AgoraUIContainer.create(layout,
                                    0, 0, layout.width, layout.height,
                                    AgoraContainerType.OneToOne)

                            container?.setDeviceListener(settingListener)
                            chatManager?.let {
                                container?.setChatListener(it)
                            }

                            container?.whiteBoard()?.getContainer().let {
                                whiteBoardContainer = it
                                whiteBoardManager = WhiteBoardManager(this@OneToOneClassActivity, launchConfig!!, it!!)
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
                autoPublish = true, needUserListener = true, callback = object : EduCallback<EduStudent?> {
            override fun onSuccess(res: EduStudent?) {
                whiteBoardManager!!.initBoardWithRoomToken(preCheckData!!.board.boardId,
                        preCheckData!!.board.boardToken, launchConfig!!.userUuid)
                initVideoManager()
                initRoomStatusManager()
                chatManager?.initChat()
                oneToOneVideoManager?.notifyUserDetailInfo(EduUserRole.STUDENT)
                oneToOneVideoManager?.notifyUserDetailInfo(EduUserRole.TEACHER)
                setRoomJoinSuccess()
                checkProcessSuccess()
            }

            override fun onFailure(error: EduError) {
                joinFailed(error.type, error.msg)
            }
        })
    }

    private fun initVideoManager() {
        getLocalUser(object : EduCallback<EduUser?> {
            override fun onSuccess(user: EduUser?) {
                user?.let {
                    oneToOneVideoManager = OneToOneVideoManager(applicationContext, launchConfig!!, eduRoom!!, it)
                    oneToOneVideoManager?.let { manager ->
                        manager.managerEventListener = oneToOneVideoManagerEventListener
                        container?.setVideoGroupListener(manager.videoGroupListener)
                    }
                    screenShareManager = ScreenShareManager(this@OneToOneClassActivity, launchConfig!!, eduRoom!!, it)
                    screenShareManager?.let { manager ->
                        manager.eventListener = screenShareManagerEventListener
                        container?.setScreenShareListener(manager.screenShareListener)
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

    override fun onRemoteUsersInitialized(users: List<EduUserInfo>, classRoom: EduRoom) {
        super.onRemoteUsersInitialized(users, classRoom)
        whiteBoardManager!!.initBoardWithRoomToken(preCheckData!!.board.boardId,
                preCheckData!!.board.boardToken, launchConfig!!.userUuid)
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
        oneToOneVideoManager?.notifyUserDetailInfo(EduUserRole.TEACHER)
        screenShareManager?.upsertScreenShare(streamEvents)
    }

    override fun onRemoteStreamUpdated(streamEvents: MutableList<EduStreamEvent>, classRoom: EduRoom) {
        super.onRemoteStreamUpdated(streamEvents, classRoom)
        oneToOneVideoManager?.notifyUserDetailInfo(EduUserRole.TEACHER)
        screenShareManager?.upsertScreenShare(streamEvents)
    }

    override fun onRemoteStreamsRemoved(streamEvents: MutableList<EduStreamEvent>, classRoom: EduRoom) {
        super.onRemoteStreamsRemoved(streamEvents, classRoom)
        oneToOneVideoManager?.notifyUserDetailInfo(EduUserRole.TEACHER)
        screenShareManager?.removeScreenShare(streamEvents)
    }

    override fun onLocalStreamAdded(streamEvent: EduStreamEvent) {
        super.onLocalStreamAdded(streamEvent)
        oneToOneVideoManager?.addLocalStream(streamEvent)
        oneToOneVideoManager?.notifyUserDetailInfo(EduUserRole.STUDENT)
    }

    override fun onLocalStreamUpdated(streamEvent: EduStreamEvent) {
        super.onLocalStreamUpdated(streamEvent)
        oneToOneVideoManager?.updateLocalStream(streamEvent)
        oneToOneVideoManager?.notifyUserDetailInfo(EduUserRole.STUDENT)
    }

    override fun onLocalStreamRemoved(streamEvent: EduStreamEvent) {
        super.onLocalStreamRemoved(streamEvent)
        oneToOneVideoManager?.removeLocalStream(streamEvent)
    }

    override fun onRoomStatusChanged(type: EduRoomChangeType, operatorUser: EduUserInfo?, classRoom: EduRoom) {
        super.onRoomStatusChanged(type, operatorUser, classRoom)
        roomStatusManager?.updateClassState(type)
    }

    override fun onRemoteVideoStateChanged(rtcChannel: RtcChannel?, uid: Int, state: Int, reason: Int, elapsed: Int) {
        super.onRemoteVideoStateChanged(rtcChannel, uid, state, reason, elapsed)
        oneToOneVideoManager?.notifyUserDetailInfo(EduUserRole.TEACHER)
    }

    override fun onLocalVideoStateChanged(localVideoState: Int, error: Int) {
        super.onLocalVideoStateChanged(localVideoState, error)
        oneToOneVideoManager?.updateLocalCameraState(localVideoState)
    }

    override fun onAudioVolumeIndicationOfLocalSpeaker(speakers: Array<out IRtcEngineEventHandler.AudioVolumeInfo>?, totalVolume: Int) {
        super.onAudioVolumeIndicationOfLocalSpeaker(speakers, totalVolume)
        oneToOneVideoManager?.updateAudioVolume(speakers)
    }

    override fun onAudioVolumeIndicationOfRemoteSpeaker(speakers: Array<out IRtcEngineEventHandler.AudioVolumeInfo>?, totalVolume: Int) {
        super.onAudioVolumeIndicationOfRemoteSpeaker(speakers, totalVolume)
        oneToOneVideoManager?.updateAudioVolume(speakers)
    }

    override fun onRemoteUserPropertiesChanged(classRoom: EduRoom, userInfo: EduUserInfo, cause: MutableMap<String, Any>?) {
        super.onRemoteUserPropertiesChanged(classRoom, userInfo, cause)
        oneToOneVideoManager?.updateRemoteCameraState(userInfo, cause)
        oneToOneVideoManager?.notifyUserDetailInfo(EduUserRole.TEACHER)
    }
}