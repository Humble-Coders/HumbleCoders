package com.humblecoders.humblecoders

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    courseTitle: String,
    sectionTitle: String,
    videos: List<SectionVideo>,
    onBackPressed: () -> Unit,
    userProfile: UserProfile? = null
) {
    var selectedVideo by remember { mutableStateOf<SectionVideo?>(null) }
    var isPlayerVisible by remember { mutableStateOf(false) }
    
    // Face verification state
    var attendanceManager by remember { 
        mutableStateOf<AttendanceManager?>(null) 
    }
    var attendanceState by remember { mutableStateOf(AttendanceUiState()) }
    var hasTriggeredVerification by remember { mutableStateOf(false) }
    var showFaceVerification by remember { mutableStateOf(false) }
    val context=LocalContext.current
    
    // Initialize attendance manager
    LaunchedEffect(Unit) {
        attendanceManager = AttendanceManager(
            faceRepository = FaceRegistrationRepository(),
            userRepository = UserRepository(),
            context = context
        )
    }
    
    // Observe attendance state
    LaunchedEffect(attendanceManager) {
        attendanceManager?.let { manager ->
            manager.uiState.collect { state ->
                attendanceState = state
                android.util.Log.d("VideoPlayerScreen", "Attendance state updated: isVerifying=${state.isVerifying}, attendanceMarked=${state.attendanceMarked}, message=${state.verificationMessage}")
            }
        }
    }
    
    // Log videos for debugging
    LaunchedEffect(videos) {
        android.util.Log.d("VideoPlayerScreen", "Received ${videos.size} videos for course: $courseTitle, section: $sectionTitle")
        videos.forEachIndexed { index, video ->
            android.util.Log.d("VideoPlayerScreen", "Video $index: title=${video.title}, videoUrls count=${video.videoUrls.size}")
            video.videoUrls.forEachIndexed { urlIndex, url ->
                android.util.Log.d("VideoPlayerScreen", "  URL $urlIndex: $url")
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = sectionTitle,
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
                            imageVector = Icons.Default.ArrowBack,
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
                    VideoPlayerCard(
                        video = selectedVideo!!,
                        onClosePlayer = {
                            isPlayerVisible = false
                            selectedVideo = null
                            hasTriggeredVerification = false
                        },
                        userProfile = userProfile,
                        attendanceManager = attendanceManager,
                        hasTriggeredVerification = hasTriggeredVerification,
                        onVerificationTriggered = { 
                            hasTriggeredVerification = true
                            showFaceVerification = true
                        }
                    )
                }
            }
            
            item {
                Text(
                    text = "Videos in this section",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
            }
            
            items(videos) { video ->
                VideoItemCard(
                    video = video,
                    onClick = {
                        selectedVideo = video
                        isPlayerVisible = true
                    }
                )
            }
        }
        
        // Automatic Face Verification Dialog
        AutomaticFaceVerification(
            isVisible = showFaceVerification,
            onFaceCaptured = { bitmap ->
                android.util.Log.d("VideoPlayerScreen", "Face captured, starting attendance verification")
                android.util.Log.d("VideoPlayerScreen", "User profile: $userProfile")
                android.util.Log.d("VideoPlayerScreen", "Selected video: $selectedVideo")
                android.util.Log.d("VideoPlayerScreen", "Attendance manager: $attendanceManager")
                
                userProfile?.let { profile ->
                    selectedVideo?.let { video ->
                        android.util.Log.d("VideoPlayerScreen", "Calling markAttendance for user: ${profile.uid}, course: $courseTitle, video: ${video.title}")
                        attendanceManager?.markAttendance(
                            userId = profile.uid,
                            courseTitle = courseTitle,
                            videoTitle = video.title,
                            faceImage = bitmap
                        )
                    } ?: run {
                        android.util.Log.e("VideoPlayerScreen", "Selected video is null")
                    }
                } ?: run {
                    android.util.Log.e("VideoPlayerScreen", "User profile is null")
                }
            },
            onDismiss = {
                showFaceVerification = false
                attendanceManager?.clearAttendanceState()
            },
            attendanceState = attendanceState
        )
    }
}

