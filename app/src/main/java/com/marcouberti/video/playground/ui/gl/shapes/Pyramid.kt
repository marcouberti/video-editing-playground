package com.marcouberti.video.playground.ui.gl.shapes

import android.opengl.GLES32
import com.marcouberti.video.playground.checkGlError
import com.marcouberti.video.playground.ui.gllights.loadShader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Draw a pyramid.
 */
class Pyramid {
    private val vertexShaderCode =
        """
            attribute vec3 aVertexPosition;
            uniform mat4 uMVPMatrix;
            varying vec4 vColor;
            attribute vec4 aVertexColor;
            
            void main() {
                gl_Position = uMVPMatrix *vec4(aVertexPosition,1.0);
                vColor=aVertexColor;
            }
            """.trimIndent()
    private val fragmentShaderCode =
        """
            precision mediump float;
            varying vec4 vColor; 
            void main() {
                gl_FragColor = vColor;
            }
            """.trimIndent()
    private val vertexBuffer: FloatBuffer
    private val colorBuffer: FloatBuffer
    private val mProgram: Int
    private var mPositionHandle = 0
    private var mColorHandle = 0
    private var mMVPMatrixHandle = 0
    private val vertexCount // number of vertices
            : Int

    // number of coordinates per vertex in this array
    val COORDS_PER_VERTEX = 3
    val COLOR_PER_VERTEX = 4

    private val vertexStride = COORDS_PER_VERTEX * 4 // 4 bytes per vertex
    private val colorStride = COLOR_PER_VERTEX * 4 // 4 bytes per vertex

    var pyramidVertex = floatArrayOf( // front face
        -0.0f, 1.0f, 0.0f,
        -1.0f, -1.0f, 1.0f,
        1.0f, -1.0f, 1.0f,  // right face
        -0.0f, 1.0f, 0.0f,
        1.0f, -1.0f, 1.0f,
        1.0f, -1.0f, -1.0f,  // back face
        -0.0f, 1.0f, 0.0f,
        1.0f, -1.0f, -1.0f,
        -1.0f, -1.0f, -1.0f,  // left face
        -0.0f, 1.0f, 0.0f,
        -1.0f, -1.0f, -1.0f,
        -1.0f, -1.0f, 1.0f
    )

    var pyramidColor = floatArrayOf( // front face
        -0.0f, 1.0f, 0.0f, 0.2f,
        1.0f, 0.0f, 0.0f, 0.2f,
        1.0f, 0.0f, 0.0f,  0.2f, // right face
        -0.0f, 1.0f, 0.0f, 0.2f,
        1.0f, 0.0f, 0.0f, 0.2f,
        1.0f, 0.0f, 0.0f, 0.2f,  // back face
        -0.0f, 1.0f, 0.0f, 0.2f,
        1.0f, 0.0f, 0.0f, 0.2f,
        1.0f, 0.0f, 0.0f,  0.2f, // left face
        -0.0f, 1.0f, 0.0f, 0.2f,
        1.0f, 0.0f, 0.0f, 0.2f,
        1.0f, 0.0f, 0.0f, 0.2f,
    )

    fun draw(mvpMatrix: FloatArray?) {
        GLES32.glUseProgram(mProgram) // Add program to OpenGL environment
        // get handle to vertex shader's vPosition member
        mPositionHandle = GLES32.glGetAttribLocation(mProgram, "aVertexPosition")
        // Enable a handle to the triangle vertices
        GLES32.glEnableVertexAttribArray(mPositionHandle)

        // get handle to vertex shader's aVertexColor member
        mColorHandle = GLES32.glGetAttribLocation(mProgram, "aVertexColor")
        // Enable a handle to the triangle color
        GLES32.glEnableVertexAttribArray(mColorHandle)

        // get handle to shape's transformation matrix
        mMVPMatrixHandle = GLES32.glGetUniformLocation(mProgram, "uMVPMatrix")
        checkGlError("glGetUniformLocation")

        // Apply the projection and view transformation
        GLES32.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrix, 0)
        checkGlError("glUniformMatrix4fv")
        //set the attribute of the vertex to point to the vertex buffer
        GLES32.glVertexAttribPointer(
            mPositionHandle, COORDS_PER_VERTEX,
            GLES32.GL_FLOAT, false, vertexStride, vertexBuffer
        )

        //set the attribute of the vertex colors to point to the color buffer
        GLES32.glVertexAttribPointer(
            mColorHandle, COLOR_PER_VERTEX,
            GLES32.GL_FLOAT, false, colorStride, colorBuffer
        )
        // Draw the triangle fan
        GLES32.glDrawArrays(GLES32.GL_TRIANGLE_FAN, 0, vertexCount)
    }

    init {
        // initialize vertex byte buffer for shape coordinates
        val bb = ByteBuffer.allocateDirect(pyramidVertex.size * 4) // (# of coordinate values * 4 bytes per float)
        bb.order(ByteOrder.nativeOrder())
        vertexBuffer = bb.asFloatBuffer()
        vertexBuffer.put(pyramidVertex)
        vertexBuffer.position(0)
        vertexCount = pyramidVertex.size / COORDS_PER_VERTEX

        // initialise the color buffer
        val cb = ByteBuffer.allocateDirect(pyramidColor.size * 4) // (# of coordinate values * 4 bytes per float)

        cb.order(ByteOrder.nativeOrder())
        colorBuffer = cb.asFloatBuffer()
        colorBuffer.put(pyramidColor)
        colorBuffer.position(0)

        // prepare shaders and OpenGL program
        val vertexShader: Int = loadShader(GLES32.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader: Int =
            loadShader(GLES32.GL_FRAGMENT_SHADER, fragmentShaderCode)
        mProgram = GLES32.glCreateProgram() // create empty OpenGL Program
        GLES32.glAttachShader(mProgram, vertexShader) // add the vertex shader to program
        GLES32.glAttachShader(mProgram, fragmentShader) // add the fragment shader to program
        GLES32.glLinkProgram(mProgram) // link the  OpenGL program to create an executable
    }
}
