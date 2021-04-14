package io.agora.uikit.impl.users

enum class AgoraUIUserRole(val value: Int) {
    Teacher(1),
    Student(2),
    Assistant(3)
}

data class AgoraUIUserInfo(
        val userUuid: String,
        val userName: String,
        val role: AgoraUIUserRole = AgoraUIUserRole.Student
) {
    override fun equals(other: Any?): Boolean {
        if (other == null || other !is AgoraUIUserInfo) {
            return false
        }
        return other.userUuid == userUuid && other.userName == userName && other.role == role
    }
}

data class AgoraUIUserDetailInfo(val user: AgoraUIUserInfo, val streamUuid: String) {
    var isSelf: Boolean = true
    var onLine: Boolean = false
    var coHost: Boolean = false
    var boardGranted: Boolean = false
    var cameraState: AgoraUIDeviceState = AgoraUIDeviceState.UnAvailable
    var microState: AgoraUIDeviceState = AgoraUIDeviceState.UnAvailable
    var enableVideo: Boolean = false
    var enableAudio: Boolean = false
    var rewardCount: Int = -1

    constructor(user: AgoraUIUserInfo, streamUuid: String, isSelf: Boolean = true, onLine: Boolean = false, coHost: Boolean,
                boardGranted: Boolean, cameraState: AgoraUIDeviceState, microState: AgoraUIDeviceState,
                enableVideo: Boolean, enableAudio: Boolean, rewardCount: Int) : this(user, streamUuid) {
        this.isSelf = isSelf
        this.onLine = onLine
        this.coHost = coHost
        this.boardGranted = boardGranted
        this.cameraState = cameraState
        this.microState = microState
        this.enableVideo = enableVideo
        this.enableAudio = enableAudio
        this.rewardCount = rewardCount
    }

    fun copy(): AgoraUIUserDetailInfo {
        return AgoraUIUserDetailInfo(user, streamUuid, isSelf, onLine, coHost, boardGranted, cameraState,
                microState, enableVideo, enableAudio, rewardCount)
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is AgoraUIUserDetailInfo) {
            return false
        }
        return other.user == user && other.streamUuid == streamUuid && other.isSelf == isSelf
                && other.onLine == onLine && other.coHost == coHost && other.boardGranted == boardGranted
                && other.cameraState == cameraState && other.microState == microState
                && other.enableVideo == enableVideo && other.enableAudio == enableAudio
                && other.rewardCount == rewardCount
    }
}

enum class AgoraUIDeviceState(val value: Int) {
    UnAvailable(0),
    Available(1),
    Closed(2)
}

enum class AgoraUIVideoMode(val value: Int) {
    Single(0),
    Pair(1)
}