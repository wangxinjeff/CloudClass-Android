package io.agora.uikit.interfaces.protocols

import io.agora.uikit.AgoraUIError
import io.agora.uikit.impl.room.AgoraUIConnectionState
import io.agora.uikit.impl.room.ClassState
import io.agora.uikit.impl.room.AgoraUINetworkState

interface IAgoraUIRoom {
    fun setClassroomName(name: String)

    fun setClassState(state: ClassState)

    fun setClassTime(time: String)

    fun setNetworkState(stateAgoraUI: AgoraUINetworkState)

    fun setConnectionState(state: AgoraUIConnectionState)

    fun showClassTips(msg: String)

    fun showErrorInfo(error: AgoraUIError)
}