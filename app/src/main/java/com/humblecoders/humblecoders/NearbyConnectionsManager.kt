package com.humblecoders.humblecoders

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.nearby.connection.*
import com.google.android.gms.nearby.Nearby
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class NearbyConnectionsManager(private val context: Context) {
    
    data class Endpoint(
        val id: String,
        val name: String,
        val isConnected: Boolean = false
    )
    
    data class TransferStatus(
        val isTransferring: Boolean = false,
        val isReceiving: Boolean = false,
        val fileName: String = "",
        val progress: Int = 0
    )
    
    private val connectionsClient = Nearby.getConnectionsClient(context)
    
    private val _discoveredEndpoints = MutableStateFlow<List<Endpoint>>(emptyList())
    val discoveredEndpoints: StateFlow<List<Endpoint>> = _discoveredEndpoints.asStateFlow()
    
    private val _connectedEndpoints = MutableStateFlow<List<Endpoint>>(emptyList())
    val connectedEndpoints: StateFlow<List<Endpoint>> = _connectedEndpoints.asStateFlow()
    
    private val _isAdvertising = MutableStateFlow(false)
    val isAdvertising: StateFlow<Boolean> = _isAdvertising.asStateFlow()
    
    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()
    
    private val _transferStatus = MutableStateFlow(TransferStatus())
    val transferStatus: StateFlow<TransferStatus> = _transferStatus.asStateFlow()
    
    private val _receivedFiles = MutableStateFlow<List<String>>(emptyList())
    val receivedFiles: StateFlow<List<String>> = _receivedFiles.asStateFlow()
    
    private val receivedFilesDir = File(context.filesDir, "received_videos")
    
    init {
        if (!receivedFilesDir.exists()) {
            receivedFilesDir.mkdirs()
        }
    }
    
    private fun hasRequiredPermissions(): Boolean {
        val permissions = mutableListOf<String>()
        
        // Add location permissions first (required for all versions)
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        
        // Add Android 12+ permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }
        
        val allGranted = permissions.all { permission ->
            val isGranted = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            Log.d("NearbyConnectionsManager", "Permission $permission: ${if (isGranted) "GRANTED" else "DENIED"}")
            isGranted
        }
        
        Log.d("NearbyConnectionsManager", "All permissions granted: $allGranted")
        return allGranted
    }
    
    private fun getMissingPermissions(): Array<String> {
        val permissions = mutableListOf<String>()
        
        // Add location permissions first (required for all versions)
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        
        // Add Android 12+ permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            }
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            }
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
            }
        }
        
        Log.d("NearbyConnectionsManager", "Missing permissions: ${permissions.joinToString()}")
        return permissions.toTypedArray()
    }
    
    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            Log.d("NearbyConnectionsManager", "Connection initiated with ${connectionInfo.endpointName}")
            // Automatically accept all incoming connections for seamless sharing
            connectionsClient.acceptConnection(endpointId, payloadCallback)
            Log.d("NearbyConnectionsManager", "Automatically accepted connection from ${connectionInfo.endpointName}")
        }
        
        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    Log.d("NearbyConnectionsManager", "Connected to $endpointId")
                    val endpoint = _discoveredEndpoints.value.find { it.id == endpointId }
                    if (endpoint != null) {
                        val connectedEndpoint = endpoint.copy(isConnected = true)
                        _connectedEndpoints.value = _connectedEndpoints.value + connectedEndpoint
                        
                        // Trigger automatic download when connection is established
                        Log.d("NearbyConnectionsManager", "Connection established - triggering automatic downloads")
                        onConnectionEstablished?.invoke()
                    }
                }
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                    Log.d("NearbyConnectionsManager", "Connection rejected by $endpointId")
                }
                ConnectionsStatusCodes.STATUS_ERROR -> {
                    Log.e("NearbyConnectionsManager", "Connection error with $endpointId")
                }
            }
        }
        
        override fun onDisconnected(endpointId: String) {
            Log.d("NearbyConnectionsManager", "Disconnected from $endpointId")
            _connectedEndpoints.value = _connectedEndpoints.value.filter { it.id != endpointId }
        }
    }
    
    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, discoveredEndpointInfo: DiscoveredEndpointInfo) {
            Log.d("NearbyConnectionsManager", "Found endpoint: ${discoveredEndpointInfo.endpointName}")
            val endpoint = Endpoint(endpointId, discoveredEndpointInfo.endpointName)
            _discoveredEndpoints.value = _discoveredEndpoints.value + endpoint
        }
        
        override fun onEndpointLost(endpointId: String) {
            Log.d("NearbyConnectionsManager", "Lost endpoint: $endpointId")
            _discoveredEndpoints.value = _discoveredEndpoints.value.filter { it.id != endpointId }
        }
    }
    
    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            when (payload.type) {
                Payload.Type.FILE -> {
                    Log.d("NearbyConnectionsManager", "Receiving file payload from $endpointId")
                    _transferStatus.value = TransferStatus(
                        isReceiving = true,
                        fileName = "Receiving file...",
                        progress = 0
                    )
                    Log.d("NearbyConnectionsManager", "File reception started automatically")
                }
                Payload.Type.BYTES -> {
                    Log.d("NearbyConnectionsManager", "Receiving byte payload from $endpointId")
                    // Handle byte payloads if needed
                }
            }
        }
        
        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            when (update.status) {
                PayloadTransferUpdate.Status.IN_PROGRESS -> {
                    val progress = if (update.totalBytes > 0) {
                        (update.bytesTransferred * 100 / update.totalBytes).toInt()
                    } else 0
                    
                    _transferStatus.value = _transferStatus.value.copy(progress = progress)
                }
                PayloadTransferUpdate.Status.SUCCESS -> {
                    Log.d("NearbyConnectionsManager", "File transfer completed successfully from $endpointId")
                    _transferStatus.value = TransferStatus()
                    
                    // The file is automatically saved by the Nearby Connections API
                    // We need to find the actual file path and add it to our list
                    update.payloadId?.let { payloadId ->
                        // Find the received file in the received files directory
                        val receivedFiles = receivedFilesDir.listFiles()
                        val latestFile = receivedFiles?.maxByOrNull { it.lastModified() }
                        
                        latestFile?.let { file ->
                            val filePath = file.absolutePath
                            _receivedFiles.value = _receivedFiles.value + filePath
                            Log.d("NearbyConnectionsManager", "File automatically saved to: $filePath")
                            
                            // Notify that a new file was received
                            android.util.Log.d("NearbyConnectionsManager", "New file received: ${file.name}")
                        }
                    }
                }
                PayloadTransferUpdate.Status.FAILURE -> {
                    Log.e("NearbyConnectionsManager", "Transfer failed")
                    _transferStatus.value = TransferStatus()
                }
                PayloadTransferUpdate.Status.CANCELED -> {
                    Log.d("NearbyConnectionsManager", "Transfer canceled")
                    _transferStatus.value = TransferStatus()
                }
            }
        }
    }
    
    fun startDiscovery() {
        Log.d("NearbyConnectionsManager", "Starting discovery")
        
        if (!hasRequiredPermissions()) {
            val missingPermissions = getMissingPermissions()
            Log.e("NearbyConnectionsManager", "Missing permissions: ${missingPermissions.joinToString()}")
            _isDiscovering.value = false
            return
        }
        
        _isDiscovering.value = true
        
        val discoveryOptions = DiscoveryOptions.Builder()
            .setStrategy(Strategy.P2P_STAR)
            .build()
            
        Log.d("NearbyConnectionsManager", "Starting discovery with service ID: com.humblecoders.humblecoders.SERVICE_ID")
        connectionsClient.startDiscovery(
            "com.humblecoders.humblecoders.SERVICE_ID",
            endpointDiscoveryCallback,
            discoveryOptions
        ).addOnSuccessListener { _ ->
            Log.d("NearbyConnectionsManager", "Discovery started successfully")
        }.addOnFailureListener { e ->
            Log.e("NearbyConnectionsManager", "Failed to start discovery", e)
            _isDiscovering.value = false
            
            // If it's a permission error, try to recheck permissions
            if (e.message?.contains("MISSING_PERMISSION") == true) {
                Log.w("NearbyConnectionsManager", "Permission error detected, rechecking permissions")
                // The permission might have been granted but not yet processed
                // We'll let the UI handle retrying
            }
        }
    }
    
    fun stopDiscovery() {
        Log.d("NearbyConnectionsManager", "Stopping discovery")
        connectionsClient.stopDiscovery()
        _isDiscovering.value = false
    }
    
    fun startAdvertising() {
        Log.d("NearbyConnectionsManager", "Starting advertising")
        
        if (!hasRequiredPermissions()) {
            val missingPermissions = getMissingPermissions()
            Log.e("NearbyConnectionsManager", "Missing permissions: ${missingPermissions.joinToString()}")
            _isAdvertising.value = false
            return
        }
        
        _isAdvertising.value = true
        
        val advertisingOptions = AdvertisingOptions.Builder()
            .setStrategy(Strategy.P2P_STAR)
            .build()
            
        Log.d("NearbyConnectionsManager", "Starting advertising with service ID: com.humblecoders.humblecoders.SERVICE_ID")
        connectionsClient.startAdvertising(
            "HumbleCoders Device",
            "com.humblecoders.humblecoders.SERVICE_ID",
            connectionLifecycleCallback,
            advertisingOptions
        ).addOnSuccessListener { _ ->
            Log.d("NearbyConnectionsManager", "Advertising started successfully")
        }.addOnFailureListener { e ->
            Log.e("NearbyConnectionsManager", "Failed to start advertising", e)
            _isAdvertising.value = false
        }
    }
    
    fun stopAdvertising() {
        Log.d("NearbyConnectionsManager", "Stopping advertising")
        connectionsClient.stopAdvertising()
        _isAdvertising.value = false
    }
    
    fun sendFile(filePath: String, endpoint: Endpoint) {
        Log.d("NearbyConnectionsManager", "Sending file: $filePath to ${endpoint.name}")
        
        val file = File(filePath)
        if (!file.exists()) {
            Log.e("NearbyConnectionsManager", "File does not exist: $filePath")
            return
        }
        
        _transferStatus.value = TransferStatus(
            isTransferring = true,
            fileName = file.name,
            progress = 0
        )
        
        val payload = Payload.fromFile(file)
        connectionsClient.sendPayload(endpoint.id, payload)
    }
    
    fun connectToEndpoint(endpoint: Endpoint) {
        Log.d("NearbyConnectionsManager", "Connecting to ${endpoint.name}")
        connectionsClient.requestConnection(
            "HumbleCoders Device",
            endpoint.id,
            connectionLifecycleCallback
        )
    }
    
    fun getReceivedVideoParts(videoTitle: String): List<String> {
        val allReceivedFiles = receivedFilesDir.listFiles()
            ?.filter { it.isFile && it.extension == "mp4" }
            ?.sortedBy { it.lastModified() }
            ?.map { it.absolutePath }
            ?: emptyList()
            
        Log.d("NearbyConnectionsManager", "Found ${allReceivedFiles.size} received files for video: $videoTitle")
        allReceivedFiles.forEach { filePath ->
            Log.d("NearbyConnectionsManager", "Received file: $filePath")
        }
        
        return allReceivedFiles
    }
    
    fun getRequiredPermissions(): Array<String> {
        return getMissingPermissions()
    }
    
    fun hasAllRequiredPermissions(): Boolean {
        return hasRequiredPermissions()
    }
    
    // Callback for when a connection is established - trigger automatic downloads
    private var onConnectionEstablished: (() -> Unit)? = null
    
    fun setOnConnectionEstablishedCallback(callback: () -> Unit) {
        onConnectionEstablished = callback
    }
    
    fun startAutomaticSharing() {
        Log.d("NearbyConnectionsManager", "Starting automatic sharing mode")
        if (hasRequiredPermissions()) {
            startDiscovery()
            startAdvertising()
            Log.d("NearbyConnectionsManager", "Automatic sharing started - ready to send and receive files")
        } else {
            Log.w("NearbyConnectionsManager", "Cannot start automatic sharing - missing permissions")
        }
    }
    
    fun cleanup() {
        Log.d("NearbyConnectionsManager", "Cleaning up")
        stopDiscovery()
        stopAdvertising()
        connectionsClient.stopAllEndpoints()
        _discoveredEndpoints.value = emptyList()
        _connectedEndpoints.value = emptyList()
        _transferStatus.value = TransferStatus()
    }
}
