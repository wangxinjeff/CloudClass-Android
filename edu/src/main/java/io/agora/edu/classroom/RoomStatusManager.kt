package io.agora.edu.classroom

import android.content.Context
import android.os.Handler
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.Log
import io.agora.edu.R
import io.agora.edu.common.bean.response.RoomPreCheckRes
import io.agora.edu.launch.AgoraEduLaunchConfig
import io.agora.edu.util.TimeUtil
import io.agora.education.api.EduCallback
import io.agora.education.api.base.EduError
import io.agora.education.api.room.EduRoom
import io.agora.education.api.room.data.EduRoomChangeType
import io.agora.education.api.room.data.EduRoomState
import io.agora.education.api.room.data.EduRoomStatus
import io.agora.education.api.statistics.ConnectionState
import io.agora.education.api.statistics.NetworkQuality
import io.agora.uikit.impl.room.AgoraUIConnectionState
import io.agora.uikit.impl.room.ClassState
import io.agora.uikit.impl.room.AgoraUINetworkState
import io.agora.uikit.interfaces.listeners.IAgoraUIRoomListener

class RoomStatusManager(
        context: Context,
        var launchConfig: AgoraEduLaunchConfig,
        var preCheckData: RoomPreCheckRes,
        var eduRoom: EduRoom?
) : IAgoraUIRoomListener {
    private val tag = "RoomStatusManager"
    private var contextApp = context.applicationContext
    private var handler: Handler? = Handler(contextApp.mainLooper)
    private var started = false
    private var startTime: Long = 0L
    private var duration: Long = 0L
    private val reverseRunnable: Runnable = object : Runnable {
        override fun run() {
            var minutes = (startTime - TimeUtil.currentTimeMillis()) / 1000 / 60
            var seconds = (startTime - TimeUtil.currentTimeMillis()) / 1000 % 60
            minutes = if (minutes < 0) 0 else minutes
            seconds = if (seconds < 0) 0 else seconds
            val timeStr = String.format(contextApp.getString(R.string.reward_window_classtime), minutes, seconds)
            eventListener?.onClassTime(timeStr)
            if (TimeUtil.currentTimeMillis() > startTime) {
                handler!!.removeCallbacks(this)
            } else {
                handler!!.postDelayed(this, 1000)
            }
        }
    }
    private val positiveRunnable: Runnable = object : Runnable {
        override fun run() {
            val startedTime = (TimeUtil.currentTimeMillis() - startTime) / 1000
            val minutes0 = startedTime / 60
            val seconds0 = startedTime % 60
            val timeStr = String.format(contextApp.getString(R.string.reward_window_classtime), minutes0, seconds0)
            eventListener?.onClassTime(timeStr)
            val a = duration - startedTime
            val b = closeDelay + duration - startedTime
            var tips = -1
            var minutes1: Long = 0
            var seconds1: Long = 0
            if (a in Class_Will_End_Warn_Time_Min until Class_Will_End_Warn_Time_Max) {
                minutes1 = 5
                tips = R.string.toast_classtime_until_class_end
            } else if (a > -1 && a < 1) {
                minutes1 = closeDelay / 60
                seconds1 = closeDelay % 60
                tips = R.string.toast_classtime_until_class_close_0
            } else if (a > -closeDelay - 1 && a <= -closeDelay) {
//                eventListener?.onForceLeave()
                eventListener?.onClassState(ClassState.Destroyed)
            } else if (b in Class_Force_Leave_Warn_Time_Min until Class_Force_Leave_Warn_Time_Max) {
                minutes1 = 1
                tips = R.string.toast_classtime_until_class_close_1
            }
            if (tips != -1) {
                val countdownTips = buildCountdownTips(tips, minutes1, seconds1)
                eventListener?.onClassTips(countdownTips.toString())
            }
            handler!!.postDelayed(this, 1000)
        }
    }
    private val Class_Will_End_Warn_Time_Min = (5 * 60 - 1).toLong()
    private val Class_Will_End_Warn_Time_Max = (5 * 60 + 1).toLong()
    private var closeDelay: Long = 0L
    private val Class_Force_Leave_Warn_Time_Min = (1 * 60 - 1).toLong()
    private val Class_Force_Leave_Warn_Time_Max = (1 * 60 + 1).toLong()
    private var curNetworkState = AgoraUINetworkState.Unknown

    var eventListener: RoomStatusManagerEventListener? = null

    init {

    }

    fun dispose() {
        handler?.removeCallbacksAndMessages(null)
        handler = null
        eduRoom = null
    }

    fun initClassState() {
        eduRoom?.getRoomStatus(object : EduCallback<EduRoomStatus> {
            override fun onSuccess(res: EduRoomStatus?) {
                res?.let {
                    setClassStarted(it.courseState == EduRoomState.START, preCheckData.startTime,
                            preCheckData.duration, preCheckData.closeDelay)
                }
            }

            override fun onFailure(error: EduError) {
            }
        })
    }

    fun updateClassState(event: EduRoomChangeType) {
        if (event == EduRoomChangeType.CourseState) {
            eduRoom?.getRoomStatus(object : EduCallback<EduRoomStatus> {
                override fun onSuccess(res: EduRoomStatus?) {
                    res?.let {
                        if (res.courseState == EduRoomState.START) {
                            setClassStarted(true, preCheckData.startTime,
                                    preCheckData.duration, preCheckData.closeDelay)
                        } else if (res.courseState == EduRoomState.END) {
                            setClassEnd()
                        }
                    }
                }

                override fun onFailure(error: EduError) {
                }
            })
        }
    }

    fun setClassStarted(started: Boolean, startTime: Long, duration: Long, closeDelay: Long) {
        this.started = started
        this.startTime = startTime
        this.duration = duration
        this.closeDelay = closeDelay
        if (!started) {
            eventListener?.onClassState(ClassState.Init)
            reverseRunnable.run()
        } else {
            handler!!.removeCallbacks(reverseRunnable)
            eventListener?.onClassState(ClassState.Start)
            positiveRunnable.run()
        }
    }

    private fun setClassEnd() {
        eventListener?.onClassState(ClassState.End)
    }

    private fun buildCountdownTips(tips: Int, minutes: Long, seconds: Long): SpannableString {
        var time: String? = null
        var content: SpannableString? = null
        if (tips == R.string.toast_classtime_until_class_close_0) {
            if (seconds != 0L) {
                time = String.format(contextApp.getString(R.string.toast_classtime_until_class_close_0_args),
                        minutes, seconds)
                time = String.format(contextApp.getString(tips), time)
                content = SpannableString(time.toString())
                val minutesStart = time.indexOf(minutes.toString())
                content.setSpan(ForegroundColorSpan(contextApp.resources.getColor(R.color.toast_classtime_countdown_time)),
                        minutesStart, minutesStart + minutes.toString().length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                val secondsStart = time.lastIndexOf(seconds.toString())
                content.setSpan(ForegroundColorSpan(contextApp.resources.getColor(R.color.toast_classtime_countdown_time)),
                        secondsStart, secondsStart + seconds.toString().length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            } else {
                time = String.format(contextApp.getString(R.string.toast_classtime_until_class_close_0_arg),
                        minutes)
                time = String.format(contextApp.getString(tips), time)
                content = SpannableString(time.toString())
                val minutesStart = time.indexOf(minutes.toString())
                content.setSpan(ForegroundColorSpan(contextApp.resources.getColor(R.color.toast_classtime_countdown_time)),
                        minutesStart, minutesStart + minutes.toString().length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        } else {
            time = String.format(contextApp.getString(R.string.toast_classtime_until_class_close_0_arg), minutes)
            time = String.format(contextApp.getString(tips), time)
            content = SpannableString(time.toString())
            val start = time.indexOf(minutes.toString())
            content.setSpan(ForegroundColorSpan(contextApp.resources.getColor(R.color.toast_classtime_countdown_time)),
                    start, start + minutes.toString().length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return content
    }

    fun updateNetworkState(quality: NetworkQuality) {
        var state = AgoraUINetworkState.Unknown
        when (quality) {
            NetworkQuality.UNKNOWN -> state = AgoraUINetworkState.Unknown
            NetworkQuality.GOOD -> state = AgoraUINetworkState.Good
            NetworkQuality.POOR -> state = AgoraUINetworkState.Medium
            NetworkQuality.BAD -> {
                state = AgoraUINetworkState.Bad
                if (curNetworkState != AgoraUINetworkState.Bad) {
                    eventListener?.onClassTips(contextApp.getString(R.string.toast_classroom_network_bad))
                }
            }
        }
        curNetworkState = state
        eventListener?.onNetworkState(curNetworkState)
    }

    fun updateConnectionState(connectionState: ConnectionState) {
        var state = AgoraUIConnectionState.Disconnected
        when (connectionState) {
            ConnectionState.DISCONNECTED -> {
                state = AgoraUIConnectionState.Disconnected
            }
            ConnectionState.CONNECTING -> {
                state = AgoraUIConnectionState.Connecting
            }
            ConnectionState.CONNECTED -> {
                state = AgoraUIConnectionState.Connected
            }
            ConnectionState.RECONNECTING -> {
                state = AgoraUIConnectionState.Reconnecting
            }
            ConnectionState.ABORTED -> {
                state = AgoraUIConnectionState.Aborted
                abortedByRemoteLogin()
            }
        }
        eventListener?.onConnectionState(state)
    }

    private fun abortedByRemoteLogin() {
        eventListener?.onClassTips(contextApp.getString(R.string.remoteloginerror))
        eventListener?.onAbortedByRemoteLogin()
    }

    override fun onLeave() {
        Log.i(tag, "onLeave")
        eventListener?.onForceLeave()
    }

}

interface RoomStatusManagerEventListener {
    fun onClassName(name: String)

    fun onClassState(state: ClassState)

    fun onClassTime(timeStr: String)

    fun onNetworkState(stateAgoraUI: AgoraUINetworkState)

    fun onConnectionState(state: AgoraUIConnectionState)

    fun onClassTips(msg: String)

    fun onForceLeave()

    fun onAbortedByRemoteLogin()
}