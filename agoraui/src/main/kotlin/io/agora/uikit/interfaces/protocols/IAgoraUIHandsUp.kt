package io.agora.uikit.interfaces.protocols

import io.agora.uikit.AgoraUIError
import io.agora.uikit.impl.handsup.AgoraUIHandsUpState

interface IAgoraUIHandsUp {
    fun setHandsUpEnable(enable: Boolean)
    fun updateHandsUpState(state: AgoraUIHandsUpState, coHost: Boolean)
    fun updateHandsUpStateResult(error: AgoraUIError?)
    fun showHandsUpTips(tips: String)
}