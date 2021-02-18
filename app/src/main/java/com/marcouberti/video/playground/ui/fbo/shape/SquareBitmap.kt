package com.marcouberti.video.playground.ui.fbo.shape

import android.opengl.GLES20
import com.marcouberti.video.playground.ui.gllights.loadShader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer


private const val COORDS_PER_VERTEX = 3

private var squareCoords = floatArrayOf(
    -0.5F, 0.5F, 0.0f,      // top left
    -0.5F, -0.5F, 0.0f,      // bottom left
    0.5F, -0.5F, 0.0f,      // bottom right
    0.5F, 0.5F, 0.0f,       // top right
)

// S, T (or X, Y)
// Texture coordinate data.
// Because images have a Y axis pointing downward (values increase as you move down the image) while
// OpenGL has a Y axis pointing upward, we adjust for that here by flipping the Y axis.
// What's more is that the texture coordinates are the same for every face.
private var textureCoords = floatArrayOf(
    1.0f, 0.0f, 0.0f,
    1.0f, 1.0f, 0.0f,
    0.0f, 1.0f, 0.0f,
    0.0f, 0.0f, 0.0f,
)

private var positionHandle: Int = 0
private var mColorHandle: Int = 0
private var mTimeHandle: Int = 0

/** This will be used to pass in the texture.  */
private var mTextureUniformHandle = 0

/** This will be used to pass in model texture coordinate information.  */
private var mTextureCoordinateHandle = 0

private val vertexCount: Int = squareCoords.size / COORDS_PER_VERTEX
private val vertexStride: Int = COORDS_PER_VERTEX * 4 // 4 bytes per vertex

class SquareBitmap(
    private val textureHandle: Int
) {

    private val drawOrder = shortArrayOf(0, 1, 2, 0, 2, 3) // order to draw vertices

    // This matrix member variable provides a hook to manipulate
    // the coordinates of the objects that use this vertex shader
    // the matrix must be included as a modifier of gl_Position
    // Note that the uMVPMatrix factor *must be first* in order
    // for the matrix multiplication product to be correct.
    private val vertexShaderCode =
        """
            uniform mat4 uMVPMatrix;
            attribute vec4 vPosition;
            
            // Per-vertex texture coordinate information we will pass in.
            attribute vec2 a_TexCoordinate; 
            
            // This will be passed into the fragment shader.
            varying vec2 v_TexCoordinate;   
            
            void main() {  
                gl_Position = uMVPMatrix * vPosition;
                // Pass through the texture coordinate.
                v_TexCoordinate = a_TexCoordinate.xy;
            }
        """

    // Use to access and set the view transformation
    private var vPMatrixHandle: Int = 0

    private val fragmentShaderCode =
        """
            precision mediump float;
            
            // The time
            uniform float u_time;
            
            // The input color
            uniform vec4 u_color;
            
            // The input texture.
            uniform sampler2D u_Texture;    
            
            // Interpolated texture coordinate per fragment.
            varying vec2 v_TexCoordinate;
            
            void main() {
                // rgb displacement
                vec2 d = vec2(0.0, 0.0); //vec2(sin(u_time), cos(u_time));
                
                vec3 r = texture2D(u_Texture, v_TexCoordinate + d).rgb;
                vec3 g = texture2D(u_Texture, v_TexCoordinate + d * 1.1).rgb;
                vec3 b = texture2D(u_Texture, v_TexCoordinate + d * 1.2).rgb;
                
                vec3 glitch = vec3(r.r, g.g, b.b);
            
                // Multiply the color by texture value to get final output color.
                gl_FragColor = vec4(glitch, 1.0);
            }
        """

    private var mProgram: Int

    init {
        val vertexShader: Int = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader: Int = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

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

    // Set color with red, green, blue and alpha (opacity) values
    val color = floatArrayOf(0.1f, 0.5f, 1f, 1f)

    // initialize vertex byte buffer for shape coordinates
    private val vertexBuffer: FloatBuffer =
        // (# of coordinate values * 4 bytes per float)
        ByteBuffer.allocateDirect(squareCoords.size * 4).run {
            order(ByteOrder.nativeOrder())
            asFloatBuffer().apply {
                put(squareCoords)
                position(0)
            }
        }

    // initialize vertex byte buffer for texture coordinates
    private val textureBuffer: FloatBuffer =
        // (# of coordinate values * 4 bytes per float)
        ByteBuffer.allocateDirect(textureCoords.size * 4).run {
            order(ByteOrder.nativeOrder())
            asFloatBuffer().apply {
                put(textureCoords)
                position(0)
            }
        }

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

    fun draw(mvpMatrix: FloatArray, seconds: Float) {
        // Add program to OpenGL ES environment
        GLES20.glUseProgram(mProgram)

        // get handle to vertex shader's vPosition member
        positionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition").also {

            // Enable a handle to the square vertices
            GLES20.glEnableVertexAttribArray(it)

            // Prepare the triangle coordinate data
            GLES20.glVertexAttribPointer(
                it,
                COORDS_PER_VERTEX,
                GLES20.GL_FLOAT,
                false,
                vertexStride,
                vertexBuffer
            )

            // get handle to fragment shader's vColor member
            mColorHandle = GLES20.glGetUniformLocation(mProgram, "u_color").also { colorHandle ->

                // Set color for drawing the triangle
                GLES20.glUniform4fv(colorHandle, 1, color, 0)
            }

            // get handle to fragment shader's time member
            mTimeHandle = GLES20.glGetUniformLocation(mProgram, "u_time").also { timeHandle ->
                GLES20.glUniform1f(timeHandle, seconds)
            }

            mTextureUniformHandle = GLES20.glGetUniformLocation(mProgram, "u_Texture")
            mTextureCoordinateHandle = GLES20.glGetAttribLocation(mProgram, "a_TexCoordinate")

            // Set the active texture unit to texture unit 0.
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)

            // Bind the texture to this unit.
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle)

            GLES20.glVertexAttribPointer(
                mTextureCoordinateHandle,
                COORDS_PER_VERTEX,
                GLES20.GL_FLOAT,
                false,
                vertexStride,
                textureBuffer
            )

            GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle)

            // Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0.
            GLES20.glUniform1i(mTextureUniformHandle, 0)

            // get handle to shape's transformation matrix
            vPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix")

            // Pass the projection and view transformation to the shader
            GLES20.glUniformMatrix4fv(vPMatrixHandle, 1, false, mvpMatrix, 0)


            // Draw the square
            GLES20.glDrawElements(
                GLES20.GL_TRIANGLES, drawOrder.size,
                GLES20.GL_UNSIGNED_SHORT, drawListBuffer
            )

            // Disable vertex array
            GLES20.glDisableVertexAttribArray(it)
        }
    }
}
