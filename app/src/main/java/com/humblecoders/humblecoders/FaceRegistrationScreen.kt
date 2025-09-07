package com.humblecoders.humblecoders

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FaceRegistrationScreen(
    authViewModel: AuthViewModel,
    onBackPressed: () -> Unit,
    onRegistrationComplete: () -> Unit,
    onRegisterLater: () -> Unit,
    userViewModel: UserViewModel? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val faceViewModel = remember { FaceRegistrationViewModel(userViewModel) }
    val faceUiState by faceViewModel.uiState.collectAsState()
    val authUiState by authViewModel.uiState.collectAsState()
    
    var isCapturing by remember { mutableStateOf(false) }
    var capturedImage by remember { mutableStateOf<Bitmap?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var statusMessage by remember { mutableStateOf("") }
    
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    
    val imageCapture = remember { ImageCapture.Builder().build() }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    
    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }
    
    // Handle camera permission
    LaunchedEffect(cameraPermissionState.status) {
        if (cameraPermissionState.status.isGranted) {
            // Permission granted, start camera
        } else if (cameraPermissionState.status.shouldShowRationale) {
            // Show rationale
        } else {
            // Request permission
            cameraPermissionState.launchPermissionRequest()
        }
    }
    
    // Handle captured image processing
    LaunchedEffect(capturedImage) {
        capturedImage?.let { bitmap ->
            isProcessing = true
            statusMessage = "Processing face data..."
            progress = 0.3f
            
            // Get Firebase UID for the current user
            val firebaseUid = authViewModel.getCurrentUser()?.uid
            if (firebaseUid != null) {
                // Call the actual face registration API
                faceViewModel.registerFace(firebaseUid, listOf(bitmap), context)
            } else {
                statusMessage = "User not authenticated. Please sign in again."
                isProcessing = false
                progress = 0f
            }
        }
    }
    
    // Handle face registration state changes
    LaunchedEffect(faceUiState.isRegistrationComplete, faceUiState.error, faceUiState.isLoading) {
        if (faceUiState.isRegistrationComplete) {
            progress = 1.0f
            statusMessage = "Registration complete!"
            isProcessing = false
            kotlinx.coroutines.delay(1000)
            onRegistrationComplete()
        } else if (faceUiState.error != null) {
            statusMessage = "Registration failed: ${faceUiState.error}"
            isProcessing = false
            progress = 0f
        } else if (faceUiState.isLoading) {
            isProcessing = true
            progress = faceUiState.progress
            statusMessage = faceUiState.statusMessage
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Top App Bar
        TopAppBar(
            title = { },
            navigationIcon = {
                IconButton(onClick = onBackPressed) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.Black
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.White
            )
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            
            // Title
            Text(
                text = "Face Registration",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Description
            Text(
                text = "Your face registration will be used for randomized attendance checks during lectures. Attendance is required to receive the certificate.",
                fontSize = 16.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Face Registration Illustration
            if (capturedImage == null) {
                // Camera Preview
                if (cameraPermissionState.status.isGranted) {
                    CameraPreview(
                        imageCapture = imageCapture,
                        lifecycleOwner = lifecycleOwner,
                        cameraExecutor = cameraExecutor,
                        modifier = Modifier
                            .size(300.dp, 400.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                } else {
                    // Placeholder when permission not granted
                    Box(
                        modifier = Modifier
                            .size(300.dp, 400.dp)
                            .background(Color.Gray.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.facerec),
                            contentDescription = "Face Registration",
                            modifier = Modifier.size(200.dp)
                        )
                    }
                }
            } else {
                // Show captured image with processing overlay
                Box(
                    modifier = Modifier
                        .size(300.dp, 400.dp)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    capturedImage?.let { bitmap ->
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Captured Face",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    
                    if (isProcessing) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.7f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(
                                    progress = progress,
                                    modifier = Modifier.size(60.dp),
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = statusMessage,
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Error Message Display
            if (faceUiState.error != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (faceUiState.showRetry) Color(0xFFFFEBEE) else Color(0xFFFFF3E0)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (faceUiState.showRetry) "Face Not Detected" else "Error",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (faceUiState.showRetry) Color(0xFFD32F2F) else Color(0xFFF57C00)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = faceUiState.error ?: "Unknown error occurred",
                            fontSize = 14.sp,
                            color = Color(0xFF424242),
                            textAlign = TextAlign.Center
                        )
                        if (faceUiState.showRetry) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Please try again with better lighting or a clearer photo",
                                fontSize = 12.sp,
                                color = Color(0xFF666666),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Main Action Button
            Button(
                onClick = {
                    if (capturedImage == null) {
                        // Capture image
                        isCapturing = true
                        captureImage(
                            imageCapture = imageCapture,
                            context = context,
                            onImageCaptured = { bitmap ->
                                capturedImage = bitmap
                                isCapturing = false
                                // Start processing immediately after capture
                                val firebaseUid = authViewModel.getCurrentUser()?.uid
                                if (firebaseUid != null) {
                                    faceViewModel.registerFace(firebaseUid, listOf(bitmap), context)
                                }
                            }
                        )
                    }
                },
                enabled = !isCapturing && !isProcessing && cameraPermissionState.status.isGranted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4263A6)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isCapturing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = if (capturedImage == null) "Capture Face" else "Processing...",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            // Retry Button (shown when face detection fails)
            if (faceUiState.showRetry) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        val firebaseUid = authViewModel.getCurrentUser()?.uid
                        if (firebaseUid != null) {
                            faceViewModel.retryRegistration(firebaseUid, context)
                        }
                    },
                    enabled = !isProcessing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE74C3C)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Retry with Same Photo",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        capturedImage = null
                        faceViewModel.clearError()
                    },
                    enabled = !isProcessing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF95A5A6)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Take New Photo",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Note
            Text(
                text = "Note: Your face data is stored securely and used only for authentication.",
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Register Later Button
            TextButton(
                onClick = onRegisterLater,
                enabled = !isProcessing
            ) {
                Text(
                    text = "Register Later",
                    color = Color.Gray,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "→",
                    color = Color.Gray,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun CameraPreview(
    imageCapture: ImageCapture,
    lifecycleOwner: LifecycleOwner,
    cameraExecutor: ExecutorService,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    
    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProvider = cameraProviderFuture.get()
            
            val preview = Preview.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            
            preview.setSurfaceProvider(previewView.surfaceProvider)
            
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                Log.e("CameraPreview", "Use case binding failed", e)
            }
            
            previewView
        },
        modifier = modifier
    )
}

private fun captureImage(
    imageCapture: ImageCapture,
    context: Context,
    onImageCaptured: (Bitmap) -> Unit
) {
    val photoFile = File(
        context.getExternalFilesDir(null),
        "face_capture_${System.currentTimeMillis()}.jpg"
    )
    
    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
    
    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                bitmap?.let { onImageCaptured(it) }
            }
            
            override fun onError(exception: ImageCaptureException) {
                Log.e("ImageCapture", "Photo capture failed: ${exception.message}", exception)
            }
        }
    )
}
