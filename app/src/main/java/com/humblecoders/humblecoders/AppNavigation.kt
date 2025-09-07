package com.humblecoders.humblecoders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import android.app.Activity
import android.content.Context
import com.humblecoders.humblecoders.SignInScreen
import com.humblecoders.humblecoders.SignUpScreen
import com.humblecoders.humblecoders.WelcomeScreen
import com.humblecoders.humblecoders.AuthViewModel

@Composable
fun AppNavigation(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    userViewModel: UserViewModel,
    paymentViewModel: PaymentViewModel,
    context: Context
) {
    val uiState by authViewModel.uiState.collectAsState()
    val userProfile by userViewModel.userProfile.collectAsState()

    // Load user profile when authenticated
    LaunchedEffect(uiState.isAuthenticated) {
        if (uiState.isAuthenticated) {
            android.util.Log.d("AppNavigation", "User authenticated, loading profile")
            userViewModel.loadUserProfile()
        }
    }
    
    // Also load profile when userProfile is null but user is authenticated
    LaunchedEffect(uiState.isAuthenticated, userProfile) {
        if (uiState.isAuthenticated && userProfile == null) {
            android.util.Log.d("AppNavigation", "User authenticated but profile is null, loading profile")
            userViewModel.loadUserProfile()
        }
    }
    
    // Check if profile exists but has empty email and refresh it
    LaunchedEffect(userProfile) {
        if (userProfile != null && userProfile!!.email.isEmpty()) {
            android.util.Log.w("AppNavigation", "Profile exists but email is empty, refreshing with auth data")
            userViewModel.refreshProfileWithAuthData()
        }
    }

    val startDestination = when {
        !uiState.isAuthenticated -> Screen.Welcome.route
        userProfile == null -> Screen.Dashboard.route // Loading state, show dashboard
        userProfile?.isOnboardingComplete == false -> Screen.FaceRegistration.route
        else -> Screen.Dashboard.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Welcome.route) {
            WelcomeScreen(
                authViewModel = authViewModel,
                onSignInClick = {
                    navController.navigate(Screen.SignIn.route)
                },
                onSignUpClick = {
                    navController.navigate(Screen.SignUp.route)
                },
                onGoogleSignInSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.SignIn.route) {
            SignInScreen(
                authViewModel = authViewModel,
                onNavigateToSignUp = {
                    navController.navigate(Screen.SignUp.route) {
                        popUpTo(Screen.SignIn.route) { inclusive = true }
                    }
                },
                onSignInSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                },
                onBackPressed = {navController.popBackStack()}
            )
        }

        composable(Screen.SignUp.route) {
            SignUpScreen(
                authViewModel = authViewModel,
                onNavigateToSignIn = {
                    navController.navigate(Screen.SignIn.route) {
                        popUpTo(Screen.SignUp.route) { inclusive = true }
                    }
                },
                onSignUpSuccess = {
                    navController.navigate(Screen.FaceRegistration.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                },
                onBackPressed = { navController.popBackStack() }
            )
        }

        composable(Screen.Dashboard.route) {
            val userProfile by userViewModel.userProfile.collectAsState()
            val courseViewModel = remember { CourseViewModel() }
            
            // Load user profile when dashboard is accessed
            LaunchedEffect(Unit) {
                userViewModel.loadUserProfile()
            }
            
            MainScreen(
                userProfile = userProfile,
                onSignOut = {
                    authViewModel.signOut()
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                courseViewModel = courseViewModel,
                navController = navController
            )
        }
        
        composable(Screen.FaceRegistration.route) {
            FaceRegistrationScreen(
                authViewModel = authViewModel,
                onBackPressed = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.FaceRegistration.route) { inclusive = true }
                    }
                },
                onRegistrationComplete = {
                    navController.navigate(Screen.SkillsInterests.route)
                },
                onRegisterLater = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.FaceRegistration.route) { inclusive = true }
                    }
                },
                userViewModel = userViewModel
            )
        }
        
        composable(Screen.SkillsInterests.route) {
            SkillsInterestsScreen(
                onComplete = { userProfile ->
                    // Save user profile to Firestore
                    userViewModel.updateUserProfile(userProfile)
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                userViewModel = userViewModel
            )
        }
        
                    composable(Screen.TestVerification.route) {
                        TestVerificationScreen(
                            onBackPressed = {
                                navController.popBackStack()
                            },
                            authViewModel = authViewModel,
                            faceViewModel = remember { FaceRegistrationViewModel(userViewModel) }
                        )
                    }

                    composable(
                        route = Screen.CourseDetail.route,
                        arguments = listOf(
                            navArgument("courseTitle") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val courseTitle = backStackEntry.arguments?.getString("courseTitle") ?: ""
                        
                        // Create ViewModel to fetch course details
                        val courseViewModel = remember { CourseViewModel() }
                        val courseDetails by courseViewModel.courseDetails.collectAsState()
                        val isLoading by courseViewModel.isLoading.collectAsState()
                        val isProcessingPayment by paymentViewModel.isProcessingPayment.collectAsState()
                        val paymentResult by paymentViewModel.paymentResult.collectAsState()
                        
                        // Fetch course details when the screen is loaded
                        LaunchedEffect(courseTitle) {
                            if (courseTitle.isNotEmpty()) {
                                courseViewModel.loadCourseDetails(courseTitle)
                            }
                        }

                        // Handle payment result
                        LaunchedEffect(paymentResult) {
                            paymentResult?.let { result ->
                                android.util.Log.d("AppNavigation", "Payment result received: $result")
                                when (result) {
                                    is PaymentResult.Success -> {
                                        android.util.Log.d("AppNavigation", "Payment successful, enrolling user in course: $courseTitle")
                                        try {
                                            userViewModel.enrollInCourse(courseTitle)
                                            paymentViewModel.clearPaymentResult()
                                        } catch (e: Exception) {
                                            android.util.Log.e("AppNavigation", "Error enrolling user in course", e)
                                        }
                                    }
                                    is PaymentResult.Error -> {
                                        android.util.Log.e("AppNavigation", "Payment error: ${result.message}")
                                        paymentViewModel.clearPaymentResult()
                                    }
                                }
                            }
                        }
                        
                        if (isLoading || isProcessingPayment) {
                            // Show loading indicator
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator()
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = if (isProcessingPayment) "Processing Payment..." else "Loading Course...",
                                        fontSize = 16.sp,
                                        color = Color(0xFF666666)
                                    )
                                    
                                    if (isProcessingPayment) {
                                        Spacer(modifier = Modifier.height(24.dp))
                                        Text(
                                            text = "If payment is stuck, please restart the app",
                                            fontSize = 12.sp,
                                            color = Color(0xFF999999),
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(
                                            onClick = {
                                                paymentViewModel.resetPaymentState()
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFFE53E3E)
                                            )
                                        ) {
                                            Text(
                                                text = "Reset Payment",
                                                color = Color.White,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                        } else if (courseDetails != null) {
                            val userProfile by userViewModel.userProfile.collectAsState()
                        
                        CourseDetailScreen(
                                courseDetails = courseDetails!!,
                                userProfile = userProfile,
                                courseViewModel = courseViewModel,
                            onBackPressed = {
                                navController.popBackStack()
                            },
                            onGroupStudy = {
                                // Navigate to group study with first section
                                val firstSection = courseDetails?.sections?.firstOrNull()
                                if (firstSection != null) {
                                    navController.navigate(
                                        Screen.GroupStudy.createRoute(
                                            courseTitle = courseTitle,
                                            sectionTitle = firstSection.title
                                        )
                                    )
                                }
                            },
                            onBuyNow = {
                                    // Handle buy now - start payment
                                    android.util.Log.d("AppNavigation", "Buy Now clicked")
                                    android.util.Log.d("AppNavigation", "User profile: $userProfile")
                                    android.util.Log.d("AppNavigation", "User email: ${userProfile?.email}")
                                    android.util.Log.d("AppNavigation", "Course details: $courseDetails")
                                    
                                    val userEmail = userProfile?.email ?: ""
                                    if (userEmail.isNotEmpty()) {
                                        android.util.Log.d("AppNavigation", "Starting payment with email: $userEmail")
                                        paymentViewModel.startPayment(
                                            activity = context as Activity,
                                            courseDetails = courseDetails!!,
                                            userEmail = userEmail
                                        )
                                    } else {
                                        android.util.Log.e("AppNavigation", "User email is empty, refreshing profile with auth data")
                                        userViewModel.refreshProfileWithAuthData()
                                        // Try to get email from Firebase Auth directly as fallback
                                        val authUser = userViewModel.getCurrentUser()
                                        val authEmail = authUser?.email ?: ""
                                        if (authEmail.isNotEmpty()) {
                                            android.util.Log.d("AppNavigation", "Using Firebase Auth email: $authEmail")
                                            paymentViewModel.startPayment(
                                                activity = context as Activity,
                                                courseDetails = courseDetails!!,
                                                userEmail = authEmail
                                            )
                                        } else {
                                            android.util.Log.e("AppNavigation", "No email available from Firebase Auth either")
                                        }
                                    }
                            },
                            onRateCourse = {
                                    // Handle rate course - this will be called from the rating dialog
                                    // The actual rating submission is handled in the dialog
                            },
                            onCreateSchedule = {
                                // Handle create schedule
                                },
                                onWatchVideos = { courseTitle, videos ->
                                    // Navigate to video player with first section
                                    val firstSection = courseDetails?.sections?.firstOrNull()
                                    if (firstSection != null) {
                                        navController.navigate(
                                            Screen.VideoPlayer.createRoute(
                                                courseTitle = courseTitle,
                                                sectionTitle = firstSection.title
                                            )
                                        )
                                    }
                                }
                            )
                        } else {
                            // Show error state
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Course not found")
                            }
                        }
                    }
                    
                    composable(
                        route = Screen.VideoPlayer.route,
                        arguments = listOf(
                            navArgument("courseTitle") { type = NavType.StringType },
                            navArgument("sectionTitle") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val courseTitle = backStackEntry.arguments?.getString("courseTitle") ?: ""
                        val sectionTitle = backStackEntry.arguments?.getString("sectionTitle") ?: ""
                        
                        // Create ViewModel to fetch course details
                        val courseViewModel = remember { CourseViewModel() }
                        val courseDetails by courseViewModel.courseDetails.collectAsState()
                        val isLoading by courseViewModel.isLoading.collectAsState()
                        
                        // Fetch course details when the screen is loaded
                        LaunchedEffect(courseTitle) {
                            if (courseTitle.isNotEmpty()) {
                                courseViewModel.loadCourseDetails(courseTitle)
                            }
                        }
                        
                        // Get videos from the specific section
                        val sectionVideos = remember(courseDetails, sectionTitle) {
                            courseDetails?.sections?.find { it.title == sectionTitle }?.videos ?: emptyList()
                        }
                        
                        if (isLoading) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        } else {
                            val userProfile by userViewModel.userProfile.collectAsState()
                            VideoPlayerScreen(
                                courseTitle = courseTitle,
                                sectionTitle = sectionTitle,
                                videos = sectionVideos,
                                onBackPressed = {
                                    navController.popBackStack()
                                },
                                userProfile = userProfile
                            )
                        }
                    }
                    
                    composable(
                        route = Screen.GroupStudy.route,
                        arguments = listOf(
                            navArgument("courseTitle") { type = NavType.StringType },
                            navArgument("sectionTitle") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val courseTitle = backStackEntry.arguments?.getString("courseTitle") ?: ""
                        val sectionTitle = backStackEntry.arguments?.getString("sectionTitle") ?: ""
                        
                        // Create ViewModel to fetch course details
                        val courseViewModel = remember { CourseViewModel() }
                        val courseDetails by courseViewModel.courseDetails.collectAsState()
                        val isLoading by courseViewModel.isLoading.collectAsState()
                        
                        // Fetch course details when the screen is loaded
                        LaunchedEffect(courseTitle) {
                            if (courseTitle.isNotEmpty()) {
                                courseViewModel.loadCourseDetails(courseTitle)
                            }
                        }
                        
                        // Get videos from the specific section
                        val sectionVideos = remember(courseDetails, sectionTitle) {
                            courseDetails?.sections?.find { it.title == sectionTitle }?.videos ?: emptyList()
                        }
                        
                        if (isLoading) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        } else {
                            GroupStudyScreen(
                                courseTitle = courseTitle,
                                sectionTitle = sectionTitle,
                                videos = sectionVideos,
                                onBackPressed = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
    }
}

sealed class Screen(val route: String) {
    object Welcome : Screen("welcome")
    object SignIn : Screen("signin")
    object SignUp : Screen("signup")
    object Dashboard : Screen("dashboard")
    object FaceRegistration : Screen("face_registration")
    object SkillsInterests : Screen("skills_interests")
    object TestVerification : Screen("test_verification")
    object CourseDetail : Screen("course_detail/{courseTitle}") {
        fun createRoute(courseTitle: String) = "course_detail/$courseTitle"
    }
    object VideoPlayer : Screen("video_player/{courseTitle}/{sectionTitle}") {
        fun createRoute(courseTitle: String, sectionTitle: String) = "video_player/$courseTitle/$sectionTitle"
    }
    
    object GroupStudy : Screen("group_study/{courseTitle}/{sectionTitle}") {
        fun createRoute(courseTitle: String, sectionTitle: String) = "group_study/$courseTitle/$sectionTitle"
    }
}

@Composable
fun DashboardScreen(
    authViewModel: AuthViewModel,
    navController: NavHostController,
    onSignOut: () -> Unit
) {
    val faceViewModel = remember { FaceRegistrationViewModel() }
    val faceUiState by faceViewModel.uiState.collectAsState()
    val authUiState by authViewModel.uiState.collectAsState()
    
    var showVerificationDialog by remember { mutableStateOf(false) }
    var verificationResult by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(faceUiState.verificationResponse) {
        faceUiState.verificationResponse?.let { response ->
            verificationResult = """
                Status: ${response.status}
                Error: ${response.error ?: "None"}
                Results: ${response.result?.joinToString("\n") { 
                    "Person: ${it.person}, Confidence: ${it.confidence}, Face: ${it.face}"
                } ?: "No matches found"}
            """.trimIndent()
            showVerificationDialog = true
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Welcome to Student Portal!",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Test Verification Button
        Button(
            onClick = {
                navController.navigate(Screen.TestVerification.route)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4263A6)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Test Face Verification",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onSignOut,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF6C63FF)
            )
        ) {
            Text("Sign Out")
        }
    }
    
    // Verification Result Dialog
    if (showVerificationDialog) {
        AlertDialog(
            onDismissRequest = { 
                showVerificationDialog = false
                verificationResult = null
            },
            title = { Text("Face Verification Result") },
            text = { 
                Text(
                    text = verificationResult ?: "No result",
                    fontSize = 12.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { 
                        showVerificationDialog = false
                        verificationResult = null
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }
}