@Composable
fun VideoPlayerCard(
    video: SectionVideo,
    onClosePlayer: () -> Unit,
    userProfile: UserProfile? = null,
    attendanceManager: AttendanceManager? = null,
    hasTriggeredVerification: Boolean = false,
    onVerificationTriggered: () -> Unit = {}
) {
    var isVideoPlaying by remember { mutableStateOf(false) }
    
    // 10-second timer for automatic face verification
    LaunchedEffect(isVideoPlaying, hasTriggeredVerification) {
        if (isVideoPlaying && !hasTriggeredVerification) {
            kotlinx.coroutines.delay(10000) // 10 seconds
            if (isVideoPlaying && !hasTriggeredVerification) {
                onVerificationTriggered()
            }
        }
    }
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
            VideoPlayer(
                videoUrls = video.videoUrls,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                onPlayingStateChanged = { playing ->
                    isVideoPlaying = playing
                }
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
fun VideoItemCard(
    video: SectionVideo,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
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
            
            // Play Button
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play Video",
                tint = Color(0xFF4A90E2),
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
fun VideoPlayer(
    videoUrls: List<String>,
    modifier: Modifier = Modifier,
    onPlayingStateChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var playerView by remember { mutableStateOf<PlayerView?>(null) }
    
    LaunchedEffect(videoUrls) {
        android.util.Log.d("VideoPlayer", "Creating ExoPlayer for ${videoUrls.size} videos")
        exoPlayer?.release()
        exoPlayer = createExoPlayer(context, videoUrls, onPlayingStateChanged)
        playerView?.player = exoPlayer
    }
    
    DisposableEffect(Unit) {
        onDispose {
            android.util.Log.d("VideoPlayer", "Releasing ExoPlayer")
            exoPlayer?.release()
        }
    }
    
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                useController = true
                player = exoPlayer
                playerView = this
                android.util.Log.d("VideoPlayer", "PlayerView created and player set")
            }
        },
        modifier = modifier,
        update = { view ->
            view.player = exoPlayer
            android.util.Log.d("VideoPlayer", "PlayerView updated with player")
        }
    )
}

private fun createExoPlayer(context: Context, videoUrls: List<String>, onPlayingStateChanged: (Boolean) -> Unit): ExoPlayer {
    android.util.Log.d("VideoPlayer", "Creating ExoPlayer with ${videoUrls.size} video URLs")
    
    val exoPlayer = ExoPlayer.Builder(context).build()
    
    // Create MediaItems for each video URL
    val mediaItems = videoUrls.mapIndexed { index, url ->
        android.util.Log.d("VideoPlayer", "Adding video $index: $url")
        MediaItem.fromUri(Uri.parse(url))
    }
    
    // Set the playlist to play videos consecutively
    exoPlayer.setMediaItems(mediaItems)
    exoPlayer.prepare()
    exoPlayer.playWhenReady = true
    
    // Set repeat mode to not repeat the entire playlist
    exoPlayer.repeatMode = Player.REPEAT_MODE_OFF
    
    // Add player state listener for debugging and playing state tracking
    exoPlayer.addListener(object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            android.util.Log.d("VideoPlayer", "Playback state changed: $playbackState")
            val isPlaying = playbackState == Player.STATE_READY && exoPlayer.isPlaying
            onPlayingStateChanged(isPlaying)
        }
        
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            android.util.Log.d("VideoPlayer", "Is playing changed: $isPlaying")
            onPlayingStateChanged(isPlaying)
        }
        
        override fun onPlayerError(error: PlaybackException) {
            android.util.Log.e("VideoPlayer", "Player error: ${error.message}")
            onPlayingStateChanged(false)
        }
        
        override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
            android.util.Log.d("VideoPlayer", "Video size: ${videoSize.width}x${videoSize.height}")
        }
    })
    
    android.util.Log.d("VideoPlayer", "ExoPlayer created and prepared")
    return exoPlayer
}
