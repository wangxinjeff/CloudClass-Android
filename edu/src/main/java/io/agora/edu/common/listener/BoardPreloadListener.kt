package io.agora.edu.common.listener

interface BoardPreloadListener {
    fun onStartDownload(url: String)
    fun onProgress(url: String, progress: Double)
    fun onComplete(url: String)
    fun onFailed(url: String)
}