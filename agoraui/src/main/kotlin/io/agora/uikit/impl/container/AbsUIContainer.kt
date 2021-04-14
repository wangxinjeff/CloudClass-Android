package io.agora.uikit.impl.container

import android.content.Context
import android.content.res.Configuration
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import io.agora.uikit.AgoraUIError
import io.agora.uikit.component.toast.AgoraUIToastManager
import io.agora.uikit.impl.chat.AgoraUIChatItem
import io.agora.uikit.impl.chat.AgoraUIChatWindow
import io.agora.uikit.impl.handsup.AgoraUIHandsUp
import io.agora.uikit.impl.handsup.AgoraUIHandsUpState
import io.agora.uikit.impl.room.AgoraUIConnectionState
import io.agora.uikit.impl.room.AgoraUIRoomStatus
import io.agora.uikit.impl.room.ClassState
import io.agora.uikit.impl.room.AgoraUINetworkState
import io.agora.uikit.impl.screenshare.AgoraUIScreenShare
import io.agora.uikit.impl.setting.DeviceConfig
import io.agora.uikit.impl.tool.AgoraUIToolBar
import io.agora.uikit.impl.users.AgoraUIReward
import io.agora.uikit.impl.users.AgoraUIRoster
import io.agora.uikit.impl.users.AgoraUIUserDetailInfo
import io.agora.uikit.impl.users.AgoraUserListVideoLayout
import io.agora.uikit.impl.video.AgoraUIVideoGroup
import io.agora.uikit.impl.whiteboard.AgoraUIWhiteBoard
import io.agora.uikit.impl.whiteboard.paging.AgoraUIPagingControl
import io.agora.uikit.interfaces.listeners.*
import io.agora.uikit.interfaces.protocols.*

abstract class AbsUIContainer : IAgoraUIContainer {

    private var room: IAgoraUIRoom = object : IAgoraUIRoom {
        override fun setClassroomName(name: String) {
            roomStatus?.setClassroomName(name)
        }

        override fun setClassState(state: ClassState) {
            roomStatus?.setClassState(state)
        }

        override fun setClassTime(time: String) {
            roomStatus?.setClassTime(time)
        }

        override fun setNetworkState(stateAgoraUI: AgoraUINetworkState) {
            roomStatus?.setNetworkState(stateAgoraUI)
        }

        override fun setConnectionState(state: AgoraUIConnectionState) {
            Log.i("AbsUIContainer", "setConnectionState->$state")
        }

        override fun showClassTips(msg: String) {
            AgoraUIToastManager.showShort(msg)
        }

        override fun showErrorInfo(error: AgoraUIError) {
            showError(error)
        }
    }

    private val whiteboard = object : IAgoraUIWhiteboard {
        override fun setDrawingConfig(config: AgoraUIDrawingConfig) {
            toolbar?.setConfig(config)
        }

        override fun setDrawingEnabled(enabled: Boolean) {
            toolbar?.setWhiteboardFunctionEnabled(enabled)
        }

        override fun getContainer(): ViewGroup? {
            return whiteboardWindow?.getBoardContainer()
        }

        override fun setPageNo(no: Int, count: Int) {
            pageControlWindow?.setPageNo(no, count)
        }

        override fun setPagingEnabled(enabled: Boolean) {
            pageControlWindow?.setPagingEnable(enabled)
        }

        override fun setZoomEnable(zoomOutEnable: Boolean?, zoomInEnable: Boolean?) {
            pageControlWindow?.setZoomEnable(zoomOutEnable, zoomInEnable)
        }

        override fun setFullScreenEnable(enabled: Boolean) {
            pageControlWindow?.setResizeScreenEnable(enabled)
        }

        override fun setIsFullScreen(fullScreen: Boolean) {
            pageControlWindow?.setFullScreen(fullScreen)
            setFullScreen(fullScreen)
        }

        override fun setInteractionEnabled(enabled: Boolean) {
            pageControlWindow?.setEnabled(enabled)
        }

        override fun setLoadingVisible(visible: Boolean) {
            whiteboardWindow?.setLoadingVisible(visible)
        }

        override fun setDownloadProgress(url: String, progress: Float) {
            whiteboardWindow?.setDownloadProgress(url, progress)
        }

        override fun setDownloadTimeout(url: String) {
            whiteboardWindow?.setDownloadTimeOut(url)
        }

        override fun setDownloadCompleted(url: String) {
            whiteboardWindow?.setDownloadComplete(url)
        }

        override fun downloadError(url: String) {
            whiteboardWindow?.downloadError(url)
        }

        override fun cancelCurDownload() {
            whiteboardWindow?.cancelCurDownload()
        }

        override fun showPermissionTips(granted: Boolean) {
            AgoraUIToastManager.whiteBoardPermissionTips(granted)
        }
    }

