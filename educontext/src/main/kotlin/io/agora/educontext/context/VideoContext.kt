package io.agora.educontext.context

import android.view.ViewGroup
import io.agora.educontext.AbsHandlerPool
import io.agora.educontext.EduContextRenderConfig
import io.agora.educontext.EduContextVideoConfig
import io.agora.educontext.eventHandler.IVideoHandler

abstract class VideoContext : AbsHandlerPool<IVideoHandler>() {
    abstract fun updateVideo(enabled: Boolean)

    abstract fun updateAudio(enabled: Boolean)

    abstract fun renderVideo(viewGroup: ViewGroup?, streamUuid: String,
                             renderConfig: EduContextRenderConfig = EduContextRenderConfig())

    abstract fun setVideoConfig(config: EduContextVideoConfig)
}