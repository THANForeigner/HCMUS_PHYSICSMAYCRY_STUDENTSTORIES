package com.example.afinal.data.network

import com.example.afinal.data.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {
  @Multipart
  @POST("api/process")
  suspend fun uploadAudio(
          @Part audio_file: MultipartBody.Part?,
          @Part("text_input") textInput: RequestBody?,
          @Part("list_tags") tags: RequestBody,
          @Part("title") title: RequestBody,
          @Part("user_id") userId: RequestBody,
          @Part("collection_name") collectionName: RequestBody
  ): Response<GenericResponse>

  @POST("api/search") suspend fun search(@Body req: SearchRequest): Response<ApiResponse>

  @POST("api/recommend")
  suspend fun getRecommendations(@Body req: RecommendRequest): Response<ApiResponse>

  @POST("api/interact")
  suspend fun sendInteraction(@Body req: InteractRequest): Response<GenericResponse>

  @POST("api/comment") suspend fun sendComment(@Body req: CommentRequest): Response<GenericResponse>
}

object RetrofitClient {
  private const val BASE_URL = "https://emergently-basipetal-marge.ngrok-free.dev"

  val api: ApiService by lazy {
    Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
  }
}
