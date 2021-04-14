package io.agora.edu.classroom

import android.content.Context
import android.os.Handler
import android.util.Log
import com.google.gson.Gson
import io.agora.edu.R
import io.agora.edu.common.api.Chat
import io.agora.edu.common.bean.request.ChatTranslateReq
import io.agora.edu.common.bean.response.ChatRecordItem
import io.agora.edu.common.bean.response.ChatTranslateRes
import io.agora.edu.common.impl.ChatImpl
import io.agora.edu.launch.AgoraEduLaunchConfig
import io.agora.education.api.EduCallback
import io.agora.education.api.base.EduError
import io.agora.education.api.base.EduError.Companion.internalError
import io.agora.education.api.room.EduRoom
import io.agora.education.api.room.data.EduRoomChangeType
import io.agora.education.api.room.data.EduRoomStatus
import io.agora.uikit.impl.chat.AgoraUIChatItem
import io.agora.uikit.impl.chat.AgoraUIChatItemType
import io.agora.uikit.impl.chat.AgoraUIChatSource
import io.agora.uikit.impl.chat.AgoraUIChatState
import io.agora.uikit.interfaces.listeners.IAgoraUIChatListener
import java.util.*

class ChatManager(
        private var context: Context,
        private var eduRoom: EduRoom?,
        private val launchConfig: AgoraEduLaunchConfig) : IAgoraUIChatListener {

    private val tag = ChatManager::javaClass.name
    private val chat: Chat = ChatImpl(launchConfig.appId, launchConfig.roomUuid)
    private var initMuteChatState = true

    var eventListener: ChatManagerEventListener? = null

    companion object {
        const val recordCounts = 10
    }

    fun dispose() {
        eduRoom = null
    }

    fun initChat() {
        notifyMuteChatStatus(EduRoomChangeType.AllStudentsChat)
        Handler().postDelayed({
            pullChatRecords(null, recordCounts, true)
        }, 500)
    }

    private fun setRoomChat(message: String, timestamp: Long) {
        chat.roomChat(launchConfig.userUuid, message, object : EduCallback<Int?> {
            override fun onSuccess(res: Int?) {
                if (res != null) {
                    eventListener?.onSendLocalMessageResult(message, launchConfig.userUuid, res,
                            timestamp, true)
                } else {
                    eventListener?.onSendLocalMessageResult(message, launchConfig.userUuid, 0,
                            timestamp, false)
                }
            }

            override fun onFailure(error: EduError) {
                Log.e(tag, "onSendLocalMessage fail: ${error.type}, ${error.msg}")
                eventListener?.onSendLocalMessageResult(message, launchConfig.userUuid, 0, timestamp, false)
            }
        })
    }

    private fun pullChatRecords(nextId: String?, count: Int, reverse: Boolean) {
        chat.pullRecords(nextId, count, reverse, object : EduCallback<MutableList<ChatRecordItem>> {
            override fun onSuccess(res: MutableList<ChatRecordItem>?) {
                if (res != null) {
                    Log.i(tag, "pullChatRecords result->" + Gson().toJson(res))
                    val result: MutableList<AgoraUIChatItem> = ArrayList()
                    res.forEach { item ->
                        result.add(toAgoraUIChatItem(item))
                    }
                    eventListener?.onFetchMessageHistoryResult(true, result)
                } else {
                    Log.e(tag, "pullChatRecords failed!")
                    eventListener?.onFetchMessageHistoryResult(false, null)
                }
            }

            override fun onFailure(error: EduError) {
                Log.e(tag, "pullChatRecords failed->" + Gson().toJson(error))
            }
        })
    }

    private fun toAgoraUIChatItem(item: ChatRecordItem): AgoraUIChatItem {
        return AgoraUIChatItem(
                name = item.fromUser.userName,
                uid = item.fromUser.userUuid,
                message = item.message,
                messageId = item.messageId,
                type = AgoraUIChatItemType.Text,
                source = if (item.fromUser.userUuid == launchConfig.userUuid)
                    AgoraUIChatSource.Local
                else AgoraUIChatSource.Remote,
                timestamp = item.sendTime
        )
    }

    fun translate(msg: String?, to: String?, callback: EduCallback<String?>) {
        val req = ChatTranslateReq(msg!!, to!!)
        chat.translate(req, object : EduCallback<ChatTranslateRes?> {
            override fun onSuccess(res: ChatTranslateRes?) {
                if (res != null) {
                    Log.i(tag, "translate result->" + res.translation)
                    callback.onSuccess(res.translation)
                } else {
                    Log.e(tag, "translate failed!")
                    callback.onFailure(internalError("no translate result found"))
                }
            }

            override fun onFailure(error: EduError) {
                Log.e(tag, "translate failed->" + Gson().toJson(error))
                callback.onFailure(error)
            }
        })
    }

    fun notifyMuteChatStatus(type: EduRoomChangeType) {
        if (type == EduRoomChangeType.AllStudentsChat) {
            eduRoom?.getRoomStatus(object : EduCallback<EduRoomStatus> {
                override fun onSuccess(res: EduRoomStatus?) {
                    res?.let {
                        eventListener?.onMuteChat(!it.isStudentChatAllowed)
                        if(initMuteChatState) {
                            initMuteChatState = false
                            return
                        }
                        eventListener?.onShowChatTips(context.getString(if (it.isStudentChatAllowed)
                            R.string.chat_window_chat_enable else R.string.chat_window_chat_disable))
                    }
                }

                override fun onFailure(error: EduError) {
                }
            })
        }
    }

    override fun onSendLocalMessage(message: String, timestamp: Long) {
        Log.i(tag, "onSendLocalMessage $message")
        // Add this message to chat UI, and wait for the server result
        val item = AgoraUIChatItem(
                name = launchConfig?.userName ?: "",
                uid = launchConfig?.userUuid ?: "",
                message = message,
                source = AgoraUIChatSource.Local,
                state = AgoraUIChatState.InProgress,
                timestamp = timestamp)
        eventListener?.onAddMsg(item)
        setRoomChat(message, timestamp)
    }

    override fun onFetchMessageHistory(startId: Int?, count: Int) {
        Log.i(tag, "onFetchMessageHistory start: $startId, count: $count")
        pullChatRecords(startId?.toString(), count, true)
    }
}

interface ChatManagerEventListener {
    fun onAddMsg(item: AgoraUIChatItem)

    fun onSendLocalMessageResult(message: String, uid: String, messageId: Int, timestamp: Long, success: Boolean)

    fun onFetchMessageHistoryResult(success: Boolean, list: MutableList<AgoraUIChatItem>?)

    fun onMuteChat(mute: Boolean)

    fun onShowChatTips(msg: String)
}
