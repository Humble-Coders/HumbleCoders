package com.humblecoders.humblecoders

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import java.io.File
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import android.net.wifi.p2p.WifiP2pDevice

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupStudyScreen(
    courseTitle: String,
    sectionTitle: String,
    videos: List<SectionVideo>,
    onBackPressed: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var selectedVideo by remember { mutableStateOf<SectionVideo?>(null) }
    var isPlayerVisible by remember { mutableStateOf(false) }
    var downloadedParts by remember { mutableStateOf<Map<String, List<String>>>(emptyMap()) }
    var downloadProgress by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var isAutoDownloading by remember { mutableStateOf(false) }
    
    val downloadManager = remember { VideoDownloadManager(context) }
    val nearbyConnectionsManager = remember { NearbyConnectionsManager(context) }
    
    // Set up automatic download callback when connection is established
    LaunchedEffect(nearbyConnectionsManager) {
        nearbyConnectionsManager.setOnConnectionEstablishedCallback {
            android.util.Log.d("GroupStudyScreen", "Connection established - receiving shared videos from peer device")
            isAutoDownloading = true
            coroutineScope.launch {
                videos.forEach { video ->
                    android.util.Log.d("GroupStudyScreen", "Receiving shared video: ${video.title}")
                    video.videoUrls.forEachIndexed { partIndex, videoUrl ->
                        try {
                            val result = downloadManager.downloadVideoPart(
                                videoTitle = video.title,
                                partIndex = partIndex,
                                videoUrl = videoUrl
                            ) { progress ->
                                android.util.Log.d("GroupStudyScreen", "Receiving ${video.title} part $partIndex: $progress%")
                            }
                            
                            if (result.isSuccess) {
                                val downloadedFile = result.getOrNull()!!
                                android.util.Log.d("GroupStudyScreen", "Received shared file: ${video.title} part $partIndex")
                                
                                // Update downloaded parts immediately
                                val currentDownloads = downloadedParts[video.title] ?: emptyList()
                                val newDownloads = currentDownloads + downloadedFile
                                downloadedParts = downloadedParts + (video.title to newDownloads)
                            } else {
                                android.util.Log.e("GroupStudyScreen", "Failed to receive ${video.title} part $partIndex", result.exceptionOrNull())
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("GroupStudyScreen", "Error receiving ${video.title} part $partIndex", e)
                        }
                    }
                }
                android.util.Log.d("GroupStudyScreen", "All shared videos received successfully")
                isAutoDownloading = false
            }
        }
    }
    
    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        android.util.Log.d("GroupStudyScreen", "Permission request result: $permissions")
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            android.util.Log.d("GroupStudyScreen", "All permissions granted, starting automatic sharing")
            // Add a small delay to ensure permissions are fully processed
            coroutineScope.launch {
                kotlinx.coroutines.delay(500) // 500ms delay
                nearbyConnectionsManager.startAutomaticSharing()
            }
        } else {
            android.util.Log.e("GroupStudyScreen", "Some permissions were denied: ${permissions.filter { !it.value }}")
        }
    }
    
    val discoveredEndpoints by nearbyConnectionsManager.discoveredEndpoints.collectAsState()
    val connectedEndpoints by nearbyConnectionsManager.connectedEndpoints.collectAsState()
    val isAdvertising by nearbyConnectionsManager.isAdvertising.collectAsState()
    val isDiscovering by nearbyConnectionsManager.isDiscovering.collectAsState()
    val transferStatus by nearbyConnectionsManager.transferStatus.collectAsState()
    val receivedFiles by nearbyConnectionsManager.receivedFiles.collectAsState()
    
    // Refresh downloaded parts when new files are received
    LaunchedEffect(receivedFiles) {
        if (receivedFiles.isNotEmpty()) {
            android.util.Log.d("GroupStudyScreen", "New files received, refreshing downloaded parts")
            videos.forEach { video ->
                val existingDownloads = downloadManager.getDownloadedVideoParts(video.title)
                val receivedVideoParts = nearbyConnectionsManager.getReceivedVideoParts(video.title)
                val allVideoParts = (existingDownloads + receivedVideoParts).distinct()
                if (allVideoParts.isNotEmpty()) {
                    downloadedParts = downloadedParts + (video.title to allVideoParts)
                    android.util.Log.d("GroupStudyScreen", "Refreshed video ${video.title}: ${allVideoParts.size} total parts")
                }
            }
        }
    }
    
    var showDeviceList by remember { mutableStateOf(false) }
    var selectedVideoPart by remember { mutableStateOf<Pair<String, Int>?>(null) }
    
    // Load existing downloads and start P2P discovery
    LaunchedEffect(videos) {
        android.util.Log.d("GroupStudyScreen", "Received ${videos.size} videos for course: $courseTitle, section: $sectionTitle")
        videos.forEachIndexed { index, video ->
            android.util.Log.d("GroupStudyScreen", "Video $index: title=${video.title}, videoUrls count=${video.videoUrls.size}")
            
            // Load existing downloads for this video
            val existingDownloads = downloadManager.getDownloadedVideoParts(video.title)
            val receivedVideoParts = nearbyConnectionsManager.getReceivedVideoParts(video.title)
            
            // Merge downloaded and received files
            val allVideoParts = (existingDownloads + receivedVideoParts).distinct()
            if (allVideoParts.isNotEmpty()) {
                downloadedParts = downloadedParts + (video.title to allVideoParts)
                android.util.Log.d("GroupStudyScreen", "Video ${video.title}: ${existingDownloads.size} downloaded + ${receivedVideoParts.size} received = ${allVideoParts.size} total parts")
            }
        }
        
        // Check permissions before starting Nearby Connections
        android.util.Log.d("GroupStudyScreen", "Checking permissions for Nearby Connections")
        if (nearbyConnectionsManager.hasAllRequiredPermissions()) {
            android.util.Log.d("GroupStudyScreen", "All permissions already granted, starting automatic sharing")
            nearbyConnectionsManager.startAutomaticSharing()
        } else {
            android.util.Log.d("GroupStudyScreen", "Requesting permissions for Nearby Connections")
            val requiredPermissions = nearbyConnectionsManager.getRequiredPermissions()
            android.util.Log.d("GroupStudyScreen", "Requesting permissions: ${requiredPermissions.joinToString()}")
            permissionLauncher.launch(requiredPermissions)
        }
    }
    
    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            nearbyConnectionsManager.stopDiscovery()
            nearbyConnectionsManager.stopAdvertising()
            nearbyConnectionsManager.cleanup()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Group Study - $sectionTitle",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = courseTitle,
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2C3E50)
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF5F5F5))
        ) {
            if (isPlayerVisible && selectedVideo != null) {
                item {
                    GroupStudyVideoPlayerCard(
                        video = selectedVideo!!,
                        downloadedParts = downloadedParts,
                        onClosePlayer = {
                            isPlayerVisible = false
                            selectedVideo = null
                        }
                    )
                }
            }
            
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select a video for group study",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Button(
                        onClick = {
                            android.util.Log.d("GroupStudyScreen", "Manual refresh triggered")
                            videos.forEach { video ->
                                val existingDownloads = downloadManager.getDownloadedVideoParts(video.title)
                                val receivedVideoParts = nearbyConnectionsManager.getReceivedVideoParts(video.title)
                                val allVideoParts = (existingDownloads + receivedVideoParts).distinct()
                                if (allVideoParts.isNotEmpty()) {
                                    downloadedParts = downloadedParts + (video.title to allVideoParts)
                                    android.util.Log.d("GroupStudyScreen", "Manual refresh - Video ${video.title}: ${allVideoParts.size} total parts")
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF7F8C8D)
                        )
                    ) {
                        Text("Sync", color = Color.White, fontSize = 12.sp)
                    }
                }
            }
            
            // Nearby Connections Status
            item {
                NearbyConnectionsStatusCard(
                    isAdvertising = isAdvertising,
                    isDiscovering = isDiscovering,
                    discoveredEndpoints = discoveredEndpoints,
                    connectedEndpoints = connectedEndpoints,
                    transferStatus = transferStatus,
                    isAutoDownloading = isAutoDownloading,
                    onShowDevices = { showDeviceList = true }
                )
            }
            
            // Device List Dialog
            if (showDeviceList) {
                item {
                    EndpointListDialog(
                        endpoints = discoveredEndpoints,
                        onEndpointSelected = { endpoint ->
                            if (selectedVideoPart != null) {
                                val (videoTitle, partIndex) = selectedVideoPart!!
                                val filePath = downloadManager.getDownloadedVideoPartFile(videoTitle, partIndex).absolutePath
                                nearbyConnectionsManager.connectToEndpoint(endpoint)
                                // Send file after connection is established
                                nearbyConnectionsManager.sendFile(filePath, endpoint)
                                selectedVideoPart = null
                            }
                            showDeviceList = false
                        },
                        onDismiss = { 
                            showDeviceList = false
                            selectedVideoPart = null
                        }
                    )
                }
            }
            
            items(videos) { video ->
                GroupStudyVideoItemCard(
                    video = video,
                    downloadedParts = downloadedParts,
                    downloadProgress = downloadProgress,
                    onVideoClick = {
                        val allPartsDownloaded = (downloadedParts[video.title]?.size ?: 0) >= video.videoUrls.size
                        if (allPartsDownloaded) {
                            selectedVideo = video
                            isPlayerVisible = true
                        } else {
                            android.util.Log.d("GroupStudyScreen", "Cannot play video - not all parts downloaded")
                        }
                    },
                    onDownloadPart = { partIndex, videoTitle ->
                        coroutineScope.launch {
                            val progressKey = "$videoTitle-$partIndex"
                            downloadProgress = downloadProgress + (progressKey to 0)
                            
                            val result = downloadManager.downloadVideoPart(
                                videoTitle = videoTitle,
                                partIndex = partIndex,
                                videoUrl = video.videoUrls[partIndex]
                            ) { progress ->
                                downloadProgress = downloadProgress + (progressKey to progress)
                            }
                            
                            downloadProgress = downloadProgress - progressKey
                            
                            if (result.isSuccess) {
                                // Update downloaded parts
                                val currentDownloads = downloadedParts[videoTitle] ?: emptyList()
                                val newDownloads = currentDownloads + result.getOrNull()!!
                                downloadedParts = downloadedParts + (videoTitle to newDownloads)
                                android.util.Log.d("GroupStudyScreen", "Download completed for $videoTitle part $partIndex")
                            } else {
                                android.util.Log.e("GroupStudyScreen", "Download failed for $videoTitle part $partIndex", result.exceptionOrNull())
                            }
                        }
                    },
                    onSharePart = { partIndex, videoTitle ->
                        // Store selected video part and show device selection
                        selectedVideoPart = Pair(videoTitle, partIndex)
                        showDeviceList = true
                    }
                )
            }
        }
    }
}

