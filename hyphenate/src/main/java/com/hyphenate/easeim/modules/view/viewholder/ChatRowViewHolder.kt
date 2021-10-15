package com.hyphenate.easeim.modules.view.viewholder

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.hyphenate.EMCallBack
import com.hyphenate.chat.EMMessage
import com.hyphenate.easeim.R
import com.hyphenate.easeim.modules.constant.EaseConstant
import com.hyphenate.easeim.modules.utils.EaseDateUtils
import com.hyphenate.easeim.modules.view.`interface`.MessageListItemClickListener
import com.hyphenate.util.EMLog
import java.util.*

abstract class ChatRowViewHolder(
        view: View,
        private val itemClickListener: MessageListItemClickListener,
        val context: Context
) : RecyclerView.ViewHolder(view) {
    companion object {
        private const val TAG = "ChatRowViewHolder"
    }

    private val avatar: ImageView? = itemView.findViewById(R.id.iv_avatar)
    val name: TextView? = itemView.findViewById(R.id.tv_name)
    val role: TextView? = itemView.findViewById(R.id.tv_role)
    private val proBar: ProgressBar? = itemView.findViewById(R.id.progress_bar)
    private val reSend: ImageView? = itemView.findViewById(R.id.resend)
    private val time: TextView? = itemView.findViewById(R.id.tv_time)
    lateinit var message: EMMessage
    val mainThreadHandler = Handler(Looper.getMainLooper())
    private val callback = ChatCallback()


    open fun setUpView(message: EMMessage) {
        this.message = message
        avatar?.let {
            Glide.with(context).load(
                    message.getStringAttribute(EaseConstant.AVATAR_URL, "")
            ).apply(RequestOptions.bitmapTransform(CircleCrop())).error(R.mipmap.default_avatar)
                    .into(
                            avatar
                    )
        }

        name?.text = message.getStringAttribute(EaseConstant.NICK_NAME, "")
        if (message.getIntAttribute(EaseConstant.ROLE, EaseConstant.ROLE_STUDENT) == EaseConstant.ROLE_TEACHER) {
            role?.text = context.getString(R.string.teacher)
            role?.visibility = View.VISIBLE
        }else {
            role?.visibility = View.GONE
        }
        time?.text = EaseDateUtils.getTimestampString(context, Date(message.msgTime))
        onSetUpView()
        setListener()
        handleMessage()
    }

    abstract fun onSetUpView()

    private fun setListener() {
        reSend?.setOnClickListener {
            itemClickListener.onResendClick(message)
        }
    }

    private fun handleMessage() {
        message.setMessageStatusCallback(callback)
        mainThreadHandler.post {
            when (message.status()) {
                EMMessage.Status.CREATE -> onMessageCreate()
                EMMessage.Status.SUCCESS -> onMessageSuccess()
                EMMessage.Status.INPROGRESS -> onMessageInProgress()
                EMMessage.Status.FAIL -> onMessageError()
                else -> EMLog.e(TAG, "default status")
            }
        }
    }

    inner class ChatCallback : EMCallBack {
        override fun onSuccess() {
            mainThreadHandler.post {
                onMessageSuccess()
            }
        }

        override fun onError(code: Int, error: String?) {
            mainThreadHandler.post {
                onMessageError()
                itemClickListener.onMessageError(message, code, error)
            }
        }

        override fun onProgress(progress: Int, status: String?) {
            mainThreadHandler.post {
                onMessageInProgress()
            }
        }

    }

    private fun onMessageCreate() {
        setStatus(View.VISIBLE, View.GONE)
    }

    open fun onMessageSuccess() {
        setStatus(View.GONE, View.GONE)
    }

    fun onMessageError() {
        setStatus(View.GONE, View.VISIBLE)
    }

    open fun onMessageInProgress() {
        setStatus(View.VISIBLE, View.GONE)
    }

    private fun setStatus(progressVisible: Int, reSendVisible: Int) {
        proBar?.visibility = progressVisible
        reSend?.visibility = reSendVisible
    }
}