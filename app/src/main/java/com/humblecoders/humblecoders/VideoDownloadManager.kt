package com.humblecoders.humblecoders

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.nio.channels.Channels

class VideoDownloadManager(private val context: Context) {
    
    private val downloadsDir = File(context.filesDir, "downloaded_videos")
    
    init {
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }
    }
    
    suspend fun downloadVideoPart(
        videoTitle: String,
        partIndex: Int,
        videoUrl: String,
        onProgress: (Int) -> Unit = {}
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d("VideoDownloadManager", "Starting download for $videoTitle part $partIndex")
            
            val fileName = "${videoTitle.replace(" ", "_")}_part_${partIndex}.mp4"
            val file = File(downloadsDir, fileName)
            
            // Create video title directory if it doesn't exist
            val videoDir = File(downloadsDir, videoTitle.replace(" ", "_"))
            if (!videoDir.exists()) {
                videoDir.mkdirs()
            }
            
            val finalFile = File(videoDir, "part_${partIndex}.mp4")
            
            val url = URL(videoUrl)
            val connection = url.openConnection()
            val contentLength = connection.contentLength
            
            Log.d("VideoDownloadManager", "Content length: $contentLength bytes")
            
            connection.getInputStream().use { inputStream ->
                FileOutputStream(finalFile).use { outputStream ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalBytesRead = 0
                    
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead
                        
                        if (contentLength > 0) {
                            val progress = (totalBytesRead * 100 / contentLength).toInt()
                            onProgress(progress)
                        }
                    }
                }
            }
            
            Log.d("VideoDownloadManager", "Download completed: ${finalFile.absolutePath}")
            Result.success(finalFile.absolutePath)
            
        } catch (e: Exception) {
            Log.e("VideoDownloadManager", "Download failed for $videoTitle part $partIndex", e)
            Result.failure(e)
        }
    }
    
    fun getDownloadedVideoParts(videoTitle: String): List<String> {
        val videoDir = File(downloadsDir, videoTitle.replace(" ", "_"))
        if (!videoDir.exists()) return emptyList()
        
        return videoDir.listFiles()
            ?.filter { it.isFile && it.extension == "mp4" }
            ?.sortedBy { it.name }
            ?.map { it.absolutePath }
            ?: emptyList()
    }
    
    fun isVideoPartDownloaded(videoTitle: String, partIndex: Int): Boolean {
        val videoDir = File(downloadsDir, videoTitle.replace(" ", "_"))
        val partFile = File(videoDir, "part_${partIndex}.mp4")
        return partFile.exists()
    }
    
    fun deleteVideoPart(videoTitle: String, partIndex: Int): Boolean {
        val videoDir = File(downloadsDir, videoTitle.replace(" ", "_"))
        val partFile = File(videoDir, "part_${partIndex}.mp4")
        return if (partFile.exists()) {
            partFile.delete()
        } else {
            false
        }
    }
    
    fun clearAllDownloads() {
        if (downloadsDir.exists()) {
            downloadsDir.deleteRecursively()
            downloadsDir.mkdirs()
        }
    }
    
    fun getDownloadedVideoUri(videoTitle: String, partIndex: Int): Uri? {
        val videoDir = File(downloadsDir, videoTitle.replace(" ", "_"))
        val partFile = File(videoDir, "part_${partIndex}.mp4")
        return if (partFile.exists()) {
            Uri.fromFile(partFile)
        } else {
            null
        }
    }
    
    fun getDownloadedVideoPartFile(videoTitle: String, partIndex: Int): File {
        val videoDir = File(downloadsDir, videoTitle.replace(" ", "_"))
        return File(videoDir, "part_${partIndex}.mp4")
    }
}
