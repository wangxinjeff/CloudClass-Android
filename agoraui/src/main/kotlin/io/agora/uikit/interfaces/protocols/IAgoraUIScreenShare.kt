package io.agora.uikit.interfaces.protocols

interface IAgoraUIScreenShare {
    fun updateScreenShareState(sharing: Boolean, streamUuid: String)

    fun showScreenShareTips(tips: String)
}