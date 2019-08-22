package com.yxt.ffmpegandroid.camera;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;

import com.yxt.ffmpegandroid.egl.YUEGLSurfaceView;


public class CameraView extends YUEGLSurfaceView {
    private CameraRender cameraRender;
    private YUCamera yuCamera;

    private int cameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;

    private int textureId = -1;

    public CameraView(Context context) {
        this(context, null);
    }

    public CameraView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public CameraView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        cameraRender = new CameraRender(context);
        setRender(cameraRender);
        yuCamera = new YUCamera(context);
        prevewAngle(context);
        cameraRender.setOnSurfaceCreateListener(new CameraRender.OnSurfaceCreateListener() {
            @Override
            public void onSurfaceCreate(SurfaceTexture surfaceTexture, int textureId) {
                yuCamera.initCamera(surfaceTexture, cameraId);
                CameraView.this.textureId = textureId;
            }
        });

    }

    public void prevewAngle(Context context) {
        int angle = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
        Log.e("angleTotal", "  " + angle);
        cameraRender.restMatrix();
        switch (angle) {
            case Surface.ROTATION_0:
                Log.e("angle", "0");
                if (cameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    cameraRender.setAngle(90, 0, 0, 1);
                    cameraRender.setAngle(180, 1, 0, 0);
                } else {
                    cameraRender.setAngle(90, 0, 0, 1);
                }
                break;
            case Surface.ROTATION_90:
                Log.e("angle", "90");
                if (cameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    cameraRender.setAngle(180, 0, 0, 1);
                    cameraRender.setAngle(180, 0, 1, 0);
                } else {
                    cameraRender.setAngle(180, 0, 0, 1);

                }
                break;
            case Surface.ROTATION_180:
                Log.e("angle", "180");
                if (cameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    cameraRender.setAngle(90, 0, 0, 1);
                    cameraRender.setAngle(180, 0, 1, 0);
                } else {
                    cameraRender.setAngle(-90, 0, 0, 1);
                }
                break;
            case Surface.ROTATION_270:
                Log.e("angle", "270");
                if (cameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    cameraRender.setAngle(180, 0, 1, 0);
                }
                break;
        }
    }

    public int getTextureId() {
        return textureId;
    }

    public void onDestory() {
        if (yuCamera != null) {
            yuCamera.stopPreview();
        }
    }
}
