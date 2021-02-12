package com.marcouberti.video.playground.ui.transformations

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.bendingspoons.opengltest1.ui.main.shapes.*
import com.marcouberti.video.playground.math.Quaternion
import com.marcouberti.video.playground.math.Vector3
import com.marcouberti.video.playground.math.degreesToRadians
import com.marcouberti.video.playground.math.radiansToDegrees
import com.marcouberti.video.playground.ui.transformations.shapes.ShapeState
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class TransformationsRenderer : GLSurfaceView.Renderer {
    // vPMatrix is an abbreviation for "Model View Projection Matrix"
    private val vpMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)

    var position: Vector3 = Vector3.zero
    var orientation: Quaternion = Quaternion(angleRadians = 0f, axis = Vector3.versorX)

    fun rotate(angleAroundX: Float, angleAroundY: Float) {
        val rotation = Quaternion(
            angleRadians = angleAroundX.degreesToRadians(),
            axis = Vector3.versorX,
        ) * Quaternion(
            angleRadians = angleAroundY.degreesToRadians(),
            axis = Vector3.versorY,
        )
        orientation = rotation * orientation
    }

    fun translate(dx: Float, dy: Float) {
        position += Vector3(dx, dy, 0f)
    }

    fun rotateShape(index: Int, angleAroundX: Float, angleAroundY: Float) {
        shapes[index].rotate(angleAroundX, angleAroundY)
    }

    fun translateShape(index: Int, dx: Float, dy: Float) {
        shapes[index].translate(dx, dy)
    }

    private var shapes = mutableListOf<ShapeState>()

    override fun onSurfaceCreated(gl: GL10, config: EGLConfig) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        shapes.add(ShapeState(Pentagon3D(), position = Vector3(0f, 1.2f, 0f)))
        shapes.add(ShapeState(Cube(), position = Vector3(0f, -1.2f, 1f)))
    }

    override fun onSurfaceChanged(gl: GL10, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)

        val ratio: Float = width.toFloat() / height.toFloat()

        // this projection matrix is applied to object coordinates
        // in the onDrawFrame() method
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 1f, 70f)
    }

    override fun onDrawFrame(gl: GL10) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glClearDepthf(1.0f) //set up the depth buffer
        GLES20.glEnable(GLES20.GL_DEPTH_TEST) //enable depth test (so, it will not look through the surfaces)
        GLES20.glDepthFunc(GLES20.GL_LEQUAL) //indicate what type of depth test

        // Set the camera position (View matrix)
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, -5f, 0f, 0f, 0f, 0f, 1.0f, 0.0f)

        // Calculate the projection and view transformation
        Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        val modelMatrix = FloatArray(16)
        val mvpMatrix = FloatArray(16)
        Matrix.setIdentityM(modelMatrix, 0)

        Matrix.translateM(modelMatrix, 0, position.x, position.y, position.z)
        val rotationMatrix = FloatArray(16)
        val angle = orientation.angle.radiansToDegrees()
        val axis = orientation.axis
        Matrix.setRotateM(rotationMatrix, 0, angle, axis.x, axis.y, axis.z)
        Matrix.multiplyMM(modelMatrix, 0, modelMatrix, 0, rotationMatrix, 0)

        // Combine the rotation matrix with the projection and camera view
        // Note that the vpMatrix factor *must be first* in order
        // for the matrix multiplication product to be correct.
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)

        shapes.forEach { it.draw(mvpMatrix) }
    }
}
