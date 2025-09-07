package com.humblecoders.humblecoders

import android.content.Context
import android.net.wifi.p2p.WifiP2pInfo
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.*
import java.net.*
import kotlinx.coroutines.*

class FileTransferManager(private val context: Context) {
    
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val _transferStatus = MutableStateFlow<TransferStatus>(TransferStatus())
    val transferStatus: StateFlow<TransferStatus> = _transferStatus.asStateFlow()
    
    private val _receivedFiles = MutableStateFlow<List<String>>(emptyList())
    val receivedFiles: StateFlow<List<String>> = _receivedFiles.asStateFlow()
    
    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null
    
    data class TransferStatus(
        val isTransferring: Boolean = false,
        val progress: Int = 0,
        val fileName: String = "",
        val isReceiving: Boolean = false,
        val error: String? = null
    )
    
    fun startServer(port: Int = 8888) {
        coroutineScope.launch {
            try {
                serverSocket = ServerSocket(port)
                _transferStatus.value = TransferStatus(isReceiving = true)
                Log.d("FileTransferManager", "Server started on port $port")
                
                while (true) {
                    val socket = serverSocket?.accept()
                    if (socket != null) {
                        handleIncomingConnection(socket)
                    }
                }
            } catch (e: Exception) {
                Log.e("FileTransferManager", "Server error", e)
                _transferStatus.value = TransferStatus(error = e.message)
            }
        }
    }
    
    fun sendFile(filePath: String, targetIp: String, port: Int = 8888) {
        coroutineScope.launch {
            try {
                _transferStatus.value = TransferStatus(isTransferring = true, fileName = filePath)
                
                val file = File(filePath)
                if (!file.exists()) {
                    _transferStatus.value = TransferStatus(error = "File not found: $filePath")
                    return@launch
                }
                
                clientSocket = Socket(targetIp, port)
                val outputStream = clientSocket?.getOutputStream()
                val fileInputStream = FileInputStream(file)
                
                // Send file name and size
                val fileName = file.name
                val fileSize = file.length()
                
                val fileNameBytes = fileName.toByteArray(Charsets.UTF_8)
                val fileNameLength = fileNameBytes.size
                
                // Send file name length, file name, and file size
                outputStream?.write(intToByteArray(fileNameLength))
                outputStream?.write(fileNameBytes)
                outputStream?.write(longToByteArray(fileSize))
                
                // Send file content
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalBytesRead = 0
                
                while (fileInputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream?.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead
                    
                    val progress = ((totalBytesRead * 100) / fileSize).toInt()
                    _transferStatus.value = TransferStatus(
                        isTransferring = true,
                        progress = progress,
                        fileName = fileName
                    )
                }
                
                fileInputStream.close()
                clientSocket?.close()
                
                _transferStatus.value = TransferStatus(isTransferring = false)
                Log.d("FileTransferManager", "File sent successfully: $fileName")
                
            } catch (e: Exception) {
                Log.e("FileTransferManager", "Send error", e)
                _transferStatus.value = TransferStatus(error = e.message)
            }
        }
    }
    
    private suspend fun handleIncomingConnection(socket: Socket) {
        try {
            val inputStream = socket.getInputStream()
            
            // Read file name length
            val fileNameLengthBytes = ByteArray(4)
            inputStream.read(fileNameLengthBytes)
            val fileNameLength = byteArrayToInt(fileNameLengthBytes)
            
            // Read file name
            val fileNameBytes = ByteArray(fileNameLength)
            inputStream.read(fileNameBytes)
            val fileName = String(fileNameBytes, Charsets.UTF_8)
            
            // Read file size
            val fileSizeBytes = ByteArray(8)
            inputStream.read(fileSizeBytes)
            val fileSize = byteArrayToLong(fileSizeBytes)
            
            Log.d("FileTransferManager", "Receiving file: $fileName, size: $fileSize")
            
            // Create received files directory
            val receivedDir = File(context.filesDir, "received_videos")
            if (!receivedDir.exists()) {
                receivedDir.mkdirs()
            }
            
            val receivedFile = File(receivedDir, fileName)
            val fileOutputStream = FileOutputStream(receivedFile)
            
            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalBytesRead = 0
            
            while (totalBytesRead < fileSize) {
                bytesRead = inputStream.read(buffer, 0, minOf(buffer.size, (fileSize - totalBytesRead).toInt()))
                if (bytesRead == -1) break
                
                fileOutputStream.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead
                
                val progress = ((totalBytesRead * 100) / fileSize).toInt()
                _transferStatus.value = TransferStatus(
                    isReceiving = true,
                    progress = progress,
                    fileName = fileName
                )
            }
            
            fileOutputStream.close()
            socket.close()
            
            // Add to received files list
            val currentFiles = _receivedFiles.value.toMutableList()
            currentFiles.add(receivedFile.absolutePath)
            _receivedFiles.value = currentFiles
            
            _transferStatus.value = TransferStatus(isReceiving = false)
            Log.d("FileTransferManager", "File received successfully: $fileName")
            
        } catch (e: Exception) {
            Log.e("FileTransferManager", "Receive error", e)
            _transferStatus.value = TransferStatus(error = e.message)
        }
    }
    
    fun stopServer() {
        try {
            serverSocket?.close()
            clientSocket?.close()
            _transferStatus.value = TransferStatus(isReceiving = false)
        } catch (e: Exception) {
            Log.e("FileTransferManager", "Error stopping server", e)
        }
    }
    
    fun cleanup() {
        coroutineScope.cancel()
        stopServer()
    }
    
    // Helper functions for byte array conversions
    private fun intToByteArray(value: Int): ByteArray {
        return byteArrayOf(
            (value shr 24).toByte(),
            (value shr 16).toByte(),
            (value shr 8).toByte(),
            value.toByte()
        )
    }
    
    private fun byteArrayToInt(bytes: ByteArray): Int {
        return (bytes[0].toInt() and 0xFF shl 24) or
                (bytes[1].toInt() and 0xFF shl 16) or
                (bytes[2].toInt() and 0xFF shl 8) or
                (bytes[3].toInt() and 0xFF)
    }
    
    private fun longToByteArray(value: Long): ByteArray {
        return byteArrayOf(
            (value shr 56).toByte(),
            (value shr 48).toByte(),
            (value shr 40).toByte(),
            (value shr 32).toByte(),
            (value shr 24).toByte(),
            (value shr 16).toByte(),
            (value shr 8).toByte(),
            value.toByte()
        )
    }
    
    private fun byteArrayToLong(bytes: ByteArray): Long {
        return (bytes[0].toLong() and 0xFF shl 56) or
                (bytes[1].toLong() and 0xFF shl 48) or
                (bytes[2].toLong() and 0xFF shl 40) or
                (bytes[3].toLong() and 0xFF shl 32) or
                (bytes[4].toLong() and 0xFF shl 24) or
                (bytes[5].toLong() and 0xFF shl 16) or
                (bytes[6].toLong() and 0xFF shl 8) or
                (bytes[7].toLong() and 0xFF)
    }
}
