package io.agora.edu.classroom

import android.content.Context
import android.view.ViewGroup
import io.agora.edu.R
import io.agora.edu.launch.AgoraEduLaunchConfig
import io.agora.education.api.EduCallback
import io.agora.education.api.base.EduError
import io.agora.education.api.room.EduRoom
import io.agora.education.api.stream.data.EduStreamEvent
import io.agora.education.api.stream.data.EduStreamInfo
import io.agora.education.api.stream.data.VideoSourceType
import io.agora.education.api.user.EduUser
import io.agora.uikit.interfaces.listeners.IAgoraUIScreenShareListener

class ScreenShareManager(
        val context: Context,
        var launchConfig: AgoraEduLaunchConfig,
        var eduRoom: EduRoom?,
        var eduUser: EduUser
) {
    private val tag = "ScreenShareManager"

    var eventListener: ScreenShareManagerEventListener? = null
    private var localScreenStream: EduStreamInfo? = null

    @Volatile private var isScreenShare: Boolean = false

    var screenShareStateChangedListener: ((Boolean) -> Unit) = object : ((Boolean) -> Unit) {
        override fun invoke(p1: Boolean) {

        }
    }

    val screenShareListener: IAgoraUIScreenShareListener = object : IAgoraUIScreenShareListener {
        override fun onScreenShareState(sharing: Boolean) {
            if (sharing != isScreenShare) {
                isScreenShare = sharing
                screenShareStateChangedListener(isScreenShare)
            }
        }

        override fun onRenderContainer(container: ViewGroup?, streamUuid: String) {
            eduRoom?.getFullStreamList(object : EduCallback<MutableList<EduStreamInfo>> {
                override fun onSuccess(res: MutableList<EduStreamInfo>?) {
                    res?.find { it.streamUuid == streamUuid }?.let {
                        isScreenShare = true
                        eduUser.setStreamView(it, launchConfig.roomUuid, container, isScreenShare)
                    }
                }

                override fun onFailure(error: EduError) {
                }
            })
        }
    }

    fun dispose() {
        eduRoom = null
    }

    fun isScreenSharing(): Boolean {
        return isScreenShare
    }

    fun restoreScreenShare() {
        eduRoom?.getFullStreamList(object : EduCallback<MutableList<EduStreamInfo>> {
            override fun onSuccess(res: MutableList<EduStreamInfo>?) {
                res?.find { it.videoSourceType == VideoSourceType.SCREEN }?.let {
                    if (localScreenStream != null && localScreenStream == it) {
                        return
                    } else {
                        eventListener?.onScreenShare(true, it.streamUuid)
                        eventListener?.onScreenShareTips(String.format(
                                context.getString(R.string.screen_share_start_message_format),
                                it.publisher.userName))
                    }
                }
            }

            override fun onFailure(error: EduError) {
            }
        })
    }

    fun upsertScreenShare(streamEvents: MutableList<EduStreamEvent>) {
        val screenShareStream = streamEvents.find {
            it.modifiedStream.videoSourceType == VideoSourceType.SCREEN
        }?.modifiedStream
        screenShareStream?.let {
            localScreenStream = screenShareStream
            eventListener?.onScreenShare(true, localScreenStream!!.streamUuid)
            eventListener?.onScreenShareTips(String.format(
                    context.getString(R.string.screen_share_start_message_format),
                    it.publisher.userName))
        }
    }

    fun removeScreenShare(streamEvents: MutableList<EduStreamEvent>) {
        localScreenStream = streamEvents?.find {
            it.modifiedStream.videoSourceType == VideoSourceType.SCREEN
        }?.modifiedStream
        localScreenStream?.let {
            eventListener?.onScreenShare(false, it.streamUuid)
            eventListener?.onScreenShareTips(String.format(
                    context.getString(R.string.screen_share_end_message_format),
                    it.publisher.userName))
        }
    }
}

interface ScreenShareManagerEventListener {
    fun onScreenShare(sharing: Boolean, streamUuid: String)

    fun onScreenShareTips(tip: String)
}