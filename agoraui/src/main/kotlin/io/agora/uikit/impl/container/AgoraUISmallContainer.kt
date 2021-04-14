package io.agora.uikit.impl.container

import android.content.res.Resources
import android.graphics.Rect
import android.view.ViewGroup
import io.agora.uikit.R
import io.agora.uikit.component.toast.AgoraUIToastManager
import io.agora.uikit.impl.chat.AgoraUIChatWindow
import io.agora.uikit.impl.chat.OnChatWindowAnimateListener
import io.agora.uikit.impl.handsup.AgoraUIHandsUp
import io.agora.uikit.impl.room.AgoraUIRoomStatus
import io.agora.uikit.impl.screenshare.AgoraUIScreenShare
import io.agora.uikit.impl.tool.AgoraUIToolBarBuilder
import io.agora.uikit.impl.tool.AgoraUIToolType
import io.agora.uikit.impl.users.*
import io.agora.uikit.impl.video.AgoraUIVideoGroup
import io.agora.uikit.impl.whiteboard.AgoraUIWhiteBoardBuilder
import io.agora.uikit.impl.whiteboard.paging.AgoraUIPagingControlBuilder
import io.agora.uikit.interfaces.protocols.IAgoraUIUserList

class AgoraUISmallClassContainer : AbsUIContainer() {
    private val tag = "AgoraUISmallClassContainer"

    private var statusBarHeight = 0

    // margin for tool and page control
    private var componentMargin = 0

    private var margin = 0
    private var shadow = 0
    private var border = 0

    private var width = 0
    private var height = 0
    private var top = 0
    private var left = 0

    private val chatRect = Rect()
    private val chatFullScreenRect = Rect()
    private val chatFullScreenHideRect = Rect()
    private val whiteboardDefaultRect = Rect()
    private val whiteboardFullScreenRect = Rect()
    private val whiteboardNoStudentVideoRect = Rect()
    private val handsUpRect = Rect()
    private val handsUpFullScreenRect = Rect()
    private var handsUpAnimateRect = Rect()

    private var toolbarTopNoStudent = 0
    private var toolbarTopHasStudent = 0
    private var toolbarHeightNoStudent = 0
    private var toolbarHeightHasStudent = 0

    private var isFullScreen = false

    private var userList: IAgoraUIUserList = object : IAgoraUIUserList {
        override fun updateUserList(list: MutableList<AgoraUIUserDetailInfo>) {
            roster?.updateUserList(list)
        }

        override fun updateCoHostList(list: MutableList<AgoraUIUserDetailInfo>) {
            studentVideoGroup?.updateCoHostList(list)
            val hasCoHost = list.size > 0
            studentVideoGroup?.show(hasCoHost)
            if (isFullScreen) {
              return
            }
            if (hasCoHost) {
                whiteboardWindow?.setRect(whiteboardDefaultRect)
                screenShareWindow?.setRect(whiteboardDefaultRect)
                toolbar?.setVerticalPosition(toolbarTopHasStudent, toolbarHeightHasStudent)
            } else {
                whiteboardWindow?.setRect(whiteboardNoStudentVideoRect)
                screenShareWindow?.setRect(whiteboardNoStudentVideoRect)
                toolbar?.setVerticalPosition(toolbarTopNoStudent, toolbarHeightNoStudent)
            }
        }

        override fun showUserReward(userInfo: AgoraUIUserInfo) {
            if (rewardWindow?.isShowing() == false) {
                rewardWindow?.show()
            }
        }

        override fun kickedOut() {
            kickOut()
        }

        override fun updateAudioVolumeIndication(value: Int, streamUuid: String) {
            studentVideoGroup?.updateAudioVolumeIndication(value, streamUuid)
        }

        override fun showUserTips(message: String) {
            AgoraUIToastManager.showShort(message)
        }
    }

