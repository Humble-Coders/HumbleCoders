package com.humblecoders.humblecoders

import android.graphics.Bitmap
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun TestVerificationScreen(
    onBackPressed: () -> Unit,
    authViewModel: AuthViewModel,
    faceViewModel: FaceRegistrationViewModel
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val faceUiState by faceViewModel.uiState.collectAsState()
    
    var capturedImage by remember { mutableStateOf<Bitmap?>(null) }
    var showResultDialog by remember { mutableStateOf(false) }
    var cameraExecutor by remember { mutableStateOf<ExecutorService?>(null) }
    
    // Get stored person UUID from registration
    val personUuid = faceViewModel.getStoredPersonUuid(context)
    
    val cameraPermissionState = rememberPermissionState(
        android.Manifest.permission.CAMERA
    )
    
    LaunchedEffect(faceUiState.verificationResponse) {
        faceUiState.verificationResponse?.let {
            showResultDialog = true
        }
    }
    
    // Initialize camera executor
    LaunchedEffect(Unit) {
        cameraExecutor = Executors.newSingleThreadExecutor()
    }
    
    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor?.shutdown()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Top App Bar
        TopAppBar(
            title = { Text("Test Face Verification") },
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
            
            Text(
                text = "Test Face Verification",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Take a photo to test face verification against your registered face",
                fontSize = 16.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
            
            if (personUuid == null) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Error: No face registered. Please complete face registration first.",
                    color = Color.Red,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
            } else {
                Spacer(modifier = Modifier.height(20.dp))
                
                Text(
                    text = "Person UUID: $personUuid",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Verification Threshold: 0.1 (Very Low - More Permissive)",
                    fontSize = 12.sp,
                    color = Color(0xFF4CAF50),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Camera Preview or Captured Image
                if (capturedImage != null) {
                    // Show captured image
                    Image(
                        bitmap = capturedImage!!.asImageBitmap(),
                        contentDescription = "Captured Image",
                        modifier = Modifier
                            .size(300.dp)
                            .background(Color.Gray.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Retake Button
                    Button(
                        onClick = { capturedImage = null },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF757575)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Retake Photo",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Verify Button
                    Button(
                        onClick = {
                            capturedImage?.let { bitmap ->
                                personUuid?.let { uuid ->
                                    faceViewModel.verifyFace(uuid, bitmap)
                                }
                            }
                        },
                        enabled = !faceUiState.isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (faceUiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = if (faceUiState.isLoading) "Verifying..." else "Verify Face",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    // Camera Preview
                    if (cameraPermissionState.status.isGranted) {
                        CameraPreview(
                            onImageCaptured = { bitmap ->
                                capturedImage = bitmap
                            },
                            lifecycleOwner = lifecycleOwner,
                            cameraExecutor = cameraExecutor
                        )
                        
                        Spacer(modifier = Modifier.height(20.dp))
                    } else {
                        // Permission request
                        Box(
                            modifier = Modifier
                                .size(300.dp)
                                .background(Color.Gray.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Camera permission required",
                                    color = Color.Gray,
                                    fontSize = 16.sp,
                                    textAlign = TextAlign.Center
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Button(
                                    onClick = { cameraPermissionState.launchPermissionRequest() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF4263A6)
                                    )
                                ) {
                                    Text("Grant Permission")
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Status Message
            if (faceUiState.statusMessage.isNotEmpty()) {
                Text(
                    text = faceUiState.statusMessage,
                    color = if (faceUiState.error != null) Color.Red else Color.Black,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
            
            faceUiState.error?.let { error ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = error,
                    color = Color.Red,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
    
    // Result Dialog
    if (showResultDialog) {
        Dialog(
            onDismissRequest = { showResultDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "Verification Result",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    faceUiState.verificationResponse?.let { response ->
                        Text(
                            text = "Status: ${response.status}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Threshold Used: 0.1 (Very Low)",
                            fontSize = 12.sp,
                            color = Color(0xFF4CAF50)
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Show probability if available
                        response.probability?.let { probability ->
                            val confidence = probability
                            val meetsThreshold = confidence >= 0.1
                            Text(
                                text = "Probability: ${String.format("%.2f", confidence)} ${if (meetsThreshold) "✓" else "✗"}",
                                fontSize = 14.sp,
                                color = if (meetsThreshold) Color(0xFF4CAF50) else Color.Red,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        
                        // Show message if available
                        response.message?.let { message ->
                            Text(
                                text = "Message: $message",
                                fontSize = 14.sp,
                                color = Color(0xFF4CAF50)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        
                        // Show face rectangle if available
                        response.rectangle?.let { rect ->
                            Text(
                                text = "Face Detected:",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "  Position: (${rect.left}, ${rect.top}) to (${rect.right}, ${rect.bottom})",
                                fontSize = 12.sp,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        
                        // Show rotation if available
                        response.rotation?.let { rotation ->
                            Text(
                                text = "Rotation: ${rotation}°",
                                fontSize = 12.sp,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        
                        // Show error if available
                        response.error?.let { error ->
                            Text(
                                text = "Error: $error",
                                fontSize = 14.sp,
                                color = Color.Red
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        
                        // Legacy result format (if available)
                        if (response.result != null && response.result.isNotEmpty()) {
                            Text(
                                text = "Legacy Results:",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            response.result.forEach { result ->
                                Text(
                                    text = "• Person: ${result.person}",
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(start = 16.dp)
                                )
                                val confidence = result.confidence
                                val meetsThreshold = confidence >= 0.1
                                Text(
                                    text = "  Confidence: ${String.format("%.2f", confidence)} ${if (meetsThreshold) "✓" else "✗"}",
                                    fontSize = 12.sp,
                                    color = if (meetsThreshold) Color(0xFF4CAF50) else Color.Red,
                                    modifier = Modifier.padding(start = 16.dp)
                                )
                                Text(
                                    text = "  Face ID: ${result.face}",
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(start = 16.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = { showResultDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
fun CameraPreview(
    onImageCaptured: (Bitmap) -> Unit,
    lifecycleOwner: LifecycleOwner,
    cameraExecutor: ExecutorService?
) {
    val context = LocalContext.current
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    
    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                
                val preview = Preview.Builder().build()
                val imageCaptureBuilder = ImageCapture.Builder()
                imageCapture = imageCaptureBuilder.build()
                
                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
                
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                    preview.setSurfaceProvider(previewView.surfaceProvider)
                } catch (exc: Exception) {
                    // Handle error
                }
            }, ContextCompat.getMainExecutor(ctx))
            
            previewView
        },
        modifier = Modifier
            .size(300.dp)
            .background(Color.Gray.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
    )
    
    // Capture button
    Spacer(modifier = Modifier.height(16.dp))
    
    Button(
        onClick = {
            imageCapture?.let { capture ->
                val photoFile = java.io.File.createTempFile("verification_photo", ".jpg")
                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                
                capture.takePicture(
                    outputOptions,
                    cameraExecutor ?: Executors.newSingleThreadExecutor(),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            val bitmap = android.graphics.BitmapFactory.decodeFile(photoFile.absolutePath)
                            onImageCaptured(bitmap)
                        }
                        
                        override fun onError(exception: ImageCaptureException) {
                            // Handle error
                        }
                    }
                )
            }
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
            text = "Capture Photo",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}