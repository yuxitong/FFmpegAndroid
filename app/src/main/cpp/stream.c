#include <jni.h>
#include "libavcodec/avcodec.h"
#include "libavformat/avformat.h"
#include "libavutil/log.h"
#include "libavutil/time.h"
#include "Android/log.h"

#define LOGE(format, ...) __android_log_print(ANDROID_LOG_ERROR, "(>_<)", format, ##__VA_ARGS__)
int *isPlay = 0;


//传入文件开始推流
JNIEXPORT jint JNICALL
Java_com_yxt_ffmpegandroid_jni_KStream_startStream(JNIEnv *env, jobject obj, jstring input_jstr,
                                                   jstring output_jstr) {

    //获取java对象
//    jclass jmapclass = (*env)->FindClass("com/yxt/ffmpegandroid/jni/KStream");
    jclass class = (*env)->FindClass(env, "com/yxt/ffmpegandroid/jni/KStream");
//    jclass class = (*env)->FindClass(env, obj);
    jmethodID isStream = (*env)->GetMethodID(env, class, "isStream", "(I)V");


    int error = 0;
    //java string -> c char*
    //视频文件所在路径
    const char *input_cstr = (*env)->GetStringUTFChars(env, input_jstr, NULL);
    //推送的流媒体地址
    const char *output_cstr = (*env)->GetStringUTFChars(env, output_jstr, NULL);
    //封装格式(读入，写出)（解封装，得到frame）
    AVFormatContext *inFmtCtx = avformat_alloc_context(), *outFmtCtx = NULL;
    AVOutputFormat *ofmt = NULL;
    AVPacket pkt;


    //注册组件
    av_register_all();
    //初始化网络
    avformat_network_init();
    int ret = 0;
    LOGE("视频文件地址 '%s'", input_cstr);

    //Input
    if ((ret = avformat_open_input(&inFmtCtx, input_cstr, 0, 0)) < 0) {
//        av_strerror(ret, buf, 1024);
//        printf("Couldn't open file %s:", input_cstr, ret);
//        LOGE("Couldn't open file %s: ", input_cstr, ret);
        error = 2;
        LOGE("Could not open input file.");
        goto end;
    }
    //获取文件信息
    if ((ret = avformat_find_stream_info(inFmtCtx, 0)) < 0) {
        LOGE("Failed to retrieve input stream information");
        error = 3;
        goto end;
    }
    //获取视频的索引位置
    int videoindex = -1;
    for (int i = 0; i < inFmtCtx->nb_streams; i++)
        if (inFmtCtx->streams[i]->codec->codec_type == AVMEDIA_TYPE_VIDEO) {
            videoindex = i;
            break;
        }
    //输出封装格式，推送flv封装格式的视频流
    avformat_alloc_output_context2(&outFmtCtx, NULL, "flv", output_cstr); //RTMP
    //avformat_alloc_output_context2(&outFmtCtx, NULL, "mpegts", output_cstr);//UDP

    if (!outFmtCtx) {
        LOGE("Could not create output context\n");
        ret = AVERROR_UNKNOWN;
        error = 4;
        goto end;
    }


    for (int i = 0; i < inFmtCtx->nb_streams; i++) {
        //解码器，解码上下文保持一致
        AVStream *in_stream = inFmtCtx->streams[i];
        AVStream *out_stream = avformat_new_stream(outFmtCtx, in_stream->codec->codec);
        if (!out_stream) {
            LOGE("Failed allocating output stream\n");
            ret = AVERROR_UNKNOWN;
            error = 5;
            goto end;
        }
        //复制解码器上下文的 设置
        ret = avcodec_copy_context(out_stream->codec, in_stream->codec);
        if (ret < 0) {
            LOGE("Failed to copy context from input to output stream codec context\n");
            error = 6;
            goto end;
        }
        //全局的header
        out_stream->codec->codec_tag = 0;
        if (outFmtCtx->oformat->flags & AVFMT_GLOBALHEADER)
            out_stream->codec->flags |= CODEC_FLAG_GLOBAL_HEADER;
    }


    //打开输出的AVIOContext IO流上下文
    ofmt = outFmtCtx->oformat;
    //Open output URL
    if (!(ofmt->flags & AVFMT_NOFILE)) {
        ret = avio_open(&outFmtCtx->pb, output_cstr, AVIO_FLAG_WRITE);
        if (ret < 0) {
            LOGE("Could not open output URL '%s'", output_cstr);
            error = 7;
            goto end;
        }
    }
    //先写一个头
    ret = avformat_write_header(outFmtCtx, NULL);
    if (ret < 0) {
        LOGE("Error occurred when opening output URL\n");
        error = 8;
        goto end;
    }


    int frame_index = 0;
    int64_t start_time = av_gettime();



    while (1) {
        if (isPlay != 0) {
            break;
        }
        AVStream *in_stream, *out_stream;
        //Get an AVPacket
        ret = av_read_frame(inFmtCtx, &pkt);
        if (ret < 0)
            break;
        //FIX：No PTS (Example: Raw H.264)
        //raw stream 裸流
        //PTS:Presentation Time Stamp 解码后视频帧要在什么时候取出来
        //DTS:送入解码器后什么时候标识进行解码
        if (pkt.pts == AV_NOPTS_VALUE) {
            //Write PTS
            AVRational time_base1 = inFmtCtx->streams[videoindex]->time_base;
            //Duration between 2 frames (us)
            int64_t calc_duration =
                    (int64_t) ((double) AV_TIME_BASE /
                               av_q2d(inFmtCtx->streams[videoindex]->r_frame_rate));
            //Parameters
            pkt.pts = (int64_t) ((double) (frame_index * calc_duration) /
                                 (double) (av_q2d(time_base1) * AV_TIME_BASE));
            pkt.dts = pkt.pts;
            pkt.duration = (int64_t) ((double) calc_duration /
                                      (double) (av_q2d(time_base1) * AV_TIME_BASE));
        }
        //读入速度比较快，可以在这里调整读取速度减轻服务器压力
        if (pkt.stream_index == videoindex) {
            AVRational time_base = inFmtCtx->streams[videoindex]->time_base;
            AVRational time_base_q = {1, AV_TIME_BASE};
            int64_t pts_time = av_rescale_q(pkt.dts, time_base, time_base_q);
            int64_t now_time = av_gettime() - start_time;
            if (pts_time > now_time)
                av_usleep((unsigned int) (pts_time - now_time));

        }

        in_stream = inFmtCtx->streams[pkt.stream_index];
        out_stream = outFmtCtx->streams[pkt.stream_index];
        /* copy packet */
        //Convert PTS/DTS
        pkt.pts = av_rescale_q_rnd(pkt.pts, in_stream->time_base, out_stream->time_base,
                                   AV_ROUND_NEAR_INF | AV_ROUND_PASS_MINMAX);
        pkt.dts = av_rescale_q_rnd(pkt.dts, in_stream->time_base, out_stream->time_base,
                                   AV_ROUND_NEAR_INF | AV_ROUND_PASS_MINMAX);
        pkt.duration = av_rescale_q(pkt.duration, in_stream->time_base, out_stream->time_base);
        pkt.pos = -1;
        //Print to Screen
        if (pkt.stream_index == videoindex) {
            LOGE("Send %8d video frames to output URL\n", frame_index);
            frame_index++;
        }
        //写数据
        //ret = av_write_frame(outFmtCtx, &pkt);
        ret = av_interleaved_write_frame(outFmtCtx, &pkt);

        if (ret < 0) {
            LOGE("Error muxing packet\n");
            break;
        }
        av_free_packet(&pkt);

    }
    isPlay = 0;
    //写结尾
    av_write_trailer(outFmtCtx);
    (*env)->CallVoidMethod(env, obj, isStream, 0);

    end:
    //释放自愿
    avformat_close_input(&inFmtCtx);
    /* 关闭输出流 */
    if (outFmtCtx && !(ofmt->flags & AVFMT_NOFILE))
        avio_close(outFmtCtx->pb);
    avformat_free_context(outFmtCtx);
    if (ret < 0 && ret != AVERROR_EOF) {
        LOGE("Error occurred.\n");
        (*env)->CallVoidMethod(env, obj, isStream, error);
        return -1;
    }
    return 0;
}

//停止推流
JNIEXPORT jint JNICALL
Java_com_yxt_ffmpegandroid_jni_KStream_stop(JNIEnv *env, jobject obj) {
    isPlay = 1;
//    jclass class = (*env)->FindClass(env, "com/yxt/ffmpegandroid/jni/KStream");
////    jclass class = (*env)->FindClass(env, obj);
//    jmethodID isStream = (*env)->GetMethodID(env, class, "isStream", "(I)V");
//    (*env)->CallVoidMethod(env, obj, isStream, 1);
}