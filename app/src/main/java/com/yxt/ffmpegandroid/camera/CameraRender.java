package com.yxt.ffmpegandroid.camera;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.view.WindowManager;


import com.yxt.ffmpegandroid.R;
import com.yxt.ffmpegandroid.egl.YUEGLSurfaceView;
import com.yxt.ffmpegandroid.utils.ShaderUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class CameraRender implements YUEGLSurfaceView.YuGLRender, SurfaceTexture.OnFrameAvailableListener {
    private Context context;


    private float[] vertexData = {
            -1f, -1f,
            1f, -1f,
            -1f, 1f,
            1f, 1f,
    };
    private float[] fragmentData = {
//            0f, 0f,
//            1f, 0f,
//            0f, 1f,
//            1f, 1f
            0f, 1f,
            1f, 1f,
            0f, 0f,
            1f, 0f

    };

    private FloatBuffer vertexBuffer;
    private FloatBuffer fragmentBuffer;

    private int program;
    private int vPosition;
    private int fPosition;
    private int vboId;
    private int fboId;
    private int fboTextureId;

    private int cameraTextureId;
    private int umatrixl;
    private float[] matrix = new float[16];

    private SurfaceTexture surfaceTexture;

    private OnSurfaceCreateListener onSurfaceCreateListener;

    private CameraFboRender cameraFboRender;

    //屏幕的宽
    private int screenWidth;
    //屏幕的高
    private int screenHeight;
    //实际控件的宽
    private int width;
    private int height;


    public CameraRender(Context context) {
        this.context = context;

        cameraFboRender = new CameraFboRender(context);
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


    public void restMatrix(){
        //初始化矩阵
        Matrix.setIdentityM(matrix,0);
    }
    public void setAngle(float angle,float x,float y,float z){
        Matrix.rotateM(matrix,0,angle,x,y,z);

    }


    @Override
    public void onSurfaceCreated() {
        try {
            cameraFboRender.onCreate();
            String vertexSource = ShaderUtils.getRawResource(context, R.raw.vertex_shader);
            String fragmentSource = ShaderUtils.getRawResource(context, R.raw.fragment_shader);

            program = ShaderUtils.createProgram(vertexSource, fragmentSource);
            vPosition = GLES20.glGetAttribLocation(program, "v_Position");
            fPosition = GLES20.glGetAttribLocation(program, "f_Position");
            umatrixl = GLES20.glGetUniformLocation(program,"u_Matrix");
            //创建顶点坐标缓存空间（可以解决每次在Cpu中将顶点坐标送到GPU里）
            int[] vbos = new int[1];
            GLES20.glGenBuffers(1, vbos, 0);
            vboId = vbos[0];
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId);
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vertexData.length * 4 + fragmentData.length * 4, null, GLES20.GL_STATIC_DRAW);
            GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0, vertexData.length * 4, vertexBuffer);
            GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, vertexData.length * 4, fragmentData.length * 4, fragmentBuffer);
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

            //创建fbo  离屏渲染
            int[] fbos = new int[1];
            GLES20.glGenBuffers(1, fbos, 0);
            fboId = fbos[0];
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboId);


            int[] textureIds = new int[1];
            GLES20.glGenTextures(1, textureIds, 0);
            fboTextureId = textureIds[0];
            //绑定纹理ID
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fboTextureId);
//            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
//            GLES20.glUniform1i(sampler, 0);

            //环绕设置 超出纹理坐标    S是横坐标  T是纵坐标   GL_REPEAT重复
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);
            //过滤 缩小放大  线性
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

            WindowManager wm = (WindowManager) context
                    .getSystemService(Context.WINDOW_SERVICE);

            screenWidth = wm.getDefaultDisplay().getWidth();
            screenHeight = wm.getDefaultDisplay().getHeight();
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, screenWidth, screenHeight, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);


            GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, fboTextureId, 0);

            //判断 fbo是否绑定正常
            if (GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER) != GLES20.GL_FRAMEBUFFER_COMPLETE) {


            } else {

            }

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

            int[] textureidseos = new int[1];
            GLES20.glGenTextures(1, textureidseos, 0);
            cameraTextureId = textureidseos[0];

            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, cameraTextureId);
            //环绕设置 超出纹理坐标    S是横坐标  T是纵坐标   GL_REPEAT重复
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);
            //过滤 缩小放大  线性
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

            surfaceTexture = new SurfaceTexture(cameraTextureId);
            surfaceTexture.setOnFrameAvailableListener(this);

            if (onSurfaceCreateListener != null) {
                onSurfaceCreateListener.onSurfaceCreate(surfaceTexture,fboTextureId);
            }
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSurfaceChange(int width, int height) {
//        cameraFboRender.onChange(width,height);
//        GLES20.glViewport(0,0,width,height);
        this.width = width;
        this.height = height;

    }

    @Override
    public void onDrawFrame() {

        surfaceTexture.updateTexImage();

        //清屏
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glClearColor(1.0f, 0.0f, 0.0f, 1.0f);
        //使用program
        GLES20.glUseProgram(program);

        GLES20.glViewport(0,0,screenWidth,screenHeight);
        //使用矩阵并赋值
        GLES20.glUniformMatrix4fv(umatrixl,1,false,matrix,0);
        //绑定离屏渲染
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboId);
        //绑定顶点坐标
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId);
//        //使用正交投影
//        GLES20.glUniformMatrix4fv(uMtrix, 1, false, matrix, 0);
//        //绑定纹理
//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, imgTextureId);



        GLES20.glEnableVertexAttribArray(vPosition);
        GLES20.glVertexAttribPointer(vPosition, 2, GLES20.GL_FLOAT, false, 8, 0);
        GLES20.glEnableVertexAttribArray(fPosition);
        GLES20.glVertexAttribPointer(fPosition, 2, GLES20.GL_FLOAT, false, 8, vertexData.length * 4);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        cameraFboRender.onChange(width,height);
        cameraFboRender.onDrawFrame(fboTextureId);
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
    }

    public void setOnSurfaceCreateListener(OnSurfaceCreateListener onSurfaceCreateListener) {
        this.onSurfaceCreateListener = onSurfaceCreateListener;
    }

    public interface OnSurfaceCreateListener {
        void onSurfaceCreate(SurfaceTexture surfaceTexture, int textureId);
    }
}