@Composable
fun GroupStudyVideoPlayerCard(
    video: SectionVideo,
    downloadedParts: Map<String, List<String>>,
    onClosePlayer: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp
        )
    ) {
        Column {
            // Video Player
            GroupStudyVideoPlayer(
                video = video,
                downloadedParts = downloadedParts,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
            
            // Video Info
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = video.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C3E50)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = video.description,
                    fontSize = 14.sp,
                    color = Color(0xFF666666),
                    lineHeight = 20.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = onClosePlayer,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE74C3C)
                    )
                ) {
                    Text("Close Player", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun GroupStudyVideoItemCard(
    video: SectionVideo,
    downloadedParts: Map<String, List<String>>,
    downloadProgress: Map<String, Int>,
    onVideoClick: () -> Unit,
    onDownloadPart: (Int, String) -> Unit,
    onSharePart: (Int, String) -> Unit
) {
    val downloadedVideoParts = downloadedParts[video.title] ?: emptyList()
    val allPartsDownloaded = downloadedVideoParts.size >= video.videoUrls.size
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onVideoClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Video Thumbnail
                AsyncImage(
                    model = video.thumbnailUrl,
                    contentDescription = "Video Thumbnail",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Video Info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = video.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50)
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = video.description,
                        fontSize = 14.sp,
                        color = Color(0xFF666666),
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                
            // Play Button - only show if all parts are downloaded
            if (allPartsDownloaded) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play Video",
                    tint = Color(0xFF27AE60),
                    modifier = Modifier.size(32.dp)
                )
            } else {
                Text(
                    text = "${downloadedVideoParts.size}/${video.videoUrls.size}",
                    fontSize = 12.sp,
                    color = Color(0xFF7F8C8D),
                    modifier = Modifier.padding(8.dp)
                )
            }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Video Parts Section
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Video Parts (${video.videoUrls.size} parts)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C3E50)
                )
                
                if (allPartsDownloaded) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "✓ Ready to Play",
                        fontSize = 12.sp,
                        color = Color(0xFF27AE60),
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "(${downloadedVideoParts.size}/${video.videoUrls.size} received)",
                        fontSize = 12.sp,
                        color = Color(0xFF7F8C8D)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Parts Grid
            video.videoUrls.forEachIndexed { index, url ->
                val progressKey = "${video.title}-$index"
                val progress = downloadProgress[progressKey] ?: 0
                
                VideoPartCard(
                    partIndex = index + 1,
                    url = url,
                    isDownloaded = downloadedVideoParts.size > index,
                    downloadProgress = progress,
                    onDownload = { onDownloadPart(index, video.title) },
                    onShare = { onSharePart(index, video.title) }
                )
                
                if (index < video.videoUrls.size - 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun VideoPartCard(
    partIndex: Int,
    url: String, // @Suppress("UNUSED_PARAMETER") - kept for future use
    isDownloaded: Boolean,
    downloadProgress: Int = 0,
    onDownload: () -> Unit,
    onShare: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDownloaded) Color(0xFFE8F5E8) else Color(0xFFF8F9FA)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Part $partIndex",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C3E50)
                )
                
                Text(
                    text = when {
                        isDownloaded -> "Received ✓"
                        downloadProgress > 0 -> "Receiving... $downloadProgress%"
                        else -> "Not received"
                    },
                    fontSize = 12.sp,
                    color = when {
                        isDownloaded -> Color(0xFF27AE60)
                        downloadProgress > 0 -> Color(0xFF4A90E2)
                        else -> Color(0xFF7F8C8D)
                    }
                )
                
                if (downloadProgress > 0 && !isDownloaded) {
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { downloadProgress / 100f },
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF4A90E2),
                        trackColor = Color(0xFFE0E0E0)
                    )
                }
            }
            
            if (isDownloaded) {
                IconButton(
                    onClick = onShare,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share Part $partIndex",
                        tint = Color(0xFF4A90E2)
                    )
                }
            } else {
                IconButton(
                    onClick = onDownload,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Request Part $partIndex",
                        tint = Color(0xFFE74C3C)
                    )
                }
            }
        }
    }
}

