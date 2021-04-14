package io.agora.uikit.interfaces.protocols

import android.view.View
import android.view.ViewGroup
import io.agora.uikit.AgoraUIError
import io.agora.uikit.impl.container.AgoraUI1v1Container
import io.agora.uikit.impl.container.AgoraUISmallClassContainer
import io.agora.uikit.impl.container.AgoraContainerType
import io.agora.uikit.component.toast.AgoraUIToastManager
import io.agora.uikit.impl.container.AgoraUILargeClassContainer
import io.agora.uikit.impl.setting.DeviceConfig
import io.agora.uikit.impl.users.AgoraUIRoster
import io.agora.uikit.interfaces.listeners.*

interface IAgoraUIContainer {
    fun init(layout: ViewGroup, left: Int, top: Int, width: Int, height: Int)

    fun room(): IAgoraUIRoom?

    fun chat(): IAgoraUIChat?

    fun whiteBoard(): IAgoraUIWhiteboard?

    fun videoGroup(): IAgoraUIVideo?

    fun screenShare(): IAgoraUIScreenShare?

    fun handsUp(): IAgoraUIHandsUp

    fun userList(): IAgoraUIUserList?

    fun roster(anchor: View): AgoraUIRoster?

    fun deviceConfig(): DeviceConfig

    fun setDeviceListener(listener: IAgoraUIDeviceListener)

    fun setRoomListener(listener: IAgoraUIRoomListener)

    fun setChatListener(listener: IAgoraUIChatListener)

    fun setWhiteboardListener(listener: IAgoraUIWhiteboardListener)

    fun setVideoGroupListener(listener: IAgoraUIVideoGroupListener)

    fun setScreenShareListener(listener: IAgoraUIScreenShareListener)

    fun setHandsUpListener(listener: IAgoraUIHandsUpListener)

    fun setUserListListener(listener: IAgoraUIUserListListener)

    fun showLeave()

    fun kickOut()

    fun showError(error: AgoraUIError)

    fun showTips(msg: String)
}

object AgoraUIContainer {
    fun create(layout: ViewGroup, left: Int, top: Int,
               width: Int, height: Int, type: AgoraContainerType): IAgoraUIContainer {
        AgoraUIToastManager.init(layout.context)
        return when (type) {
            AgoraContainerType.OneToOne -> {
                val container = AgoraUI1v1Container()
                container.init(layout, left, top, width, height)
                container
            }
            AgoraContainerType.SmallClass -> {
                val container = AgoraUISmallClassContainer()
                container.init(layout, left, top, width, height)
                container
            }
            AgoraContainerType.LargeClass -> {
                val container = AgoraUILargeClassContainer()
                container.init(layout, left, top, width, height)
                container
            }
        }
    }
}