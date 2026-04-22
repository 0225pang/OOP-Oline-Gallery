package com.example.galleryapp.network

import android.content.Context
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class GalleryRepository {
    private var baseUrl: String = ApiClient.DEFAULT_BASE_URL
    private val httpClient = OkHttpClient()

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

    suspend fun deleteImage(fileName: String): Result<UploadResult> {
        return runCatching {
            api().deleteFile(fileName)
        }
    }

    suspend fun saveRemoteImageToAlbum(context: Context, fileInfo: FileInfo): Result<String> {
        return runCatching {
            withContext(Dispatchers.IO) {
                val fileName = fileInfo.fileName ?: error("missing file name")
                val url = "${baseUrl}file/${Uri.encode(fileName)}"
                val request = Request.Builder().url(url).get().build()
                val response = httpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    response.close()
                    error("download failed: http ${response.code}")
                }

                val bytes = response.body?.bytes() ?: run {
                    response.close()
                    error("empty image body")
                }
                val detectedMime = fileInfo.mime
                    ?: response.header("Content-Type")
                    ?: "image/jpeg"
                response.close()

                val saveName = buildSaveName(fileName, detectedMime)
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, saveName)
                    put(MediaStore.Images.Media.MIME_TYPE, detectedMime)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/GalleryComposeApp")
                    }
                }

                val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: throw IOException("failed to create media item")
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(bytes)
                    output.flush()
                } ?: throw IOException("failed to open media output")

                uri.toString()
            }
        }
    }

    private fun buildSaveName(fileName: String, mime: String): String {
        if (fileName.contains(".")) {
            return fileName
        }
        val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: "jpg"
        return "$fileName.$ext"
    }

    private fun queryFileName(context: Context, uri: Uri): String? {
        return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
        }
    }
}
