/**
 * ©2024 NodeMedia
 * <p>
 * Copyright © 2015 - 2024 NodeMedia.All Rights Reserved.
 */

package cn.nodemedia;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.util.Size;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class NodePublisher {
    static {
        System.loadLibrary("NodeMediaClient");
    }

    public static final int LOG_LEVEL_ERROR = 0;
    public static final int LOG_LEVEL_INFO = 1;
    public static final int LOG_LEVEL_DEBUG = 2;

    public static final int NMC_CAMERA_FRONT = 0;
    public static final int NMC_CAMERA_BACK = 1;

    public static final int NMC_CODEC_ID_H264 = 27;
    public static final int NMC_CODEC_ID_H265 = 173;
    public static final int NMC_CODEC_ID_AAC = 86018;
    public static final int NMC_CODEC_ID_OPUS = 86076;
    public static final int NMC_CODEC_ID_PCMA = 65543;
    public static final int NMC_CODEC_ID_PCMU = 65542;

    public static final int NMC_PROFILE_AUTO = 0;
    public static final int NMC_PROFILE_H264_BASELINE = 66;
    public static final int NMC_PROFILE_H264_MAIN = 77;
    public static final int NMC_PROFILE_H264_HIGH = 100;
    public static final int NMC_PROFILE_H265_MAIN = 1;
    public static final int NMC_PROFILE_AAC_LC = 1;
    public static final int NMC_PROFILE_AAC_HE = 4;
    public static final int NMC_PROFILE_AAC_HE_V2 = 28;

    public static final int VIDEO_ORIENTATION_PORTRAIT = 0;
    public static final int VIDEO_ORIENTATION_LANDSCAPE_RIGHT = 90;
    public static final int VIDEO_ORIENTATION_LANDSCAPE_LEFT = 270;

    // 特效参数字符串常量
    public static final String EFFECTOR_BRIGHTNESS = "brightness";
    public static final String EFFECTOR_CONTRAST = "contrast";
    public static final String EFFECTOR_SATURATION = "saturation";
    public static final String EFFECTOR_SHARPEN = "sharpen";
    public static final String EFFECTOR_SMOOTHSKIN = "smoothskin";
    public static final String EFFECTOR_STYLE = "style";

    public static final int EFFECTOR_STYLE_ID_ORIGINAL = 0;
    public static final int EFFECTOR_STYLE_ID_ENHANCED = 1;
    public static final int EFFECTOR_STYLE_ID_FAIRSKIN = 2;
    public static final int EFFECTOR_STYLE_ID_COOL = 3;
    public static final int EFFECTOR_STYLE_ID_FILM = 4;
    public static final int EFFECTOR_STYLE_ID_BOOST = 5;

    /**
     * 自动对焦
     */
    public static final int FLAG_AF = 1;

    /**
     * 自动曝光
     */
    public static final int FLAG_AE = 1 << 1;

    /**
     * 自动白平衡
     */
    public static final int FLAG_AWB = 1 << 2;


    private static final String TAG = "NodeMedia.java";
    private OnNodePublisherEventListener onNodePublisherEventListener;
    private GLCameraView mGLCameraView;
    private Camera2Manager camera2Manager;
    private int mCameraID = -1;
    private Context ctx;
    private long id;
    private int videoOrientation = VIDEO_ORIENTATION_PORTRAIT;
    private int videoWidth = 720; // jni层会修改这个值
    private int videoHeight = 1280; // jni层会修改这个值
    private int cameraWidth = 0;
    private int cameraHeight = 0;
    private int surfaceWidth = 0;
    private int surfaceHeight = 0;
    private boolean isCameraOpened = false;
    private boolean isScreenCreated = false;
    private final FrameLayout.LayoutParams sLayoutParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
            Gravity.CENTER);

    public NodePublisher(Context context, String license) {
        ctx = context;
        id = jniInit(context, license);
        camera2Manager = new Camera2Manager(context);
        camera2Manager.setCameraStateListener(new Camera2Manager.CameraStateListener() {
            @Override
            public void onCameraOpened() {
                Log.d(TAG, "Camera opened successfully");
                isCameraOpened = true;
                Size previewSize = camera2Manager.getPreviewSize();
                cameraWidth = previewSize.getWidth();
                cameraHeight = previewSize.getHeight();
                if (isScreenCreated) {
                    boolean isFrontCamera = mCameraID == NMC_CAMERA_FRONT;
                    int sensorOrientation = camera2Manager.getSensorOrientation();
                    // 获取摄像头传感器方向并计算正确的旋转角度
                    Log.d(TAG, "Camera Open - Sensor rotation degrees: " + sensorOrientation);
                    GPUImageChange(surfaceWidth, surfaceHeight, cameraWidth, cameraHeight, videoOrientation, sensorOrientation, isFrontCamera);
                }
            }

            @Override
            public void onCameraClosed() {
                Log.d(TAG, "Camera closed");
                isCameraOpened = false;
            }

            @Override
            public void onCameraError(String error) {
                Log.e(TAG, "Camera error: " + error);
                if(onNodePublisherEventListener != null) {
                    onNodePublisherEventListener.onEventCallback(NodePublisher.this, 2104, error);
                }
            }
        });
    }

    @Override
    protected void finalize() throws Throwable {
        jniFree();
        super.finalize();
    }

    public void setOnNodePublisherEventListener(OnNodePublisherEventListener onNodePublisherEventListener) {
        this.onNodePublisherEventListener = onNodePublisherEventListener;
    }

    public void attachView(ViewGroup vg) {
        if (this.mGLCameraView == null) {
            this.mGLCameraView = new GLCameraView(this.ctx);
            this.mGLCameraView.setLayoutParams(sLayoutParams);
            this.mGLCameraView.setKeepScreenOn(true);
            vg.addView(this.mGLCameraView);
            Log.d(TAG, "GLCameraView attached");
        }
    }

    public void detachView() {
        if (this.mGLCameraView != null) {
            this.mGLCameraView.setVisibility(GLSurfaceView.GONE);
            this.mGLCameraView.setKeepScreenOn(false);
            this.mGLCameraView = null;
        }
    }

    public void setVideoOrientation(int orientation) {
        this.videoOrientation = orientation;
    }

    public void openCamera(int cameraID) {
        mCameraID = cameraID;
        if (!isCameraOpened && mGLCameraView != null && mGLCameraView.surfaceTexture != null) {
            camera2Manager.openCamera(mCameraID, mGLCameraView.surfaceTexture, videoWidth, videoHeight);
        }
    }

    public void closeCamera() {
        if (isCameraOpened && camera2Manager != null) {
            camera2Manager.closeCamera();
            isCameraOpened = false;
        }
    }

    public void switchCamera() {
        if (camera2Manager != null) {
            closeCamera();
            mCameraID = mCameraID == NMC_CAMERA_FRONT ? NMC_CAMERA_BACK : NMC_CAMERA_FRONT;
            openCamera(mCameraID);
        }
    }

    public void setZoomRatio(float ratio) {
        if (camera2Manager != null) {
            camera2Manager.setZoomRatio(ratio);
        }
    }
    public void setTorchEnable(boolean enable) {
        if (camera2Manager != null) {
            camera2Manager.enableTorch(enable);
        }
    }

    public void startFocusAndMeteringCenter() {
        if (camera2Manager != null) {
            camera2Manager.startFocusAndMeteringCenter();
        }
    }

    public void startFocusAndMetering(float w, float h, float x, float y, int mod) {
        if (camera2Manager != null) {
            camera2Manager.startFocusAndMetering(x, y, w, h, mod);
        }
    }

    private void onEvent(int event, String msg) {
//        Log.d(TAG, "on Event: " + event + " Message:" + msg);
        if (this.onNodePublisherEventListener != null) {
            this.onNodePublisherEventListener.onEventCallback(this, event, msg);
        }
    }

    private native long jniInit(Context context, String license);

    private native void jniFree();

    public native void setLogLevel(int logLevel);

    public native void setHWAccelEnable(boolean enable);

    public native void setDenoiseEnable(boolean enable);

    public native void setCameraFrontMirror(boolean mirror);

    public native void setAudioCodecParam(int codec, int profile, int sampleRate, int channels, int bitrate);

    public native void setVideoCodecParam(int codec, int profile, int width, int height, int fps, int bitrate);

    public native void setKeyFrameInterval(int keyFrameInterval);

    public native void setCryptoKey(String cryptoKey);

    public native void setVolume(float volume);

    public native void setEffectParameter(String parameter, float value);

    public native void setEffectStyle(int style);

    public native void setFlvIdExt(boolean idExt);

    public native int addOutput(String url);

    public native int removeOutputs();

    public native int start(String url);

    public native int stop();

    private native int GPUImageCreate();

    private native int GPUImageChange(int sw, int sh, int cw, int ch, int vo, int so, boolean f);

    private native int GPUImageDraw(float[] mtx, int len);

    private native int GPUImageDestroy();

    private class GLCameraView extends GLSurfaceView implements GLSurfaceView.Renderer {
        public SurfaceTexture surfaceTexture;
        private final float[] transformMatrix = new float[16];

        protected GLCameraView(Context context) {
            super(context);
            setEGLContextClientVersion(2);
            setRenderer(this);
            setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        }

        @Override
        public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
            Log.d(TAG, "on Surface Created");
            int textureId = GPUImageCreate();
            surfaceTexture = new SurfaceTexture(textureId);
            surfaceTexture.setOnFrameAvailableListener(surfaceTexture -> requestRender());
            isScreenCreated = true;
            if (!isCameraOpened && mCameraID >= 0) {
                camera2Manager.openCamera(mCameraID, surfaceTexture, videoWidth, videoHeight);
            }
        }

        @Override
        public void onSurfaceChanged(GL10 gl10, int w, int h) {
            Log.d(TAG, "on Surface Changed " + w + "x" + h);
            surfaceWidth = w;
            surfaceHeight = h;
            boolean isFrontCamera = mCameraID == NMC_CAMERA_FRONT;
            int sensorOrientation = camera2Manager.getSensorOrientation();
            GPUImageChange(surfaceWidth, surfaceHeight, cameraWidth, cameraHeight, videoOrientation, sensorOrientation, isFrontCamera);
        }

        @Override
        public void onDrawFrame(GL10 gl10) {
//            Log.d(TAG, "on Draw Frame");
            surfaceTexture.updateTexImage();
            surfaceTexture.getTransformMatrix(transformMatrix);
            GPUImageDraw(transformMatrix, transformMatrix.length);
        }

        @Override
        protected void onDetachedFromWindow() {
            Log.d(TAG, "on Detached From Window");
            super.onDetachedFromWindow();
            queueEvent(NodePublisher.this::GPUImageDestroy);
            isScreenCreated = false;
        }
    }

    public interface OnNodePublisherEventListener {
        void onEventCallback(NodePublisher publisher, int event, String msg);
    }
}