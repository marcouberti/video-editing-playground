package com.marcouberti.video.playground.ui.gl.shapes

import android.opengl.GLES32
import android.util.Pair
import com.marcouberti.video.playground.checkGlError
import com.marcouberti.video.playground.ui.gl.loadShader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Use GL_POINT to draw an ellipse border.
 */
class EllipseBorder {
    private val vertexShaderCode =
        """
            attribute vec3 aVertexPosition;
            uniform mat4 uMVPMatrix;
            varying vec4 vColor;
            
            void main() {
                gl_Position = uMVPMatrix *vec4(aVertexPosition,1.0);
                gl_PointSize=2.0;
                vColor=vec4(0.5,0.0,0.0,1.0);
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
    private val mProgram: Int
    private var mPositionHandle = 0
    private var mMVPMatrixHandle = 0
    private val vertexCount // number of vertices
            : Int


    // number of coordinates per vertex in this array
    val COORDS_PER_VERTEX = 3
    var triangleVertex = mutableListOf<Float>()

    private val vertexStride = COORDS_PER_VERTEX * 4 // 4 bytes per vertex

    private fun computeEllipseVertices() {
        val NUM_POINTS = 1000
        val RADIUS = 1.0
        val points = mutableListOf<Pair<Float, Float>>()
        for (i in 0 until NUM_POINTS) {
            val angle = Math.toRadians(i.toDouble() / NUM_POINTS * 360.0)
            points.add(Pair(
                (Math.cos(angle) * RADIUS).toFloat() * 0.1f,
                (Math.sin(angle) * RADIUS).toFloat() * 0.2f // stretch the circle
            ))
        }

        // add border vertices
        for (i in points.indices) {
            triangleVertex.add(points[i].first)
            triangleVertex.add(points[i].second)
            triangleVertex.add(1f)
        }
    }

    fun draw(mvpMatrix: FloatArray?) {
        GLES32.glUseProgram(mProgram) // Add program to OpenGL environment
        // get handle to vertex shader's vPosition member
        mPositionHandle = GLES32.glGetAttribLocation(mProgram, "aVertexPosition")
        // Enable a handle to the triangle vertices
        GLES32.glEnableVertexAttribArray(mPositionHandle)
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
        // Draw the triangle fan
        GLES32.glDrawArrays(GLES32.GL_POINTS, 0, vertexCount)
    }

    init {
        computeEllipseVertices()

        // initialize vertex byte buffer for shape coordinates
        val bb =
            ByteBuffer.allocateDirect(triangleVertex.size * 4) // (# of coordinate values * 4 bytes per float)
        bb.order(ByteOrder.nativeOrder())
        vertexBuffer = bb.asFloatBuffer()
        vertexBuffer.put(triangleVertex.toFloatArray())
        vertexBuffer.position(0)
        vertexCount = triangleVertex.size / COORDS_PER_VERTEX
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
