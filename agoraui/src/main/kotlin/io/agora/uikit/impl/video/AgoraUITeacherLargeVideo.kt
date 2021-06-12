package io.agora.uikit.impl.video

import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.cardview.widget.CardView
import io.agora.educontext.*
import io.agora.uikit.R
import io.agora.uikit.educontext.handlers.RoomHandler
import io.agora.uikit.educontext.handlers.UserHandler
import io.agora.uikit.impl.AbsComponent
import io.agora.educontext.EduContextDeviceState.Available

class AgoraUITeacherLargeVideo(
        context: Context,
        private val eduContext: EduContextPool?,
        parent: ViewGroup,
        left: Int,
        top: Int,
        width: Int,
        height: Int,
        shadowWidth: Float) : AbsComponent() {
    private val tag = "AgoraUITeacherLargeVideo"

    private val view: View = LayoutInflater.from(context).inflate(R.layout.agora_video_large_layout, parent, false)
    private val cardView: CardView = view.findViewById(R.id.cardView)
    private val videoContainer: FrameLayout = view.findViewById(R.id.videoContainer)
    private val videoOffLayout: LinearLayout = view.findViewById(R.id.video_off_layout)
    private val offLineLoadingLayout: LinearLayout = view.findViewById(R.id.offLine_loading_layout)
    private val noCameraLayout: LinearLayout = view.findViewById(R.id.no_camera_layout)
    private val cameraDisableLayout: LinearLayout = view.findViewById(R.id.camera_disable_layout)

    private var teacherInfo: EduContextUserDetailInfo? = null

    @Volatile
    private var isLargeMode = false

    private val userHandler = object : UserHandler() {
        override fun onUserListUpdated(list: MutableList<EduContextUserDetailInfo>) {
            super.onUserListUpdated(list)
            list.find { it.user.role == EduContextUserRole.Teacher }?.let {
                val currentVideoOpen = teacherInfo?.let { info ->
                    info.onLine && info.enableVideo && info.cameraState == Available
                } ?: false
                val newVideoOpen = it.onLine && it.enableVideo && it.cameraState == Available
                teacherInfo = it.copy()
                if (isLargeMode) {
                    (context as? Activity)?.runOnUiThread {
                        upsertLargeVideoState2(currentVideoOpen, newVideoOpen)
                    }
                }
            }
        }
    }

    private val rooMHandler = object : RoomHandler() {
        override fun onFlexRoomPropsInitialized(properties: MutableMap<String, Any>) {
            super.onFlexRoomPropsInitialized(properties)
            val tmp = properties["teacherRenderMode"] ?: "0"
            val teacherRenderMode = tmp.toString().toFloat().toInt()
            if (teacherRenderMode == 0) {
                isLargeMode = false
                (context as? Activity)?.runOnUiThread {
                    upsertLargeVideoState(false)
                }
            } else {
                isLargeMode = true
                (context as? Activity)?.runOnUiThread {
                    upsertLargeVideoState(true)
                }
            }
        }

        override fun onFlexRoomPropsChanged(changedProperties: MutableMap<String, Any>,
                                            properties: MutableMap<String, Any>,
                                            cause: MutableMap<String, Any>?,
                                            operator: EduContextUserInfo?) {
            super.onFlexRoomPropsChanged(changedProperties, properties, cause, operator)
            if (cause != null && cause.isNotEmpty()) {
                val causeType = cause["cause"].toString().toFloat().toInt()
                if (causeType == 0) {
                    val tmp = properties["teacherRenderMode"] ?: "0"
                    val teacherRenderMode = tmp.toString().toFloat().toInt()
                    if (teacherRenderMode == 0) {
                        isLargeMode = false
                        (context as? Activity)?.runOnUiThread {
                            upsertLargeVideoState(false)
                        }
                    } else {
                        isLargeMode = true
                        (context as? Activity)?.runOnUiThread {
                            upsertLargeVideoState(true)
                        }
                    }
                }
            }
        }
    }

    init {
        cardView.cardElevation = shadowWidth
        val radius = context.resources.getDimensionPixelSize(R.dimen.agora_video_view_corner)
        cardView.radius = radius.toFloat()
        val layoutParams = cardView.layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.width = width
        layoutParams.height = height
        layoutParams.leftMargin = left
        layoutParams.topMargin = top
        parent.addView(view)

        eduContext?.userContext()?.addHandler(userHandler)
        eduContext?.roomContext()?.addHandler(rooMHandler)
    }

    private fun setVideoPlaceHolder(info: EduContextUserDetailInfo) {
        videoContainer.visibility = View.GONE
        videoOffLayout.visibility = View.GONE
        offLineLoadingLayout.visibility = View.GONE
        noCameraLayout.visibility = View.GONE
        cameraDisableLayout.visibility = View.GONE
        if (!info.onLine) {
            offLineLoadingLayout.visibility = View.VISIBLE
        } else if (info.cameraState == EduContextDeviceState.Closed) {
            cameraDisableLayout.visibility = View.VISIBLE
        } else if (info.cameraState == EduContextDeviceState.UnAvailable) {
            noCameraLayout.visibility = View.VISIBLE
        } else if (info.cameraState == EduContextDeviceState.Available) {
            if (info.enableVideo) {
                videoContainer.visibility = View.VISIBLE
            } else {
                videoOffLayout.visibility = View.VISIBLE
            }
        }
    }

    /**only called on UIThread
     * @param render  render stream or cancel */
    private fun upsertLargeVideoState(render: Boolean) {
        teacherInfo?.let {
            setVideoPlaceHolder(it)
            if (render) {
                eduContext?.userContext()?.renderVideo(videoContainer, it.streamUuid,
                        EduContextRenderConfig(EduContextRenderMode.HIDDEN))
            } else {
//                eduContext?.userContext()?.renderVideo(null, it.streamUuid,
//                        EduContextRenderConfig(EduContextRenderMode.HIDDEN))
            }
        }
    }

    // only called on UIThread
    private fun upsertLargeVideoState2(lastVideoOpen: Boolean, newVideoOpen: Boolean) {
        teacherInfo?.let {
            setVideoPlaceHolder(it)
            if (!lastVideoOpen && newVideoOpen) {
                eduContext?.userContext()?.renderVideo(videoContainer, it.streamUuid,
                        EduContextRenderConfig(EduContextRenderMode.HIDDEN))
            }
        }
    }

    override fun setRect(rect: Rect) {
        view.post {
            val params = view.layoutParams as ViewGroup.MarginLayoutParams
            params.topMargin = rect.top
            params.leftMargin = rect.left
            params.width = rect.right - rect.left
            params.height = rect.bottom - rect.top
            view.layoutParams = params
        }
    }
}
