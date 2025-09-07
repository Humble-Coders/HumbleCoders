package com.humblecoders.humblecoders

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailScreen(
    courseDetails: CourseDetails,
    userProfile: UserProfile?,
    courseViewModel: CourseViewModel,
    onBackPressed: () -> Unit,
    onGroupStudy: () -> Unit = {},
    onBuyNow: () -> Unit = {},
    onRateCourse: () -> Unit = {},
    onCreateSchedule: () -> Unit = {},
    onWatchVideos: (String, List<SectionVideo>) -> Unit = { _, _ -> }
) {
    // Add debugging to see when courseDetails changes
    LaunchedEffect(courseDetails.rating) {
        android.util.Log.d("CourseDetailScreen", "Course rating updated: ${courseDetails.rating}")
    }
    var showScheduleDialog by remember { mutableStateOf(false) }
    val scheduleViewModel = remember { ScheduleViewModel() }
    val scheduleUiState by scheduleViewModel.uiState.collectAsState()
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Course Details",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
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
        },
        bottomBar = {
            CourseActionButtons(
                courseDetails = courseDetails,
                userProfile = userProfile,
                courseViewModel = courseViewModel,
                onGroupStudy = onGroupStudy,
                onBuyNow = onBuyNow,
                onRateCourse = onRateCourse,
                onCreateSchedule = { showScheduleDialog = true },
                onWatchVideos = onWatchVideos
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // Course Thumbnail
            item {
                CourseThumbnailSection(courseDetails = courseDetails)
            }

            // Course Info
            item {
                CourseInfoSection(courseDetails = courseDetails)
            }

            // Course Description
            item {
                CourseDescriptionSection(courseDetails = courseDetails)
            }

            // Course Sections
            items(courseDetails.sections) { section ->
                CourseSectionItem(section = section)
            }
        }
    }
    
    // Schedule Input Dialog
    ScheduleInputDialog(
        isVisible = showScheduleDialog,
        onDismiss = { showScheduleDialog = false },
            onGenerateSchedule = { timeframe ->
                android.util.Log.d("CourseDetailScreen", "Generating schedule for timeframe: $timeframe")
                android.util.Log.d("CourseDetailScreen", "User profile: $userProfile")
                android.util.Log.d("CourseDetailScreen", "User email: ${userProfile?.email}")
                userProfile?.let { profile ->
                    android.util.Log.d("CourseDetailScreen", "User profile found: ${profile.uid}, email: ${profile.email}")
                    scheduleViewModel.generateSchedule(
                        courseDetails = courseDetails,
                        userTimeframe = timeframe,
                        userId = profile.uid
                    )
                } ?: run {
                    android.util.Log.e("CourseDetailScreen", "User profile is null")
                }
                showScheduleDialog = false
            },
        courseTitle = courseDetails.title
    )
    
    // Schedule Generation Loading Dialog
    if (scheduleUiState.isGenerating) {
        Dialog(
            onDismissRequest = { /* Prevent dismissal during generation */ },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF4A90E2),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Generating your personalized study schedule...",
                        fontSize = 16.sp,
                        color = Color(0xFF2C3E50),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This may take a few moments",
                        fontSize = 14.sp,
                        color = Color(0xFF666666),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
    
    // Schedule Display Dialog
    if (scheduleUiState.generatedSchedule != null) {
        Dialog(
            onDismissRequest = { scheduleViewModel.clearGeneratedSchedule() },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            ScheduleDisplayDialog(
                schedule = scheduleUiState.generatedSchedule!!,
                onDismiss = { scheduleViewModel.clearGeneratedSchedule() }
            )
        }
    }
    
    // Schedule Error Dialog
    if (scheduleUiState.error != null) {
        AlertDialog(
            onDismissRequest = { scheduleViewModel.clearError() },
            title = {
                Text("Schedule Generation Failed")
            },
            text = {
                Text(scheduleUiState.error ?: "Unknown error occurred")
            },
            confirmButton = {
                TextButton(
                    onClick = { scheduleViewModel.clearError() }
                ) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun ScheduleDisplayDialog(
    schedule: StudySchedule,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.9f)
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Your Study Schedule",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50)
                    )
                    Text(
                        text = schedule.courseTitle,
                        fontSize = 14.sp,
                        color = Color(0xFF666666)
                    )
                }
                
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF666666)
                    )
                }
            }
            
            Divider(color = Color(0xFFE0E0E0))
            
            // Schedule Overview
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ScheduleInfoChip(
                    icon = Icons.Default.Schedule,
                    title = "Duration",
                    value = schedule.totalDuration,
                    color = Color(0xFF4A90E2)
                )
                
                ScheduleInfoChip(
                    icon = Icons.Default.Timer,
                    title = "Hours/Week",
                    value = "${schedule.estimatedHoursPerWeek} hrs",
                    color = Color(0xFF27AE60)
                )
                
                ScheduleInfoChip(
                    icon = Icons.Default.School,
                    title = "Level",
                    value = schedule.difficultyLevel,
                    color = Color(0xFFFF9800)
                )
            }
            
            Divider(color = Color(0xFFE0E0E0))
            
            // Weekly Schedule
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(schedule.schedule) { weekSchedule ->
                    WeekScheduleCard(weekSchedule = weekSchedule)
                }
            }
        }
    }
}

