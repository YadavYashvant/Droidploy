package com.example.droidploy.utils

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

object FileManager {

    fun copyProjectFromUri(context: Context, uri: Uri, destName: String): String? {
        val destDir = File(context.filesDir, destName)
        if (destDir.exists()) destDir.deleteRecursively()
        destDir.mkdirs()

        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                // Check if it's a zip file (magic bytes PK)
                // For simplicity, we assume the user selects a zip file for now
                // or a folder if using ACTION_OPEN_DOCUMENT_TREE (which requires different handling)
                
                // If it's a zip:
                ZipInputStream(inputStream).use { zipStream ->
                    var entry = zipStream.nextEntry
                    while (entry != null) {
                        val newFile = File(destDir, entry.name)
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
            return destDir.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