    private val chat = object : IAgoraUIChat {
        override fun addMessage(item: AgoraUIChatItem) {
            chatWindow?.addMessage(item)
        }

        override fun sendLocalMessageResult(message: String, uid: String,
                                            messageId: Int, timestamp: Long, success: Boolean) {
            chatWindow?.sendLocalMessageResult(message, uid, messageId, timestamp, success)
        }

        override fun fetchMessageHistoryResult(success: Boolean, list: MutableList<AgoraUIChatItem>?) {
            chatWindow?.fetchMessageHistoryResult(success, list)
        }

        override fun allowChat(allowed: Boolean) {
            chatWindow?.allowChat(allowed)
        }

        override fun showChatTips(msg: String) {
            AgoraUIToastManager.showShort(msg)
        }
    }

    private val videoGroup = object : IAgoraUIVideo {
        override fun upsertUserDetailInfo(info: AgoraUIUserDetailInfo) {
            videoGroupWindow?.upsertUserDetailInfo(info)
        }

        override fun updateAudioVolumeIndication(value: Int, streamUuid: String) {
            videoGroupWindow?.updateAudioVolumeIndication(value, streamUuid)
        }

        override fun updateMediaMessage(msg: String) {
            videoGroupWindow?.updateMediaMessage(msg)
        }
    }

    private val screenShare = object : IAgoraUIScreenShare {
        override fun updateScreenShareState(sharing: Boolean, streamUuid: String) {
            screenShareWindow?.updateScreenShareState(sharing, streamUuid)
        }

        override fun showScreenShareTips(tips: String) {
            AgoraUIToastManager.showShort(tips)
        }
    }

    private val handsUp = object : IAgoraUIHandsUp {
        override fun setHandsUpEnable(enable: Boolean) {
            handsUpWindow?.setHandsUpEnable(enable)
        }

        override fun updateHandsUpState(state: AgoraUIHandsUpState, coHost: Boolean) {
            handsUpWindow?.updateHandsUpState(state, coHost)
        }

        override fun updateHandsUpStateResult(error: AgoraUIError?) {
            handsUpWindow?.updateHandsUpStateResult(error)
        }

        override fun showHandsUpTips(tips: String) {
            AgoraUIToastManager.showShort(tips)
        }
    }

    protected var roomStatus: AgoraUIRoomStatus? = null
    protected var toolbar: AgoraUIToolBar? = null
    protected var chatWindow: AgoraUIChatWindow? = null
    protected var whiteboardWindow: AgoraUIWhiteBoard? = null
    protected var pageControlWindow: AgoraUIPagingControl? = null
    protected var videoGroupWindow: AgoraUIVideoGroup? = null
    protected var screenShareWindow: AgoraUIScreenShare? = null
    protected var handsUpWindow: AgoraUIHandsUp? = null
    protected var studentVideoGroup: AgoraUserListVideoLayout? = null
    protected var roster: AgoraUIRoster? = null
    protected var rewardWindow: AgoraUIReward? = null

    private var roomListener: IAgoraUIRoomListener? = null
    private var whiteboardListener: IAgoraUIWhiteboardListener? = null
    private var chatListener: IAgoraUIChatListener? = null
    private var deviceListener: IAgoraUIDeviceListener? = null
    private var videoGroupListener: IAgoraUIVideoGroupListener? = null
    private var screenShareListener: IAgoraUIScreenShareListener? = null
    private var handsUpListener: IAgoraUIHandsUpListener? = null
    private var userListListener: IAgoraUIUserListListener? = null

    private var deviceConfig = DeviceConfig()

