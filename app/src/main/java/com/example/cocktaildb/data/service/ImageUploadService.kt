package com.example.cocktaildb.data.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.cocktaildb.BuildConfig
import com.example.cocktaildb.data.model.ImgBBResponse
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class ImageUploadService {

    companion object {
        private const val TAG = "ImageUploadService"
        private const val IMGBB_API_URL = "https://api.imgbb.com/1/upload"
        private const val MAX_IMAGE_SIZE = 1024 * 1024 * 2
    }

    private val gson = Gson()
    suspend fun uploadRecipeImage(context: Context, imageUri: Uri, recipeId: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting ImgBB upload for image: $imageUri")

                if (BuildConfig.IMGBB_API_KEY.isEmpty()) {
                    Log.e(TAG, "ImgBB API key not found in BuildConfig")
                    return@withContext Result.failure(IOException("ImgBB API key not configured"))
                }

                val base64Image = convertUriToBase64(context, imageUri)
                    ?: return@withContext Result.failure(IOException("Failed to convert image to Base64"))
                
                Log.d(TAG, "Image converted to Base64, size: ${base64Image.length} characters")

                val postData = buildString {
                    append("key=").append(URLEncoder.encode(BuildConfig.IMGBB_API_KEY, "UTF-8"))
                    append("&image=").append(URLEncoder.encode(base64Image, "UTF-8"))
                    append("&name=").append(URLEncoder.encode("recipe_${recipeId}_${System.currentTimeMillis()}", "UTF-8"))
                    append("&expiration=0")
                }

                val url = URL(IMGBB_API_URL)
                val connection = url.openConnection() as HttpURLConnection
                
                try {
                    connection.requestMethod = "POST"
                    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                    connection.setRequestProperty("Content-Length", postData.length.toString())
                    connection.doOutput = true
                    connection.connectTimeout = 30000
                    connection.readTimeout = 30000

                    Log.d(TAG, "Sending request to ImgBB API...")

                    val outputStream = connection.outputStream
                    val writer = OutputStreamWriter(outputStream, "UTF-8")
                    writer.write(postData)
                    writer.flush()
                    writer.close()
                    outputStream.close()

                    val responseCode = connection.responseCode
                    Log.d(TAG, "Response code: $responseCode")

                    if (responseCode != HttpURLConnection.HTTP_OK) {
                        val errorStream = connection.errorStream
                        val errorResponse = errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                        Log.e(TAG, "ImgBB API error: $responseCode - $errorResponse")
                        return@withContext Result.failure(IOException("ImgBB upload failed: $responseCode - $errorResponse"))
                    }

                    val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
                    Log.d(TAG, "ImgBB response received: $responseBody")

                    val imgbbResponse = gson.fromJson(responseBody, ImgBBResponse::class.java)
                    
                    if (!imgbbResponse.success) {
                        Log.e(TAG, "ImgBB upload failed: ${imgbbResponse.error?.message}")
                        return@withContext Result.failure(IOException("ImgBB upload failed: ${imgbbResponse.error?.message}"))
                    }
                    
                    val imageUrl = imgbbResponse.data?.url 
                        ?: return@withContext Result.failure(IOException("No image URL in ImgBB response"))
                    
                    Log.d(TAG, "ImgBB upload successful! URL: $imageUrl")
                    
                    Result.success(imageUrl)
                    
                } finally {
                    connection.disconnect()
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error uploading to ImgBB: ${e.message}", e)
                Result.failure(e)
            }
        }
    }

    private fun convertUriToBase64(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return null

            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            
            if (originalBitmap == null) {
                Log.e(TAG, "Failed to decode image from URI")
                return null
            }

            val compressedBitmap = compressBitmap(originalBitmap)

            val outputStream = ByteArrayOutputStream()
            compressedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            val imageBytes = outputStream.toByteArray()
            outputStream.close()
            
            if (imageBytes.size > MAX_IMAGE_SIZE) {
                Log.w(TAG, "Image size (${imageBytes.size}) exceeds limit ($MAX_IMAGE_SIZE), further compression needed")
            }
            
            Log.d(TAG, "Image compressed to ${imageBytes.size} bytes")
            
            Base64.encodeToString(imageBytes, Base64.NO_WRAP)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error converting URI to Base64: ${e.message}", e)
            null
        }
    }

    private fun compressBitmap(original: Bitmap): Bitmap {
        val maxWidth = 1200
        val maxHeight = 1200
        
        val width = original.width
        val height = original.height
        
        if (width <= maxWidth && height <= maxHeight) {
            return original
        }
        
        val ratio = minOf(maxWidth.toFloat() / width, maxHeight.toFloat() / height)
        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()
        
        Log.d(TAG, "Compressing image from ${width}x${height} to ${newWidth}x${newHeight}")
        
        return Bitmap.createScaledBitmap(original, newWidth, newHeight, true)
    }

    fun isImgBBUrl(url: String): Boolean {
        return url.contains("i.ibb.co") || url.contains("imgbb.com")
    }

    suspend fun deleteRecipeImage(imageUrl: String): Result<Boolean> {
        Log.w(TAG, "ImgBB free tier doesn't support image deletion")
        return Result.success(true)
    }
}