    override fun init(layout: ViewGroup, left: Int, top: Int, width: Int, height: Int) {
        super.init(layout, left, top, width, height)

        this.width = width
        this.height = height
        this.left = left
        this.top = top

        initValues(layout.context.resources)

        roomStatus = AgoraUIRoomStatus(layout, width, statusBarHeight, left, top)
        roomStatus!!.setContainer(this)

        calculateVideoSize()
        val teacherVideoW = AgoraUIConfig.SmallClass.teacherVideoWidth
        val teacherVideoH = AgoraUIConfig.SmallClass.teacherVideoHeight
        val teacherVideoTop = statusBarHeight + margin
        val teacherVideoLeft = width - teacherVideoW
        videoGroupWindow = AgoraUIVideoGroup(layout.context, layout,
                teacherVideoLeft, teacherVideoTop, teacherVideoW,
                teacherVideoH, 0, AgoraUIVideoMode.Single)
        videoGroupWindow!!.setContainer(this)

        val studentVideoTop = statusBarHeight + margin
        val studentVideoLeft = border
        val studentVideoWidth = teacherVideoLeft - margin - border
        val studentVideoHeight = if (AgoraUIConfig.isLargeScreen)
            AgoraUIConfig.SmallClass.studentVideoHeightLargeScreen
        else AgoraUIConfig.SmallClass.studentVideoHeightSmallScreen

        studentVideoGroup = AgoraUserListVideoLayout(layout.context, layout,
                studentVideoWidth, studentVideoHeight, studentVideoLeft, studentVideoTop, 0f)
        studentVideoGroup!!.setContainer(this)
        studentVideoGroup!!.show(false)

        val whiteboardW = width - teacherVideoW - margin - border
        val whiteboardH = height - statusBarHeight - margin - border

        // Rect when student video list is shown
        whiteboardDefaultRect.set(border, studentVideoTop + studentVideoHeight + margin,
                whiteboardW, height - border)
        whiteboardNoStudentVideoRect.set(border, statusBarHeight + margin, whiteboardW, height - border)
        whiteboardFullScreenRect.set(border, statusBarHeight + margin, width - border, height - border)
        whiteboardWindow = AgoraUIWhiteBoardBuilder(layout.context, layout)
                .width(whiteboardW)
                .height(whiteboardH)
                .top(statusBarHeight + margin.toFloat())
                .shadowWidth(0f).build()
        whiteboardWindow!!.setContainer(this)

        screenShareWindow = AgoraUIScreenShare(layout.context, layout,
                whiteboardW, whiteboardH, border, statusBarHeight + margin, 0f)
        screenShareWindow!!.setContainer(this)

        val pagingControlHeight = layout.context.resources.getDimensionPixelSize(R.dimen.agora_paging_control_height)
        val pagingControlLeft = componentMargin
        val pagingControlTop = height - pagingControlHeight - margin - componentMargin
        pageControlWindow = AgoraUIPagingControlBuilder(layout.context, layout)
                .height(pagingControlHeight)
                .left(pagingControlLeft.toFloat())
                .top(pagingControlTop.toFloat())
                .shadowWidth(shadow.toFloat())
                .build()
        pageControlWindow!!.setContainer(this)

        toolbarTopNoStudent = whiteboardNoStudentVideoRect.top + componentMargin
        toolbarTopHasStudent = whiteboardDefaultRect.top + componentMargin
        toolbarHeightNoStudent = pagingControlTop - componentMargin - toolbarTopNoStudent
        toolbarHeightHasStudent = pagingControlTop - componentMargin - toolbarTopHasStudent
        toolbar = AgoraUIToolBarBuilder(layout.context, layout)
                .foldTop(toolbarTopNoStudent)
                .unfoldTop(toolbarTopNoStudent)
                .unfoldLeft(border + componentMargin)
                .unfoldHeight(toolbarHeightNoStudent)
                .shadowWidth(shadow)
                .build()
        toolbar!!.setToolbarType(AgoraUIToolType.All)
        toolbar!!.setContainer(this)
        toolbar?.setVerticalPosition(toolbarTopNoStudent, toolbarHeightNoStudent)

        val chatLeft = width - teacherVideoW - border
        val chatTop = teacherVideoTop + teacherVideoH + margin
        val chatHeight = height - chatTop - border
        chatRect.set(chatLeft, chatTop, chatLeft + teacherVideoW, chatTop + chatHeight)
        chatWindow = AgoraUIChatWindow(layout, teacherVideoW, chatHeight, chatLeft, chatTop, shadow)
        chatWindow?.let {
            it.setContainer(this)
            it.setClosable(false)
            it.showShadow(false)
        }

        // chat window is larger when whiteboard is full screen
        val chatFullScreenLeft = width - teacherVideoW - margin
        val chatFullScreenRight = width - componentMargin
        val chatFullScreenBottom = height - margin
        val chatFullScreenTop: Int = if (AgoraUIConfig.isLargeScreen) {
            chatFullScreenBottom - (height * AgoraUIConfig.chatHeightLargeScreenRatio).toInt()
        } else {
            statusBarHeight + margin + componentMargin
        }

        chatFullScreenRect.set(chatFullScreenLeft, chatFullScreenTop,
                chatFullScreenRight, chatFullScreenBottom)
        val chatFullScreenHideTop = chatFullScreenBottom - chatWindow?.hideIconSize!!
        val chatFullScreenHideLeft = chatFullScreenRight - chatWindow?.hideIconSize!!
        chatFullScreenHideRect.set(chatFullScreenHideLeft, chatFullScreenHideTop, chatFullScreenRight, chatFullScreenBottom)

        chatWindow!!.setAnimateListener(object : OnChatWindowAnimateListener {
            private var lastLeft = 0

            override fun onChatWindowAnimate(enlarge: Boolean, fraction: Float, left: Int,
                                             top: Int, width: Int, height: Int) {
                if (fraction.compareTo(0) == 0) lastLeft = left

                val chatWindowWidth = chatFullScreenRight - chatFullScreenLeft
                val diff = left - lastLeft
                lastLeft = left

                if (chatWindowWidth - left < chatWindow!!.hideIconSize) {
                    if (!enlarge) {
                        handsUpWindow?.setRect(handsUpFullScreenRect)
                        handsUpAnimateRect = Rect(handsUpFullScreenRect)
                    }
                    return
                }

                handsUpAnimateRect.left += diff
                handsUpAnimateRect.right += diff
                handsUpWindow?.setRect(handsUpAnimateRect)
            }
        })

        val handsUpWidth = layout.context.resources.getDimensionPixelSize(R.dimen.agora_hands_up_view_w)
        val handsUpHeight = layout.context.resources.getDimensionPixelSize(R.dimen.agora_hands_up_view_h)
        val handsUpTop = height - margin - handsUpHeight
        val handsUpLeft = whiteboardDefaultRect.right - margin - handsUpWidth
        handsUpRect.set(handsUpLeft, handsUpTop, handsUpLeft + handsUpWidth, handsUpTop + handsUpHeight)
        handsUpWindow = AgoraUIHandsUp(layout.context, layout, handsUpLeft, handsUpTop, handsUpWidth, handsUpHeight)
        handsUpWindow!!.setContainer(this)
        handsUpFullScreenRect.set(chatFullScreenHideLeft - margin - handsUpWidth, handsUpTop,
                chatFullScreenHideLeft - margin, handsUpTop + handsUpHeight)

        roster = AgoraUIRoster(layout)
    }

