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
 * Simple utility class for loading images from URLs
 * without using external libraries
 */
object ImageLoader {

    /**
     * Loads an image from a URL into an ImageView
     * @param url The URL of the image to load
     * @param imageView The ImageView to load the image into
     * @param placeholderResId The resource ID of a placeholder image to show while loading
     */
    fun loadImage(url: String?, imageView: ImageView, placeholderResId: Int) {
        if (url.isNullOrEmpty()) {
            imageView.setImageResource(placeholderResId)
            return
        }

        // Set placeholder while loading
        imageView.setImageResource(placeholderResId)

        if (url.startsWith("content://") || url.startsWith("file://")) {
            try {

                if (url.contains("com.miui.gallery.open") || url.contains("gallery.open")) {
                    println("ImageLoader: Detected temporary gallery URI, using placeholder: $url")
                    imageView.setImageResource(placeholderResId)
                    return
                }
                
                val uri = android.net.Uri.parse(url)
                imageView.setImageURI(uri)
                println("ImageLoader: Loaded local image: $url")
            } catch (e: Exception) {
                println("ImageLoader: Failed to load local image: ${e.message}")
                imageView.setImageResource(placeholderResId)
            }
        } else {
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
                    e.printStackTrace()
                    imageView.setImageResource(placeholderResId)
                }
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
        return try {
            Log.d("ImageLoader", "Starting to copy image from URI: $sourceUri")
            val imagesDir = File(context.filesDir, "recipe_images")
            if (!imagesDir.exists()) {
                val created = imagesDir.mkdirs()
                Log.d("ImageLoader", "Created images directory: $created at ${imagesDir.absolutePath}")
            } else {
                Log.d("ImageLoader", "Images directory already exists at: ${imagesDir.absolutePath}")
            }
            val fileName = "recipe_${UUID.randomUUID()}.jpg"
            val destinationFile = File(imagesDir, fileName)
            Log.d("ImageLoader", "Target file: ${destinationFile.absolutePath}")

            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                FileOutputStream(destinationFile).use { outputStream ->
                    val bytesWritten = inputStream.copyTo(outputStream)
                    Log.d("ImageLoader", "Copied $bytesWritten bytes")
                }
            }
            
            if (destinationFile.exists() && destinationFile.length() > 0) {
                Log.d("ImageLoader", "Image copied successfully to: ${destinationFile.absolutePath}, size: ${destinationFile.length()} bytes")
                destinationFile.absolutePath
            } else {
                Log.e("ImageLoader", "File was not created or is empty")
                null
            }
            
        } catch (e: IOException) {
            Log.e("ImageLoader", "IOException copying image: ${e.message}", e)
            null
        } catch (e: Exception) {
            Log.e("ImageLoader", "Unexpected error copying image: ${e.message}", e)
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
