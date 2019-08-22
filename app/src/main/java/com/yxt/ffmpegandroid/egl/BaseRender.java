package com.yxt.ffmpegandroid.egl;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLES20;


import com.yxt.ffmpegandroid.R;
import com.yxt.ffmpegandroid.utils.ShaderUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public abstract class BaseRender {

    protected Context context;
    protected float[] vertexData = {
            -1f, -1f,
            1f, -1f,
            -1f, 1f,
            1f, 1f,


            0f, 0f,
            0f, 0f,
            0f, 0f,
            0f, 0f,
    };
    protected float[] fragmentData = {
            0f, 1f,
            1f, 1f,
            0f, 0f,
            1f, 0f
    };
    protected FloatBuffer vertexBuffer;
    protected FloatBuffer fragmentBuffer;
    protected int program;
    protected int vPosition;
    protected int fPosition;
    protected int sampler;
    protected int vboId;
    private int bitmapTextureId;
    private Bitmap bitmap;
    public BaseRender(Context context) {
        this.context = context;


        bitmap = ShaderUtils.createTextImage("移动开发小学生", 50, "#FF0000", "#00000000", 0);
        float r = 1.0f * bitmap.getWidth() / bitmap.getHeight();
        float w = r * 0.1f;
        vertexData[8] = 0.8f - w;
        vertexData[9] = -0.8f;

        vertexData[10] = 0.8f;
        vertexData[11] = -0.8f;

        vertexData[12] = 0.8f - w;
        vertexData[13] = -0.7f;

        vertexData[14] = 0.8f;
        vertexData[15] = -0.7f;


        vertexBuffer = ByteBuffer.allocateDirect(vertexData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vertexData);
        vertexBuffer.position(0);
        fragmentBuffer = ByteBuffer.allocateDirect(fragmentData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(fragmentData);
        fragmentBuffer.position(0);
    }

    public void onCreate() {
        try {
            //设置透明渲染
            GLES20.glEnable (GLES20.GL_BLEND);
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

            String vertexSource = ShaderUtils.getRawResource(context, R.raw.vertex_shader_screen);

            String fragmentSource = ShaderUtils.getRawResource(context, R.raw.fragment_shader_screen);
            program = ShaderUtils.createProgram(vertexSource, fragmentSource);
            vPosition = GLES20.glGetAttribLocation(program, "v_Position");
            fPosition = GLES20.glGetAttribLocation(program, "f_Position");
            sampler = GLES20.glGetUniformLocation(program, "sTexture");

            int[] vbos = new int[1];
            GLES20.glGenBuffers(1, vbos, 0);
            vboId = vbos[0];
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId);
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vertexData.length * 4 + fragmentData.length * 4, null, GLES20.GL_STATIC_DRAW);
            GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0, vertexData.length * 4, vertexBuffer);
            GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, vertexData.length * 4, fragmentData.length * 4, fragmentBuffer);
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
            bitmapTextureId = ShaderUtils.loadBitmapTexture(bitmap);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onChange(int width,int height){
        GLES20.glViewport(0,0,width,height);
    }

    public void onDrawFrame(int textureId){
        //清屏
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glClearColor(1.0f, 0.0f, 0.0f, 1.0f);
        //使用program
        GLES20.glUseProgram(program);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId);

        //绑定fbo纹理
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glEnableVertexAttribArray(vPosition);
        GLES20.glVertexAttribPointer(vPosition, 2, GLES20.GL_FLOAT, false, 8, 0);
        GLES20.glEnableVertexAttribArray(fPosition);
        GLES20.glVertexAttribPointer(fPosition, 2, GLES20.GL_FLOAT, false, 8, vertexData.length * 4);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);


        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, bitmapTextureId);
        GLES20.glEnableVertexAttribArray(vPosition);
        GLES20.glVertexAttribPointer(vPosition, 2, GLES20.GL_FLOAT, false, 8, 32);
        GLES20.glEnableVertexAttribArray(fPosition);
        GLES20.glVertexAttribPointer(fPosition, 2, GLES20.GL_FLOAT, false, 8, vertexData.length * 4);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);


        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
    }
}
