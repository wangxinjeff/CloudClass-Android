package io.agora.edu.classroom

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.text.TextUtils
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.google.gson.Gson
import com.herewhite.sdk.*
import com.herewhite.sdk.domain.*
import io.agora.base.ToastManager
import io.agora.edu.R
import io.agora.edu.classroom.widget.whiteboard.BoardPreloadManager
import io.agora.edu.classroom.widget.whiteboard.BoardPreloadEventListener
import io.agora.edu.common.bean.board.BoardExt
import io.agora.edu.common.bean.board.BoardState
import io.agora.edu.launch.AgoraEduCourseware
import io.agora.edu.launch.AgoraEduLaunchConfig
import io.agora.edu.launch.AgoraEduSDK
import io.agora.edu.util.ColorUtil
import io.agora.education.impl.Constants
import io.agora.report.ReportManager
import io.agora.uikit.impl.tool.AgoraUIApplianceType
import io.agora.uikit.interfaces.protocols.AgoraUIDrawingConfig
import io.agora.uikit.interfaces.listeners.IAgoraUIWhiteboardListener
import io.agora.whiteboard.netless.listener.BoardEventListener
import io.agora.whiteboard.netless.listener.GlobalStateChangeListener
import io.agora.whiteboard.netless.manager.BoardProxy
import org.json.JSONObject
import java.io.File
import java.lang.Exception

