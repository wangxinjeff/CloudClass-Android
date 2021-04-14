package io.agora.uikit.interfaces.listeners

import android.view.ViewGroup

interface IAgoraUIScreenShareListener {
    fun onScreenShareState(sharing: Boolean)

    fun onRenderContainer(container: ViewGroup?, streamUuid: String)
}