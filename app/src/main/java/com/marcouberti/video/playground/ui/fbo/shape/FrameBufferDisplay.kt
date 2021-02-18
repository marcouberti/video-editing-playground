package com.marcouberti.video.playground.ui.fbo.shape

import android.opengl.GLES32
import android.opengl.Matrix
import android.util.Log
import com.marcouberti.video.playground.ui.fbo.loadShader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer

/**
 * We create a Frame Buffer to draw in as a texture.
 * Later we can draw this texture using a Plane.
 */
class FrameBufferDisplay(pwidth: Int, pheight: Int) {

    private val vertexShaderCode = """
        attribute vec3 aVertexPosition;
        uniform mat4 uMVPMatrix;
        attribute vec2 aTextureCoordinate;
        varying vec2 vTextureCoordinate;
        
        void main() {
            gl_Position = uMVPMatrix *vec4(aVertexPosition,1.0);
            vTextureCoordinate=aTextureCoordinate;
        }
        """.trimIndent()

    private val fragmentShaderCode = """
        precision lowp float;
        varying vec2 vTextureCoordinate;
        uniform sampler2D uTextureSampler;
        void main() {
            vec4 fragmentColor=texture2D(uTextureSampler,vec2(vTextureCoordinate.s,vTextureCoordinate.t));
            if(fragmentColor.rgb == vec3(1.0, 0.0, 1.0)) {
                gl_FragColor=vec4(1.0, 0.0, 0.0, 1.0);
            } else {
                gl_FragColor=vec4(fragmentColor.rgb,fragmentColor.a);
            }
        }
        """.trimIndent()

    private val vertexBuffer: FloatBuffer
    private val indexBuffer: IntBuffer
    private val textureBuffer: FloatBuffer
    private val mProgram: Int
    private val mPositionHandle: Int
    private val mTextureCoordHandle: Int
    private val mMVPMatrixHandle: Int
    private val TextureHandle: Int

    var frameBuffer: IntArray
    var frameBufferTextureID: IntArray
    var width: Int
    var height: Int
    var renderBuffer: IntArray
    var mProjMatrix = FloatArray(16) //projection matrix
    private val textureStride = TEXTURE_PER_VERTEX * 4 //bytes per texture coordinates
    private val vertexStride = COORDS_PER_VERTEX * 4 // 4 bytes per vertex

