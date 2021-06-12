package io.agora.uikit.impl.video

import android.app.Activity
import android.content.Context
import android.graphics.Rect
import android.view.*
import android.widget.LinearLayout
import io.agora.educontext.*
import io.agora.uikit.educontext.handlers.RoomHandler
import io.agora.uikit.educontext.handlers.UserHandler
import io.agora.uikit.interfaces.listeners.IAgoraUIVideoListener
import io.agora.uikit.educontext.handlers.VideoHandler
import io.agora.uikit.impl.AbsComponent

class AgoraUIVideoGroup(
        context: Context,
        private val eduContext: EduContextPool?,
        parent: ViewGroup,
        left: Int,
        top: Int,
        width: Int,
        height: Int,
        margin: Int,
        mode: EduContextVideoMode = EduContextVideoMode.Single) : AbsComponent(), IAgoraUIVideoListener {
    private val tag = "AgoraUIVideoGroup"

    private val videoLayout = LinearLayout(context)
    private var remoteVideo: AgoraUIVideo? = null
    private var localVideo: AgoraUIVideo? = null
    private var remoteUserDetailInfo: EduContextUserDetailInfo? = null
    private var localUserDetailInfo: EduContextUserDetailInfo? = null

    private val videoGroupHandler = object : VideoHandler() {
        override fun onUserDetailInfoUpdated(info: EduContextUserDetailInfo) {
            if (info.user.role == EduContextUserRole.Teacher) {
                remoteUserDetailInfo = info
                remoteVideo?.upsertUserDetailInfo(info)
            } else if (info.user.role == EduContextUserRole.Student) {
                localUserDetailInfo = info
                localVideo?.upsertUserDetailInfo(info)
            }
        }

        override fun onVolumeUpdated(volume: Int, streamUuid: String) {
            if (streamUuid == remoteUserDetailInfo?.streamUuid) {
                remoteVideo?.updateAudioVolumeIndication(volume, streamUuid)
            } else if (streamUuid == localUserDetailInfo?.streamUuid) {
                localVideo?.updateAudioVolumeIndication(volume, streamUuid)
            }
        }
    }

    private val rooMHandler = object : RoomHandler() {
        override fun onFlexRoomPropsInitialized(properties: MutableMap<String, Any>) {
            super.onFlexRoomPropsInitialized(properties)
            val tmp = properties["teacherRenderMode"] ?: "0"
            val teacherRenderMode = tmp.toString().toFloat().toInt()
            if (teacherRenderMode == 0) {
                (context as? Activity)?.runOnUiThread {
                    pushInRemoteVideo()
                }
            } else {
                (context as? Activity)?.runOnUiThread {
                    pullOutRemoteVideo()
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
                        (context as? Activity)?.runOnUiThread {
                            pushInRemoteVideo()
                        }
                    } else {
                        (context as? Activity)?.runOnUiThread {
                            pullOutRemoteVideo()
                        }
                    }
                }
            }
        }
    }

    init {
        parent.addView(videoLayout, width, height)
        val videoParams = videoLayout.layoutParams as ViewGroup.MarginLayoutParams
        videoParams.leftMargin = left
        videoParams.topMargin = top
        videoLayout.layoutParams = videoParams
        videoLayout.orientation = LinearLayout.VERTICAL

        val remoteLayout = LinearLayout(context)
        remoteLayout.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT, 1.0f)
        videoLayout.addView(remoteLayout)
        remoteVideo = AgoraUIVideo(context, remoteLayout, 0.0f, 0.0f, 0f)
        remoteVideo?.videoListener = this

        if (mode == EduContextVideoMode.Pair) {
            val localLayout = LinearLayout(context)
            localLayout.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT, 1.0f)
            videoLayout.addView(localLayout)
            val params = localLayout.layoutParams as ViewGroup.MarginLayoutParams
            params.topMargin = margin
            localLayout.layoutParams = params
            localVideo = AgoraUIVideo(context, localLayout, 0.0f, 0.0f, 0f)
            localVideo?.videoListener = this
        }

        eduContext?.videoContext()?.addHandler(videoGroupHandler)
        eduContext?.roomContext()?.addHandler(rooMHandler)
    }

    // Stop rendering in standard mode
    // only called on UIThread
    fun pullOutRemoteVideo() {
        remoteUserDetailInfo?.let {
            remoteVideo?.isLargeMode = true
            remoteVideo?.upsertUserDetailInfo2(it)
//            eduContext?.videoContext()?.renderVideo(null, it.streamUuid)
        }
    }

    // Restore standard mode rendering
    // only called on UIThread
    fun pushInRemoteVideo() {
        remoteUserDetailInfo?.let {
            remoteVideo?.isLargeMode = false
            remoteVideo?.upsertUserDetailInfo2(it)
        }
    }

    override fun onUpdateVideo(enable: Boolean) {
        eduContext?.videoContext()?.updateVideo(enable)
    }

    override fun onUpdateAudio(enable: Boolean) {
        eduContext?.videoContext()?.updateAudio(enable)
    }

    override fun onRendererContainer(viewGroup: ViewGroup?, streamUuid: String) {
        eduContext?.videoContext()?.renderVideo(viewGroup, streamUuid)
    }

    override fun setRect(rect: Rect) {
        videoLayout.post {
            val params = videoLayout.layoutParams as ViewGroup.MarginLayoutParams
            params.topMargin = rect.top
            params.leftMargin = rect.left
            params.width = rect.right - rect.left
            params.height = rect.bottom - rect.top
            videoLayout.layoutParams = params
        }
    }
}