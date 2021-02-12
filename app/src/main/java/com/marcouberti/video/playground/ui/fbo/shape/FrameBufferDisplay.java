package com.marcouberti.video.playground.ui.fbo.shape;

import android.opengl.GLES32;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static com.marcouberti.video.playground.ui.fbo.MyGLRendererKt.loadShader;

public class FrameBufferDisplay {
    private final String vertexShaderCode =
            "attribute vec3 aVertexPosition;" + "uniform mat4 uMVPMatrix;" +
                    "attribute vec4 aVertexColor;" +
                    "attribute vec2 aTextureCoordinate; " +//texture coordinate
                    "varying vec2 vTextureCoordinate;" +
                    "void main() {" +
                    "gl_Position = uMVPMatrix *vec4(aVertexPosition,1.0);" +
                    "vTextureCoordinate=aTextureCoordinate;" +
                    "}";
    private final String fragmentShaderCode = "precision lowp float;" +
            "varying vec2 vTextureCoordinate;" +
            "uniform sampler2D uTextureSampler;" +//texture
            "void main() {" +
            "vec4 fragmentColor=texture2D(uTextureSampler,vec2(vTextureCoordinate.s,vTextureCoordinate.t));" +//load the color texture
            "gl_FragColor=vec4(fragmentColor.rgb,fragmentColor.a);" +//the fragment color
            "}";
    private final FloatBuffer vertexBuffer;
    private final IntBuffer indexBuffer;
    private final FloatBuffer textureBuffer;
    private final int mProgram;
    private int mPositionHandle;
    private int mTextureCoordHandle;
    private int mMVPMatrixHandle;
    private int TextureHandle;
    //--------
    public int frameBuffer[];
    public int frameBufferTextureID[];
    public int width, height;
    public int renderBuffer[];

    public float[] mProjMatrix = new float[16];//projection matrix

    // number of coordinates per vertex in this array
    static final int COORDS_PER_VERTEX = 3;
    static final int TEXTURE_PER_VERTEX = 2;//no of texture coordinates per vertex
    private final int textureStride = TEXTURE_PER_VERTEX * 4;//bytes per texture coordinates
    private final int vertexStride = COORDS_PER_VERTEX * 4; // 4 bytes per vertex

    // to draw the FB we use a Plane
    static float DisplayVertex[] = {
            //front face
            -1.0f, -1.0f, 1.0f,
            1.0f, -1.0f, 1.0f,
            1.0f, 1.0f, 1.0f,
            -1.0f, 1.0f, 1.0f,};
    static int DisplayIndex[] = {
            0, 1, 2, 0, 2, 3,//front face
    };
    static float DisplayTextureCoords[] = {
            //front face
            0, 0,
            1, 0,
            1, 1,
            0, 1,};

    public FrameBufferDisplay(int pwidth, int pheight) {

        // initialize vertex byte buffer for shape coordinates
        ByteBuffer bb = ByteBuffer.allocateDirect(DisplayVertex.length * 4);// (# of coordinate values * 4 bytes per float)
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(DisplayVertex);
        vertexBuffer.position(0);
        IntBuffer ib = IntBuffer.allocate(DisplayIndex.length);
        indexBuffer = ib;
        indexBuffer.put(DisplayIndex);
        indexBuffer.position(0);
        ByteBuffer tb = ByteBuffer.allocateDirect(DisplayTextureCoords.length * 4);
        tb.order(ByteOrder.nativeOrder());
        textureBuffer = tb.asFloatBuffer();
        textureBuffer.put(DisplayTextureCoords);
        textureBuffer.position(0);

        // prepare shaders and OpenGL program
        int vertexShader = loadShader(GLES32.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES32.GL_FRAGMENT_SHADER, fragmentShaderCode);
        mProgram = GLES32.glCreateProgram();             // create empty OpenGL Program
        GLES32.glAttachShader(mProgram, vertexShader);   // add the vertex shader to program
        GLES32.glAttachShader(mProgram, fragmentShader); // add the fragment shader to program
        GLES32.glLinkProgram(mProgram);                  // link the  OpenGL program to create an executable
        GLES32.glUseProgram(mProgram);// Add program to OpenGL environment

        // get handle to vertex shader's vPosition member
        mPositionHandle = GLES32.glGetAttribLocation(mProgram, "aVertexPosition");
        // Enable a handle to the triangle vertices
        GLES32.glEnableVertexAttribArray(mPositionHandle);
        // Prepare the triangle coordinate data
        GLES32.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX, GLES32.GL_FLOAT, false, vertexStride, vertexBuffer);
        // get handle to shape's transformation matrix
        mTextureCoordHandle = GLES32.glGetAttribLocation(mProgram, "aTextureCoordinate");//texture coordinates
        GLES32.glEnableVertexAttribArray(mTextureCoordHandle);
        GLES32.glVertexAttribPointer(mTextureCoordHandle, TEXTURE_PER_VERTEX, GLES32.GL_FLOAT, false, textureStride, textureBuffer);
        TextureHandle = GLES32.glGetUniformLocation(mProgram, "uTextureSampler");//texture
        mMVPMatrixHandle = GLES32.glGetUniformLocation(mProgram, "uMVPMatrix");

