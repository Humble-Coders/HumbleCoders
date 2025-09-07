package com.humblecoders.humblecoders

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface LuxandApiService {
    
    @Multipart
    @POST("v2/person")
    @Headers("token: ${LuxandConfig.API_TOKEN}")
    suspend fun createPerson(
        @Part("name") name: RequestBody,
        @Part photos: List<MultipartBody.Part>,
        @Part("store") store: RequestBody,
        @Part("collections") collections: RequestBody,
        @Part("unique") unique: RequestBody
    ): Response<CreatePersonResponse>
    
    @Multipart
    @POST("v2/person/{personId}")
    @Headers("token: ${LuxandConfig.API_TOKEN}")
    suspend fun addFace(
        @Path("personId") personId: String,
        @Part photos: List<MultipartBody.Part>,
        @Part("store") store: RequestBody
    ): Response<AddFaceResponse>
    
    @Multipart
    @POST("photo/verify/{uuid}")
    @Headers("token: ${LuxandConfig.API_TOKEN}")
    suspend fun verifyFace(
        @Path("uuid") uuid: String,
        @Part photo: MultipartBody.Part
    ): Response<VerifyFaceResponse>
}

data class CreatePersonResponse(
    val uuid: String,
    val name: String,
    val store: String,
    val created: String,
    val photos: List<String>? = null
)

data class AddFaceResponse(
    val uuid: String,
    val photos: List<String>? = null
)

data class VerifyFaceResponse(
    val status: String,
    val message: String?,
    val probability: Double?,
    val rectangle: FaceRectangle?,
    val rotation: Int?,
    val result: List<VerificationResult>? = null,
    val error: String? = null
)

data class FaceRectangle(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
)

data class VerificationResult(
    val person: String,
    val confidence: Double,
    val face: String
)
