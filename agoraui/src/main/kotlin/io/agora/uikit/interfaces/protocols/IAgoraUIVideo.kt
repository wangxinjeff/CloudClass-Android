package io.agora.uikit.interfaces.protocols

import io.agora.uikit.impl.users.AgoraUIUserDetailInfo


interface IAgoraUIVideo {

    fun upsertUserDetailInfo(info: AgoraUIUserDetailInfo)

    fun updateAudioVolumeIndication(value: Int, streamUuid: String)

    fun updateMediaMessage(msg: String)
}