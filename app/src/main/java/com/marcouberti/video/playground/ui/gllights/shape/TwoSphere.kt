package com.marcouberti.video.playground.ui.gllights.shape

import android.opengl.GLES32
import com.marcouberti.video.playground.checkGlError
import com.marcouberti.video.playground.ui.gllights.loadShader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.util.*

/**
 * Draw two sphere filling the buffers and drawing sequentially.
 */
class TwoSphere {
    private val vertexShaderCode =
            """
            attribute vec3 aVertexPosition;
            uniform mat4 uMVPMatrix;
            varying vec4 vColor;
            attribute vec4 aVertexColor;
            uniform vec3 uPointLightingLocation; 
            varying float vPointLightWeighting; 
            
            void main() {
                vec4 mvPosition=uMVPMatrix*vec4(aVertexPosition,1.0); 
                vec3 lightDirection=normalize(uPointLightingLocation-mvPosition.xyz); 
                float dist_from_light=distance(uPointLightingLocation, mvPosition.xyz); 
                vPointLightWeighting=100.0/(dist_from_light*dist_from_light); 
                gl_Position = uMVPMatrix * vec4(aVertexPosition,1.0);
                vColor=aVertexColor;
            }
            """.trimIndent()
    private val fragmentShaderCode =
            """
            precision mediump float;
            varying vec4 vColor; 
            varying float vPointLightWeighting;
            void main() {
                gl_FragColor = vec4(vColor.xyz*vPointLightWeighting,1);
            }
            """.trimIndent()
    
    private val vertexBuffer: FloatBuffer
    private val colorBuffer: FloatBuffer
    private val indexBuffer: IntBuffer

    private val vertexBuffer2: FloatBuffer
    private val colorBuffer2: FloatBuffer
    private val indexBuffer2: IntBuffer
    
    private val mProgram: Int
    private var mPositionHandle = 0
    private var mPointLightLocationHandle = 0
    private var mColorHandle = 0
    private var mMVPMatrixHandle = 0
    private var vertexCount // number of vertices
            : Int

    // number of coordinates per vertex in this array
    private val COORDS_PER_VERTEX = 3
    private val COLOR_PER_VERTEX = 4

    private val vertexStride = COORDS_PER_VERTEX * 4 // 4 bytes per vertex
    private val colorStride = COLOR_PER_VERTEX * 4 // 4 bytes per vertex

    private lateinit var SphereVertex: FloatArray
    private lateinit var  SphereIndex: IntArray
    private lateinit var  SphereColor: FloatArray

    private lateinit var Sphere2Vertex: FloatArray
    private lateinit var  Sphere2Index: IntArray
    private lateinit var  Sphere2Color: FloatArray

    private val pointLightLocation = floatArrayOf(10f, 10f, 0f)

    private fun createSphere(radius: Float, nolatitude: Int, nolongitude: Int) {
        val vertices = FloatArray(65535)
        val index = IntArray(65535)
        val color = FloatArray(65535)
        val vertices2 = FloatArray(65535)
        val index2 = IntArray(65535)
        val color2 = FloatArray(65535)
        var vertedindex = 0
        var colorindex = 0
        var indx = 0
        var vertedindex2 = 0
        var colorindex2 = 0
        var indx2 = 0
        val dist = 3f
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
                vertices2[vertedindex2++] = (radius * x).toFloat()
                vertices2[vertedindex2++] = (radius * cosTheta).toFloat() - dist
                vertices2[vertedindex2++] = (radius * z).toFloat()
                color[colorindex++] = Math.abs(tcolor)
                color[colorindex++] = 1 - Math.abs(tcolor)
                color[colorindex++] = 0f
                color[colorindex++] = 0f
                color2[colorindex2++] = 0f
                color2[colorindex2++] = 1f
                color2[colorindex2++] = Math.abs(tcolor)
                color2[colorindex2++] = 0f
                tcolor += tcolorinc
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
                index2[indx2++] = P0
                index2[indx2++] = P1
                index2[indx2++] = P0 + 1
                index2[indx2++] = P1
                index2[indx2++] = P1 + 1
                index2[indx2++] = P0 + 1
            }
        }

        // SET THE BUFFERS
        SphereVertex = Arrays.copyOf(vertices, vertedindex)
        SphereIndex = Arrays.copyOf(index, indx)
        SphereColor = Arrays.copyOf(color, colorindex)
        Sphere2Vertex = Arrays.copyOf(vertices2, vertedindex2)
        Sphere2Index = Arrays.copyOf(index2, indx2)
        Sphere2Color = Arrays.copyOf(color2, colorindex2)
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

        // get handle to point light position
        mPointLightLocationHandle = GLES32.glGetUniformLocation(mProgram, "uPointLightingLocation")
        // set point light location
        GLES32.glUniform3fv(mPointLightLocationHandle, 1, pointLightLocation, 0)

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
        GLES32.glDrawElements(
                GLES32.GL_TRIANGLES,
                //GLES32.GL_LINES,
                SphereIndex.size,
                GLES32.GL_UNSIGNED_INT,
                indexBuffer
        )

        //set the attribute of the vertex to point to the vertex buffer
        GLES32.glVertexAttribPointer(
                mPositionHandle, COORDS_PER_VERTEX,
                GLES32.GL_FLOAT, false, vertexStride, vertexBuffer2
        )

        //set the attribute of the vertex colors to point to the color buffer
        GLES32.glVertexAttribPointer(
                mColorHandle, COLOR_PER_VERTEX,
                GLES32.GL_FLOAT, false, colorStride, colorBuffer2
        )
        // Draw the triangle fan
        GLES32.glDrawElements(
                GLES32.GL_TRIANGLES,
                //GLES32.GL_LINES,
                Sphere2Index.size,
                GLES32.GL_UNSIGNED_INT,
                indexBuffer2
        )
    }

    init {
        createSphere(1f, 30, 30)

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

        // initialize vertex byte buffer for shape coordinates

        // initialize vertex byte buffer for shape coordinates
        val bb2 = ByteBuffer.allocateDirect(Sphere2Vertex.size * 4) // (# of coordinate values * 4 bytes per float)
        bb2.order(ByteOrder.nativeOrder())
        vertexBuffer2 = bb2.asFloatBuffer()
        vertexBuffer2.put(Sphere2Vertex)
        vertexBuffer2.position(0)
        vertexCount = Sphere2Vertex.size / COORDS_PER_VERTEX
        
        val cb2 = ByteBuffer.allocateDirect(Sphere2Color.size * 4) // (# of coordinate values * 4 bytes per float)
        cb2.order(ByteOrder.nativeOrder())
        colorBuffer2 = cb2.asFloatBuffer()
        colorBuffer2.put(Sphere2Color)
        colorBuffer2.position(0)
        
        val ib2 = IntBuffer.allocate(Sphere2Index.size)
        indexBuffer2 = ib2
        indexBuffer2.put(Sphere2Index)
        indexBuffer2.position(0)

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
