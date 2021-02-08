package com.marcouberti.video.playground.ui.gl.shapes

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLES32
import com.marcouberti.video.R
import com.marcouberti.video.playground.checkGlError
import com.marcouberti.video.playground.loadTexture
import com.marcouberti.video.playground.ui.gllights.loadShader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.util.*

/**
 * Draw a sphere with a texture.
 */
class SphereTexture(context: Context) {
    private val vertexShaderCode =
            """
            attribute vec3 aVertexPosition;
            uniform mat4 uMVPMatrix;
            varying vec4 vColor;
            attribute vec4 aVertexColor;
           
            attribute vec2 aTextureCoordinate; // Per-vertex texture coordinate information we will pass in.
            varying vec2 vTextureCoordinate; // This will be passed into the fragment shader.
            
            void main() {
                gl_Position = uMVPMatrix *vec4(aVertexPosition,1.0);
                vColor=aVertexColor;
                // Pass through the texture coordinate.
                vTextureCoordinate = aTextureCoordinate.xy;
            }
            """.trimIndent()
    private val fragmentShaderCode =
            """
            precision mediump float;
            varying vec4 vColor; 
            varying vec2 vTextureCoordinate;
            uniform bool uUseTexture;
            uniform sampler2D uTextureSampler; // texture
            void main() {
                if(uUseTexture) {
                    gl_FragColor = texture2D(uTextureSampler, vec2(vTextureCoordinate.s, vTextureCoordinate.t));
                } else {
                    gl_FragColor = vColor;
                }
            }
            """.trimIndent()

    private val vertexBuffer: FloatBuffer
    private val colorBuffer: FloatBuffer
    private val indexBuffer: IntBuffer
    private val textureBuffer: FloatBuffer // buffer for texture coordinates

    private val mProgram: Int
    private var mPositionHandle = 0
    private var mColorHandle = 0
    private var mMVPMatrixHandle = 0
    private val vertexCount // number of vertices
            : Int

    private var mTextureCoordinateHandle = 0 // handle for texture coordinates
    private var textureImageHandle = 0 // texture image handle
    private var textureSamplerHandle = 0 // texture sampler handle
    private var useTextureHandle = 0 // use texture handle

    // number of coordinates per vertex in this array
    private val COORDS_PER_VERTEX = 3
    private val COLOR_PER_VERTEX = 4
    private val TEXTURE_PER_VERTEX = 2

    private val vertexStride = COORDS_PER_VERTEX * 4 // 4 bytes per vertex
    private val colorStride = COLOR_PER_VERTEX * 4 // 4 bytes per vertex
    private val textureStride = TEXTURE_PER_VERTEX * 4 // 4 bytes per vertex

    private lateinit var SphereVertex: FloatArray
    private lateinit var SphereIndex: IntArray
    private lateinit var SphereColor: FloatArray
    private lateinit var TextureCoordinateDate: FloatArray