        width = pwidth / 2; // set the width to be half of the screen size
        height = pheight;
        float ratio = (float) width / height;
        float left = -ratio;
        float right = ratio;
        // create a perspective projection matrix for drawing objects in the frame buffer
        Matrix.frustumM(mProjMatrix, 0, left, right, -1f, 1f, 1.5f, 300f);

        //-------------------------
        frameBuffer = new int[1];
        frameBufferTextureID = new int[2];
        renderBuffer = new int[1];
        CreateFrameBuffers(width, height);
    }

    public void draw(float[] mMVPMatrix) {
        GLES32.glUseProgram(mProgram);// Add program to OpenGL environment
        // Apply the projection and view transformation
        GLES32.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);
        GLES32.glActiveTexture(GLES32.GL_TEXTURE1);//set the active texture to unit 0
        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D,frameBufferTextureID[0]);//bind the texture to this unit
        GLES32.glUniform1i(TextureHandle,1);//tell the uniform sampler to use this texture i
        GLES32.glVertexAttribPointer(mTextureCoordHandle,TEXTURE_PER_VERTEX, GLES32.GL_FLOAT,false,textureStride,textureBuffer);
        //set the attribute of the vertex to point to the vertex buffer
        GLES32.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX,
                GLES32.GL_FLOAT, false, vertexStride, vertexBuffer);
        // Draw the 2D plane

        GLES32.glDrawElements(GLES32.GL_TRIANGLES,DisplayIndex.length, GLES32.GL_UNSIGNED_INT,indexBuffer);
    }

    public void initaliseTexture(int whichTexture, int textureID, int width, int height, int pixel_format, int type) {
        GLES32.glActiveTexture(whichTexture);//activate the texture
        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, textureID);//bind the texture with the ID
        GLES32.glTexParameterf(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_MIN_FILTER, GLES32.GL_NEAREST);//set the min filter
        GLES32.glTexParameterf(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_MAG_FILTER, GLES32.GL_NEAREST);//set the mag filter
        GLES32.glTexParameterf(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_WRAP_S, GLES32.GL_CLAMP_TO_EDGE);//set the wrap for the edge s
        GLES32.glTexParameterf(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_WRAP_T, GLES32.GL_CLAMP_TO_EDGE);//set the wrap for the edge t
        GLES32.glTexImage2D(GLES32.GL_TEXTURE_2D, 0, pixel_format, width, height, 0, pixel_format, type, null);//set the format to be RGBA
    }

    public void CreateFrameBuffers(int width, int height) {
        GLES32.glGenTextures(1, frameBufferTextureID, 0);//generate 2 texture objects
        GLES32.glGenFramebuffers(1, frameBuffer, 0);//generate a framebuffer object
        //bind the framebuffer for drawing
        GLES32.glBindFramebuffer(GLES32.GL_DRAW_FRAMEBUFFER, frameBuffer[0]);
        //initialise texture (i.e. glActivateTextgure...glBindTexture...glTexImage2D....)
        initaliseTexture(GLES32.GL_TEXTURE1, frameBufferTextureID[0], width, height, GLES32.GL_RGBA, GLES32.GL_UNSIGNED_BYTE);
        GLES32.glFramebufferTexture2D(GLES32.GL_FRAMEBUFFER, GLES32.GL_COLOR_ATTACHMENT0, GLES32.GL_TEXTURE_2D, frameBufferTextureID[0], 0);
        GLES32.glGenRenderbuffers(1, renderBuffer, 0);
        GLES32.glBindRenderbuffer(GLES32.GL_RENDERBUFFER, renderBuffer[0]);
        GLES32.glRenderbufferStorage(GLES32.GL_RENDERBUFFER, GLES32.GL_DEPTH_COMPONENT24, width, height);
        GLES32.glFramebufferRenderbuffer(GLES32.GL_FRAMEBUFFER, GLES32.GL_DEPTH_ATTACHMENT, GLES32.GL_RENDERBUFFER, renderBuffer[0]);
        int status = GLES32.glCheckFramebufferStatus(GLES32.GL_FRAMEBUFFER);
        if (status != GLES32.GL_FRAMEBUFFER_COMPLETE) {
            Log.d("framebuffer", "Error in creating framebuffer");
        }
        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, 0);//unbind the texture
        GLES32.glBindFramebuffer(GLES32.GL_FRAMEBUFFER, 0);//unbind the framebuffer
    }
}
