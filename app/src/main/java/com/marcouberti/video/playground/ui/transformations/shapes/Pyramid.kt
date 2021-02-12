package com.bendingspoons.opengltest1.ui.main.shapes

import android.opengl.GLES20
import kotlin.math.sqrt

class Pyramid : Shape {
  // Use to access and set the view transformation
  private var vPMatrixHandle: Int = 0

  private val mProgram: Int

  private var positionHandle: Int = 0
  private var mColorHandle: Int = 0

  private val COORDS_PER_VERTEX = 3
  private val vertexStride: Int = COORDS_PER_VERTEX * 4 // 4 bytes per vertex

  // Set color with red, green, blue and alpha (opacity) values
  val fillColor = floatArrayOf(
      1f, 0f, 0f, 1f,
      0f, 1f, 0f, 1f,
      0f, 0f, 1f, 1f,

      1f, 0f, 0f, 1f,
      0f, 1f, 0f, 1f,
      0f, 0f, 1f, 1f,

      1f, 0f, 0f, 1f,
      0f, 1f, 0f, 1f,
      0f, 0f, 1f, 1f,

      1f, 0f, 0f, 1f,
      0f, 1f, 0f, 1f,
      0f, 0f, 1f, 1f,
  )

  private val pyramid = listOf(
      0f, 1f, 0f,
      -1f, -1f, 1f,
      1f, -1f, 1f,

      0f, 1f, 0f,
      1f, -1f, 1f,
      1f, -1f, -1f,

      0f, 1f, 0f,
      1f, -1f, -1f,
      -1f, -1f, -1f,

      0f, 1f, 0f,
      -1f, -1f, -1f,
      -1f, -1f, 1f,
  )

  val vertexBuffer = vertexBuffer(pyramid)

  init {
    val vertexShader = Shape.loadShader(GLES20.GL_VERTEX_SHADER, Shape.vertexShaderCode)
    val fragmentShader = Shape.loadShader(GLES20.GL_FRAGMENT_SHADER, Shape.fragmentShaderCode)

    mProgram = GLES20.glCreateProgram().also {
      // add the vertex shader to program
      GLES20.glAttachShader(it, vertexShader)
      // add the fragment shader to program
      GLES20.glAttachShader(it, fragmentShader)
      // creates OpenGL ES program executables
      GLES20.glLinkProgram(it)
    }
  }

  override fun draw(mvpMatrix: FloatArray) {
    // Add program to OpenGL ES environment
    GLES20.glUseProgram(mProgram)

    // get handle to vertex shader's vPosition member
    positionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition").also {

      // Enable a handle to the triangle vertices
      GLES20.glEnableVertexAttribArray(it)

      // Prepare the triangle coordinate data
      GLES20.glVertexAttribPointer(
          it,
          COORDS_PER_VERTEX,
          GLES20.GL_FLOAT,
          false,
          vertexStride,
          vertexBuffer,
      )

      // get handle to fragment shader's vColor member
      mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor").also { colorHandle ->
        // Set color for drawing the triangle
        GLES20.glEnableVertexAttribArray(it)

        // Prepare the triangle coordinate data
        GLES20.glVertexAttribPointer(
            it,
            COORDS_PER_VERTEX,
            GLES20.GL_FLOAT,
            false,
            vertexStride,
            vertexBuffer,
        )
        GLES20.glUniform4fv(colorHandle, fillColor.size/4, fillColor, 0)
      }

      // get handle to shape's transformation matrix
      vPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix")

      // Pass the projection and view transformation to the shader
      GLES20.glUniformMatrix4fv(vPMatrixHandle, 1, false, mvpMatrix, 0)

      // Draw the shape
      GLES20.glDrawArrays(
          GLES20.GL_TRIANGLE_FAN,
          0,
          vertexBuffer.remaining() / 3,
      )

      // Disable vertex array
      GLES20.glDisableVertexAttribArray(it)
    }
  }
}
