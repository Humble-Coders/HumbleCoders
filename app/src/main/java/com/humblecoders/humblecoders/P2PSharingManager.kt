package com.humblecoders.humblecoders

import android.content.Context
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.*
import java.io.File

class P2PSharingManager(private val context: Context) {
    
    private val wifiDirectManager = WiFiDirectManager(context)
    private val fileTransferManager = FileTransferManager(context)
    
    private val _isSharing = MutableStateFlow(false)
    val isSharing: StateFlow<Boolean> = _isSharing.asStateFlow()
    
    private val _connectedDevices = MutableStateFlow<List<WifiP2pDevice>>(emptyList())
    val connectedDevices: StateFlow<List<WifiP2pDevice>> = _connectedDevices.asStateFlow()
    
    private val _transferStatus = MutableStateFlow<FileTransferManager.TransferStatus>(FileTransferManager.TransferStatus())
    val transferStatus: StateFlow<FileTransferManager.TransferStatus> = _transferStatus.asStateFlow()
    
    private val _receivedFiles = MutableStateFlow<List<String>>(emptyList())
    val receivedFiles: StateFlow<List<String>> = _receivedFiles.asStateFlow()
    
    init {
        // Observe WiFi Direct connection info
        CoroutineScope(Dispatchers.Main).launch {
            wifiDirectManager.connectionInfo.collect { info ->
                if (info != null && info.groupFormed) {
                    _isSharing.value = true
                    startFileServer()
                } else {
                    _isSharing.value = false
                    fileTransferManager.stopServer()
                }
            }
        }
        
        // Observe discovered devices
        CoroutineScope(Dispatchers.Main).launch {
            wifiDirectManager.discoveredDevices.collect { devices ->
                _connectedDevices.value = devices
            }
        }
        
        // Observe transfer status
        CoroutineScope(Dispatchers.Main).launch {
            fileTransferManager.transferStatus.collect { status ->
                _transferStatus.value = status
            }
        }
        
        // Observe received files
        CoroutineScope(Dispatchers.Main).launch {
            fileTransferManager.receivedFiles.collect { files ->
                _receivedFiles.value = files
            }
        }
    }
    
    fun startDiscovery() {
        Log.d("P2PSharingManager", "Starting device discovery")
        wifiDirectManager.startDiscovery()
    }
    
    fun stopDiscovery() {
        Log.d("P2PSharingManager", "Stopping device discovery")
        wifiDirectManager.stopDiscovery()
    }
    
    fun connectToDevice(device: WifiP2pDevice) {
        Log.d("P2PSharingManager", "Connecting to device: ${device.deviceName}")
        wifiDirectManager.connectToDevice(device)
    }
    
    fun disconnect() {
        Log.d("P2PSharingManager", "Disconnecting from all devices")
        wifiDirectManager.disconnect()
        fileTransferManager.stopServer()
    }
    
    fun sendVideoPart(filePath: String, targetDevice: WifiP2pDevice) {
        CoroutineScope(Dispatchers.Main).launch {
            val connectionInfo = wifiDirectManager.connectionInfo.value
            if (connectionInfo != null && connectionInfo.groupFormed) {
                val targetIp = if (connectionInfo.isGroupOwner) {
                    connectionInfo.groupOwnerAddress.hostAddress
                } else {
                    connectionInfo.groupOwnerAddress.hostAddress
                }
                
                if (targetIp != null) {
                    Log.d("P2PSharingManager", "Sending file to $targetIp: $filePath")
                    fileTransferManager.sendFile(filePath, targetIp)
                } else {
                    Log.e("P2PSharingManager", "Target IP not available")
                }
            } else {
                Log.e("P2PSharingManager", "Not connected to any device")
            }
        }
    }
    
    private fun startFileServer() {
        Log.d("P2PSharingManager", "Starting file server")
        fileTransferManager.startServer()
    }
    
    fun getReceivedVideoParts(videoTitle: String): List<String> {
        val receivedDir = File(context.filesDir, "received_videos")
        if (!receivedDir.exists()) return emptyList()
        
        return receivedDir.listFiles()
            ?.filter { file -> file.isFile && file.name.startsWith(videoTitle.replace(" ", "_")) }
            ?.sortedBy { file -> file.name }
            ?.map { file -> file.absolutePath }
            ?: emptyList()
    }
    
    fun clearReceivedFiles() {
        val receivedDir = File(context.filesDir, "received_videos")
        if (receivedDir.exists()) {
            receivedDir.deleteRecursively()
            receivedDir.mkdirs()
        }
        _receivedFiles.value = emptyList()
    }
    
    fun cleanup() {
        wifiDirectManager.cleanup()
        fileTransferManager.cleanup()
    }
}
