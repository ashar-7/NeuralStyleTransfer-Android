package com.se7en.styletransfer

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import com.theartofdev.edmodo.cropper.CropImage
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: MainViewModel
    private lateinit var styleTransferModel: StyleTransferModel
    private var contentBitmap: Bitmap? = null
    private var styleBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        styleTransferModel = StyleTransferModel(this)

        viewModel = ViewModelProvider(this, object: ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return MainViewModel(styleTransferModel) as T
            }
        }).get()

        selectContentImage.setOnClickListener {
            Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
            }.also { intent ->
                if(intent.resolveActivity(packageManager) != null) {
                    startActivityForResult(intent, CONTENT_IMAGE_PICK)
                }
            }
        }

        selectStyleImage.setOnClickListener {
            Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
            }.also { intent ->
                if(intent.resolveActivity(packageManager) != null) {
                    startActivityForResult(intent, STYLE_IMAGE_PICK)
                }
            }
        }

        generateButton.setOnClickListener {
            onGenerateClicked()
        }

        saveButton.setOnClickListener {
            if(ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PERMISSION_GRANTED) {
                saveOutput()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    PERMISSION_CODE
                )
            }
        }

        viewModel.output.observe(this, Observer {
            outputImage.setImageBitmap(it)
        })
    }

    private fun onGenerateClicked() {
        val content = contentBitmap
        val style = styleBitmap
        when {
            content == null && style == null ->
                Toast.makeText(this, "Please select an input and a style image", Toast.LENGTH_SHORT).show()
            content == null ->
                Toast.makeText(this, "Please select an input image", Toast.LENGTH_SHORT).show()
            style == null ->
                Toast.makeText(this, "Please select a style image", Toast.LENGTH_SHORT).show()

            else -> viewModel.execute(content, style)
        }
    }

    private fun saveOutput() {
        val output = viewModel.output.value
        if(output != null) {
            val uri = Utils.saveBitmapToGallery(this, output)
            if(uri == null) {
                Toast.makeText(this, "Error: Image could not be saved!", Toast.LENGTH_SHORT).show()
            } else {
                val path =
                    if(Build.VERSION.SDK_INT >= 29) "Pictures/MEAM"
                    else uri.toFile().parent

                Toast.makeText(this, "Saved to $path", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == CONTENT_IMAGE_PICK && resultCode == Activity.RESULT_OK) {
            val imageUri = data?.data!!

            val cropIntent = CropImage.activity(imageUri)
                .setAspectRatio(1, 1)
                .setFixAspectRatio(true)
                .getIntent(this)
            startActivityForResult(cropIntent, CONTENT_IMAGE_CROP)
        }

        if(requestCode == STYLE_IMAGE_PICK && resultCode == Activity.RESULT_OK) {
            val imageUri = data?.data!!

            val cropIntent = CropImage.activity(imageUri)
                .setAspectRatio(1, 1)
                .setFixAspectRatio(true)
                .getIntent(this)
            startActivityForResult(cropIntent, STYLE_IMAGE_CROP)
        }

        if(requestCode == CONTENT_IMAGE_CROP && resultCode == Activity.RESULT_OK) {
            contentBitmap = BitmapFactory.decodeStream(
                contentResolver.openInputStream(CropImage.getActivityResult(data).uri)
            )
            contentImage.setImageBitmap(contentBitmap)
        }

        if(requestCode == STYLE_IMAGE_CROP && resultCode == Activity.RESULT_OK) {
            styleBitmap = BitmapFactory.decodeStream(
                contentResolver.openInputStream(CropImage.getActivityResult(data).uri)
            )
            styleImage.setImageBitmap(styleBitmap)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSION_CODE -> {
                if ((grantResults.isNotEmpty() &&
                            grantResults[0] == PERMISSION_GRANTED)) {
                    saveOutput()
                }
            }
        }
    }

    companion object {
        private const val PERMISSION_CODE = 0
        private const val CONTENT_IMAGE_PICK = 1
        private const val STYLE_IMAGE_PICK = 2
        private const val CONTENT_IMAGE_CROP = 3
        private const val STYLE_IMAGE_CROP = 4
    }
}
