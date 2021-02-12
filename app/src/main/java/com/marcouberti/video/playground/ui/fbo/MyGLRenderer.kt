package com.marcouberti.video.playground.ui.fbo

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLES32
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.SystemClock
import com.marcouberti.video.R
import com.marcouberti.video.playground.loadTexture
import com.marcouberti.video.playground.ui.fbo.shape.FrameBufferDisplay
import com.marcouberti.video.playground.ui.fbo.shape.SquareBitmap
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
    private val mRotationMatrix = FloatArray(16) // camera rotation matrix
    private val mRotationMatrix2 = FloatArray(16) // camera rotation matrix

    private lateinit var mSquare: SquareBitmap

    @Volatile
    var angleX: Float = 0f

    @Volatile
    var angleY: Float = 0f

    private var viewPortWidth: Int = 0
    private var viewPortHeigh: Int = 0
    private var mFBO: FrameBufferDisplay? = null

    override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {
        // Set the background frame color
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

        // initialize a square with Bitmap
        val textureHandle = loadTexture(context, R.drawable.splice_logo)
        mSquare = SquareBitmap(textureHandle)
    }

    override fun onDrawFrame(unused: GL10) {

        GLES32.glViewport(0, 0, viewPortWidth, viewPortHeigh) // set the viewport size

        // Redraw background color
        GLES32.glClear(GLES32.GL_COLOR_BUFFER_BIT or GLES32.GL_DEPTH_BUFFER_BIT)
        GLES32.glClearDepthf(1.0f) //set up the depth buffer
        GLES32.glDepthFunc(GLES32.GL_LEQUAL) //indicate what type of depth test
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)

        val scratch = FloatArray(16)

        Matrix.setIdentityM(mMVPMatrix, 0) //set the model view projection matrix to an identity matrix
        Matrix.setIdentityM(mMVMatrix, 0) //set the model view  matrix to an identity matrix
        Matrix.setIdentityM(mModelMatrix, 0) //set the model matrix to an identity matrix
        //Matrix.setRotateM(mRotationMatrix, 0, 30f, 0f, 1f, 0f) //rotate around the y-axis

        // Set the camera position (View matrix)
        Matrix.setLookAtM(mViewMatrix, 0,
                0.0f, 0f, 5.0f,  //camera is at (0,0,5)
                0f, 0f, 0f,  //looks at the origin
                0f, 1f, 0.0f) //head is down (set to (0,1,0) to look from the top)

        // Rotate the model
        Matrix.setRotateM(mRotationMatrix, 0, angleX, 1f, 0f, 0.0f)
        Matrix.setRotateM(mRotationMatrix2, 0, angleY, 0f, 1f, 0.0f)
        Matrix.multiplyMM(mModelMatrix, 0, mModelMatrix, 0, mRotationMatrix, 0)
        Matrix.multiplyMM(mModelMatrix, 0, mModelMatrix, 0, mRotationMatrix2, 0)

        // Translate the model
        Matrix.translateM(mModelMatrix, 0, 0.0f, 0.0f, 0f) // don't translate

        // Calculate the projection and view transformation
        Matrix.multiplyMM(mMVMatrix, 0, mViewMatrix, 0, mModelMatrix, 0)
        Matrix.multiplyMM(scratch, 0, mProjectionMatrix, 0, mMVMatrix, 0)

        // Time
        val time = SystemClock.uptimeMillis().toShort()
        val seconds = 0.001f * time.toInt()

        // 1 - Draw shape on the screen
        mSquare.draw(scratch, seconds)

        mFBO?.let { fbo ->
            // 2 - Draw shape on the FBO (texture)
            GLES32.glBindFramebuffer(GLES32.GL_FRAMEBUFFER, fbo.frameBuffer[0])
            GLES32.glViewport(0, 0, fbo.width, fbo.height)
            GLES32.glClear(GLES32.GL_COLOR_BUFFER_BIT or GLES32.GL_DEPTH_BUFFER_BIT)
            val pviewmatrix = FloatArray(16)

            Matrix.setLookAtM(pviewmatrix, 0,
                    0.0f, 0f, 9.0f,  //camera is at (0,0,5)
                    0f, 0f, 0f,  //looks at the origin
                    0f, 1f, 0.0f) //head is down (set to (0,1,0) to look from the top)

            Matrix.multiplyMM(mMVMatrix, 0, pviewmatrix, 0, mModelMatrix, 0)
            Matrix.multiplyMM(scratch, 0, fbo.mProjMatrix, 0, mMVMatrix, 0)
            mSquare.draw(scratch, seconds)
            GLES32.glBindFramebuffer(GLES32.GL_FRAMEBUFFER, 0) //render onto the screen

            // 3 - Draw the FBO on the screen (as a Plane with a texture)
            GLES32.glViewport(0, 0, viewPortWidth, viewPortHeigh) // set the viewport size
            Matrix.setIdentityM(mModelMatrix, 0) //set the model matrix to an identity matrix
            Matrix.scaleM(mModelMatrix, 0, fbo.width.toFloat() / fbo.height.toFloat(), 1f, 1f)
            Matrix.multiplyMM(mMVMatrix, 0, mViewMatrix, 0, mModelMatrix, 0)
            Matrix.multiplyMM(scratch, 0, mProjectionMatrix, 0, mMVMatrix, 0)
            fbo.draw(scratch)
        }
    }

    override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)

        viewPortWidth = width
        viewPortHeigh = height

        var ratio: Float = width.toFloat() / height.toFloat()

        // this projection matrix is applied to object coordinates
        // in the onDrawFrame() method
        // Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1f, 1f, 3f, 45f)
        if (width > height) {
            ratio = width.toFloat() / height.toFloat()
            Matrix.orthoM(mProjectionMatrix, 0, -ratio, ratio, -1f, 1f, -10f, 200f)
        } else {
            ratio = height.toFloat() / width.toFloat()
            Matrix.orthoM(mProjectionMatrix, 0, -1f, 1f, -ratio, ratio, -10f, 200f)
        }
        mFBO = FrameBufferDisplay(width, height)
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
