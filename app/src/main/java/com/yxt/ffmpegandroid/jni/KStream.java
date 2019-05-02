package com.yxt.ffmpegandroid.jni;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

/**
 * Created by 30884 on 2018/5/5.
 */

public class KStream {
    private Context context;
    Handler handler;
    //是否在推流  true 是   flase 否
    private boolean isStream = false;
    private StateListener stateListener;

    static {
        System.loadLibrary("stream");
    }

    public interface StateListener {
        //网络错误
        void netWorkError();
        //文件错误
        void fileError();
        //播放结束或者点击停止播放
        void stop();
        //未知错误
        void unknownError();
    }

    public void setCallBack(StateListener stateListener) {
        this.stateListener = stateListener;

    }

    public KStream(Context context) {
        this.context = context;
        handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                switch (msg.what) {
                    case 0:
                        Toast.makeText(KStream.this.context, "推流结束", Toast.LENGTH_SHORT).show();
                        if (stateListener != null)
                            stateListener.stop();
                        isStream = false;
                        break;
                    case 2:
                        //推流文件找不到
                        Toast.makeText(KStream.this.context, "找不到文件或文件无法打开", Toast.LENGTH_SHORT).show();
                        if (stateListener != null)
                            stateListener.fileError();
                        isStream = false;
                        break;
                    case 7:
                        //网络出现问题
                        Toast.makeText(KStream.this.context, "断网报错", Toast.LENGTH_SHORT).show();
                        if (stateListener != null)
                            stateListener.netWorkError();
                        isStream = false;
                        break;
                    default:
                        //其他异常
                        Toast.makeText(KStream.this.context, "发生未知错误，反正是停止推流了", Toast.LENGTH_SHORT).show();
                        if (stateListener != null)
                            stateListener.unknownError();
                        isStream = false;
                        break;

                }
                return true;
            }
        });
    }

    public void pushStream(final String videoStr, final String streamAddress) {
        if (!isStream)
            new Thread(new Runnable() {
                @Override
                public void run() {
                    isStream = true;
                    startStream(videoStr, streamAddress);
                }
            }).start();
    }

    private native int startStream(String videoStr, String streamAddress);

    /**
     * Mp4转换为H264
     * @param videoStr Mp4地址
     * @param h264Str H264地址
     * @return
     */
    public native int Mp4ToH2642(String videoStr, String h264Str);

    public native int stop();

    //是否推流的状态回调
    public void isStream(int b) {
        handler.sendEmptyMessage(b);
    }
}
