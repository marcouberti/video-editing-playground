package com.bendingspoons.opengltest1.ui.main.shapes

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer

interface Shape {
  companion object {
    // This matrix member variable provides a hook to manipulate
    // the coordinates of the objects that use this vertex shader
    // the matrix must be included as a modifier of gl_Position
    // Note that the uMVPMatrix factor *must be first* in order
    // for the matrix multiplication product to be correct.
    const val vertexShaderCode = """
            uniform mat4 uMVPMatrix;
            attribute vec4 vPosition;
            attribute vec4 aVertexColor;
            varying vec4 vColor;
            void main() {  
                gl_Position = uMVPMatrix * vPosition;
                vColor = aVertexColor;
            }
        """

    const val fragmentShaderCode = """
            precision mediump float;
            varying vec4 vColor;
            void main() {
                gl_FragColor = vColor;
            }
        """

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
  }

  fun draw(mvpMatrix: FloatArray)

  fun vertexBuffer(vertices: List<Float>): FloatBuffer {
    val byteBuffer = ByteBuffer.allocateDirect(4 * vertices.count())
    byteBuffer.order(ByteOrder.nativeOrder())
    val buffer = byteBuffer.asFloatBuffer()
    buffer.put(vertices.toFloatArray())
    buffer.flip()
    return buffer
  }

  fun indexBuffer(indexes: List<Int>): IntBuffer {
    val byteBuffer = ByteBuffer.allocateDirect(4 * indexes.count())
    byteBuffer.order(ByteOrder.nativeOrder())
    val buffer = byteBuffer.asIntBuffer()
    buffer.put(indexes.toIntArray())
    buffer.flip()
    return buffer
  }

  fun colorBuffer(colors: List<Float>): FloatBuffer {
    val byteBuffer = ByteBuffer.allocateDirect(4 * colors.count())
    byteBuffer.order(ByteOrder.nativeOrder())
    val buffer = byteBuffer.asFloatBuffer()
    buffer.put(colors.toFloatArray())
    buffer.flip()
    return buffer
  }
}
