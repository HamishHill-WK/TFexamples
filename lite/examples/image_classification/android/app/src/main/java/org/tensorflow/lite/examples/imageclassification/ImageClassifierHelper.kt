/*
 * Copyright 2022 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tensorflow.lite.examples.imageclassification

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.os.SystemClock
import android.util.Log
import android.view.Surface
import androidx.core.graphics.toRect
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.label.Category
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.core.vision.ImageProcessingOptions
import org.tensorflow.lite.task.vision.classifier.Classifications
import org.tensorflow.lite.task.vision.classifier.ImageClassifier

class ImageClassifierHelper(
    var threshold: Float = 0.5f,
    var numThreads: Int = 2,
    var maxResults: Int = 3,
    var currentDelegate: Int = 0,
    var currentModel: Int = 0,
    val context: Context,
    val imageClassifierListener: ClassifierListener?
) {
    private var imageClassifier: ImageClassifier? = null

    init {
        setupImageClassifier()
    }

    fun clearImageClassifier() {
        imageClassifier = null
    }

    private fun setupImageClassifier() {
        val optionsBuilder = ImageClassifier.ImageClassifierOptions.builder()
            .setScoreThreshold(threshold)
            .setMaxResults(maxResults)

        val baseOptionsBuilder = BaseOptions.builder().setNumThreads(numThreads)

        when (currentDelegate) {
            DELEGATE_CPU -> {
                // Default
            }
            DELEGATE_GPU -> {
                if (CompatibilityList().isDelegateSupportedOnThisDevice) {
                    baseOptionsBuilder.useGpu()
                } else {
                    imageClassifierListener?.onError("GPU is not supported on this device")
                }
            }
            DELEGATE_NNAPI -> {
                baseOptionsBuilder.useNnapi()
            }
        }

        optionsBuilder.setBaseOptions(baseOptionsBuilder.build())

        val modelName =
            when (currentModel) {
                MODEL_MOBILENETV1 -> "D6E25_Val_Aug_model5_EN1_82_fp16.tflite"
                MODEL_EFFICIENTNETV0 -> "D6E25_Val_Aug_model5_EN1_82_fp16.tflite"
                MODEL_EFFICIENTNETV1 -> "D6E30_Val_Aug_model5_EN1_76_fp16.tflite"
                MODEL_EFFICIENTNETV2 -> "D6E25_Val_Aug_model5_EN0_80_fp16.tflite"
                else -> "mobilenetv1.tflite"
            }

        //D6E20_NoAug_model2_89_fp16.tflite is the best model so far -hh
        //E10_Val_97_model4_fp16 also performs well when close but requires more testing -hh
        //model2_78 worked well on ROI app
        //mnist failed to indetify any faces correctly with roi
        //

        try {
            imageClassifier =
                ImageClassifier.createFromFileAndOptions(context, modelName, optionsBuilder.build())
        } catch (e: IllegalStateException) {
            imageClassifierListener?.onError(
                "Image classifier failed to initialize. See error logs for details"
            )
            Log.e(TAG, "TFLite failed to load model with error: " + e.message)
        }
    }

    var boxX = 0.0f
    var boxY = 0.0f
    val halfSide = 112.0f
    var rect: RectF = RectF(boxX-halfSide,boxY+halfSide,
        boxX+halfSide,boxY-halfSide)

    fun classify (image: Bitmap, rotation: Int) : MutableList<Category>? {
        if (imageClassifier == null) {
            setupImageClassifier()
        }

        // Inference time is the difference between the system time at the start and finish of the
        // process
        var inferenceTime = SystemClock.uptimeMillis()

        // Create preprocessor for the image.
        // See https://www.tensorflow.org/lite/inference_with_metadata/
        //            lite_support#imageprocessor_architecture
        val imageProcessor =
            ImageProcessor.Builder()
                .build()

        val newImage = Bitmap.createScaledBitmap(image, 224,224, true)
        // Preprocess the image and convert it into a TensorImage for classification.
        val tensorImage = imageProcessor.process(TensorImage.fromBitmap(newImage))


        rect = RectF(boxX-halfSide,boxY+halfSide,
                        boxX+halfSide,boxY-halfSide)


        val rect1 = rect
        Log.d(TAG," ${boxX-halfSide}, ${boxY+halfSide}, ${boxX+halfSide},${boxY-halfSide}")
        //Log.d(TAG," ${rect1.left}, ${rect1.top}, ${rect1.right},${rect1.bottom}")

        val imageProcessingOptions = ImageProcessingOptions.builder().setRoi(rect.toRect())
            .setOrientation(getOrientationFromRotation(rotation))
            .build()

        Log.d(TAG, " ROI : ${imageProcessingOptions.roi}")

        val results = imageClassifier?.classify(tensorImage, imageProcessingOptions)
        inferenceTime = SystemClock.uptimeMillis() - inferenceTime
        imageClassifierListener?.onResults(
            results,
            inferenceTime
        )
        return results?.get(0)?.categories
    }

    fun setWidHeight(height: Float, width: Float){
        boxX = width
        boxY = height
    }
    // Receive the device rotation (Surface.x values range from 0->3) and return EXIF orientation
    // http://jpegclub.org/exif_orientation.html
    private fun getOrientationFromRotation(rotation: Int) : ImageProcessingOptions.Orientation {
        when (rotation) {
            Surface.ROTATION_270 ->
                return ImageProcessingOptions.Orientation.BOTTOM_RIGHT
            Surface.ROTATION_180 ->
                return ImageProcessingOptions.Orientation.RIGHT_BOTTOM
            Surface.ROTATION_90 ->
                return ImageProcessingOptions.Orientation.TOP_LEFT
            else ->
                return ImageProcessingOptions.Orientation.RIGHT_TOP
        }
    }

    interface ClassifierListener {
        fun onError(error: String)
        fun onResults(
            results: List<Classifications>?,
            inferenceTime: Long
        )
    }

    companion object {
        const val DELEGATE_CPU = 0
        const val DELEGATE_GPU = 1
        const val DELEGATE_NNAPI = 2
        const val MODEL_MOBILENETV1 = 0
        const val MODEL_EFFICIENTNETV0 = 1
        const val MODEL_EFFICIENTNETV1 = 2
        const val MODEL_EFFICIENTNETV2 = 3

        private const val TAG = "ImageClassifierHelper"
    }
}
