package com.se7en.styletransfer

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.net.toUri
import java.io.File
import java.util.*

object Utils {
    fun saveBitmapToGallery(context: Context, bitmap: Bitmap): Uri? {
        val uri: Uri?
        val filename = UUID.randomUUID().toString()
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/StyleTransfer")
            }

            uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            uri?.apply {
                resolver.openOutputStream(this).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
                }
            }
        } else {
            val rootDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES
            ).toString()
            val root = File("$rootDir/StyleTransfer")
            root.mkdirs()

            File(root, "$filename.png").let {
                if (it.exists()) it.delete()
                it.createNewFile()

                it.outputStream().use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
                }
                uri = it.toUri()
            }
        }

        MediaScannerConnection.scanFile(context, arrayOf(uri?.path), null
        ) { _, _ ->  }

        return uri
    }
}