class WhiteBoardManager(
        val context: Context,
        val launchConfig: AgoraEduLaunchConfig,
        private val whiteBoardViewContainer: ViewGroup) : CommonCallbacks, BoardEventListener,
        GlobalStateChangeListener, BoardPreloadEventListener,
        IAgoraUIWhiteboardListener {
    private val tag = "WhiteBoardManager"

    private lateinit var whiteBoardAppId: String
    private lateinit var whiteSdk: WhiteSdk
    private var whiteBoardView: WhiteboardView = WhiteboardView(context)
    private val boardProxy = BoardProxy()
    private var curLocalUuid: String? = null
    private var curLocalToken: String? = null
    private var localUserUuid: String? = null
    private val miniScale = 0.1
    private val maxScale = 10.0
    private val scaleStepper = 0.2
    private var curBoardState: BoardState? = null
    private var curGranted: Boolean = false
    private var followTips = false
    private var curFollowState = false
    var whiteBoardManagerEventListener: WhiteBoardManagerEventListener? = null
    private var curSceneState: SceneState? = null
    private var boardPreloadManager: BoardPreloadManager? = null
    private var courseware: AgoraEduCourseware? = null
    private val defaultCoursewareName = "init"
    private var scenePpts: Array<Scene?>? = null
    private var loadPreviewPpt: Boolean = true
    private var lastSceneDir: String? = null
    private var inputTips = false
    private var transform = false
    private val webViewClient = object : WebViewClient() {
        override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
            val host = request?.url?.host
            host?.let {
                boardPreloadManager?.let {
                    val response = boardPreloadManager!!.checkCache(request)
                    response?.let {
                        Log.e(tag, "blocked link:${request?.url.toString()}")
                        Log.e(tag, "response is not null")
                        return response
                    }
                }
            }
            return super.shouldInterceptRequest(view, request)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private val onTouchListener = View.OnTouchListener { v, event ->
        if (event!!.action == MotionEvent.ACTION_DOWN) {
            whiteBoardView.requestFocus()
            if (boardProxy.isDisableCameraTransform && !boardProxy.isDisableDeviceInputs) {
                ToastManager.showShort(R.string.follow_tips)
                return@OnTouchListener true
            }
        }
        return@OnTouchListener false
    }

    init {
        whiteBoardView.settings.allowFileAccessFromFileURLs = true
        whiteBoardView.webViewClient = webViewClient
        whiteBoardView.setOnTouchListener(onTouchListener)
        whiteBoardView.addOnLayoutChangeListener { v: View?, left: Int, top: Int, right: Int,
                                                   bottom: Int, oldLeft: Int, oldTop: Int,
                                                   oldRight: Int, oldBottom: Int ->
            if (context is Activity && (context.isFinishing) ||
                    (context as Activity).isDestroyed) {
                return@addOnLayoutChangeListener
            }
            boardProxy.refreshViewSize()
        }
        whiteBoardManagerEventListener?.onDisableDeviceInput(boardProxy.isDisableDeviceInputs)
        val layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT)
        whiteBoardView.layoutParams = layoutParams
        whiteBoardViewContainer.addView(whiteBoardView)
        boardProxy.setListener(this)
    }

    fun initData(roomUuid: String, whiteBoardAppId: String, courseware: AgoraEduCourseware?) {
        Log.e(tag, "initWithAppId")
        this.whiteBoardAppId = whiteBoardAppId
        WhiteDisplayerState.setCustomGlobalStateClass(BoardState::class.java)
        val configuration = WhiteSdkConfiguration(whiteBoardAppId, true)
        configuration.isEnableIFramePlugin = true
        configuration.isUserCursor = true
        Log.e(tag, "newWhiteSdk---0")
        whiteSdk = WhiteSdk(whiteBoardView, context, configuration, this)
        Log.e(tag, "newWhiteSdk---1")
        boardProxy.setListener(this)
        boardPreloadManager = BoardPreloadManager(context, roomUuid)
        boardPreloadManager?.listener = this
        /**Data type conversion */
        courseware?.let { ware ->
            this.courseware = courseware
            ware.scenes?.let {
                scenePpts = arrayOfNulls(it.size)
                for (i in it.indices) {
                    var pptPage: PptPage? = null
                    val element = it[i]
                    val ppt = element.ppt
                    if (ppt != null) {
                        pptPage = PptPage(ppt.src, ppt.width, ppt.height)
                    }
                    val scene = Scene(element.name, pptPage)
                    scenePpts!![i] = scene
                }
            }
        }
    }

    fun initBoardWithRoomToken(uuid: String?, boardToken: String?, localUserUuid: String?) {
        if (TextUtils.isEmpty(uuid) || TextUtils.isEmpty(boardToken)) {
            return
        }
        curLocalUuid = uuid
        curLocalToken = boardToken
        this.localUserUuid = localUserUuid
        whiteBoardViewContainer.post {
            boardProxy.getRoomPhase(object : Promise<RoomPhase> {
                override fun then(phase: RoomPhase) {
                    Constants.AgoraLog.e(tag + ":then->" + phase.name)
                    if (phase != RoomPhase.connected) {
                        whiteBoardManagerEventListener?.onWhiteBoardLoadingStatus(true)
                        val params = RoomParams(uuid, boardToken)
                        params.cameraBound = CameraBound(miniScale, maxScale)
                        params.isDisableNewPencil = false
                        boardProxy.init(whiteSdk, params)
                        ReportManager.getAPaasReporter().reportWhiteBoardStart()
                    }
                }

                override fun catchEx(t: SDKError) {
                    Constants.AgoraLog.e(tag + ":catchEx->" + t.message)
                    ToastManager.showShort(t.message!!)
                }
            })
        }
    }

    fun disableDeviceInputs(disabled: Boolean) {
        val a = boardProxy.isDisableDeviceInputs
        if (disabled != a) {
            if (!inputTips) {
                inputTips = true
            } else {
//                ToastManager.showShort(if (disabled) R.string.revoke_board else R.string.authorize_board)
            }
        }
        whiteBoardManagerEventListener?.onDisableDeviceInput(disabled)
        boardProxy.disableDeviceInputs(disabled)
    }

    fun disableCameraTransform(disabled: Boolean) {
        val a = boardProxy.isDisableCameraTransform
        if (disabled != a) {
            if (disabled) {
                if (!transform) {
                    transform = true
                } else {
//                    ToastManager.showShort(R.string.follow_tips);
                }
                boardProxy.disableDeviceInputsTemporary(true)
            } else {
                boardProxy.disableDeviceInputsTemporary(boardProxy.isDisableDeviceInputs)
            }
        }
        whiteBoardManagerEventListener?.onDisableCameraTransform(disabled)
        boardProxy.disableCameraTransform(disabled)
    }

    fun setWritable(writable: Boolean) {
        boardProxy.setWritable(writable)
    }

    fun isGranted(userUuid: String): Boolean {
        return curBoardState?.isGranted(userUuid) ?: false
    }

    fun releaseBoard() {
        Constants.AgoraLog.e("$tag:releaseBoard")
        boardProxy.disconnect()
        whiteBoardView.removeAllViews()
        whiteBoardView.destroy()
    }

    private fun cancelCurPreloadBySwitchScene() {
        boardPreloadManager?.cancelPreload()
        whiteBoardManagerEventListener?.onCancelCurDownload()
    }

    private fun applianceConvert(type: AgoraUIApplianceType): String {
        return when (type) {
            AgoraUIApplianceType.Select -> {
                Appliance.SELECTOR
            }
            AgoraUIApplianceType.Pen -> {
                Appliance.PENCIL
            }
            AgoraUIApplianceType.Rect -> {
                Appliance.RECTANGLE
            }
            AgoraUIApplianceType.Circle -> {
                Appliance.ELLIPSE
            }
            AgoraUIApplianceType.Line -> {
                Appliance.STRAIGHT
            }
            AgoraUIApplianceType.Eraser -> {
                Appliance.ERASER
            }
            AgoraUIApplianceType.Text -> {
                Appliance.TEXT
            }
        }
    }

    private fun applianceConvert(appliance: String): AgoraUIApplianceType {
        return when (appliance) {
            Appliance.SELECTOR -> {
                AgoraUIApplianceType.Select
            }
            Appliance.PENCIL -> {
                AgoraUIApplianceType.Pen
            }
            Appliance.RECTANGLE -> {
                AgoraUIApplianceType.Rect
            }
            Appliance.ELLIPSE -> {
                AgoraUIApplianceType.Circle
            }
            Appliance.STRAIGHT -> {
                AgoraUIApplianceType.Line
            }
            Appliance.ERASER -> {
                AgoraUIApplianceType.Eraser
            }
            Appliance.TEXT -> {
                AgoraUIApplianceType.Text
            }
            else -> {
                AgoraUIApplianceType.Select
            }
        }
    }

    private fun initWhiteBoardAppliance() {
        boardProxy.appliance = Appliance.SELECTOR
    }

    /** CommonCallbacks */
    override fun throwError(args: Any?) {
        Log.e(tag, "throwError->${Gson().toJson(args)}")
    }

    override fun urlInterrupter(sourceUrl: String?): String? {
        return null
    }

    override fun onPPTMediaPlay() {
    }

    override fun onPPTMediaPause() {
    }

    override fun onMessage(`object`: JSONObject?) {
    }

    override fun sdkSetupFail(error: SDKError?) {
        Log.e(tag, "sdkSetupFail->${error?.jsStack}")
    }

    /** BoardEventListener */
    override fun onJoinSuccess(state: GlobalState?) {
        Constants.AgoraLog.e(tag + ":onJoinSuccess->" + Gson().toJson(state))
        onGlobalStateChanged(state)
        // set default config
        val config = AgoraUIDrawingConfig()
        config.activeAppliance = applianceConvert(Appliance.SELECTOR)
        config.color = ColorUtil.converRgbToArgb(boardProxy.strokeColor)
        config.fontSize = boardProxy.textSize.toInt()
        config.thick = boardProxy.strokeWidth.toInt()
        whiteBoardManagerEventListener?.onWhiteBoardJoinSuccess(config)
    }

    override fun onJoinFail(error: SDKError?) {
        whiteBoardManagerEventListener?.onWhiteBoardJoinFail(error)
        whiteBoardManagerEventListener?.onWhiteBoardLoadingStatus(false)
    }

    override fun onRoomPhaseChanged(phase: RoomPhase?) {
        Constants.AgoraLog.e(tag + ":onRoomPhaseChanged->" + phase!!.name)
        Log.e(tag, "whiteboard initialization completed")
        if (phase == RoomPhase.connected) {
            initWhiteBoardAppliance()
        }
        whiteBoardManagerEventListener?.onWhiteBoardLoadingStatus(phase != RoomPhase.connected)
    }

    override fun onSceneStateChanged(state: SceneState?) {
        var download = false
        state?.let {
            curSceneState = state
            val index: Int = curSceneState!!.scenePath.lastIndexOf(File.separator)
            val dir: String = curSceneState!!.scenePath.substring(0, index)
            if (TextUtils.isEmpty(lastSceneDir)) {
                lastSceneDir = dir
                download = true
            } else if (!lastSceneDir.equals(dir)) {
                lastSceneDir = dir
                download = true
            }
        }
        if (download) {
            var curDownloadUrl: String
            val resourceName = state!!.scenePath.split(File.separator.toRegex()).toTypedArray()[1]
            if (resourceName == defaultCoursewareName) {
                cancelCurPreloadBySwitchScene()
            } else if (resourceName == courseware?.resourceName) {
                cancelCurPreloadBySwitchScene()
                /**Open the download(the download module will check whether it exists locally)*/
                courseware?.resourceUrl?.let {
                    curDownloadUrl = it
                    boardPreloadManager?.preload(curDownloadUrl)
                }
            } else if (curBoardState != null && curBoardState!!.materialList != null) {
                for (taskInfo in curBoardState!!.materialList) {
                    if (taskInfo.resourceName == resourceName && !TextUtils.isEmpty(resourceName)
                            && !TextUtils.isEmpty(taskInfo.taskUuid) && taskInfo.ext == BoardExt.pptx) {
                        Constants.AgoraLog.e("$tag:Start to download the courseware set by the teacher0")
                        cancelCurPreloadBySwitchScene()
                        /**Open the download(the download module will check whether it exists locally)*/
                        curDownloadUrl = String.format(AgoraEduSDK.DYNAMIC_URL, taskInfo.taskUuid)
                        Constants.AgoraLog.e("$tag:Start to download the courseware set by the teacher1")
                        boardPreloadManager?.preload(curDownloadUrl)
                    }
                }
            }
        }
        Constants.AgoraLog.e("$tag:onSceneStateChanged")
        whiteBoardManagerEventListener?.let {
            state?.let { state ->
                it.onSceneStateChanged(state)
            }
        }
    }

    override fun onMemberStateChanged(state: MemberState?) {
        whiteBoardManagerEventListener?.let {
            state?.let { v ->
                it.onMemberStateChanged(v)
            }
        }
    }

    override fun onDisconnectWithError(e: Exception?) {
        Constants.AgoraLog.e("$tag:onDisconnectWithError->${e?.printStackTrace()}")
        initBoardWithRoomToken(curLocalUuid, curLocalToken, localUserUuid)
    }

    /** GlobalStateChangeListener */
    override fun onGlobalStateChanged(state: GlobalState?) {
        state?.let {
            val latestBoardState = state as BoardState
            if (latestBoardState.isFullScreen == curBoardState?.isFullScreen) {
            } else if (latestBoardState!!.isFullScreen) {
                whiteBoardManagerEventListener?.onFullScreen(true)
                whiteBoardManagerEventListener?.onBoardSizeChangedFollowBroadcaster(true)
            } else if (!latestBoardState!!.isFullScreen) {
                whiteBoardManagerEventListener?.onFullScreen(false)
                whiteBoardManagerEventListener?.onBoardSizeChangedFollowBroadcaster(false)
            }
            curBoardState = state as BoardState
            if (!curBoardState!!.isTeacherFirstLogin && courseware != null && scenePpts != null
                    && loadPreviewPpt) {
                loadPreviewPpt = false
                boardProxy.putScenes(File.separator + courseware?.resourceName, scenePpts!!, 0)
                boardProxy.setScenePath(File.separator + courseware?.resourceName + File.separator
                        + scenePpts!![0]!!.name, object : Promise<Boolean> {
                    override fun then(t: Boolean) {
                        Constants.AgoraLog.e("$tag:setScenePath->$t")
                        if (t) {
                            boardProxy.scalePptToFit()
                        }
                    }

                    override fun catchEx(t: SDKError?) {
                        Constants.AgoraLog.e("$tag:catchEx->${t?.message}")
                    }
                })
            }
            if (curBoardState != null) {
                val granted = curBoardState!!.isGranted(localUserUuid)
                disableDeviceInputs(!granted)
                if (granted != curGranted) {
                    curGranted = granted
                    whiteBoardManagerEventListener?.onGrantedChanged(granted)
                }
                val follow = curBoardState!!.isFollow
                if (followTips) {
                    if (curFollowState != follow) {
                        curFollowState = follow
                        // ToastManager.showShort(follow ? R.string.open_follow_board : R.string.relieve_follow_board);
                    }
                } else {
                    followTips = true
                    curFollowState = follow
                }
                disableCameraTransform(!granted)
            }
        }
    }

    /** BoardPreloadManager */
    override fun onBoardResourceStartDownload(url: String) {
        Log.e(tag, "onBoardResourceStartDownload")
    }

    override fun onBoardResourceProgress(url: String, progress: Double) {
        Log.e(tag, "onBoardResourceProgress->$progress")
        whiteBoardManagerEventListener?.onBoardResourceProgress(url, progress)
    }

    override fun onBoardResourceLoadTimeout(url: String) {
        Log.e(tag, "onBoardResourceLoadTimeout")
        whiteBoardManagerEventListener?.onBoardResourceLoadTimeout(url)
    }

    override fun onBoardResourceReady(url: String) {
        Log.e(tag, "onBoardResourceReady")
        whiteBoardManagerEventListener?.onBoardResourceReady(url)
    }

    override fun onBoardResourceLoadFailed(url: String) {
        Log.e(tag, "onBoardResourceLoadFailed")
        whiteBoardManagerEventListener?.onBoardResourceLoadFailed(url)
    }

    /** IAgoraUIWhiteboardListener */
    override fun onApplianceSelected(type: AgoraUIApplianceType) {
        boardProxy.appliance = applianceConvert(type)
    }

    override fun onColorSelected(color: Int) {
        val rgb = ColorUtil.colorToArray(color)
        boardProxy.strokeColor = rgb
    }

    override fun onFontSizeSelected(size: Int) {
        boardProxy.textSize = size.toDouble()
    }

    override fun onThicknessSelected(thick: Int) {
        boardProxy.strokeWidth = thick.toDouble()
    }

    override fun onRosterSelected(anchor: View) {
        whiteBoardManagerEventListener?.onRoster(anchor)
    }

    override fun onBoardInputEnabled(enabled: Boolean) {
        whiteBoardManagerEventListener?.onBoardInputEnable(enabled)
    }

    override fun onDownloadSkipped(url: String?) {
        boardPreloadManager?.cancelPreload()
    }

    override fun onDownloadCanceled(url: String?) {
        boardPreloadManager?.cancelPreload()
    }

    override fun onDownloadRetry(url: String?) {
        boardPreloadManager?.let {
            url?.let { v -> it.preload(v) }
        }
    }

    override fun onBoardFullScreen(full: Boolean) {
        Log.e(tag, "onFullScreen->$full")
        whiteBoardManagerEventListener?.onFullScreen(full)
    }

    override fun onBoardZoomOut() {
        Log.e(tag, "onZoomOut")
        var curScale = boardProxy.zoomScale
        curScale -= scaleStepper
        if (curScale in miniScale..maxScale) {
            boardProxy.zoom(curScale)
        }
    }

    override fun onBoardZoomIn() {
        Log.e(tag, "onZoomIn")
        var curScale = boardProxy.zoomScale
        curScale += scaleStepper
        if (curScale in miniScale..maxScale) {
            boardProxy.zoom(curScale)
        }
    }

    override fun onBoardPrevPage() {
        Log.e(tag, "onPrevPage")
        boardProxy.pptPreviousStep()
    }

    override fun onBoardNextPage() {
        Log.e(tag, "onNextPage")
        boardProxy.pptNextStep()
    }
}

interface WhiteBoardManagerEventListener {
    fun onWhiteBoardJoinSuccess(config: AgoraUIDrawingConfig)

    fun onWhiteBoardJoinFail(error: SDKError?)

    fun onWhiteBoardLoadingStatus(loading: Boolean)

    fun onDisableDeviceInput(disable: Boolean)

    fun onDisableCameraTransform(disable: Boolean)

    fun onSceneStateChanged(state: SceneState)

    fun onMemberStateChanged(state: MemberState)

    fun onBoardSizeChangedFollowBroadcaster(full: Boolean)

    fun onRoster(anchor: View)

    fun onBoardInputEnable(enable: Boolean)

    fun onBoardResourceProgress(url: String, progress: Double)

    fun onBoardResourceLoadTimeout(url: String)

    fun onBoardResourceReady(url: String)

    fun onBoardResourceLoadFailed(url: String)

    fun onFullScreen(full: Boolean)

    fun onCancelCurDownload()

    fun onGrantedChanged(enabled: Boolean)
}