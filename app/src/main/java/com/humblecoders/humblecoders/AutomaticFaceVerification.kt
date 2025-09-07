package com.humblecoders.humblecoders

import android.content.Context
import android.graphics.Bitmap
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
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
fun AutomaticFaceVerification(
    isVisible: Boolean,
    onFaceCaptured: (Bitmap) -> Unit,
    onDismiss: () -> Unit,
    attendanceState: AttendanceUiState
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
    
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var cameraExecutor by remember { mutableStateOf<ExecutorService?>(null) }
    var isCapturing by remember { mutableStateOf(false) }
    var countdown by remember { mutableStateOf(3) }
    var showCountdown by remember { mutableStateOf(false) }
    
    // Initialize camera executor and image capture
    LaunchedEffect(Unit) {
        cameraExecutor = Executors.newSingleThreadExecutor()
        imageCapture = ImageCapture.Builder().build()
    }
    
    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor?.shutdown()
        }
    }
    
    // Auto-capture with countdown if camera permission is granted
    LaunchedEffect(isVisible, cameraPermissionState.status) {
        if (isVisible && cameraPermissionState.status.isGranted && !isCapturing && !showCountdown) {
            showCountdown = true
            countdown = 3
            
            // Countdown from 3 to 1
            repeat(3) {
                kotlinx.coroutines.delay(1000)
                if (isVisible && !isCapturing) {
                    countdown--
                } else {
                    return@repeat
                }
            }
            
            showCountdown = false
            
            if (isVisible && !isCapturing) {
                android.util.Log.d("AutomaticFaceVerification", "Attempting to capture image")
                isCapturing = true
                imageCapture?.let { capture ->
                    android.util.Log.d("AutomaticFaceVerification", "ImageCapture is available, taking picture")
                    capture.takePicture(
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageCapturedCallback() {
                            override fun onCaptureSuccess(image: ImageProxy) {
                                android.util.Log.d("AutomaticFaceVerification", "Image captured successfully")
                                val bitmap = imageProxyToBitmap(image)
                                if (bitmap != null) {
                                    android.util.Log.d("AutomaticFaceVerification", "Bitmap created, calling onFaceCaptured")
                                    onFaceCaptured(bitmap)
                                } else {
                                    android.util.Log.e("AutomaticFaceVerification", "Failed to create bitmap from image")
                                }
                                image.close()
                                isCapturing = false
                            }
                            
                            override fun onError(exception: ImageCaptureException) {
                                android.util.Log.e("AutomaticFaceVerification", "Image capture failed", exception)
                                isCapturing = false
                            }
                        }
                    )
                } ?: run {
                    android.util.Log.e("AutomaticFaceVerification", "ImageCapture is null, cannot take picture")
                    isCapturing = false
                }
            }
        }
    }
    
    if (isVisible) {
        Dialog(
            onDismissRequest = onDismiss,
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
                    // Title
                    Text(
                        text = "Automatic Attendance Verification",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2C3E50),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Status message
                    Text(
                        text = when {
                            attendanceState.isVerifying -> "Verifying your attendance..."
                            attendanceState.attendanceMarked == true -> "Attendance marked successfully!"
                            attendanceState.attendanceMarked == false -> "Attendance not marked. Please try again."
                            showCountdown -> "Please look at the camera. Capturing in $countdown..."
                            else -> "Please look at the camera. Verification will happen automatically in a few seconds, or tap 'Capture & Verify' to take the picture now."
                        },
                        fontSize = 14.sp,
                        color = Color(0xFF666666),
                        textAlign = TextAlign.Center
                    )
                    
                    // Debug info
                    Text(
                        text = "Debug: isVerifying=${attendanceState.isVerifying}, attendanceMarked=${attendanceState.attendanceMarked}",
                        fontSize = 10.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Camera preview or result
                    if (attendanceState.isVerifying) {
                        // Show loading indicator
                        Box(
                            modifier = Modifier
                                .size(200.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF5F5F5)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(
                                    color = Color(0xFF4A90E2),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Verifying...",
                                    fontSize = 14.sp,
                                    color = Color(0xFF666666)
                                )
                            }
                        }
                    } else if (attendanceState.attendanceMarked != null) {
                        // Show result
                        Box(
                            modifier = Modifier
                                .size(200.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (attendanceState.attendanceMarked) Color(0xFFE8F5E8) else Color(0xFFFFEBEE)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = if (attendanceState.attendanceMarked) Icons.Default.CheckCircle else Icons.Default.Error,
                                    contentDescription = "Result",
                                    tint = if (attendanceState.attendanceMarked) Color(0xFF4CAF50) else Color(0xFFF44336),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = if (attendanceState.attendanceMarked) "Attendance Marked!" else "Attendance Not Marked",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (attendanceState.attendanceMarked) Color(0xFF4CAF50) else Color(0xFFF44336)
                                )
                                
                                // Auto-dismiss after 3 seconds
                                LaunchedEffect(Unit) {
                                    kotlinx.coroutines.delay(3000)
                                    onDismiss()
                                }
                            }
                        }
                    } else {
                        // Show camera preview
                        if (cameraPermissionState.status.isGranted) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box {
                                    CameraPreview(
                                        onImageCaptured = { bitmap ->
                                            onFaceCaptured(bitmap)
                                        },
                                        imageCapture = imageCapture,
                                        lifecycleOwner = lifecycleOwner,
                                        modifier = Modifier
                                            .size(200.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                    )
                                    
                                    // Countdown overlay
                                    if (showCountdown) {
                                        Box(
                                            modifier = Modifier
                                                .size(200.dp)
                                                .background(
                                                    Color.Black.copy(alpha = 0.5f),
                                                    RoundedCornerShape(12.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = countdown.toString(),
                                                fontSize = 48.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                // Manual capture button
                                Button(
                                    onClick = {
                                        if (!isCapturing) {
                                            android.util.Log.d("AutomaticFaceVerification", "Manual capture button clicked")
                                            isCapturing = true
                                            imageCapture?.let { capture ->
                                                android.util.Log.d("AutomaticFaceVerification", "Taking picture manually")
                                                capture.takePicture(
                                                    ContextCompat.getMainExecutor(context),
                                                    object : ImageCapture.OnImageCapturedCallback() {
                                                        override fun onCaptureSuccess(image: ImageProxy) {
                                                            android.util.Log.d("AutomaticFaceVerification", "Manual image captured successfully")
                                                            val bitmap = imageProxyToBitmap(image)
                                                            if (bitmap != null) {
                                                                android.util.Log.d("AutomaticFaceVerification", "Manual bitmap created, calling onFaceCaptured")
                                                                onFaceCaptured(bitmap)
                                                            } else {
                                                                android.util.Log.e("AutomaticFaceVerification", "Failed to create bitmap from manual capture")
                                                            }
                                                            image.close()
                                                            isCapturing = false
                                                        }
                                                        
                                                        override fun onError(exception: ImageCaptureException) {
                                                            android.util.Log.e("AutomaticFaceVerification", "Manual image capture failed", exception)
                                                            isCapturing = false
                                                        }
                                                    }
                                                )
                                            } ?: run {
                                                android.util.Log.e("AutomaticFaceVerification", "ImageCapture is null for manual capture")
                                                isCapturing = false
                                            }
                                        }
                                    },
                                    enabled = !isCapturing,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF4A90E2)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (isCapturing) {
                                        CircularProgressIndicator(
                                            color = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Text(
                                        text = if (isCapturing) "Capturing..." else "Capture & Verify",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else {
                            // Permission not granted
                            Box(
                                modifier = Modifier
                                    .size(200.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFF5F5F5)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "Camera",
                                        tint = Color(0xFF666666),
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Camera permission required",
                                        fontSize = 14.sp,
                                        color = Color(0xFF666666),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (!cameraPermissionState.status.isGranted) {
                            Button(
                                onClick = {
                                    cameraPermissionState.launchPermissionRequest()
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF4A90E2)
                                )
                            ) {
                                Text("Grant Permission", color = Color.White)
                            }
                        }
                        
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE74C3C)
                            )
                        ) {
                            Text("Close", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}


private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
    val buffer = image.planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}
