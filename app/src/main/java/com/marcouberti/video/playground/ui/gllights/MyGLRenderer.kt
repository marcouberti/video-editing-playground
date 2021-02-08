package com.marcouberti.video.playground.ui.gllights

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.SystemClock
import com.marcouberti.video.playground.ui.gllights.shape.Sphere
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MyGLRenderer(
        val context: Context
) : GLSurfaceView.Renderer {

    private val mMVPMatrix = FloatArray(16) //model view projection matrix
    private val mViewMatrix = FloatArray(16) // view matrix
    private val mProjectionMatrix = FloatArray(16) //projection mastrix
    private val mModelMatrix = FloatArray(16) //model  matrix
    private val mMVMatrix = FloatArray(16) //model view matrix
    private val cameraRotationMatrix = FloatArray(16) // camera rotation matrix
    private val cameraRotationMatrix2 = FloatArray(16) // camera rotation matrix
    private val mRotationMatrix = FloatArray(16) // model rotation matrix

    private lateinit var mSphere: Sphere

    @Volatile
    var angleX: Float = 0f
    var angleY: Float = 0f

    override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {
        // Set the background frame color
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

        // sphere
        mSphere = Sphere()
    }

    override fun onDrawFrame(unused: GL10) {
        // Redraw background color
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        val scratch = FloatArray(16)

        Matrix.setIdentityM(mMVPMatrix, 0) //set the model view projection matrix to an identity matrix
        Matrix.setIdentityM(mMVMatrix, 0) //set the model view  matrix to an identity matrix
        Matrix.setIdentityM(mModelMatrix, 0) //set the model matrix to an identity matrix
        //Matrix.setRotateM(mRotationMatrix, 0, 30f, 0f, 1f, 0f) //rotate around the y-axis

        // Set the camera position (View matrix)
        Matrix.setLookAtM(mViewMatrix, 0,
                0.0f, 0f, 1.0f,  //camera is at (0,0,1)
                0f, 0f, 0f,  //looks at the origin
                0f, 1f, 0.0f) //head is down (set to (0,1,0) to look from the top)

        Matrix.translateM(mModelMatrix, 0, 0.0f, 0.0f, -15f) //move backward for 5 units
        //Matrix.multiplyMM(mModelMatrix, 0, mModelMatrix, 0, mRotationMatrix, 0)

        // Calculate the projection and view transformation
        //calculate the model view matrix
        Matrix.multiplyMM(mMVMatrix, 0, mViewMatrix, 0, mModelMatrix, 0)
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVMatrix, 0)

        // Combine the rotation matrix with the projection and camera view
        // Note that the vPMatrix factor *must be first* in order
        // for the matrix multiplication product to be correct.
        Matrix.setRotateM(cameraRotationMatrix, 0, angleX, 1f, 0f, 0.0f)
        Matrix.setRotateM(cameraRotationMatrix2, 0, angleY, 0f, 1f, 0.0f)
        Matrix.multiplyMM(cameraRotationMatrix, 0, cameraRotationMatrix, 0, cameraRotationMatrix2, 0)
        Matrix.multiplyMM(scratch, 0, mMVPMatrix, 0, cameraRotationMatrix, 0)

        // Time
        val time = SystemClock.uptimeMillis().toShort()
        val seconds = 0.001f * time.toInt()

        // Draw sphere
        mSphere.draw(scratch)
    }

    override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)

        val ratio: Float = width.toFloat() / height.toFloat()

        // this projection matrix is applied to object coordinates
        // in the onDrawFrame() method
        Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1f, 1f, 3f, 45f)
    }
}

/**
 * Utility method to compile a shader before using it.
 */
fun loadShader(type: Int, shaderCode: String): Int {

    // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
    // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
    return GLES20.glCreateShader(type).also { shader ->

        // add the source code to the shader and compile it
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
    }
}
