package io.agora.uikit.interfaces.listeners

import android.view.View
import io.agora.uikit.impl.tool.AgoraUIApplianceType

interface IAgoraUIWhiteboardListener {
    // Whiteboard drawing config callbacks
    fun onApplianceSelected(type: AgoraUIApplianceType)

    fun onColorSelected(color: Int)

    fun onFontSizeSelected(size: Int)

    fun onThicknessSelected(thick: Int)

    fun onRosterSelected(anchor: View)

    // Whiteboard pre-load callbacks
    fun onBoardInputEnabled(enabled: Boolean)

    fun onDownloadSkipped(url: String?)

    fun onDownloadCanceled(url: String?)

    fun onDownloadRetry(url: String?)

    // Whiteboard page control callbacks
    fun onBoardFullScreen(full: Boolean)

    fun onBoardZoomOut()

    fun onBoardZoomIn()

    fun onBoardPrevPage()

    fun onBoardNextPage()
}