@Composable
fun GroupStudyVideoPlayer(
    video: SectionVideo,
    downloadedParts: Map<String, List<String>>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var playerView by remember { mutableStateOf<PlayerView?>(null) }
    
    val downloadedVideoParts = downloadedParts[video.title] ?: emptyList()
    
    // In group study mode, only use downloaded parts - never use internet URLs
    if (downloadedVideoParts.isEmpty()) {
        // Show message that video cannot be played without downloaded parts
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Video not available",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C3E50)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Download all parts to play this video",
                    fontSize = 14.sp,
                    color = Color(0xFF7F8C8D),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        return
    }
    
    val videoUrls = downloadedVideoParts
    
    LaunchedEffect(videoUrls) {
        android.util.Log.d("GroupStudyVideoPlayer", "Creating ExoPlayer for ${videoUrls.size} videos (downloaded: ${downloadedVideoParts.isNotEmpty()})")
        exoPlayer?.release()
        exoPlayer = createGroupStudyExoPlayer(context, videoUrls)
        playerView?.player = exoPlayer
    }
    
    DisposableEffect(Unit) {
        onDispose {
            android.util.Log.d("GroupStudyVideoPlayer", "Releasing ExoPlayer")
            exoPlayer?.release()
        }
    }
    
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                useController = true
                player = exoPlayer
                playerView = this
                android.util.Log.d("GroupStudyVideoPlayer", "PlayerView created and player set")
            }
        },
        modifier = modifier,
        update = { view ->
            view.player = exoPlayer
            android.util.Log.d("GroupStudyVideoPlayer", "PlayerView updated with player")
        }
    )
}

