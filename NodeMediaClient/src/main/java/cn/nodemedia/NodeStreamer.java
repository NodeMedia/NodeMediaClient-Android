package cn.nodemedia;

import android.content.Context;

import androidx.annotation.NonNull;

public class NodeStreamer {
    static {
        System.loadLibrary("NodeMediaClient");
    }

    public static final int NMC_RAW_VIDEO_YUV420 = 0;
    public static final int NMC_RAW_VIDEO_NV12 = 23;
    public static final int NMC_RAW_VIDEO_NV21 = 24;

    public static final int NMC_RAW_AUDIO_PCMU8 = 0;
    public static final int NMC_RAW_AUDIO_PCMS16 = 1;
    public static final int NMC_RAW_AUDIO_PCMS32 = 2;
    public static final int NMC_RAW_AUDIO_PCMF32 = 3;

    private Context ctx;
    private long id;

    private OnNodeStreamerEventListener nodeStreamerEventListener;

    private OnNodeStreamerMediaListener nodeStreamerMediaListener;

    public void setNodeStreamerEventListener(OnNodeStreamerEventListener nodeStreamerEventListener) {
        this.nodeStreamerEventListener = nodeStreamerEventListener;
    }

    public void setNodeStreamerMediaListener(OnNodeStreamerMediaListener nodeStreamerMediaListener) {
        this.nodeStreamerMediaListener = nodeStreamerMediaListener;
    }

    public NodeStreamer(@NonNull Context context, @NonNull String license) {
        ctx = context;
        id = jniInit(context, license);
    }

    private native long jniInit(Context context, String license);

    private native void jniFree();

    public native void setRawVideoMediaFormat(int format, int width, int height);

    public native void setRawAudioMediaFormat(int format, int sampleRate, int channels);

    public native void setEncVideoMediaFormat(int codec, int profile, int width, int height, int fps, int keyInterval, int bitrate);

    public native void setEncAudioMediaFormat(int codec, int profile, int sampleRate, int channels, int bitrate);

    public native int sendRawVideoFrame(byte[] data, int length, long timestamp);

    public native int sendRawAudioFrame(byte[] data, int length, long timestamp);

    public native int startPull(String url);

    public native int stopPull();

    public native int startPush(String url);

    public native int stopPush();

    public interface OnNodeStreamerEventListener {
        void onEventCallback(NodeStreamer publisher, int event, String msg);
    }

    public interface OnNodeStreamerMediaListener {

        void onAudioInfoCallback(NodeStreamer publisher, int format, int sampleRate, int channels);

        void onVideoInfoCallback(NodeStreamer publisher, int format, int width, int height);

        void onAudioFrameCallback(NodeStreamer publisher, byte[] data, int length, long timestamp);

        void onVideoFrameCallback(NodeStreamer publisher, byte[] data, int length, long timestamp);

    }

    private void onEvent(int event, String msg) {
//        Log.d(TAG, "on Event: " + event + " Message:" + msg);
        if (this.nodeStreamerEventListener != null) {
            this.nodeStreamerEventListener.onEventCallback(this, event, msg);
        }
    }

    private void onAudioInfo(int format, int sampleRate, int channels) {
//        Log.d(TAG, "on Event: " + event + " Message:" + msg);
        if (this.nodeStreamerMediaListener != null) {
            this.nodeStreamerMediaListener.onAudioInfoCallback(this, format, sampleRate, channels);
        }
    }

    private void onVideoInfo(int format, int width, int height) {
//        Log.d(TAG, "on Event: " + event + " Message:" + msg);
        if (this.nodeStreamerMediaListener != null) {
            this.nodeStreamerMediaListener.onVideoInfoCallback(this, format, width, height);
        }
    }

    private void onAudioFrame(byte[] data, int length, long timestamp) {
        if (this.nodeStreamerMediaListener != null) {
            this.nodeStreamerMediaListener.onAudioFrameCallback(this, data, length, timestamp);
        }
    }

    private void onVideoFrame(byte[] data, int length, long timestamp) {
        if (this.nodeStreamerMediaListener != null) {
            this.nodeStreamerMediaListener.onVideoFrameCallback(this, data, length, timestamp);
        }
    }

}
