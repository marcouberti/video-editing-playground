package com.bendingspoons.opengltest1.ui.main.shapes

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.sqrt

class Ellipse(a: Float, b: Float) : Shape {
  // Use to access and set the view transformation
  private var vPMatrixHandle: Int = 0

  private val mProgram: Int

  private var positionHandle: Int = 0
  private var mColorHandle: Int = 0

  private val COORDS_PER_VERTEX = 3
  private val vertexStride: Int = COORDS_PER_VERTEX * 4 // 4 bytes per vertex
  private val colorStride: Int = COORDS_PER_VERTEX * 4 // 4 bytes per vertex

  // Set color with red, green, blue and alpha (opacity) values
  val fillColor = floatArrayOf(1f, 0f, 0f, 1f)
  val borderColor = colorBuffer(
      listOf(
          0f, 1f, 1f, 1f
      )
  )

  val verticesOfFilledEllipse = vertexBuffer(ellipseBorderAndCenter(a = a, b = b))
  val verticesOfEllipseBorder = vertexBuffer(ellipseBorder(a = a, b = b))

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
          verticesOfFilledEllipse,
      )

      // get handle to fragment shader's vColor member
      mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor").also { colorHandle ->
        // Set color for drawing the triangle
        GLES20.glUniform4fv(colorHandle, 1, fillColor, 0)
      }

      // get handle to shape's transformation matrix
      vPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix")

      // Pass the projection and view transformation to the shader
      GLES20.glUniformMatrix4fv(vPMatrixHandle, 1, false, mvpMatrix, 0)

      // Draw the shape
      GLES20.glDrawArrays(
          GLES20.GL_TRIANGLE_FAN,
          0,
          verticesOfFilledEllipse.remaining()/3,
      )



      // Prepare the triangle coordinate data
      GLES20.glVertexAttribPointer(
          it,
          COORDS_PER_VERTEX,
          GLES20.GL_FLOAT,
          false,
          vertexStride,
          verticesOfEllipseBorder,
      )

      // get handle to fragment shader's vColor member
      mColorHandle = GLES20.glGetAttribLocation(mProgram, "aVertexColor").also { colorHandle ->
        // Set color for drawing the triangle
        GLES20.glVertexAttribPointer(
            colorHandle,
            COORDS_PER_VERTEX,
            GLES20.GL_FLOAT,
            false,
            colorStride,
            borderColor,
        )
      }

      // get handle to shape's transformation matrix
      vPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix")

      // Pass the projection and view transformation to the shader
      GLES20.glUniformMatrix4fv(vPMatrixHandle, 1, false, mvpMatrix, 0)

      GLES20.glLineWidth(200f)

      // Draw the shape
      GLES20.glDrawArrays(
          GLES20.GL_LINE_LOOP,
          0,
          verticesOfEllipseBorder.remaining()/3,
      )

      // Disable vertex array
      GLES20.glDisableVertexAttribArray(it)
    }
  }

  private fun ellipseBorder(a: Float, b: Float): List<Float> {
    val vertexCount = 1000
    return (0 .. vertexCount).flatMap { index ->
      val yPart = 2 * 2 * b * index / vertexCount
      val y = if (index <= vertexCount / 2) b - yPart else yPart - 3 * b
      val x = (if (index <= vertexCount / 2) -1 else 1) * sqrt(1.0f - (y * y) / (b * b)) * a
      listOf(x, y, 0.0f)
    }
  }

  private fun ellipseBorderAndCenter(a: Float, b: Float): List<Float> {
    return ellipseBorder(a, b) + listOf(0.0f, 0.0f, 0.0f)
  }
}
