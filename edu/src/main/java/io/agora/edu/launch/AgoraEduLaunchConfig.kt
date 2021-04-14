package io.agora.edu.launch

import android.os.Parcel
import android.os.Parcelable
import io.agora.edu.common.bean.board.sceneppt.BoardCoursewareRes
import io.agora.edu.common.bean.board.sceneppt.Conversion

/**
 * @author cjw
 * @param startTime(Unit is milliseconds)
 * @param duration(Unit is seconds)
 */
class AgoraEduLaunchConfig(val userName: String, val userUuid: String, val roomName: String,
                           val roomUuid: String,
                           val roleType: Int = AgoraEduRoleType.AgoraEduRoleTypeStudent.value,
                           val roomType: Int, val rtmToken: String, val startTime: Long?,
                           val duration: Long?) : Parcelable {
    lateinit var appId: String
    var eyeCare = 0
    lateinit var whiteBoardAppId: String
    var frontCamera = true

    constructor(userName: String, userUuid: String, roomName: String, roomUuid: String,
                roleType: Int, roomType: Int, rtmToken: String,
                startTime: Long?, duration: Long?, frontCamera: Boolean) : this(
            userName, userUuid, roomName, roomUuid, roleType, roomType, rtmToken, startTime,
            duration) {
        this.frontCamera = frontCamera
    }

    private constructor(userName: String, userUuid: String, roomName: String, roomUuid: String,
                        roleType: Int, roomType: Int, rtmToken: String,
                        startTime: Long?, duration: Long?, frontCamera: Boolean, appId: String,
                        eyeCare: Int, whiteBoardAppId: String) : this(userName, userUuid, roomName,
            roomUuid, roleType, roomType, rtmToken, startTime, duration, frontCamera) {
        this.appId = appId
        this.eyeCare = eyeCare
        this.whiteBoardAppId = whiteBoardAppId
    }

    constructor(parcel: Parcel) : this(
            parcel.readString() ?: "",
            parcel.readString() ?: "",
            parcel.readString() ?: "",
            parcel.readString() ?: "",
            parcel.readInt(),
            parcel.readInt(),
            parcel.readString() ?: "",
            parcel.readLong(),
            parcel.readLong(),
            parcel.readInt() == 1,
            parcel.readString() ?: "",
            parcel.readInt(),
            parcel.readString() ?: ""
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(userName)
        parcel.writeString(userUuid)
        parcel.writeString(roomName)
        parcel.writeString(roomUuid)
        parcel.writeInt(roleType)
        parcel.writeInt(roomType)
        parcel.writeString(rtmToken)
        startTime?.let { parcel.writeLong(it) }
        duration?.let { parcel.writeLong(it) }
        parcel.writeInt(if (frontCamera) 1 else 0)
        parcel.writeString(appId)
        parcel.writeInt(eyeCare)
        parcel.writeString(whiteBoardAppId)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<AgoraEduLaunchConfig> {
        override fun createFromParcel(parcel: Parcel): AgoraEduLaunchConfig {
            return AgoraEduLaunchConfig(parcel)
        }

        override fun newArray(size: Int): Array<AgoraEduLaunchConfig?> {
            return arrayOfNulls(size)
        }
    }
}