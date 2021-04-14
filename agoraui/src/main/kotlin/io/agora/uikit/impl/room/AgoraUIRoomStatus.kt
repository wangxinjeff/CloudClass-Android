package io.agora.uikit.impl.room

import android.annotation.SuppressLint
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import io.agora.uikit.impl.AbsComponent
import io.agora.uikit.R
import io.agora.uikit.component.dialog.AgoraUIDialogBuilder
import io.agora.uikit.interfaces.listeners.IAgoraUIRoomListener
import io.agora.uikit.interfaces.listeners.IAgoraUIDeviceListener

@SuppressLint("InflateParams")
class AgoraUIRoomStatus(parent: ViewGroup, width: Int, height: Int, left: Int, top: Int) : AbsComponent() {
    private val networkImage: AppCompatImageView
    private val className: AppCompatTextView
    private val classStateText: AppCompatTextView
    private val classTimeText: AppCompatTextView
    private val exitBtn: AppCompatImageView

    private var roomListener: IAgoraUIRoomListener? = null
    private var deviceListener: IAgoraUIDeviceListener? = null

    init {
        val layout = LayoutInflater.from(parent.context).inflate(
                R.layout.agora_status_bar_layout, parent, false)
        parent.addView(layout, width, height)
        val params = layout.layoutParams as ViewGroup.MarginLayoutParams
        params.topMargin = top
        params.leftMargin = left
        layout.layoutParams = params

        networkImage = layout.findViewById(R.id.agora_status_bar_network_state_icon)
        className = layout.findViewById(R.id.agora_status_bar_classroom_name)
        classStateText = layout.findViewById(R.id.agora_status_bar_class_started_text)
        classTimeText = layout.findViewById(R.id.agora_status_bar_class_time_text)
        exitBtn = layout.findViewById(R.id.agora_status_bar_exit_icon)

        exitBtn.setOnClickListener {
            showLeaveDialog()
        }

        setNetworkState(AgoraUINetworkState.Unknown)
    }

    fun showLeaveDialog() {
        className?.post {
            AgoraUIDialogBuilder(className.context)
                    .title(className.context.resources.getString(R.string.agora_dialog_end_class_confirm_title))
                    .message(className.context.resources.getString(R.string.agora_dialog_end_class_confirm_message))
                    .negativeText(className.context.resources.getString(R.string.cancel))
                    .positiveText(className.context.resources.getString(R.string.confirm))
                    .positiveClick(View.OnClickListener { roomListener?.onLeave() })
                    .build()
                    .show()
        }
    }

    private fun destroyClassDialog() {
        className?.post {
            AgoraUIDialogBuilder(className.context)
                    .title(className.context.resources.getString(R.string.agora_dialog_class_destroy_title))
                    .message(className.context.resources.getString(R.string.agora_dialog_class_destroy))
                    .positiveText(className.context.resources.getString(R.string.confirm))
                    .positiveClick(View.OnClickListener { roomListener?.onLeave() })
                    .build()
                    .show()
        }
    }

    fun kickOut() {
        className?.post {
            AgoraUIDialogBuilder(className.context)
                    .title(className.context.resources.getString(R.string.agora_dialog_kicked_title))
                    .message(className.context.resources.getString(R.string.agora_dialog_kicked_message))
                    .positiveText(className.context.resources.getString(R.string.confirm))
                    .positiveClick(View.OnClickListener { roomListener?.onLeave() })
                    .build()
                    .show()
        }
    }

    fun setClassroomName(name: String) {
        className.post { className.text = name }
    }

    fun setClassState(state: ClassState) {
        classStateText.post {
            if (state == ClassState.Destroyed) {
                destroyClassDialog()
                return@post
            }
            classStateText.setText(
                    when (state) {
                        ClassState.Init -> R.string.agora_room_state_not_started
                        ClassState.Start -> R.string.agora_room_state_started
                        ClassState.End -> R.string.agora_room_state_end
                        else -> return@post
                    })
            if (state == ClassState.End) {
                classStateText.setTextColor(classStateText.context.resources.getColor(R.color.agora_setting_leave_text_color))
                classTimeText.setTextColor(classStateText.context.resources.getColor(R.color.agora_setting_leave_text_color))
            }
        }
    }

    fun setClassTime(time: String) {
        classTimeText.post { classTimeText.text = time }
    }

    fun setNetworkState(stateAgoraUI: AgoraUINetworkState) {
        networkImage.post {
            networkImage.setImageResource(stateAgoraUI.getIconRes())
        }
    }

    fun setRoomListener(listener: IAgoraUIRoomListener) {
        this.roomListener = listener
    }

    fun setDeviceListener(listener: IAgoraUIDeviceListener) {
        this.deviceListener = listener
    }

    override fun setRect(rect: Rect) {

    }
}

enum class AgoraUINetworkState {
    Good, Medium, Bad, Unknown;

    fun getIconRes(): Int {
        return when (this) {
            Good -> R.drawable.agora_tool_icon_signal_good
            Medium -> R.drawable.agora_tool_icon_signal_medium
            Bad -> R.drawable.agora_tool_icon_signal_bad
            Unknown -> R.drawable.agora_tool_icon_signal_unknown
        }
    }
}

enum class AgoraUIConnectionState {
    Disconnected,
    Connecting,
    Connected,
    Reconnecting,
    Aborted
}

enum class ClassState {
    Init, Start, End, Destroyed
}