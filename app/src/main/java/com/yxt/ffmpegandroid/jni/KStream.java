package com.yxt.ffmpegandroid.jni;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.widget.Toast;

/**
 * Created by 30884 on 2018/5/5.
 */

public class KStream {
    private Context context;
    Handler handler;
    //是否在推流  true 是   flase 否
    private boolean isStream = false;
    static {
        System.loadLibrary("stream");
    }

    public KStream(Context context){
        this.context = context;
        handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                switch (msg.what){
                    case 1:
                        Toast.makeText(KStream.this.context, "推流结束", Toast.LENGTH_SHORT).show();
                        isStream = false;
                        break;
                }
                return true;
            }
        });
    }
    public void pushStream(final String videoStr,final String streamAddress){
        new Thread(new Runnable() {
            @Override
            public void run() {
                isStream = true;
                startStream(videoStr,streamAddress);
            }
        }).start();
    }

    private native int startStream(String videoStr, String streamAddress);

    public native int stop();

    //是否推流的状态回调
    public void isStream(){
        handler.sendEmptyMessage(1);
    }
}
