package io.agora.uikit.interfaces.listeners

import io.agora.uikit.impl.handsup.AgoraUIHandsUpState

interface IAgoraUIHandsUpListener {
    fun onHandsUpState(state: AgoraUIHandsUpState)
}