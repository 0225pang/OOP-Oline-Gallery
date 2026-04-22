package com.example.galleryapp.network

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class GalleryRepository {
    private var baseUrl: String = ApiClient.DEFAULT_BASE_URL

    fun getBaseUrl(): String = baseUrl

    fun setBaseUrl(newBaseUrl: String) {
        baseUrl = ApiClient.normalizeBaseUrl(newBaseUrl)
    }

    private fun api(): GalleryApi = ApiClient.createApi(baseUrl)

    suspend fun getAllImages(): Result<List<FileInfo>> {
        return runCatching { api().getAllFiles() }
    }

    suspend fun uploadImage(context: Context, uri: Uri, info: String): Result<UploadResult> {
        return runCatching {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: error("read image failed")

            val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
            val fileName = queryFileName(context, uri) ?: "upload_${System.currentTimeMillis()}.jpg"

            val fileBody = bytes.toRequestBody(mime.toMediaType())
            val filePart = MultipartBody.Part.createFormData("file", fileName, fileBody)
            val infoBody = info.toRequestBody("text/plain".toMediaType())

            api().uploadFile(filePart, infoBody)
        }
    }

    private fun queryFileName(context: Context, uri: Uri): String? {
        return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
        }
    }
}
