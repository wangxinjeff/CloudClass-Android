package io.agora.uikit.interfaces.protocols

import android.graphics.Color
import android.view.ViewGroup
import io.agora.uikit.impl.tool.AgoraUIApplianceType

data class AgoraUIDrawingConfig(
        var activeAppliance: AgoraUIApplianceType = AgoraUIApplianceType.Select,
        var color: Int = Color.WHITE,
        var fontSize: Int = 22,
        var thick: Int = 0) {

    fun set(config: AgoraUIDrawingConfig) {
        this.activeAppliance = config.activeAppliance
        this.color = config.color
        this.fontSize = config.fontSize
        this.thick = config.thick
    }
}

interface IAgoraUIWhiteboard {
    /**
     * Set the whiteboard drawing configurations, like current
     * pencil shape, color, or font size.
     */
    fun setDrawingConfig(config: AgoraUIDrawingConfig)

    /**
     * Set if the change of of drawing configuration is
     * enabled
     */
    fun setDrawingEnabled(enabled: Boolean)

    /**
     * return the container layout to hold the whiteboard UI
     */
    fun getContainer(): ViewGroup?

    fun setPageNo(no: Int, count: Int)

    /**
     * Set if paging and toolbar is enabled
     */
    fun setPagingEnabled(enabled: Boolean)

    /**
     * Set if zoom is enabled
     */
    fun setZoomEnable(zoomOutEnable: Boolean?, zoomInEnable: Boolean?)

    /**
     * Set if setting full screen is enabled
     */
    fun setFullScreenEnable(enabled: Boolean)

    /**
     * Set current full screen state
     */
    fun setIsFullScreen(fullScreen: Boolean)

    /**
     * Set if the entire page control is enabled, including paging,
     * zooming and resizing (full screen)
     * @param enabled
     */
    fun setInteractionEnabled(enabled: Boolean)

    /**
     * Called when whiteboard is in loading state
     * */
    fun setLoadingVisible(visible: Boolean)

    fun setDownloadProgress(url: String, progress: Float)

    fun setDownloadTimeout(url: String)

    fun setDownloadCompleted(url: String)

    fun downloadError(url: String)

    /**
     * Called when the current download task is canceled
     * */
    fun cancelCurDownload()

    /**
     * Called when whiteboard authorization status changes
     * */
    fun showPermissionTips(granted: Boolean)
}