package io.agora.uikit.interfaces.protocols

import io.agora.uikit.impl.users.AgoraUIUserDetailInfo
import io.agora.uikit.impl.users.AgoraUIUserInfo

interface IAgoraUIUserList {
    fun updateUserList(list:MutableList<AgoraUIUserDetailInfo>)
    fun updateCoHostList(list:MutableList<AgoraUIUserDetailInfo>)
    fun showUserReward(userInfo: AgoraUIUserInfo)
    fun kickedOut()

    fun updateAudioVolumeIndication(value:Int, streamUuid:String)
    fun showUserTips(message:String)
}