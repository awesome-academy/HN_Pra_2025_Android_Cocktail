package com.example.cocktaildb.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.widget.ImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/**
 * Comprehensive image loading and management utility
 * Combines image loading from URLs and local file management
 */
object ImageLoader {

    private const val TAG = "ImageLoader"
    
    // Constants for temporary URI patterns that should be avoided
    private val TEMPORARY_URI_PATTERNS = listOf(
        "com.miui.gallery.open",
        "gallery.open",
        "com.android.providers.media.documents/temp",
        "com.google.android.apps.photos.content",
        ".tmp",
        "/temp/",
        "/cache/"
    )
    
    // Schemes that indicate local content
    private val LOCAL_URI_SCHEMES = listOf("content://", "file://")
    
    // Schemes that indicate remote content
    private val REMOTE_URI_SCHEMES = listOf("http://", "https://")

    /**
     * Check if a URI is temporary/invalid and should not be persisted
     */
    private fun isTemporaryUri(url: String): Boolean {
        return TEMPORARY_URI_PATTERNS.any { pattern ->
            url.contains(pattern, ignoreCase = true)
        }
    }
    
    /**
     * Check if a URI is a local content URI
     */
    private fun isLocalUri(url: String): Boolean {
        return LOCAL_URI_SCHEMES.any { scheme ->
            url.startsWith(scheme, ignoreCase = true)
        }
    }
    
    /**
     * Check if a URI is a remote HTTP/HTTPS URL
     */
    private fun isRemoteUri(url: String): Boolean {
        return REMOTE_URI_SCHEMES.any { scheme ->
            url.startsWith(scheme, ignoreCase = true)
        }
    }

    fun loadImage(url: String?, imageView: ImageView, placeholderResId: Int) {
        if (url.isNullOrEmpty()) {
            imageView.setImageResource(placeholderResId)
            return
        }

        // Set placeholder while loading
        imageView.setImageResource(placeholderResId)

        when {
            isLocalUri(url) -> {
                try {
                    // Check if this is a temporary/invalid URI
                    if (isTemporaryUri(url)) {
                        Log.w(TAG, "Detected temporary/invalid URI, using placeholder: $url")
                        imageView.setImageResource(placeholderResId)
                        return
                    }
                    
                    val uri = android.net.Uri.parse(url)
                    imageView.setImageURI(uri)
                    Log.d(TAG, "Loaded local image: $url")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load local image: ${e.message}")
                    imageView.setImageResource(placeholderResId)
                }
            }
            isRemoteUri(url) -> {
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val bitmap = withContext(Dispatchers.IO) {
                            downloadImage(url)
                        }
                        if (bitmap != null) {
                            imageView.setImageBitmap(bitmap)
                        } else {
                            imageView.setImageResource(placeholderResId)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to download image: ${e.message}")
                        imageView.setImageResource(placeholderResId)
                    }
                }
            }
            else -> {
                Log.w(TAG, "Unknown URI scheme, using placeholder: $url")
                imageView.setImageResource(placeholderResId)
            }
        }
    }

    private suspend fun downloadImage(imageUrl: String): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(imageUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.connect()
                
                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val input: InputStream = connection.inputStream
                    val bitmap = BitmapFactory.decodeStream(input)
                    input.close()
                    connection.disconnect()
                    bitmap
                } else {
                    connection.disconnect()
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    fun copyImageToInternalStorage(context: Context, sourceUri: Uri): String? {
        val uriString = sourceUri.toString()

        if (isTemporaryUri(uriString)) {
            Log.w(TAG, "Attempting to copy temporary URI, this may fail: $uriString")
        }
        
        return try {
            Log.d(TAG, "Starting to copy image from URI: $sourceUri")

            val imagesDir = File(context.filesDir, "recipe_images")
            if (!imagesDir.exists()) {
                val created = imagesDir.mkdirs()
                Log.d(TAG, "Created images directory: $created at ${imagesDir.absolutePath}")
            } else {
                Log.d(TAG, "Images directory already exists at: ${imagesDir.absolutePath}")
            }

            val fileName = "recipe_${UUID.randomUUID()}.jpg"
            val destinationFile = File(imagesDir, fileName)
            Log.d(TAG, "Target file: ${destinationFile.absolutePath}")

            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                FileOutputStream(destinationFile).use { outputStream ->
                    val bytesWritten = inputStream.copyTo(outputStream)
                    Log.d(TAG, "Copied $bytesWritten bytes")
                }
            }
            
            if (destinationFile.exists() && destinationFile.length() > 0) {
                Log.d(TAG, "Image copied successfully to: ${destinationFile.absolutePath}, size: ${destinationFile.length()} bytes")
                destinationFile.absolutePath
            } else {
                Log.e(TAG, "File was not created or is empty")
                null
            }
            
        } catch (e: IOException) {
            Log.e(TAG, "IOException copying image: ${e.message}", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error copying image: ${e.message}", e)
            null
        }
    }
    fun getImageFile(context: Context, fileName: String): File {
        val imagesDir = File(context.filesDir, "recipe_images")
        return File(imagesDir, fileName)
    }

    fun deleteImageFile(context: Context, filePath: String): Boolean {
        return try {
            val file = File(filePath)
            if (file.exists()) {
                file.delete()
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("ImageLoader", "Failed to delete image: ${e.message}")
            false
        }
    }

    fun getFileUri(filePath: String): String {
        return "file://$filePath"
    }
}