private fun createGroupStudyExoPlayer(context: Context, videoUrls: List<String>): ExoPlayer {
    android.util.Log.d("GroupStudyVideoPlayer", "Creating ExoPlayer with ${videoUrls.size} local video files")
    
    val exoPlayer = ExoPlayer.Builder(context).build()
    
    // Create MediaItems for each local video file
    val mediaItems = videoUrls.mapIndexed { index, filePath ->
        android.util.Log.d("GroupStudyVideoPlayer", "Adding local video $index: $filePath")
        val file = File(filePath)
        if (file.exists()) {
            MediaItem.fromUri(Uri.fromFile(file))
        } else {
            android.util.Log.e("GroupStudyVideoPlayer", "Local file does not exist: $filePath")
            MediaItem.fromUri(Uri.EMPTY)
        }
    }.filter { it.localConfiguration?.uri != Uri.EMPTY }
    
    if (mediaItems.isEmpty()) {
        android.util.Log.e("GroupStudyVideoPlayer", "No valid local video files found")
        return exoPlayer
    }
    
    // Set the playlist to play videos consecutively
    exoPlayer.setMediaItems(mediaItems)
    exoPlayer.prepare()
    exoPlayer.playWhenReady = true
    
    // Set repeat mode to not repeat the entire playlist
    exoPlayer.repeatMode = Player.REPEAT_MODE_OFF
    
    // Add player state listener for debugging
    exoPlayer.addListener(object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            android.util.Log.d("GroupStudyVideoPlayer", "Playback state changed: $playbackState")
        }
        
        override fun onPlayerError(error: PlaybackException) {
            android.util.Log.e("GroupStudyVideoPlayer", "Player error: ${error.message}")
        }
        
        override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
            android.util.Log.d("GroupStudyVideoPlayer", "Video size: ${videoSize.width}x${videoSize.height}")
        }
    })
    
    android.util.Log.d("GroupStudyVideoPlayer", "ExoPlayer created and prepared with ${mediaItems.size} local files")
    return exoPlayer
}

