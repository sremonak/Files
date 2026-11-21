package com.sremonak.files.util

import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import android.net.Uri
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File

object IntentUtils {

    fun providerUri(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.provider", file)

    /** Open a file with any capable app (viewer / player / editor of the user's choice). */
    fun openFile(context: Context, file: File): Boolean {
        val uri = providerUri(context, file)
        val mime = FileUtils.mimeFor(file) ?: "*/*"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return try {
            context.startActivity(Intent.createChooser(intent, "Open with"))
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }

    /** Share one or many files through the system share sheet. */
    fun shareFiles(context: Context, files: List<File>) {
        if (files.isEmpty()) return
        val uris = files.map { providerUri(context, it) }
        val intent = if (files.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = FileUtils.mimeFor(files[0]) ?: "*/*"
                putExtra(Intent.EXTRA_STREAM, uris[0])
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "*/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            }
        }
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {
            context.startActivity(Intent.createChooser(intent, "Share"))
        } catch (_: ActivityNotFoundException) {
            // No share target available.
        }
    }

    @Suppress("DEPRECATION")
    fun setAsWallpaper(context: Context, file: File): Boolean {
        return try {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return false
            android.app.WallpaperManager.getInstance(context).setBitmap(bitmap)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun setAsRingtone(context: Context, file: File): Boolean {
        return try {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, file.name)
                put(MediaStore.Audio.Media.MIME_TYPE, FileUtils.mimeFor(file) ?: "audio/*")
                put(MediaStore.Audio.Media.IS_RINGTONE, true)
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values) ?: return false
            resolver.openOutputStream(uri)?.use { out ->
                file.inputStream().use { input -> input.copyTo(out) }
            }
            val done = ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) }
            resolver.update(uri, done, null, null)
            RingtoneManager.setActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE, uri)
            true
        } catch (_: Exception) {
            false
        }
    }
}