@Composable
fun ScheduleInfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    color: Color
) {
    Card(
        modifier = Modifier.padding(4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = title,
                fontSize = 10.sp,
                color = Color(0xFF666666)
            )
            
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2C3E50)
            )
        }
    }
}



@Composable
fun CourseThumbnailSection(courseDetails: CourseDetails) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
    ) {
        // Course Thumbnail
        if (courseDetails.thumbnailUrl.isNotEmpty()) {
            AsyncImage(
                model = courseDetails.thumbnailUrl,
                contentDescription = courseDetails.title,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF5F5F5)),
                contentScale = ContentScale.Crop,
                onSuccess = { 
                    android.util.Log.d("CourseDetailScreen", "Successfully loaded course thumbnail: ${courseDetails.thumbnailUrl}")
                },
                onError = { error ->
                    android.util.Log.e("CourseDetailScreen", "Failed to load course thumbnail: ${courseDetails.thumbnailUrl}", error.result.throwable)
                }
            )
        } else {
            // Fallback when no thumbnail URL
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF5F5F5)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = "Course",
                    tint = Color(0xFF666666),
                    modifier = Modifier.size(64.dp)
                )
            }
        }

        // Gradient Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.7f)
                        )
                    )
                )
        )

        // Course Title Overlay
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Text(
                text = courseDetails.title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                lineHeight = 28.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Difficulty: ${courseDetails.difficultyLevel}",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
fun CourseInfoSection(courseDetails: CourseDetails) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Difficulty Level
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = "Difficulty",
                        tint = Color(0xFF4A90E2),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = courseDetails.difficultyLevel,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2C3E50)
                    )
                }

                // Total Sections
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = "Sections",
                        tint = Color(0xFF666666),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${courseDetails.sections.size} sections",
                        fontSize = 14.sp,
                        color = Color(0xFF666666)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Rating
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Rating",
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Rating: ${String.format("%.1f", courseDetails.rating)}/5.0",
                    fontSize = 14.sp,
                    color = Color(0xFF666666)
                )
                Spacer(modifier = Modifier.width(8.dp))
                // Visual star rating
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    for (i in 1..5) {
                        Icon(
                            imageVector = if (i <= courseDetails.rating) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Star $i",
                            tint = if (i <= courseDetails.rating) Color(0xFFFFD700) else Color(0xFFCCCCCC),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
            

            Spacer(modifier = Modifier.height(12.dp))

            // Total Videos
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.PlayCircleOutline,
                    contentDescription = "Videos",
                    tint = Color(0xFF666666),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Total Videos: ${courseDetails.sections.sumOf { it.videos.size }}",
                    fontSize = 14.sp,
                    color = Color(0xFF666666)
                )
            }
        }
    }
}