    override fun room(): IAgoraUIRoom? {
        return room
    }

    override fun chat(): IAgoraUIChat? {
        return chat
    }

    override fun whiteBoard(): IAgoraUIWhiteboard? {
        return whiteboard
    }

    override fun videoGroup(): IAgoraUIVideo? {
        return videoGroup
    }

    override fun screenShare(): IAgoraUIScreenShare? {
        return screenShare
    }

    override fun handsUp(): IAgoraUIHandsUp {
        return handsUp
    }

    override fun userList(): IAgoraUIUserList? {
        return null
    }

    override fun roster(anchor: View): AgoraUIRoster? {
        if (roster == null) {
            roster = AgoraUIRoster(anchor)
        } else {
            roster!!.adjustPosition(anchor)
        }
        return roster
    }

    override fun deviceConfig(): DeviceConfig {
        return deviceConfig
    }

    override fun setDeviceListener(listener: IAgoraUIDeviceListener) {
        this.deviceListener = listener
        roomStatus?.setDeviceListener(listener)
    }

    override fun setChatListener(listener: IAgoraUIChatListener) {
        this.chatListener = listener
        chatWindow?.setChatListener(listener)
    }

    override fun setWhiteboardListener(listener: IAgoraUIWhiteboardListener) {
        this.whiteboardListener = listener
        whiteboardWindow?.whiteBoardListener = listener
        toolbar?.setToolListener(listener)
        pageControlWindow?.pagingControlListener = listener
    }

    override fun setVideoGroupListener(listener: IAgoraUIVideoGroupListener) {
        this.videoGroupListener = listener
        videoGroupWindow?.videoGroupListener = listener
    }

    override fun setScreenShareListener(listener: IAgoraUIScreenShareListener) {
        this.screenShareListener = listener
        screenShareWindow?.screenShareListener = listener
    }

    override fun setHandsUpListener(listener: IAgoraUIHandsUpListener) {
        this.handsUpListener = listener
        handsUpWindow?.handsUpListener = listener
    }

    override fun setUserListListener(listener: IAgoraUIUserListListener) {
        this.userListListener = listener
        studentVideoGroup?.listener = listener
        roster?.userListListener = listener
    }

    override fun setRoomListener(listener: IAgoraUIRoomListener) {
        this.roomListener = listener
        roomStatus?.setRoomListener(listener)
    }

    override fun showLeave() {
        roomStatus?.showLeaveDialog()
    }

    override fun kickOut() {
        roomStatus?.kickOut()
    }

    override fun showError(error: AgoraUIError) {
        AgoraUIToastManager.showLong(error.msg)
    }

    override fun showTips(msg: String) {
        AgoraUIToastManager.showLong(msg)
    }

    internal fun hideSoftInput(context: Context, view: View) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
    }

    override fun init(layout: ViewGroup, left: Int, top: Int, width: Int, height: Int) {
        val screenLayout = layout.context.resources.configuration.screenLayout
        AgoraUIConfig.isLargeScreen =
                screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK >=
                        Configuration.SCREENLAYOUT_SIZE_LARGE
        rewardWindow = AgoraUIReward(layout.context, layout, 0, 0, width, height)
    }

    protected abstract fun setFullScreen(fullScreen: Boolean)

    protected abstract fun calculateVideoSize()
}

enum class AgoraContainerType {
    OneToOne, SmallClass, LargeClass
}

object AgoraUIConfig {
    const val videoWidthMaxRatio = 0.312f
    const val videoRatio = 9 / 16f
    const val clickInterval = 500L
    var isLargeScreen: Boolean = false
    const val videoPlaceHolderImgSizePercent = 0.5f
    const val videoOptionIconSizePercent = 0.14f
    const val videoOptionIconSizeMax = 54
    const val videoOptionIconSizeMaxWithLargeScreen = 36
    const val chatHeightLargeScreenRatio = 0.7f

    object SmallClass {
        const val studentVideoHeightRationSmallScreen = 0.24f
        var teacherVideoWidth = 600
        var teacherVideoHeight = 336
        var studentVideoHeightLargeScreen = 336
        var studentVideoHeightSmallScreen = 261
    }

    object OneToOneClass {
        var teacherVideoWidth = 600
    }
}