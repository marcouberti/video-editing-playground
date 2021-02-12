package com.marcouberti.video.playground.ui.transformations

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.MotionEvent

class TransformationsGLSurfaceView(context: Context, attrs: AttributeSet?) : GLSurfaceView(context, attrs) {
    enum class TouchBehavior {
        ROTATE, TRANSLATE
    }

    private val renderer: TransformationsRenderer

    private val TOUCH_ROTATE_FACTOR_W: Float = 180.0f / 1080f
    private val TOUCH_ROTATE_FACTOR_H: Float = -180.0f / 1920f
    private val TOUCH_TRANSLATE_FACTOR_W: Float = -0.01f
    private val TOUCH_TRANSLATE_FACTOR_H: Float = -0.01f
    private var previousX: Float = 0f
    private var previousY: Float = 0f

    val touchBehavior: TouchBehavior = TouchBehavior.ROTATE

    init {
        // Create an OpenGL ES 2.0 context
        setEGLContextClientVersion(2)

        renderer = TransformationsRenderer()
        setRenderer(renderer)

        // Render the view only when there is a change in the drawing data.
        // To allow the triangle to rotate automatically, this line is commented out:
        // renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        // MotionEvent reports input details from the touch screen
        // and other input controls. In this case, you are only
        // interested in events where the touch position changed.
        val x = e.x
        val y = e.y

        when (e.action) {
            MotionEvent.ACTION_MOVE -> {
                val dx = x - previousX
                val dy = y - previousY

                if (y > height * 3 / 4) {
                    when (touchBehavior) {
                        TouchBehavior.ROTATE -> {
                            renderer.rotate(dy * TOUCH_ROTATE_FACTOR_H, dx * TOUCH_ROTATE_FACTOR_W)
                        }
                        TouchBehavior.TRANSLATE -> {
                            renderer.translate(dx * TOUCH_TRANSLATE_FACTOR_W, dy * TOUCH_TRANSLATE_FACTOR_H)
                        }
                    }
                } else {
                    val shapeIndex = if (y < height / 2) 0 else 1

                    when (touchBehavior) {
                        TouchBehavior.ROTATE -> {
                            renderer.rotateShape(index = shapeIndex, dy * TOUCH_ROTATE_FACTOR_H, dx * TOUCH_ROTATE_FACTOR_W)
                        }
                        TouchBehavior.TRANSLATE -> {
                            renderer.translateShape(index = shapeIndex, dx * TOUCH_TRANSLATE_FACTOR_W, dy * TOUCH_TRANSLATE_FACTOR_H)
                        }
                    }
                }

                requestRender()
            }
        }

        previousX = x
        previousY = y
        return true
    }
}
