package com.bendingspoons.opengltest1.ui.main.shapes

import android.opengl.GLES20

class Pentagon3D: Shape {
  // Use to access and set the view transformation
  private var vPMatrixHandle: Int = 0

  private val mProgram: Int

  private var positionHandle: Int = 0
  private var mColorHandle: Int = 0

  private val COORDS_PER_VERTEX = 3
  private val COLOR_COMPONENTS_PER_VERTEX = 4

  private val vertices = listOf(
      -0.5f, -0.5f, 0.5f,
      0.5f, -0.5f, 0.5f,
      1f, 0.35f, 0.5f,
      0f, 1f, 0.5f,
      -1f, 0.35f, 0.5f,

      -0.5f, -0.5f, -0.5f,
      0.5f, -0.5f, -0.5f,
      1f, 0.35f, -0.5f,
      0f, 1f, -0.5f,
      -1f, 0.35f, -0.5f,
  )

  private val vertexIndexes = listOf(
      0, 1, 2, 0, 2, 3, 0, 3, 4,
      5, 6, 7, 5, 7, 8, 5, 8, 9,
      0, 1, 6, 0, 6, 5,
      0, 5, 9, 0, 9, 4,
      4, 3, 8, 4, 8, 9,
      3, 2, 7, 3, 7, 8,
      2, 1, 6, 2, 6, 7,
  )

  private val vertexColors = listOf(
      0f, 1f, 0f, 0.0f,
      0f, 1f, 0f, 0.0f,
      0f, 1f, 0f, 0.0f,
      0f, 1f, 0f, 0.0f,
      0f, 1f, 0f, 0.0f,

      1f, 0f, 0f, 1f,
      0f, 0f, 1f, 1f,
      0f, 0f, 1f, 1f,
      0f, 0f, 1f, 1f,
      0f, 0f, 1f, 1f,
  )

  val vertexBuffer = vertexBuffer(vertices)
  val indexBuffer = indexBuffer(vertexIndexes)
  val colorBuffer = colorBuffer(vertexColors)

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
    positionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition")
    mColorHandle = GLES20.glGetAttribLocation(mProgram, "aVertexColor")

    // Enable a handle to the triangle vertices
    GLES20.glEnableVertexAttribArray(positionHandle)
    GLES20.glEnableVertexAttribArray(mColorHandle)

    // get handle to shape's transformation matrix
    vPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix")

    // Pass the projection and view transformation to the shader
    GLES20.glUniformMatrix4fv(vPMatrixHandle, 1, false, mvpMatrix, 0)

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
        COLOR_COMPONENTS_PER_VERTEX,
        GLES20.GL_FLOAT,
        false,
        0,
        colorBuffer,
    )

    // Draw the shape
    GLES20.glDrawElements(
        GLES20.GL_TRIANGLES,
        indexBuffer.remaining(),
        GLES20.GL_UNSIGNED_INT,
        indexBuffer,
    )

    // Disable vertex array
    GLES20.glDisableVertexAttribArray(positionHandle)
    GLES20.glDisableVertexAttribArray(mColorHandle)
  }
}

