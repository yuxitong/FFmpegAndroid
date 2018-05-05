package com.yxt.ffmpegandroid;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSIONS_REQUEST_CODE = 1;

    // Used to load the 'native-lib' library on application startup.
    static {
//        System.loadLibrary("native-lib");
        System.loadLibrary("stream");
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //权限申请
        if (!allPermissionsGranted()) {
            requestPermissions(getRequiredPermissions(), PERMISSIONS_REQUEST_CODE);
            return;
        }
        // Example of a call to a native method
    }

    public void BtnClick(View view) {
        switch (view.getId()) {
            case R.id.startBtn:
                final String videoStr = ((TextView) findViewById(R.id.videoEdit)).getText().toString();
                final String rtmpStr = ((TextView) findViewById(R.id.RtmpEdit)).getText().toString();
                //这里没做限制 请一定填写好地址在点击开始推流
                if (videoStr != null && rtmpStr != null) {

                }
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        startStream(videoStr, rtmpStr);
                    }
                });
                break;
            case R.id.stopBtn:
                stop();
                break;
        }

    }

    private String[] getRequiredPermissions() {
        Activity activity = this;
        try {
            PackageInfo info =
                    activity
                            .getPackageManager()
                            .getPackageInfo(activity.getPackageName(), PackageManager.GET_PERMISSIONS);
            String[] ps = info.requestedPermissions;
            if (ps != null && ps.length > 0) {
                return ps;
            } else {
                return new String[0];
            }
        } catch (Exception e) {
            return new String[0];
        }
    }

    private boolean allPermissionsGranted() {
        for (String permission : getRequiredPermissions()) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    // stream.c里面的一个函数  用来传入文件推流的
    public native int startStream(String videoStr, String streamAddress);

    //中断推流
    public native int stop();
}
