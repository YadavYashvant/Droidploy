package com.example.droidploy.utils

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

object FileManager {

    fun copyProjectFromUri(context: Context, uri: Uri, destName: String = "user_project_${System.currentTimeMillis()}"): String? {
        val tempDir = File(context.filesDir, "${destName}_temp")
        val destDir = File(context.filesDir, destName)

        if (tempDir.exists()) tempDir.deleteRecursively()
        if (destDir.exists()) destDir.deleteRecursively()
        tempDir.mkdirs()

        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                // Extract zip to temporary directory
                ZipInputStream(inputStream).use { zipStream ->
                    var entry = zipStream.nextEntry
                    while (entry != null) {
                        val newFile = File(tempDir, entry.name)
                        if (entry.isDirectory) {
                            newFile.mkdirs()
                        } else {
                            newFile.parentFile?.mkdirs()
                            FileOutputStream(newFile).use { output ->
                                zipStream.copyTo(output)
                            }
                        }
                        zipStream.closeEntry()
                        entry = zipStream.nextEntry
                    }
                }
            }

            // Check if there's a single root folder and flatten if needed
            val extractedFiles = tempDir.listFiles() ?: emptyArray()

            if (extractedFiles.size == 1 && extractedFiles[0].isDirectory) {
                // Single root folder detected, move its contents to destDir
                val rootFolder = extractedFiles[0]
                rootFolder.renameTo(destDir)
                tempDir.deleteRecursively()
                android.util.Log.d("FileManager", "Flattened single root folder structure")
            } else {
                // Multiple files/folders at root, just rename temp to dest
                tempDir.renameTo(destDir)
            }

            android.util.Log.d("FileManager", "Project extracted to: ${destDir.absolutePath}")

            // List extracted files for debugging
            destDir.listFiles()?.forEach { file ->
                android.util.Log.d("FileManager", "Extracted: ${file.name}")
            }

            return destDir.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            android.util.Log.e("FileManager", "Failed to extract project", e)
            tempDir.deleteRecursively()
            destDir.deleteRecursively()
            return null
        }
    }
}