    private fun createSphere(radius: Float, nolatitude: Int, nolongitude: Int) {
        val vertices = FloatArray(65535)
        val index = IntArray(65535)
        val color = FloatArray(65535)
        val textureCoordinateData = FloatArray(65535)
        var textureindex = 0
        var vertedindex = 0
        var colorindex = 0
        var indx = 0
        val dist = 1f
        for (row in 0 until nolatitude + 1) {
            val theta = row * Math.PI / nolatitude
            val sinTheta = Math.sin(theta)
            val cosTheta = Math.cos(theta)
            var tcolor = 0f
            val tcolorinc = 1 / (nolongitude + 1).toFloat()
            for (col in 0 until nolongitude + 1) {
                val phi = col * 2 * Math.PI / nolongitude
                val sinPhi = Math.sin(phi)
                val cosPhi = Math.cos(phi)
                val x = cosPhi * sinTheta
                val z = sinPhi * sinTheta
                vertices[vertedindex++] = (radius * x).toFloat()
                vertices[vertedindex++] = (radius * cosTheta).toFloat() + dist
                vertices[vertedindex++] = (radius * z).toFloat()
                color[colorindex++] = Math.abs(tcolor)
                color[colorindex++] = 1 - Math.abs(tcolor)
                color[colorindex++] = 0f
                color[colorindex++] = 0f
                tcolor += tcolorinc

                textureCoordinateData[textureindex++] = col / nolongitude.toFloat() // range in (0,1)
                textureCoordinateData[textureindex++] = row / nolatitude.toFloat() // range in (0,1)
            }
        }

        //index buffer
        for (row in 0 until nolatitude) {
            for (col in 0 until nolongitude) {
                val P0 = row * (nolongitude + 1) + col
                val P1 = P0 + nolongitude + 1
                index[indx++] = P0
                index[indx++] = P1
                index[indx++] = P0 + 1
                index[indx++] = P1
                index[indx++] = P1 + 1
                index[indx++] = P0 + 1
            }
        }

        // SET THE BUFFERS
        SphereVertex = Arrays.copyOf(vertices, vertedindex)
        SphereIndex = Arrays.copyOf(index, indx)
        SphereColor = Arrays.copyOf(color, colorindex)

        TextureCoordinateDate = Arrays.copyOf(textureCoordinateData, textureindex)
    }

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

        // Texture
        mTextureCoordinateHandle = GLES32.glGetAttribLocation(mProgram, "aTextureCoordinate")
        GLES32.glEnableVertexAttribArray(mTextureCoordinateHandle);

        textureSamplerHandle = GLES32.glGetUniformLocation(mProgram, "uTextureSampler")
        useTextureHandle = GLES32.glGetUniformLocation(mProgram, "uUseTexture")

        GLES32.glActiveTexture(GLES32.GL_TEXTURE0)
        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, textureImageHandle)
        GLES32.glUniform1i(textureSamplerHandle, 0)

        GLES32.glUniform1i(useTextureHandle, 1) // enable texture

        GLES32.glVertexAttribPointer(
                mTextureCoordinateHandle, TEXTURE_PER_VERTEX,
                GLES32.GL_FLOAT, false, textureStride, textureBuffer)

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
        // Draw
        GLES32.glDrawElements(
                GLES32.GL_TRIANGLES,
                SphereIndex.size,
                GLES32.GL_UNSIGNED_INT,
                indexBuffer
        )
    }

    init {
        createSphere(1f, 30, 30)

        textureImageHandle = loadTexture(context, R.drawable.earth)

        // Set filtering
        GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_NEAREST
        )
        GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_NEAREST
        )

        // initialize texture buffer
        val tb = ByteBuffer.allocateDirect(TextureCoordinateDate.size * 4) // (# of coordinate values * 4 bytes per float)
        tb.order(ByteOrder.nativeOrder())
        textureBuffer = tb.asFloatBuffer()
        textureBuffer.put(TextureCoordinateDate)
        textureBuffer.position(0)

        // initialize vertex byte buffer for shape coordinates
        val bb = ByteBuffer.allocateDirect(SphereVertex.size * 4) // (# of coordinate values * 4 bytes per float)
        bb.order(ByteOrder.nativeOrder())
        vertexBuffer = bb.asFloatBuffer()
        vertexBuffer.put(SphereVertex)
        vertexBuffer.position(0)
        vertexCount = SphereVertex.size / COORDS_PER_VERTEX

        // initialise the color buffer
        val cb = ByteBuffer.allocateDirect(SphereColor.size * 4) // (# of coordinate values * 4 bytes per float)

        cb.order(ByteOrder.nativeOrder())
        colorBuffer = cb.asFloatBuffer()
        colorBuffer.put(SphereColor)
        colorBuffer.position(0)

        // initialise the index buffer
        val ib = IntBuffer.allocate(SphereIndex.size)
        indexBuffer = ib
        indexBuffer.put(SphereIndex)
        indexBuffer.position(0)

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
