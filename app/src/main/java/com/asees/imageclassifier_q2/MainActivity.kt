package com.asees.imageclassifier_q2

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.asees.imageclassifier_q2.ml.Model
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.min

class MainActivity : AppCompatActivity() {
    private var resultTextView: TextView? = null
    private var confidenceTextView: TextView? = null
    private var imageView: ImageView? = null
    private var takePictureButton: Button? = null
    private val imageSize = 224

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        resultTextView = findViewById<TextView>(R.id.classificationResult)
        imageView = findViewById<ImageView>(R.id.pictureImageView)
        takePictureButton = findViewById<Button>(R.id.takePictureButton)
        takePictureButton!!.setOnClickListener {
            // Launch camera if we have permission
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                startActivityForResult(cameraIntent, 1)
            } else {
                // Request camera permission if we don't have it.
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf<String>(Manifest.permission.CAMERA),
                    100
                )
            }
        }
    }


    // Declare native function for image preprocessing
    external fun preprocessImage(imgData: ByteArray)

    private fun classifyImage(image: Bitmap) {
        try {
            // Preprocess the image using JNI
            val inputByteBuffer = ByteBuffer.allocate(image.byteCount)
            image.copyPixelsToBuffer(inputByteBuffer)
            val imgData = inputByteBuffer.array()
            preprocessImage(imgData)

            val model: Model = Model.newInstance(applicationContext)

            // Creates inputs for reference.
            val inputFeature0: TensorBuffer =
                TensorBuffer.createFixedSize(intArrayOf(1, 224, 224, 3), DataType.FLOAT32)
            val outputByteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3)
            outputByteBuffer.order(ByteOrder.nativeOrder())

            // get 1D array of 224 * 224 pixels in image
            val intValues = IntArray(imageSize * imageSize)
            image.getPixels(
                intValues,
                0,
                image.width,
                0,
                0,
                image.width,
                image.height
            )

            // iterate over pixels and extract R, G, and B values. Add to bytebuffer.
            var pixel = 0
            for (i in 0 until imageSize) {
                for (j in 0 until imageSize) {
                    val `val` = intValues[pixel++] // RGB
                    outputByteBuffer.putFloat((`val` shr 16 and 0xFF) * (1f / 255f))
                    outputByteBuffer.putFloat((`val` shr 8 and 0xFF) * (1f / 255f))
                    outputByteBuffer.putFloat((`val` and 0xFF) * (1f / 255f))
                }
            }
            inputFeature0.loadBuffer(outputByteBuffer)

            // Runs model inference and gets result.
            val outputs: Model.Outputs = model.process(inputFeature0)
            val outputFeature0: TensorBuffer = outputs.getOutputFeature0AsTensorBuffer()
            val confidences: FloatArray = outputFeature0.floatArray
            // find the index of the class with the biggest confidence.
            var maxPos = 0
            var maxConfidence = 0f
            for (i in confidences.indices) {
                if (confidences[i] > maxConfidence) {
                    maxConfidence = confidences[i]
                    maxPos = i
                }
            }
            val classes = arrayOf("Cat", "Dog", "Goat", "Eagle", "Cow", "Insect")
            resultTextView!!.text = classes[maxPos]

            // Releases model resources if no longer used.
            model.close()
        } catch (e: IOException) {
            // TODO Handle the exception
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            var image = data!!.extras!!["data"] as Bitmap?
            val dimension = min(image!!.width.toDouble(), image.height.toDouble())
                .toInt()
            image = ThumbnailUtils.extractThumbnail(image, dimension, dimension)
            imageView!!.setImageBitmap(image)
            image = Bitmap.createScaledBitmap(image, imageSize, imageSize, false)
            classifyImage(image)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    companion object {
        // Load native library
        init {
            System.loadLibrary("native-lib")
        }
    }
}
