package com.example.moneytracker

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class FileManager(private val context: Context) {

    companion object {
        private const val TAG = "FileManager"
        private const val MAX_WIDTH = 1920
        private const val MAX_HEIGHT = 1920
        private const val COMPRESSION_QUALITY = 85
    }

    fun saveImageToInternalStorage(bitmap: Bitmap): String? {
        return try {
            val filename = "IMG_${System.currentTimeMillis()}.jpg"
            val file = File(context.filesDir, filename)

            val optimizedBitmap = optimizeBitmap(bitmap)

            FileOutputStream(file).use { fos ->
                optimizedBitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESSION_QUALITY, fos)
            }

            if (optimizedBitmap != bitmap) {
                optimizedBitmap.recycle()
            }

            Log.d(TAG, "Imagen guardada: ${file.absolutePath}")
            file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error guardando imagen", e)
            null
        }
    }

    fun getImageFromInternalStorage(path: String): Bitmap? {
        return try {
            val file = File(path)
            if (file.exists()) {
                BitmapFactory.decodeFile(path)
            } else {
                Log.w(TAG, "Archivo no encontrado: $path")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cargando imagen", e)
            null
        }
    }

    fun deleteImageFromInternalStorage(path: String): Boolean {
        return try {
            val file = File(path)
            if (file.exists()) {
                val deleted = file.delete()
                Log.d(TAG, "Imagen eliminada: $path - Success: $deleted")
                deleted
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error eliminando imagen", e)
            false
        }
    }

    fun copyImageFromUri(uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val bitmap = BitmapFactory.decodeStream(inputStream)
                val correctedBitmap = fixOrientation(uri, bitmap)
                saveImageToInternalStorage(correctedBitmap)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error copiando imagen desde URI", e)
            null
        }
    }

    fun cleanupOrphanImages(validPaths: List<String?>): Int {
        var deletedCount = 0
        context.filesDir.listFiles()?.forEach { file ->
            if (file.isFile && file.name.startsWith("IMG_") && file.name.endsWith(".jpg")) {
                if (!validPaths.contains(file.absolutePath)) {
                    if (file.delete()) {
                        deletedCount++
                        Log.d(TAG, "Imagen huérfana eliminada: ${file.name}")
                    }
                }
            }
        }
        Log.d(TAG, "Total de imágenes huérfanas eliminadas: $deletedCount")
        return deletedCount
    }


    private fun optimizeBitmap(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= MAX_WIDTH && height <= MAX_HEIGHT) {
            return bitmap
        }

        val scale = minOf(
            MAX_WIDTH.toFloat() / width,
            MAX_HEIGHT.toFloat() / height
        )

        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun fixOrientation(uri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val exif = ExifInterface(inputStream)
                val orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )

                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180f)
                    ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270f)
                    else -> bitmap
                }
            } ?: bitmap
        } catch (e: IOException) {
            Log.e(TAG, "Error corrigiendo orientación", e)
            bitmap
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}