    private fun initValues(resources: Resources) {
        statusBarHeight = resources.getDimensionPixelSize(R.dimen.agora_status_bar_height)
        componentMargin = resources.getDimensionPixelSize(R.dimen.margin_medium)
        margin = resources.getDimensionPixelSize(R.dimen.margin_smaller)
        shadow = resources.getDimensionPixelSize(R.dimen.shadow_width)
        border = resources.getDimensionPixelSize(R.dimen.stroke_small)
    }

    override fun calculateVideoSize() {
        AgoraUIConfig.SmallClass.teacherVideoWidth =
                minOf((AgoraUIConfig.videoWidthMaxRatio * width).toInt(), AgoraUIConfig.SmallClass.teacherVideoWidth)
        AgoraUIConfig.SmallClass.teacherVideoHeight = (AgoraUIConfig.SmallClass.teacherVideoWidth * AgoraUIConfig.videoRatio).toInt()
        if (AgoraUIConfig.isLargeScreen) {
            AgoraUIConfig.SmallClass.studentVideoHeightLargeScreen = AgoraUIConfig.SmallClass.teacherVideoHeight
        } else {
            AgoraUIConfig.SmallClass.studentVideoHeightSmallScreen =
                    minOf(AgoraUIConfig.SmallClass.teacherVideoHeight,
                            (height * AgoraUIConfig.SmallClass.studentVideoHeightRationSmallScreen).toInt())
        }
    }

    override fun setFullScreen(fullScreen: Boolean) {
        isFullScreen = fullScreen
        if (fullScreen) {
            whiteboardWindow?.setRect(whiteboardFullScreenRect)
            screenShareWindow?.setRect(whiteboardFullScreenRect)
            chatWindow?.setFullscreenRect(fullScreen, chatFullScreenHideRect)
            chatWindow?.setFullDisplayRect(chatFullScreenRect)
            chatWindow?.setClosable(true)
            chatWindow?.showShadow(true)
            handsUpWindow?.setRect(handsUpFullScreenRect)
            handsUpAnimateRect = Rect(handsUpFullScreenRect)
            toolbar?.setVerticalPosition(toolbarTopNoStudent, toolbarHeightNoStudent)
        } else {
            if (studentVideoGroup!!.isShown()) {
                whiteboardWindow?.setRect(whiteboardDefaultRect)
                screenShareWindow?.setRect(whiteboardDefaultRect)
                toolbar?.setVerticalPosition(toolbarTopHasStudent, toolbarHeightHasStudent)
            } else {
                whiteboardWindow?.setRect(whiteboardNoStudentVideoRect)
                screenShareWindow?.setRect(whiteboardNoStudentVideoRect)
                toolbar?.setVerticalPosition(toolbarTopNoStudent, toolbarHeightNoStudent)
            }

            chatWindow?.setFullscreenRect(fullScreen, chatRect)
            chatWindow?.setFullDisplayRect(chatRect)
            chatWindow?.setClosable(false)
            chatWindow?.showShadow(false)
            handsUpWindow?.setRect(handsUpRect)
        }
    }

    override fun userList(): IAgoraUIUserList {
        return userList
    }
}