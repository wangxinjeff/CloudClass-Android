package io.agora.uikit.impl.whiteboard

import android.content.Context
import android.graphics.Rect
import android.view.*
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.RelativeLayout
import io.agora.uikit.impl.AbsComponent
import io.agora.uikit.R
import io.agora.uikit.interfaces.listeners.IAgoraUIWhiteboardListener

class AgoraUIWhiteBoard(
        context: Context,
        parent: ViewGroup,
        width: Int,
        height: Int,
        left: Float,
        top: Float,
        shadowWidth: Float) : AbsComponent(), BoardPreloadProgressJumpOverListener, BoardPreloadFailedListener {
    private val tag = "AgoraUIBoardWindow"

    private val contentView: View = LayoutInflater.from(context).inflate(R.layout.agora_board_layout, parent, false)
    private var rootLayout: RelativeLayout = contentView.findViewById(R.id.root_Layout)
    private var boardContainer: RelativeLayout = contentView.findViewById(R.id.whiteboard_container)
    private var boardLoadingView: AgoraUIBoardLoadingView = contentView.findViewById(R.id.whiteboard_loading_view)
    private var boardPreloadProgressView: AgoraUIBoardPreloadProgressView? = null
    private var boardPreloadFailedView: AgoraUIBoardPreloadFailedView? = null

    var whiteBoardListener: IAgoraUIWhiteboardListener? = null

    init {
        val w = if (width > 0) width else 1
        val h = if (height > 0) height else 1

        parent.addView(contentView, w, h)
        val params = contentView.layoutParams as ViewGroup.MarginLayoutParams
        params.topMargin = top.toInt()
        params.leftMargin = left.toInt()
        contentView.layoutParams = params

        boardLoadingView.z = shadowWidth + 100.0f
    }

    fun getBoardContainer(): ViewGroup {
        return boardContainer
    }

    fun setLoadingVisible(visible: Boolean) {
        boardLoadingView.visibility = if (visible) VISIBLE else GONE
    }

    fun setDownloadProgress(url: String, progress: Float) {
        if (boardPreloadProgressView == null) {
            boardPreloadProgressView = AgoraUIBoardPreloadProgressView.show(rootLayout, url, this)
            boardPreloadProgressView!!.z = boardLoadingView.z + 100.0f
        } else if (boardPreloadProgressView != null && boardPreloadProgressView!!.parent == null) {
            boardPreloadProgressView!!.show(rootLayout, url)
        }
        whiteBoardListener?.onBoardInputEnabled(false)
        boardPreloadProgressView!!.updateProgress(progress.toDouble())
    }

    fun setDownloadTimeOut(url: String) {
        boardPreloadProgressView?.timeout()
    }

    fun setDownloadComplete(url: String) {
        boardPreloadProgressView?.dismiss()
    }

    fun downloadError(url: String) {
        boardPreloadProgressView?.dismiss()
        if (boardPreloadFailedView == null) {
            boardPreloadFailedView = AgoraUIBoardPreloadFailedView.show(rootLayout, url, this)
            boardPreloadFailedView!!.z = boardPreloadProgressView!!.z + 100.0f
        } else {
            boardPreloadFailedView?.show(rootLayout, url)
        }
        whiteBoardListener?.onBoardInputEnabled(false)
    }

    fun cancelCurDownload() {
        if (boardPreloadProgressView?.isShowing() == true) {
            boardPreloadProgressView?.post {
                boardPreloadProgressView?.dismiss()
            }
        }
        if (boardPreloadFailedView?.isShowing() == true) {
            boardPreloadFailedView?.post {
                boardPreloadFailedView?.dismiss()
            }
        }
    }

    override fun onJumpOver(url: String?) {
        whiteBoardListener?.onDownloadSkipped(url)
    }

    override fun onProgressViewDismiss(url: String?) {
        whiteBoardListener?.onBoardInputEnabled(true)
    }

    override fun onClose(url: String?) {
        whiteBoardListener?.onDownloadCanceled(url)
    }

    override fun onRetry(url: String?) {
        whiteBoardListener?.onDownloadRetry(url)
    }

    override fun onFailedViewDismiss() {
        whiteBoardListener?.onBoardInputEnabled(true)
    }

    override fun setRect(rect: Rect) {
        contentView.post {
            val params = contentView.layoutParams as ViewGroup.MarginLayoutParams
            params.topMargin = rect.top
            params.leftMargin = rect.left
            params.width = rect.right - rect.left
            params.height = rect.bottom - rect.top
            contentView.layoutParams = params
        }
    }
}