    fun draw(mMVPMatrix: FloatArray?) {
        GLES32.glUseProgram(mProgram) // Add program to OpenGL environment
        // Apply the projection and view transformation
        GLES32.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0)
        GLES32.glActiveTexture(GLES32.GL_TEXTURE1) //set the active texture to unit 0
        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, frameBufferTextureID[0]) //bind the texture to this unit
        GLES32.glUniform1i(TextureHandle, 1) //tell the uniform sampler to use this texture i
        GLES32.glVertexAttribPointer(mTextureCoordHandle, TEXTURE_PER_VERTEX, GLES32.GL_FLOAT, false, textureStride, textureBuffer)
        //set the attribute of the vertex to point to the vertex buffer
        GLES32.glEnableVertexAttribArray(mPositionHandle)
        GLES32.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX,
                GLES32.GL_FLOAT, false, vertexStride, vertexBuffer)
        // Draw the 2D plane
        GLES32.glDrawElements(GLES32.GL_TRIANGLES, DisplayIndex.size, GLES32.GL_UNSIGNED_INT, indexBuffer)
        // Disable vertex array
        GLES32.glDisableVertexAttribArray(mPositionHandle)
    }

    fun initaliseTexture(whichTexture: Int, textureID: Int, width: Int, height: Int, pixel_format: Int, type: Int) {
        GLES32.glActiveTexture(whichTexture) //activate the texture
        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, textureID) //bind the texture with the ID
        GLES32.glTexParameterf(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_MIN_FILTER, GLES32.GL_NEAREST.toFloat()) //set the min filter
        GLES32.glTexParameterf(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_MAG_FILTER, GLES32.GL_NEAREST.toFloat()) //set the mag filter
        GLES32.glTexParameterf(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_WRAP_S, GLES32.GL_CLAMP_TO_EDGE.toFloat()) //set the wrap for the edge s
        GLES32.glTexParameterf(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_WRAP_T, GLES32.GL_CLAMP_TO_EDGE.toFloat()) //set the wrap for the edge t
        GLES32.glTexImage2D(GLES32.GL_TEXTURE_2D, 0, pixel_format, width, height, 0, pixel_format, type, null) //set the format to be RGBA
    }

    fun CreateFrameBuffers(width: Int, height: Int) {
        GLES32.glGenFramebuffers(1, frameBuffer, 0) //generate a framebuffer object
        GLES32.glGenTextures(1, frameBufferTextureID, 0) //generate a texture objects to store the color image
        initaliseTexture(GLES32.GL_TEXTURE1, frameBufferTextureID[0], width, height, GLES32.GL_RGBA, GLES32.GL_UNSIGNED_BYTE)
        GLES32.glBindFramebuffer(GLES32.GL_DRAW_FRAMEBUFFER, frameBuffer[0])
        GLES32.glFramebufferTexture2D(GLES32.GL_FRAMEBUFFER, GLES32.GL_COLOR_ATTACHMENT0, GLES32.GL_TEXTURE_2D, frameBufferTextureID[0], 0)
        GLES32.glGenRenderbuffers(1, renderBuffer, 0)
        GLES32.glBindRenderbuffer(GLES32.GL_RENDERBUFFER, renderBuffer[0])
        GLES32.glRenderbufferStorage(GLES32.GL_RENDERBUFFER, GLES32.GL_DEPTH_COMPONENT24, width, height)
        GLES32.glFramebufferRenderbuffer(GLES32.GL_FRAMEBUFFER, GLES32.GL_DEPTH_ATTACHMENT, GLES32.GL_RENDERBUFFER, renderBuffer[0])
        val status = GLES32.glCheckFramebufferStatus(GLES32.GL_FRAMEBUFFER)
        if (status != GLES32.GL_FRAMEBUFFER_COMPLETE) {
            Log.d("framebuffer", "Error in creating framebuffer")
        }
        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, 0) //unbind the texture
        GLES32.glBindFramebuffer(GLES32.GL_FRAMEBUFFER, 0) //unbind the framebuffer
    }

    companion object {
        // number of coordinates per vertex in this array
        const val COORDS_PER_VERTEX = 3
        const val TEXTURE_PER_VERTEX = 2 //no of texture coordinates per vertex

        // to draw the FB we use a Plane
        var DisplayVertex = floatArrayOf( //front face
                -0.5f, -0.5f, 0.0f,
                0.5f, -0.5f, 0.0f,
                0.5f, 0.5f, 0.0f,
                -0.5f, 0.5f, 0.0f)
        var DisplayIndex = intArrayOf(
                0, 1, 2, 0, 2, 3)
        var DisplayTextureCoords = floatArrayOf(0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f)
    }

    init {
        // initialize vertex byte buffer for shape coordinates
        val bb = ByteBuffer.allocateDirect(DisplayVertex.size * 4) // (# of coordinate values * 4 bytes per float)
        bb.order(ByteOrder.nativeOrder())
        vertexBuffer = bb.asFloatBuffer()
        vertexBuffer.put(DisplayVertex)
        vertexBuffer.position(0)
        val ib = IntBuffer.allocate(DisplayIndex.size)
        indexBuffer = ib
        indexBuffer.put(DisplayIndex)
        indexBuffer.position(0)
        val tb = ByteBuffer.allocateDirect(DisplayTextureCoords.size * 4)
        tb.order(ByteOrder.nativeOrder())
        textureBuffer = tb.asFloatBuffer()
        textureBuffer.put(DisplayTextureCoords)
        textureBuffer.position(0)

        // prepare shaders and OpenGL program
        val vertexShader = loadShader(GLES32.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES32.GL_FRAGMENT_SHADER, fragmentShaderCode)
        mProgram = GLES32.glCreateProgram() // create empty OpenGL Program
        GLES32.glAttachShader(mProgram, vertexShader) // add the vertex shader to program
        GLES32.glAttachShader(mProgram, fragmentShader) // add the fragment shader to program
        GLES32.glLinkProgram(mProgram) // link the  OpenGL program to create an executable
        GLES32.glUseProgram(mProgram) // Add program to OpenGL environment

        // get handle to vertex shader's vPosition member
        mPositionHandle = GLES32.glGetAttribLocation(mProgram, "aVertexPosition")
        // Enable a handle to the triangle vertices
        GLES32.glEnableVertexAttribArray(mPositionHandle)
        // Prepare the triangle coordinate data
        GLES32.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX, GLES32.GL_FLOAT, false, vertexStride, vertexBuffer)
        // get handle to shape's transformation matrix
        mTextureCoordHandle = GLES32.glGetAttribLocation(mProgram, "aTextureCoordinate") //texture coordinates
        GLES32.glEnableVertexAttribArray(mTextureCoordHandle)
        GLES32.glVertexAttribPointer(mTextureCoordHandle, TEXTURE_PER_VERTEX, GLES32.GL_FLOAT, false, textureStride, textureBuffer)
        TextureHandle = GLES32.glGetUniformLocation(mProgram, "uTextureSampler") //texture
        mMVPMatrixHandle = GLES32.glGetUniformLocation(mProgram, "uMVPMatrix")

        width = pwidth // /2 set the width to be half of the screen size
        height = pheight

        val ratio = width.toFloat() / height
        val left = -ratio
        // create a perspective projection matrix for drawing objects in the frame buffer
        Matrix.frustumM(mProjMatrix, 0, left, ratio, -1f, 1f, 1.5f, 300f)

        frameBuffer = IntArray(1)
        frameBufferTextureID = IntArray(2)
        renderBuffer = IntArray(1)
        CreateFrameBuffers(width, height)
    }
}
