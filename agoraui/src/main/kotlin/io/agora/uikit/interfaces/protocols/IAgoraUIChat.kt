package io.agora.uikit.interfaces.protocols

import io.agora.uikit.impl.chat.AgoraUIChatItem
import io.agora.uikit.interfaces.listeners.IAgoraUIChatListener

interface IAgoraUIChat {
    fun addMessage(item: AgoraUIChatItem)

    fun sendLocalMessageResult(message: String, uid: String, messageId: Int, timestamp: Long, success: Boolean)

    fun fetchMessageHistoryResult(success: Boolean, list: MutableList<AgoraUIChatItem>?)

    fun allowChat(allowed: Boolean)

    fun showChatTips(msg: String)
}