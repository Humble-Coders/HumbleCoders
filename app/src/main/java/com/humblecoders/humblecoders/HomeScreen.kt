package com.humblecoders.humblecoders

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import coil.compose.AsyncImage
import androidx.compose.material3.TextFieldDefaults

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onMenuClick: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onCourseClick: (Course) -> Unit = {},
    onSeeMoreClick: (String) -> Unit = {},
    userProfile: UserProfile? = null,
    courseViewModel: CourseViewModel
) {
    val scrollState = rememberScrollState()
    val courses by courseViewModel.courses.collectAsState()
    val isLoading by courseViewModel.isLoading.collectAsState()
    val errorMessage by courseViewModel.errorMessage.collectAsState()
    val searchQuery by courseViewModel.searchQuery.collectAsState()
    
    var showSearchField by remember { mutableStateOf(false) }
    
    // Load courses when screen is first displayed
    LaunchedEffect(Unit) {
        android.util.Log.d("HomeScreen", "Loading courses...")
        courseViewModel.loadCourses()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
            .verticalScroll(scrollState)
    ) {
        // Top App Bar
        TopAppBar(
            title = { 
                if (showSearchField) {
                    // Search TextField
                    TextField(
                        value = searchQuery,
                        onValueChange = { courseViewModel.updateSearchQuery(it) },
                        placeholder = {
                            Text(
                                text = "Search courses...",
                                color = Color(0xFF999999)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color(0xFF4A90E2),
                            unfocusedIndicatorColor = Color(0xFFE0E0E0),
                            cursorColor = Color(0xFF4A90E2)
                        ),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal
                        )
                    )
                } else {
                    // Normal title
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (searchQuery.isNotEmpty()) "Search Results" else "Humble Coders",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        if (searchQuery.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "(${courseViewModel.getOrderedCourses(userProfile?.preferredCourseCategory).sumOf { it.courses.size }} results)",
                                fontSize = 12.sp,
                                color = Color(0xFF666666)
                            )
                        }
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = { /* Menu UI only - no functionality */ }) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = Color.Black
                    )
                }
            },
            actions = {
                if (showSearchField) {
                    // Show close button when search field is visible
                    IconButton(onClick = { 
                        showSearchField = false
                        courseViewModel.clearSearch()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Search",
                            tint = Color(0xFFE53E3E)
                        )
                    }
                } else {
                    // Show search button when search field is hidden
                    IconButton(onClick = { 
                        showSearchField = true
                    }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = Color.Black
                        )
                    }
                }
                IconButton(onClick = { /* Notifications UI only - no functionality */ }) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = Color.Black
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.White
            )
        )
        
        
        // Hero Section - Image Carousel
        ImageCarousel()
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Course Categories - Reorder based on user preference
        if (isLoading) {
            // Show loading state
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = Color(0xFF3498DB)
                )
            }
        } else if (errorMessage != null) {
            // Show error state
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFEBEE)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Failed to load courses",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD32F2F)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "Unknown error",
                        fontSize = 14.sp,
                        color = Color(0xFF666666),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // Show courses
            val courseSections = courseViewModel.getOrderedCourses(userProfile?.preferredCourseCategory)
            android.util.Log.d("HomeScreen", "Displaying ${courseSections.size} course sections")
            
            if (courseSections.isEmpty() && searchQuery.isNotEmpty()) {
                // No search results found
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFF3E0)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = "No results",
                            tint = Color(0xFFF57C00),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No courses found",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF57C00)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Try searching with different keywords",
                            fontSize = 14.sp,
                            color = Color(0xFF666666),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                courseSections.forEachIndexed { index, section ->
                    android.util.Log.d("HomeScreen", "Section: ${section.categoryName}, Courses: ${section.courses.size}")
                    section.courses.forEach { course ->
                        android.util.Log.d("HomeScreen", "Course: ${course.title}, Thumbnail: ${course.thumbnailUrl}")
                    }
                    
                    CourseCategorySection(
                        title = if (searchQuery.isNotEmpty()) "Search results in ${section.categoryName}" else "Top courses in ${section.categoryName}",
                        courses = section.courses,
                        onCourseClick = onCourseClick,
                        onSeeMoreClick = { onSeeMoreClick(section.categoryName) }
                    )
                    
                    if (index < courseSections.size - 1) {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp)) // Bottom padding
    }
    
}

@Composable
fun ImageCarousel() {
    var currentImage by remember { mutableStateOf(0) }
    val images = listOf(R.drawable.ca, R.drawable.cs)
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(horizontal = 16.dp)
    ) {
        // Image Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent
            )
        ) {
            Image(
                painter = painterResource(id = images[currentImage]),
                contentDescription = "Carousel Image",
                modifier = Modifier
                    .fillMaxSize()
                    .clickable {
                        currentImage = (currentImage + 1) % images.size
                    },
                contentScale = ContentScale.Crop
            )
        }
        
        // Carousel indicators
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(images.size) { index ->
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            if (index == currentImage) Color(0xFF4A90E2) else Color.Gray.copy(alpha = 0.3f),
                            CircleShape
                        )
                )
            }
        }
    }
}


@Composable
fun CourseCategorySection(
    title: String,
    courses: List<Course>,
    onCourseClick: (Course) -> Unit,
    onSeeMoreClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Section header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            
            Text(
                text = "See more >",
                fontSize = 14.sp,
                color = Color(0xFF4A90E2),
                modifier = Modifier.clickable { onSeeMoreClick() }
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Horizontal course list
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            courses.forEach { course ->
                CourseCard(
                    course = course,
                    onClick = { onCourseClick(course) }
                )
            }
        }
    }
}

@Composable
fun CourseCard(
    course: Course,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Course image
            android.util.Log.d("CourseCard", "Course: ${course.title}, Thumbnail URL: '${course.thumbnailUrl}', IsEmpty: ${course.thumbnailUrl.isEmpty()}")
            if (course.thumbnailUrl.isNotEmpty()) {
                android.util.Log.d("CourseCard", "Loading image for ${course.title}: ${course.thumbnailUrl}")
                AsyncImage(
                    model = course.thumbnailUrl,
                    contentDescription = course.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentScale = ContentScale.Crop,
                    onSuccess = {
                        android.util.Log.d("CourseCard", "Successfully loaded image for ${course.title}")
                    },
                    onError = { error ->
                        android.util.Log.e("CourseCard", "Failed to load image for ${course.title}: ${error.result.throwable?.message}")
                    }
                )
            } else {
                android.util.Log.w("CourseCard", "No thumbnail URL for course: ${course.title}")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(Color(0xFFF8F9FA), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = course.title,
                        tint = Color(0xFFBDC3C7),
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Course title
            Text(
                text = course.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black,
                maxLines = 2,
                lineHeight = 18.sp
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Course price
            Text(
                text = "Rs. ${course.price}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4A90E2)
            )
        }
    }
}

// Note: Course data class is now defined in Course.kt

// Note: HeroBanner data class and getHeroBanners function removed - using ImageCarousel instead


// Note: Course data is now fetched from Firebase via CourseViewModel
