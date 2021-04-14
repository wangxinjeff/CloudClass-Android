package io.agora.uikit.impl.chat

data class AgoraUIChatItem(
        var name: String = "",
        var uid: String = "",
        var message: String = "",
        var messageId: Int = 0,
        var type: AgoraUIChatItemType = AgoraUIChatItemType.Text,
        var source: AgoraUIChatSource = AgoraUIChatSource.Remote,
        var state: AgoraUIChatState = AgoraUIChatState.Default,
        var timestamp: Long = 0) {

    fun copyValue(item: AgoraUIChatItem) {
        this.name = item.name
        this.uid = item.uid
        this.message = message
        this.messageId = messageId
        this.type = type
        this.source = item.source
        this.state = item.state
        this.timestamp = item.timestamp
    }
}

enum class AgoraUIChatItemType {
    Text
}

enum class AgoraUIChatSource {
    Local, Remote, System
}

enum class AgoraUIChatState {
    Default, InProgress, Success, Fail
}