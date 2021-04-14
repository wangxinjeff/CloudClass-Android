package io.agora.edu.classroom

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.agora.edu.R
import io.agora.edu.classroom.bean.PropertyCauseType
import io.agora.edu.classroom.bean.PropertyCauseType.Companion.HANDSUP_ENABLE_CHANGED
import io.agora.edu.classroom.bean.PropertyCauseType.Companion.COVIDEO_CHANGED
import io.agora.edu.common.bean.handsup.*
import io.agora.edu.common.impl.HandsUpImpl
import io.agora.edu.launch.AgoraEduLaunchConfig
import io.agora.education.api.EduCallback
import io.agora.education.api.base.EduError
import io.agora.education.api.room.EduRoom
import io.agora.education.api.user.EduUser
import io.agora.education.impl.Constants
import io.agora.uikit.AgoraUIError
import io.agora.uikit.impl.handsup.AgoraUIHandsUpState
import io.agora.uikit.interfaces.listeners.IAgoraUIHandsUpListener
import java.lang.ref.WeakReference

class HandsUpManager(
        context: Context,
        launchConfig: AgoraEduLaunchConfig,
        eduRoom: EduRoom?,
        eduUser: EduUser
) : BaseManager(context, launchConfig, eduRoom, eduUser), IAgoraUIHandsUpListener {
    override var tag = "HandsUpManager"

    private val processesKey = "processes"
    private val handsUpKey = "handsUp"

    private var handsUp = HandsUpImpl(launchConfig.appId, launchConfig.roomUuid)
    var eventListener: HandsUpManagerEventListener? = null

    fun initHandsUpData() {
        val processesJson = getProperty(eduRoom?.roomProperties, processesKey)
        val processesMap: MutableMap<String, Any>? = Gson().fromJson(processesJson, object : TypeToken<MutableMap<String, Any>>() {}.type)
        val handsUpJson = getProperty(processesMap, handsUpKey)
        val handsUpConfig = Gson().fromJson(handsUpJson, HandsUpConfig::class.java)
        handsUpConfig?.let {
            eventListener?.onHandsUpEnable(it.enabled == HandsUpEnableState.Enable.value)
            val coHost = it.accepted.contains(HandsUpAccept(eduUser.userInfo.userUuid))
            eventListener?.onHandsUpState(AgoraUIHandsUpState.HandsDown, coHost)
        }
    }

    fun notifyHandsUpEnable(cause: MutableMap<String, Any>?) {
        if (cause != null && cause.isNotEmpty()) {
            val causeType = cause[PropertyCauseType.CMD].toString().toFloat().toInt()
            if (causeType == HANDSUP_ENABLE_CHANGED) {
                val processesJson = getProperty(eduRoom?.roomProperties, processesKey)
                val processesMap: MutableMap<String, Any>? = Gson().fromJson(processesJson, object : TypeToken<MutableMap<String, Any>>() {}.type)
                val handsUpJson = getProperty(processesMap, handsUpKey)
                val handsUpConfig = Gson().fromJson(handsUpJson, HandsUpConfig::class.java)
                handsUpConfig?.let {
                    val enable = it.enabled == HandsUpEnableState.Enable.value
                    eventListener?.onHandsUpTips(context.getString(if (enable) R.string.handsupenable else
                        R.string.handsupdisable))
                    eventListener?.onHandsUpEnable(enable)
                }
            }
        }
    }

    fun notifyHandsUpState(cause: MutableMap<String, Any>?) {
        val localUserUuid = eduUser.userInfo.userUuid
        if (cause != null && cause.isNotEmpty()) {
            val causeType = cause[PropertyCauseType.CMD].toString().toFloat().toInt()
            if (causeType == COVIDEO_CHANGED) {
                val dataJson = cause[PropertyCauseType.DATA].toString()
                val data = Gson().fromJson(dataJson, HandsUpResData::class.java)
                var coHost = false
                var state = AgoraUIHandsUpState.Init
                when (data.actionType) {
                    HandsUpAction.StudentApply.value -> {
                        data?.addProgress?.forEach {
                            if (it.userUuid == localUserUuid) {
                                coHost = false
                                state = AgoraUIHandsUpState.HandsUp
                                eventListener?.onHandsUpTips(context.getString(R.string.handsupsuccess))
                                eventListener?.onHandsUpState(state, coHost)
                            }
                        }
                    }
                    HandsUpAction.TeacherAccept.value -> {
                        data?.addAccepted?.find { it == HandsUpAccept(localUserUuid) }?.let {
                            // The teacher gave me permission to be coHost
                            coHost = isCoHost(localUserUuid)
                            state = AgoraUIHandsUpState.HandsUp
                            eventListener?.onHandsUpTips(context.getString(R.string.covideo_accept_interactive))
                            eventListener?.onHandsUpState(state, coHost)
                        }
                    }
                    HandsUpAction.TeacherReject.value -> {
                        data?.removeProgress?.forEach {
                            // The teacher refused me to be coHost
                            if (it.userUuid == localUserUuid) {
                                coHost = false
                                state = AgoraUIHandsUpState.HandsDown
                                eventListener?.onHandsUpTips(context.getString(R.string.covideo_reject_interactive))
                                eventListener?.onHandsUpState(state, coHost)
                            }
                        }
                    }
                    HandsUpAction.StudentCancel.value -> {
                        data?.removeProgress?.forEach {
                            if (it.userUuid == localUserUuid) {
                                coHost = false
                                state = AgoraUIHandsUpState.HandsDown
                                eventListener?.onHandsUpTips(context.getString(R.string.cancelhandsupsuccess))
                                eventListener?.onHandsUpState(state, coHost)
                            }
                        }
                    }
                    HandsUpAction.TeacherAbort.value -> {
                        data?.removeAccepted?.find { it == HandsUpAccept(localUserUuid) }?.let {
                            coHost = false
                            state = AgoraUIHandsUpState.HandsDown
                            eventListener?.onHandsUpTips(context.getString(R.string.covideo_abort_interactive))
                            eventListener?.onHandsUpState(state, coHost)
                        }
                    }
                    HandsUpAction.TeacherTimeout.value -> {
                        data?.removeProgress?.forEach {
                            if (it.userUuid == localUserUuid) {
                                coHost = false
                                state = AgoraUIHandsUpState.HandsDown
                                eventListener?.onHandsUpTips(context.getString(R.string.handsuptimeout))
                                eventListener?.onHandsUpState(state, coHost)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun isCoHost(userUuid: String): Boolean {
        val processesJson = getProperty(eduRoom?.roomProperties, processesKey)
        val processesMap: MutableMap<String, Any>? = Gson().fromJson(processesJson, object : TypeToken<MutableMap<String, Any>>() {}.type)
        val handsUpJson = getProperty(processesMap, handsUpKey)
        val handsUpConfig = Gson().fromJson(handsUpJson, HandsUpConfig::class.java)
        return handsUpConfig?.accepted?.contains(HandsUpAccept(userUuid)) ?: false
    }

    private fun applyHandsUp() {
        handsUp.applyHandsUp(object : EduCallback<Boolean> {
            override fun onSuccess(res: Boolean?) {
                res?.let {
                    if (res) {
                        eventListener?.onHandsUpState(AgoraUIHandsUpState.HandsUp, false)
                    } else {
                    }
                }
            }

            override fun onFailure(error: EduError) {
                Constants.AgoraLog.e("$tag->type:${error.type},msg:${error.msg}")
                eventListener?.onHandsUpError(AgoraUIError(error.type, error.msg))
            }
        })
    }

    private fun cancelApplyHandsUp() {
        handsUp.cancelApplyHandsUp(object : EduCallback<Boolean> {
            override fun onSuccess(res: Boolean?) {
                res?.let {
                    if (res) {
                        eventListener?.onHandsUpState(AgoraUIHandsUpState.HandsDown, false)
                    } else {
                    }
                }
            }

            override fun onFailure(error: EduError) {
                Constants.AgoraLog.e("$tag->type:${error.type},msg:${error.msg}")
                eventListener?.onHandsUpError(AgoraUIError(error.type, error.msg))
            }
        })
    }

    override fun onHandsUpState(state: AgoraUIHandsUpState) {
        if (state == AgoraUIHandsUpState.HandsUp) {
            applyHandsUp()
        } else if (state == AgoraUIHandsUpState.HandsDown) {
            cancelApplyHandsUp()
        }
    }
}

enum class HandsUpResult(val value: Int) {
    Init(0),
    Accept(1),
    Reject(2)
}

interface HandsUpManagerEventListener {
    fun onHandsUpEnable(enable: Boolean)

    fun onHandsUpState(state: AgoraUIHandsUpState, coHost: Boolean)

    fun onHandsUpError(error: AgoraUIError?)

    fun onHandsUpTips(msg: String)
}