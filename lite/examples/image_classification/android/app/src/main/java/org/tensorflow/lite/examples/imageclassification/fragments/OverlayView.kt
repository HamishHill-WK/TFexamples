/*
 * Copyright 2022 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tensorflow.lite.examples.imageclassification.fragments

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import org.tensorflow.lite.examples.imageclassification.R
import org.tensorflow.lite.task.vision.detector.Detection
import java.util.*
import kotlin.math.max


class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var results: List<Detection> = LinkedList<Detection>()
    private var boxPaint = Paint()
    private var scaleFactor: Float = 1f

    private var bounds = Rect()

    init {
        initPaints()
    }

    fun clear() {
        boxPaint.reset()
        invalidate()
        initPaints()
        Log.d(TAG, "draw called")
    }

    private fun initPaints() {
        boxPaint.color = ContextCompat.getColor(context!!, R.color.bounding_box_color)
        boxPaint.strokeWidth = 8F
        boxPaint.style = Paint.Style.STROKE
    }

    var boundingBox: RectF = RectF()// 100.0f, 100.0f, 200.0f,200.0f)

    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        //for (result in results) {
            //boundingBox = result.boundingBox

            val top = boundingBox.top * scaleFactor
            val bottom = boundingBox.bottom * scaleFactor
            val left = boundingBox.left * scaleFactor
            val right = boundingBox.right * scaleFactor

        Log.d(TAG, "draw called top: ${boundingBox.top}, bottom: ${boundingBox.bottom}, " +
                "left: ${boundingBox.left}, right: ${boundingBox.right}, scale: $scaleFactor")

        Log.d(TAG, "draw called top: $top, bottom: $bottom, left: $left, right: $right")


        // Draw bounding box around detected objects
            val drawableRect = RectF(left, top, right, bottom)
            canvas.drawRect(drawableRect, boxPaint)
       // }
    }

    fun setBox(rect: RectF, imageHeight: Int,
               imageWidth: Int)
    {
        Log.d(TAG, "set box called")
        boundingBox = rect
        //scaleFactor = max(width * 1f /imageWidth , height * 1f / imageHeight)

        postInvalidate()
    }

    fun setResults(
      detectionResults: MutableList<Detection>,
      imageHeight: Int,
      imageWidth: Int,
    ) {
        results = detectionResults

        // PreviewView is in FILL_START mode. So we need to scale up the bounding box to match with
        // the size that the captured images will be displayed.
        scaleFactor = max(width * 1f / imageWidth, height * 1f / imageHeight)
    }

    companion object {
        private const val BOUNDING_RECT_TEXT_PADDING = 8
    }

    private val TAG = "OverlayView.kt"
}
