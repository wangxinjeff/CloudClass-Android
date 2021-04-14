package io.agora.uikit.interfaces.listeners

interface IAgoraUIChatListener {
    /**
     * @param message to be sent
     * @param timestamp sending timestamp
     */
    fun onSendLocalMessage(message: String, timestamp: Long)

    fun onFetchMessageHistory(startId: Int?, count: Int)
}