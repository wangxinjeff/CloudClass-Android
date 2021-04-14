package io.agora.uikit.impl.video

import android.content.Context
import android.graphics.Rect
import android.view.*
import android.widget.LinearLayout
import io.agora.uikit.interfaces.listeners.IAgoraUIVideoListener
import io.agora.uikit.component.toast.AgoraUIToastManager
import io.agora.uikit.impl.AbsComponent
import io.agora.uikit.impl.users.AgoraUIUserDetailInfo
import io.agora.uikit.impl.users.AgoraUIUserRole
import io.agora.uikit.impl.users.AgoraUIVideoMode
import io.agora.uikit.interfaces.listeners.IAgoraUIVideoGroupListener

class AgoraUIVideoGroup(
        context: Context,
        parent: ViewGroup,
        left: Int,
        top: Int,
        width: Int,
        height: Int,
        margin: Int,
        mode: AgoraUIVideoMode = AgoraUIVideoMode.Single) : AbsComponent(), IAgoraUIVideoListener {
    private val tag = "AgoraUIVideoGroup"

    var videoGroupListener: IAgoraUIVideoGroupListener? = null

    private var remoteVideo: AgoraUIVideo
    private var localVideo: AgoraUIVideo? = null
    private var remoteUserDetailInfo: AgoraUIUserDetailInfo? = null
    private var localUserDetailInfo: AgoraUIUserDetailInfo? = null

    init {
        val videoLayout = LinearLayout(context)
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
        remoteVideo.videoListener = this

        if (mode == AgoraUIVideoMode.Pair) {
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
    }

    fun upsertUserDetailInfo(info: AgoraUIUserDetailInfo) {
        if (info.user.role == AgoraUIUserRole.Teacher) {
            this.remoteUserDetailInfo = info
            remoteVideo.upsertUserDetailInfo(info)
        } else if (info.user.role == AgoraUIUserRole.Student) {
            this.localUserDetailInfo = info
            localVideo?.upsertUserDetailInfo(info)
        }
    }

    fun updateAudioVolumeIndication(value: Int, streamUuid: String) {
        if (streamUuid == remoteUserDetailInfo?.streamUuid) {
            remoteVideo.updateAudioVolumeIndication(value, streamUuid)
        } else if (streamUuid == localUserDetailInfo?.streamUuid) {
            localVideo?.updateAudioVolumeIndication(value, streamUuid)
        }
    }

    fun updateMediaMessage(msg: String) {
        AgoraUIToastManager.showShort(msg)
    }

    override fun onUpdateVideo(enable: Boolean) {
        videoGroupListener?.onUpdateVideo(enable)
    }

    override fun onUpdateAudio(enable: Boolean) {
        videoGroupListener?.onUpdateAudio(enable)
    }

    override fun onRendererContainer(viewGroup: ViewGroup?, streamUuid: String) {
        videoGroupListener?.onRendererContainer(viewGroup, streamUuid)
    }

    override fun setRect(rect: Rect) {

    }
}