package com.se7en.styletransfer

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

class StyleTransferModel(context: Context) {

    private val predictInterpreter: Interpreter
    private val transferInterpreter: Interpreter

    init {
        predictInterpreter = Interpreter(loadModelFile(context, STYLE_PREDICT_INT8_MODEL))
        transferInterpreter = Interpreter(loadModelFile(context, STYLE_TRANSFER_INT8_MODEL))
    }

    private fun loadModelFile(context: Context, modelFilename: String): ByteBuffer {
        val fileDescriptor = context.assets.openFd(modelFilename)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val file = fileChannel.map(
            FileChannel.MapMode.READ_ONLY,
            fileDescriptor.startOffset,
            fileDescriptor.declaredLength
        )
        fileDescriptor.close()
        return file.asReadOnlyBuffer()
    }

    fun execute(
        contentImage: Bitmap,
        styleImage: Bitmap
    ): Bitmap {
        val contentArray = ImageUtils.bitmapToByteBuffer(contentImage, CONTENT_IMG_SIZE, CONTENT_IMG_SIZE)
        val styleArray = ImageUtils.bitmapToByteBuffer(styleImage, STYLE_IMG_SIZE, STYLE_IMG_SIZE)

        val predictorInputs = arrayOf<Any>(styleArray)
        val predictorOutputs = HashMap<Int, Any>()
        val styleBottleneck = Array(1) { Array(1) { Array(1) { FloatArray(BOTTLENECK_SIZE) } } }
        predictorOutputs[0] = styleBottleneck

        predictInterpreter.runForMultipleInputsOutputs(
            predictorInputs, predictorOutputs
        )

        val styleTransferInputs = arrayOf(contentArray, styleBottleneck)
        val styleTransferOutputs = HashMap<Int, Any>()
        val outputImage = Array(1) { Array(CONTENT_IMG_SIZE) { Array(CONTENT_IMG_SIZE) { FloatArray(3) } } }
        styleTransferOutputs[0] = outputImage

        transferInterpreter.runForMultipleInputsOutputs(
            styleTransferInputs, styleTransferOutputs
        )

        return ImageUtils.convertArrayToBitmap(outputImage, CONTENT_IMG_SIZE, CONTENT_IMG_SIZE)
    }

    companion object {
        private const val STYLE_PREDICT_INT8_MODEL = "image-stylization-v1-256_int8_prediction_1.tflite"
        private const val STYLE_TRANSFER_INT8_MODEL = "image-stylization-v1-256_int8_transfer_1.tflite"
        private const val STYLE_IMG_SIZE = 256
        private const val CONTENT_IMG_SIZE = 384
        private const val BOTTLENECK_SIZE = 100
    }
}