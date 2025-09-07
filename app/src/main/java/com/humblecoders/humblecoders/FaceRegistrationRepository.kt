package com.humblecoders.humblecoders

import android.graphics.Bitmap
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class FaceRegistrationRepository {
    
    private val apiService: LuxandApiService
    
    init {
        val logging = HttpLoggingInterceptor()
        logging.setLevel(HttpLoggingInterceptor.Level.BODY)
        
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()
        
        val retrofit = Retrofit.Builder()
            .baseUrl(LuxandConfig.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        apiService = retrofit.create(LuxandApiService::class.java)
    }
    
    suspend fun createPerson(firebaseUid: String, faceImages: List<Bitmap>): Result<String> {
        return try {
            val name = firebaseUid.toRequestBody("text/plain".toMediaTypeOrNull())
            val store = "1".toRequestBody("text/plain".toMediaTypeOrNull())
            val collections = "".toRequestBody("text/plain".toMediaTypeOrNull())
            val unique = "0".toRequestBody("text/plain".toMediaTypeOrNull())
            
            val photos = faceImages.map { bitmap ->
                val imageFile = bitmapToFile(bitmap)
                val requestBody = imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                MultipartBody.Part.createFormData("photos", imageFile.name, requestBody)
            }
            
            val response = apiService.createPerson(name, photos, store, collections, unique)
            
            if (response.isSuccessful) {
                val personId = response.body()?.uuid
                if (personId != null) {
                    Result.success(personId)
                } else {
                    Result.failure(Exception("Failed to create person"))
                }
            } else {
                Result.failure(Exception("API Error: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun addFaceToPerson(personId: String, bitmap: Bitmap): Result<Boolean> {
        return try {
            val imageFile = bitmapToFile(bitmap)
            val requestBody = imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val imagePart = MultipartBody.Part.createFormData("photos", imageFile.name, requestBody)
            val store = "1".toRequestBody("text/plain".toMediaTypeOrNull())
            
            val response = apiService.addFace(personId, listOf(imagePart), store)
            
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                Result.failure(Exception("API Error: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun verifyFace(personUuid: String, bitmap: Bitmap): Result<VerifyFaceResponse> {
        return try {
            val imageFile = bitmapToFile(bitmap)
            val requestBody = imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val imagePart = MultipartBody.Part.createFormData("photo", imageFile.name, requestBody)
            
            val response = apiService.verifyFace(personUuid, imagePart)
            
            if (response.isSuccessful) {
                val verifyResponse = response.body()
                if (verifyResponse != null) {
                    Result.success(verifyResponse)
                } else {
                    Result.failure(Exception("Failed to verify face"))
                }
            } else {
                Result.failure(Exception("API Error: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun bitmapToFile(bitmap: Bitmap): File {
        val file = File.createTempFile("face_image", ".jpg")
        val outputStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
        outputStream.flush()
        outputStream.close()
        return file
    }
}
