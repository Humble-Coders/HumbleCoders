package com.humblecoders.humblecoders

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File

class NearbyShareManager(private val context: Context) {
    
    fun shareVideoPart(videoTitle: String, partIndex: Int, downloadManager: VideoDownloadManager) {
        try {
            val videoDir = File(context.filesDir, "downloaded_videos/${videoTitle.replace(" ", "_")}")
            val partFile = File(videoDir, "part_${partIndex}.mp4")
            
            if (!partFile.exists()) {
                Log.e("NearbyShareManager", "Video part file does not exist: ${partFile.absolutePath}")
                return
            }
            
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                partFile
            )
            
            // Use Android's built-in Nearby Share intent
            val nearbyIntent = Intent().apply {
                action = "com.google.android.gms.nearby.sharing.SHARE"
                type = "video/mp4"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "$videoTitle - Part $partIndex")
                putExtra(Intent.EXTRA_TEXT, "Video part $partIndex of $videoTitle")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            // Check if Nearby Share is available
            val nearbyResolveInfo = context.packageManager.resolveActivity(nearbyIntent, 0)
            if (nearbyResolveInfo != null) {
                Log.d("NearbyShareManager", "Using Nearby Share for $videoTitle part $partIndex")
                context.startActivity(nearbyIntent)
            } else {
                Log.d("NearbyShareManager", "Nearby Share not available, using regular share")
                shareViaRegularIntent(videoTitle, partIndex, uri)
            }
            
        } catch (e: Exception) {
            Log.e("NearbyShareManager", "Failed to share video part", e)
        }
    }
    
    private fun shareViaRegularIntent(videoTitle: String, partIndex: Int, uri: Uri) {
        try {
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "video/mp4"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "$videoTitle - Part $partIndex")
                putExtra(Intent.EXTRA_TEXT, "Video part $partIndex of $videoTitle")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            Log.d("NearbyShareManager", "Using regular share for $videoTitle part $partIndex")
            context.startActivity(Intent.createChooser(shareIntent, "Share Video Part"))
            
        } catch (e: Exception) {
            Log.e("NearbyShareManager", "Failed to share via regular intent", e)
        }
    }
    
    fun shareVideoPartViaNearby(videoTitle: String, partIndex: Int, downloadManager: VideoDownloadManager) {
        shareVideoPart(videoTitle, partIndex, downloadManager)
    }
    
    fun isNearbyShareAvailable(): Boolean {
        return try {
            // Check if Nearby Share is available by checking if the intent can be resolved
            val nearbyIntent = Intent().apply {
                action = "com.google.android.gms.nearby.sharing.SHARE"
                type = "video/mp4"
            }
            val resolveInfo = context.packageManager.resolveActivity(nearbyIntent, 0)
            resolveInfo != null
        } catch (e: Exception) {
            Log.e("NearbyShareManager", "Nearby Share not available", e)
            false
        }
    }
}
