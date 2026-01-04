/**
 * ©2024 NodeMedia.cn
 * <p>
 * Copyright © 2015 - 2024 NodeMedia.cn All Rights Reserved.
 */

package cn.nodemedia;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView;
import android.view.ViewGroup;
import android.widget.FrameLayout;

public class NodePlayer implements TextureView.SurfaceTextureListener {
    static {
        System.loadLibrary("NodeMediaClient");
    }

    public static final int LOG_LEVEL_ERROR = 0;
    public static final int LOG_LEVEL_INFO = 1;
    public static final int LOG_LEVEL_DEBUG = 2;

    public static final String RTSP_TRANSPORT_UDP = "udp";
    public static final String RTSP_TRANSPORT_TCP = "tcp";
    public static final String RTSP_TRANSPORT_UDP_MULTICAST = "udp_multicast";
    public static final String RTSP_TRANSPORT_HTTP = "http";

    private static final String TAG = "NodeMedia.java";

    private OnNodePlayerEventListener onNodePlayerEventListener = null;
    private TextureView tv = null;
    private Context ctx;
    private long id;

    private FrameLayout.LayoutParams LP = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
            Gravity.CENTER);

    /**
     * 创建NodePlayer
     *
     * @param context Android context
     * @param license 授权码
     */
    public NodePlayer(Context context, String license) {
        id = jniInit(context, license);
        ctx = context;
    }

    @Override
    protected void finalize() {
        jniFree();
    }


    /**
     * 附加到视图
     *
     * @param vg ViewGroup的子类
     */
    public void attachView(ViewGroup vg) {
        if (this.tv == null) {
            this.tv = new TextureView(ctx);
            this.tv.setLayoutParams(LP);
            this.tv.setSurfaceTextureListener(this);
            this.tv.setKeepScreenOn(true);
            vg.addView(this.tv);
        }
    }

    /**
     * 返回当前的TextureView
     *
     * @return 当前的TextureView
     */
    public TextureView getTextureView() {
        return this.tv;
    }

    /**
     * 从视图中移除
     */
    public void detachView() {
        if (this.tv != null) {
            this.tv.setKeepScreenOn(false);
            this.tv = null;
        }
    }

    /**
     * 设置事件回调
     * @param listener
     */
    public void setOnNodePlayerEventListener(OnNodePlayerEventListener listener) {
        this.onNodePlayerEventListener = listener;
    }

    private void onEvent(int event, String msg) {
//        Log.d(TAG, "on Event: " + event + " Message:" + msg);
        if (this.onNodePlayerEventListener != null) {
            this.onNodePlayerEventListener.onEventCallback(this, event, msg);
        }
    }

    private native long jniInit(Context context, String license);

    private native void jniFree();

    /**
     * 开始播放
     *
     * @param url 播放的url
     * @return
     */
    public native int start(String url);

    /**
     * 停止播放
     *
     * @return
     */
    public native int stop();

    /**
     * 暂停或恢复点播视频播放
     *
     * @param pause 是否暂停
     * @return
     */
    public native int pause(boolean pause);

    /**
     * 时移
     *
     * @param pts 时移点，单位毫秒
     * @return
     */
    public native int seek(long pts);

    /**
     * 视频截图
     *
     * @param filename 保存的文件名，jpeg格式
     * @return
     */
    public native int screenshot(String filename);

    /**
     * 开始录制
     * @param filename 保存的文件名，支持mp4,flv,mkv,ts格式
     * @return
     */
    public native int startRecord(String filename);

    /**
     * 停止录制
     * @return
     */
    public native int stopRecord();

    /**
     * 视频是否是点播回放
     *
     * @return 是否点播
     */
    public native boolean isVod();

    /**
     * 视频是否暂停了
     *
     * @return 是否暂停
     */
    public native boolean isPause();

    /**
     * 获取点播视频时长
     *
     * @return 单位毫秒
     */
    public native long getDuration();

    /**
     * 获取点播视频当前播放点
     *
     * @return 单位毫秒
     */
    public native long getCurrentPosition();

    /**
     * 获取点播视频缓冲点
     *
     * @return 单位毫秒
     */
    public native long getBufferPosition();

    /**
     * 获取点播视频缓冲百分比
     * @return 百分比
     */
    public native int getBufferPercentage();

    /**
     * 获取播放器是否正在播放
     * @return
     */
    public native boolean isPlaying();

    /**
     * 设置日志等级
     *
     * @param logLevel 等级
     */
    public native void setLogLevel(int logLevel);

    /**
     * 设置缓存时长
     *
     * @param bufferTime 单位毫秒
     */
    public native void setBufferTime(int bufferTime);

    /**
     * 设置缩放模式
     *
     * @param mode 模式
     */
    public native void setScaleMode(int mode);

    /**
     * 设置视频surface
     *
     * @param surface 视频surface
     */
    public native void setVideoSurface(Surface surface);

    /**
     * 设置RTSP的传输协议， 默认是UDP
     * @param rtspTransport
     */
    public native void setRTSPTransport(String rtspTransport);

    /**
     * 设置HTTP Referer
     * @param httpReferer
     */
    public native void setHTTPReferer(String httpReferer);

    /**
     * 设置HTTP User-Agent
     * @param httpUserAgent
     */
    public native void setHTTPUserAgent(String httpUserAgent);
    /**
     * 设置视频解密密码
     *
     * @param cryptoKey 16字节密码
     */
    public native void setCryptoKey(String cryptoKey);

    /**
     * 设置音量
     * 0.0 最小值 静音
     * 1.0 默认值 原始音量
     * @param volume 0.0 ~~ 1.0
     */
    public native void setVolume(float volume);

    /**
     * 设置是否开启硬件加速
     * @param enable 开关
     */
    public native void setHWAccelEnable(boolean enable);

    /**
     * 视频surface大小已改变
     */
    public native void resizeVideoSurface();

    public native void rotateVideo(int rotate);

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        setVideoSurface(new Surface(surfaceTexture));
//        Log.i(TAG, "onSurfaceTextureAvailable: " + width + "x" + height);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
//        Log.i(TAG, "onSurfaceTextureSizeChanged: " + width + "x" + height);
        resizeVideoSurface();
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
//        Log.i(TAG, "onSurfaceTextureDestroyed");
//        setVideoSurface(null);
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
//        Log.d(TAG, "onSurfaceTextureUpdated");
    }

    public interface OnNodePlayerEventListener {
        void onEventCallback(NodePlayer player, int event, String msg);
    }

}