@Composable
fun CourseDescriptionSection(courseDetails: CourseDetails) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "About This Course",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2C3E50)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = courseDetails.description.ifEmpty { "Learn ${courseDetails.title} from scratch with this comprehensive course. Perfect for beginners and intermediate learners who want to master the fundamentals and advance their skills." },
                fontSize = 14.sp,
                color = Color(0xFF666666),
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun CourseSectionItem(section: CourseSection) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = section.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2C3E50)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = section.description,
                fontSize = 14.sp,
                color = Color(0xFF666666),
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            // Videos in this section
            Text(
                text = "Videos (${section.videos.size})",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF2C3E50)
            )
            Spacer(modifier = Modifier.height(8.dp))

            section.videos.forEachIndexed { index, video ->
                VideoItem(
                    video = video,
                    index = index + 1
                )
                if (index < section.videos.size - 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun VideoItem(
    video: SectionVideo,
    index: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF8F9FA)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Video Thumbnail
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFE0E0E0)),
                contentAlignment = Alignment.Center
            ) {
                if (video.thumbnailUrl.isNotEmpty()) {
                    AsyncImage(
                        model = video.thumbnailUrl,
                        contentDescription = video.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        onSuccess = { 
                            android.util.Log.d("CourseDetailScreen", "Successfully loaded video thumbnail: ${video.thumbnailUrl}")
                        },
                        onError = { error ->
                            android.util.Log.e("CourseDetailScreen", "Failed to load video thumbnail: ${video.thumbnailUrl}", error.result.throwable)
                        }
                    )
                }
                
                // Play Icon Overlay
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            Color.Black.copy(alpha = 0.7f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Video Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "$index. ${video.title}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF2C3E50),
                    maxLines = 2,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = video.description,
                    fontSize = 12.sp,
                    color = Color(0xFF666666),
                    maxLines = 2,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun CourseActionButtons(
    courseDetails: CourseDetails,
    userProfile: UserProfile?,
    courseViewModel: CourseViewModel,
    onGroupStudy: () -> Unit,
    onBuyNow: () -> Unit,
    onRateCourse: () -> Unit,
    onCreateSchedule: () -> Unit,
    onWatchVideos: (String, List<SectionVideo>) -> Unit
) {
    val isEnrolled = userProfile?.enrolledCourses?.contains(courseDetails.title) ?: false
    
    var showRatingDialog by remember { mutableStateOf(false) }
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
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            if (isEnrolled) {
                // Enrolled state - show success message
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF27AE60)
                    ),
                    shape = RoundedCornerShape(25.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Enrolled",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Enrolled",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            } else {
                // Not enrolled - show buy button
                Button(
                    onClick = onBuyNow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF27AE60)
                    ),
                    shape = RoundedCornerShape(25.dp)
                ) {
                    Text(
                        text = "Buy Now - ₹${courseDetails.price}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Watch Videos Button (only for enrolled users)
            if (isEnrolled && courseDetails.sections.isNotEmpty()) {
                Button(
                    onClick = {
                        // Get all videos from all sections
                        val allVideos = courseDetails.sections.flatMap { section ->
                            section.videos
                        }
                        onWatchVideos(courseDetails.title, allVideos)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF27AE60)
                    ),
                    shape = RoundedCornerShape(25.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Watch Videos",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Watch Videos",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Secondary Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Group Study (only for enrolled students)
                if (isEnrolled) {
                    OutlinedButton(
                        onClick = onGroupStudy,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF4A90E2)
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = "Group Study",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Group Study",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Rate Course (only for enrolled users)
                if (isEnrolled) {
                    OutlinedButton(
                        onClick = { showRatingDialog = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFFF9800)
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Rate Course",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Rate",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Create Schedule
                OutlinedButton(
                    onClick = onCreateSchedule,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF9C27B0)
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Create Schedule",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Schedule",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        
        // Rating Dialog
        RatingDialog(
            isVisible = showRatingDialog,
            currentRating = courseDetails.rating,
            onDismiss = { showRatingDialog = false },
            onRatingSubmit = { newRating ->
                courseViewModel.updateCourseRating(courseDetails.title, newRating)
                showRatingDialog = false
            }
        )
        
    }
}

@Composable
fun RatingDialog(
    isVisible: Boolean,
    currentRating: Double,
    onDismiss: () -> Unit,
    onRatingSubmit: (Double) -> Unit
) {
    var selectedRating by remember { mutableStateOf(currentRating) }
    
    if (isVisible) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = "Rate This Course",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "How would you rate this course?",
                        fontSize = 16.sp,
                        color = Color(0xFF666666),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Star rating selector
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (i in 1..5) {
                            Icon(
                                imageVector = if (i <= selectedRating) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "Star $i",
                                tint = if (i <= selectedRating) Color(0xFFFFD700) else Color(0xFFCCCCCC),
                                modifier = Modifier
                                    .size(32.dp)
                                    .clickable { selectedRating = i.toDouble() }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = when {
                            selectedRating <= 1 -> "Poor"
                            selectedRating <= 2 -> "Fair"
                            selectedRating <= 3 -> "Good"
                            selectedRating <= 4 -> "Very Good"
                            else -> "Excellent"
                        },
                        fontSize = 14.sp,
                        color = Color(0xFF666666)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRatingSubmit(selectedRating)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4A90E2)
                    )
                ) {
                    Text("Submit Rating", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = Color(0xFF666666))
                }
            }
        )
    }
}