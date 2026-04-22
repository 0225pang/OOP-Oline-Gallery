package com.example.galleryapp.network

import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

data class FileInfo(
    val fileName: String?,
    val info: String?,
    val mime: String?
)

data class UploadResult(
    val succeed: Boolean,
    val message: String?,
    val savedFileName: String?
)

interface GalleryApi {
    @Multipart
    @POST("file/upload")
    suspend fun uploadFile(
        @Part file: MultipartBody.Part,
        @Part("info") info: RequestBody
    ): UploadResult

    @GET("file/all")
    suspend fun getAllFiles(): List<FileInfo>
}

object ApiClient {
    const val DEFAULT_BASE_URL = "http://43a7939f.r40.cpolar.top/"

    private val logger = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(logger)
        .build()

    fun normalizeBaseUrl(input: String): String {
        var url = input.trim()
        if (url.isEmpty()) {
            url = DEFAULT_BASE_URL
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://$url"
        }
        if (!url.endsWith("/")) {
            url += "/"
        }
        return url
    }

    fun createApi(baseUrl: String): GalleryApi {
        return Retrofit.Builder()
            .baseUrl(normalizeBaseUrl(baseUrl))
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GalleryApi::class.java)
    }
}
