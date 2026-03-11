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
import android.view.Surface;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.camera.camera2.interop.Camera2CameraInfo;
import androidx.camera.camera2.interop.ExperimentalCamera2Interop;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.Preview;
import androidx.camera.core.SurfaceOrientedMeteringPointFactory;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;


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
    private int mCameraID = -1;
    private Camera mCamera;
    private Context ctx;
    private long id;
    private int videoOrientation = 0;
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
        if (isCameraOpened) {
            return;
        }
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(ctx);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                CameraSelector cameraSelector = mCameraID == NMC_CAMERA_FRONT ? CameraSelector.DEFAULT_FRONT_CAMERA : CameraSelector.DEFAULT_BACK_CAMERA;
                Preview.SurfaceProvider provider = this.mGLCameraView.getSurfaceProvider();
                if (provider == null) {
                    return;
                }
                int rotation = Surface.ROTATION_0;
                if (videoOrientation == VIDEO_ORIENTATION_LANDSCAPE_RIGHT) {
                    rotation = Surface.ROTATION_90;
                } else if (videoOrientation == VIDEO_ORIENTATION_LANDSCAPE_LEFT) {
                    rotation = Surface.ROTATION_270;
                }
                Preview preview = new Preview.Builder()
                        .setTargetResolution(new Size(videoWidth, videoHeight))
                        .setTargetRotation(rotation)
                        .build();
                preview.setSurfaceProvider(provider);
                mCamera = cameraProvider.bindToLifecycle((LifecycleOwner) this.ctx, cameraSelector, preview);
                isCameraOpened = true;
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this.ctx));
    }

    public void closeCamera() {
        isCameraOpened = false;
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(ctx);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                cameraProvider.unbindAll();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this.ctx));
    }

    public void switchCamera() {
        mCameraID = mCameraID == NMC_CAMERA_FRONT ? NMC_CAMERA_BACK : NMC_CAMERA_FRONT;
        closeCamera();
        openCamera(mCameraID);
    }

    public void setZoomRatio(float ratio) {
        if (mCamera != null) {
            mCamera.getCameraControl().setLinearZoom(ratio);
        }
    }

    public void setTorchEnable(boolean enable) {
        if (mCamera != null) {
            mCamera.getCameraControl().enableTorch(enable);
        }
    }

    public void startFocusAndMeteringCenter() {
        startFocusAndMetering(1f, 1f, .5f, .5f, FocusMeteringAction.FLAG_AF | FocusMeteringAction.FLAG_AE | FocusMeteringAction.FLAG_AWB);
    }

    public void startFocusAndMetering(float w, float h, float x, float y, int mod) {
        if (mCamera != null) {
            MeteringPoint point = new SurfaceOrientedMeteringPointFactory(w, h).createPoint(x, y);
            FocusMeteringAction action = new FocusMeteringAction.Builder(point, mod)
                    .setAutoCancelDuration(2, TimeUnit.SECONDS)
                    .build();
            mCamera.getCameraControl().startFocusAndMetering(action);
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

    private void onViewChange() {
        if (this.cameraWidth == 0 || this.cameraHeight == 0 || this.surfaceWidth == 0 || this.surfaceHeight == 0) {
            return;
        }
        int orientation;
        switch (videoOrientation) {
            case VIDEO_ORIENTATION_LANDSCAPE_LEFT:
                orientation = Surface.ROTATION_270;
                break;
            case VIDEO_ORIENTATION_LANDSCAPE_RIGHT:
                orientation = Surface.ROTATION_90;
                break;
            default:
                orientation = Surface.ROTATION_0;
                break;
        }
        int videoRotationDegrees = mCamera.getCameraInfo().getSensorRotationDegrees(orientation);
        GPUImageChange(this.surfaceWidth, this.surfaceHeight, this.cameraWidth, this.cameraHeight, videoOrientation, videoRotationDegrees, this.mCameraID == NMC_CAMERA_FRONT);
    }

    private class GLCameraView extends GLSurfaceView implements GLSurfaceView.Renderer {
        public SurfaceTexture surfaceTexture;
        private final float[] transformMatrix = new float[16];

        protected GLCameraView(Context context) {
            super(context);
            setEGLContextClientVersion(2);
            setRenderer(this);
            setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        }

        private Preview.SurfaceProvider getSurfaceProvider() {
            if (surfaceTexture == null) {
                return null;
            }
            return request -> {
                Size resolution = request.getResolution();
                surfaceTexture.setDefaultBufferSize(resolution.getWidth(), resolution.getHeight());
                request.provideSurface(new Surface(surfaceTexture), ContextCompat.getMainExecutor(ctx), result -> {
                    result.getSurface().release();
                });
                this.queueEvent(() -> {
                    cameraWidth = resolution.getWidth();
                    cameraHeight = resolution.getHeight();
                    onViewChange();
                });
            };
        }

        @Override
        public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
            Log.d(TAG, "on Surface Created");
            int textureId = GPUImageCreate();
            surfaceTexture = new SurfaceTexture(textureId);
            surfaceTexture.setOnFrameAvailableListener(surfaceTexture -> requestRender());
            isScreenCreated = true;
            openCamera(mCameraID);
        }

        @Override
        public void onSurfaceChanged(GL10 gl10, int w, int h) {
            Log.d(TAG, "on Surface Changed " + w + "x" + h);
            surfaceWidth = w;
            surfaceHeight = h;
            onViewChange();
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