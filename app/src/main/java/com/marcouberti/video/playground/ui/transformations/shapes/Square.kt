package com.bendingspoons.opengltest1.ui.main.shapes

import android.opengl.GLES20
import com.bendingspoons.opengltest1.ui.main.shapes.Shape.Companion.fragmentShaderCode
import com.bendingspoons.opengltest1.ui.main.shapes.Shape.Companion.vertexShaderCode
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

class Square : Shape {
  private val drawOrder = shortArrayOf(0, 1, 2, 0, 2, 3) // order to draw vertices

  // Use to access and set the view transformation
  private var vPMatrixHandle: Int = 0

  private var mProgram: Int

  private val COORDS_PER_VERTEX = 3
  private val COLORS_PER_VERTEX = 4

  private var squareCoords = listOf(
      -1f, 1f, 0.0f,      // top left
      -1f, -1f, 0.0f,      // bottom left
      1f, -1f, 0.0f,      // bottom right
      1f, 1f, 0.0f       // top right
  )

  private val vertexColors = listOf(
      0f, 0f, 1f, 1f,
      0f, 1f, 1f, 1f,
      1f, 0f, 1f, 1f,
      1f, 1f, 1f, 1f,
  )

  private var positionHandle: Int = 0
  private var mColorHandle: Int = 0

  val colorBuffer = colorBuffer(vertexColors)

  init {
    val vertexShader: Int = Shape.loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
    val fragmentShader: Int = Shape.loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

    // create empty OpenGL ES Program
    mProgram = GLES20.glCreateProgram().also {

      // add the vertex shader to program
      GLES20.glAttachShader(it, vertexShader)

      // add the fragment shader to program
      GLES20.glAttachShader(it, fragmentShader)

      // creates OpenGL ES program executables
      GLES20.glLinkProgram(it)
    }
  }

  // initialize vertex byte buffer for shape coordinates
  private val vertexBuffer: FloatBuffer = vertexBuffer(squareCoords)

  // initialize byte buffer for the draw list
  private val drawListBuffer: ShortBuffer =
      // (# of coordinate values * 2 bytes per short)
      ByteBuffer.allocateDirect(drawOrder.size * 2).run {
        order(ByteOrder.nativeOrder())
        asShortBuffer().apply {
          put(drawOrder)
          position(0)
        }
      }

  override fun draw(mvpMatrix: FloatArray) {
    // Add program to OpenGL ES environment
    GLES20.glUseProgram(mProgram)

    // get handle to shape's transformation matrix
    vPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix")

    // Pass the projection and view transformation to the shader
    GLES20.glUniformMatrix4fv(vPMatrixHandle, 1, false, mvpMatrix, 0)

    // get handle to vertex shader's vPosition member
    positionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition")
    // get handle to fragment shader's vColor member
    mColorHandle = GLES20.glGetAttribLocation(mProgram, "aVertexColor")

    // Enable a handle to the triangle vertices
    GLES20.glEnableVertexAttribArray(positionHandle)
    GLES20.glEnableVertexAttribArray(mColorHandle)

    // Prepare the triangle coordinate data
    GLES20.glVertexAttribPointer(
        positionHandle,
        COORDS_PER_VERTEX,
        GLES20.GL_FLOAT,
        false,
        0,
        vertexBuffer,
    )

    GLES20.glVertexAttribPointer(
        mColorHandle,
        COLORS_PER_VERTEX,
        GLES20.GL_FLOAT,
        false,
        0,
        colorBuffer,
    )

    // Draw the square
    GLES20.glDrawElements(
        GLES20.GL_TRIANGLES,
        drawOrder.size,
        GLES20.GL_UNSIGNED_SHORT,
        drawListBuffer,
    )

    // Disable vertex array
    GLES20.glDisableVertexAttribArray(positionHandle)
    GLES20.glDisableVertexAttribArray(mColorHandle)
  }
}