@Composable
fun NearbyConnectionsStatusCard(
    isAdvertising: Boolean,
    isDiscovering: Boolean,
    discoveredEndpoints: List<com.humblecoders.humblecoders.NearbyConnectionsManager.Endpoint>,
    connectedEndpoints: List<com.humblecoders.humblecoders.NearbyConnectionsManager.Endpoint>,
    transferStatus: com.humblecoders.humblecoders.NearbyConnectionsManager.TransferStatus,
    isAutoDownloading: Boolean,
    onShowDevices: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (connectedEndpoints.isNotEmpty()) Color(0xFFE8F5E8) else Color(0xFFF8F9FA)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Nearby Sharing",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C3E50)
                )
                
                Text(
                    text = when {
                        connectedEndpoints.isNotEmpty() -> "Connected (${connectedEndpoints.size})"
                        isDiscovering -> "Discovering..."
                        isAdvertising -> "Advertising..."
                        else -> "Starting..."
                    },
                    fontSize = 12.sp,
                    color = when {
                        connectedEndpoints.isNotEmpty() -> Color(0xFF27AE60)
                        isDiscovering || isAdvertising -> Color(0xFF4A90E2)
                        else -> Color(0xFF7F8C8D)
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Found ${discoveredEndpoints.size} devices nearby",
                fontSize = 14.sp,
                color = Color(0xFF666666)
            )
            
            if (connectedEndpoints.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "✓ Connected to ${connectedEndpoints.size} device(s) - sharing enabled",
                    fontSize = 12.sp,
                    color = Color(0xFF27AE60),
                    fontWeight = FontWeight.Bold
                )
            }
            
            if (isAutoDownloading) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        color = Color(0xFF27AE60),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Receiving shared videos...",
                        fontSize = 12.sp,
                        color = Color(0xFF27AE60),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            if (transferStatus.isTransferring || transferStatus.isReceiving) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (transferStatus.isTransferring) "Sending: ${transferStatus.fileName}" else "Receiving: ${transferStatus.fileName}",
                    fontSize = 12.sp,
                    color = Color(0xFF4A90E2)
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { transferStatus.progress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF4A90E2),
                    trackColor = Color(0xFFE0E0E0)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Button(
                onClick = onShowDevices,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4A90E2)
                )
            ) {
                Text("Share Video Part", color = Color.White)
            }
        }
    }
}

@Composable
fun EndpointListDialog(
    endpoints: List<com.humblecoders.humblecoders.NearbyConnectionsManager.Endpoint>,
    onEndpointSelected: (com.humblecoders.humblecoders.NearbyConnectionsManager.Endpoint) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Select Device to Share With")
        },
        text = {
            LazyColumn {
                items(endpoints) { endpoint: com.humblecoders.humblecoders.NearbyConnectionsManager.Endpoint ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onEndpointSelected(endpoint) },
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFF8F9FA)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = endpoint.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2C3E50)
                            )
                            
                            Spacer(modifier = Modifier.weight(1f))
                            
                            Text(
                                text = "Available",
                                fontSize = 12.sp,
                                color = Color(0xFF27AE60)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
