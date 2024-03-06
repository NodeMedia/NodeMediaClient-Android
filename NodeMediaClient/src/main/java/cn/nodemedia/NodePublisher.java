/**
 * ©2024 NodeMedia
 * <p>
 * Copyright © 2015 - 2024 NodeMedia.All Rights Reserved.
 */

package cn.nodemedia;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLSurfaceView;
import android.util.Size;
import android.view.Gravity;
import android.view.Surface;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.camera.core.Camera;
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

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class NodePublisher {
    static {
        System.loadLibrary("NodeMediaClient");
    }

    public static final int LOG_LEVEL_ERROR = 0;
    public static final int LOG_LEVEL_INFO = 1;
    public static final int LOG_LEVEL_DEBUG = 2;

    public static final int NMC_CODEC_ID_H264 = 27;
    public static final int NMC_CODEC_ID_H265 = 173;
    public static final int NMC_CODEC_ID_AAC = 86018;

    public static final int NMC_PROFILE_AUTO = 0;
    public static final int NMC_PROFILE_H264_BASELINE = 66;
    public static final int NMC_PROFILE_H264_MAIN = 77;
    public static final int NMC_PROFILE_H264_HIGH = 100;
    public static final int NMC_PROFILE_H265_MAIN = 1;
    public static final int NMC_PROFILE_AAC_LC = 1;
    public static final int NMC_PROFILE_AAC_HE = 4;
    public static final int NMC_PROFILE_AAC_HE_V2 = 28;
    public static final int NMC_PROFILE_AAC_LD = 22;
    public static final int NMC_PROFILE_AAC_ELD = 38;

    public static final int VIDEO_RC_CRF = 0;
    public static final int VIDEO_RC_ABR = 1;
    public static final int VIDEO_RC_CBR = 2;
    public static final int VIDEO_RC_VBV = 3;

    public static final int VIDEO_ORIENTATION_PORTRAIT = 0;
    public static final int VIDEO_ORIENTATION_LANDSCAPE_RIGHT = 1;
    public static final int VIDEO_ORIENTATION_LANDSCAPE_LEFT = 3;

    public static final int EffectorTextureTypeT2D = 0;
    public static final int EffectorTextureTypeEOS = 1;

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
    private OnNodePublisherEffectorListener onNodePublisherEffectorListener;
    private GLCameraView glpv;
    private Camera mCamera;
    private Context ctx;
    private long id;
    private int fpsCount;
    private long fpsTime;
    private boolean isOpenFrontCamera = false;
    private int videoOrientation = Surface.ROTATION_0;
    private int videoWidth = 720;
    private int videoHeight = 1280;
    private int cameraWidth = 0;
    private int cameraHeight = 0;
    private int surfaceWidth = 0;
    private int surfaceHeight = 0;

    private final FrameLayout.LayoutParams LP = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
            Gravity.CENTER);

    public NodePublisher(@NonNull Context context, @NonNull String license) {
        ctx = context;
        id = jniInit(context, license);
    }

    public void setOnNodePublisherEventListener(OnNodePublisherEventListener onNodePublisherEventListener) {
        this.onNodePublisherEventListener = onNodePublisherEventListener;
    }

    public void setOnNodePublisherEffectorListener(OnNodePublisherEffectorListener onNodePublisherEffectorListener) {
        this.onNodePublisherEffectorListener = onNodePublisherEffectorListener;
    }

    public void attachView(@NonNull ViewGroup vg) {
        if (this.glpv == null) {
            this.glpv = new GLCameraView(this.ctx);
            this.glpv.setLayoutParams(LP);
            this.glpv.setKeepScreenOn(true);
            vg.addView(this.glpv);
        }
    }

    public void detachView() {
        if (this.glpv != null) {
            this.glpv.setKeepScreenOn(false);
            this.glpv = null;
            closeCamera();
            GPUImageDestroy();
        }
    }

    public void setVideoOrientation(int orientation) {
        this.videoOrientation = orientation;
    }

    public void openCamera(boolean frontCamera) {
        this.isOpenFrontCamera = frontCamera;
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(ctx);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindImageAnalysis(cameraProvider, this.isOpenFrontCamera);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this.ctx));
    }

    public void closeCamera() {
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
        this.isOpenFrontCamera = !this.isOpenFrontCamera;
        closeCamera();
        openCamera(this.isOpenFrontCamera);
    }

    public Camera getCamera() {
        return mCamera;
    }

    public float getMinZoomRatio() {
        if (mCamera != null && mCamera.getCameraInfo().getZoomState().getValue() != null) {
            return mCamera.getCameraInfo().getZoomState().getValue().getMinZoomRatio();
        }
        return 1.0f;
    }

    public float getMaxZoomRatio() {
        if (mCamera != null && mCamera.getCameraInfo().getZoomState().getValue() != null) {
            return mCamera.getCameraInfo().getZoomState().getValue().getMaxZoomRatio();
        }
        return 1.0f;
    }

    public float getZoomRatio() {
        if (mCamera != null && mCamera.getCameraInfo().getZoomState().getValue() != null) {
            return mCamera.getCameraInfo().getZoomState().getValue().getZoomRatio();
        }
        return 1.0f;
    }

    public float getLinearZoom() {
        if (mCamera != null && mCamera.getCameraInfo().getZoomState().getValue() != null) {
            return mCamera.getCameraInfo().getZoomState().getValue().getLinearZoom();
        }
        return 0.0f;
    }

    public void setRoomRatio(float ratio) {
        if (mCamera != null) {
            mCamera.getCameraControl().setZoomRatio(ratio);
        }
    }

    public void setLinearZoom(float zoom) {
        if (mCamera != null) {
            mCamera.getCameraControl().setLinearZoom(zoom);
        }
    }

    public void enableTorch(boolean enable) {
        if (mCamera != null) {
            mCamera.getCameraControl().enableTorch(enable);
        }
    }

    public void startFocusAndMeteringCenter() {
        startFocusAndMetering(1f, 1f, .5f, .5f, FocusMeteringAction.FLAG_AF | FocusMeteringAction.FLAG_AE | FocusMeteringAction.FLAG_AWB);
    }

    public void startFocusAndMetering(float w, float h, float x, float y, int mod) {
        if (mCamera == null || glpv == null) {
            return;
        }
        MeteringPoint point = new SurfaceOrientedMeteringPointFactory(w, h).createPoint(x, y);
        FocusMeteringAction action = new FocusMeteringAction.Builder(point, mod)
                .setAutoCancelDuration(2, TimeUnit.SECONDS)
                .build();
        mCamera.getCameraControl().startFocusAndMetering(action);
    }

    private void bindImageAnalysis(@NonNull ProcessCameraProvider cameraProvider, boolean front) {
        CameraSelector cameraSelector = front ? CameraSelector.DEFAULT_FRONT_CAMERA : CameraSelector.DEFAULT_BACK_CAMERA;
        Preview.SurfaceProvider provider = this.glpv.getSurfaceProvider();
        if(provider == null) {
            return;
        }
        Preview preview = new Preview.Builder()
                .setTargetResolution(new Size(videoWidth, videoHeight))
                .setTargetRotation(videoOrientation)
                .build();
        preview.setSurfaceProvider(provider);
        mCamera = cameraProvider.bindToLifecycle((LifecycleOwner) this.ctx, cameraSelector, preview);
    }

    private void onEvent(int event, String msg) {
//        Log.d(TAG, "on Event: " + event + " Message:" + msg);
        if (this.onNodePublisherEventListener != null) {
            this.onNodePublisherEventListener.onEventCallback(this, event, msg);
        }
    }

    private void onCreateEffector() {
        if (this.onNodePublisherEffectorListener != null) {
            this.onNodePublisherEffectorListener.onCreateEffector(this.ctx);
        }
    }

    private int onProcessEffector(int textureID) {
        if (this.onNodePublisherEffectorListener != null) {
            textureID = this.onNodePublisherEffectorListener.onProcessEffector(textureID, this.videoWidth, this.videoHeight);
        }
        return textureID;
    }

    private void onReleaseEffector() {
        if (this.onNodePublisherEffectorListener != null) {
            this.onNodePublisherEffectorListener.onReleaseEffector();
        }
    }

    protected void finalize() {
        jniFree();
    }

    private native long jniInit(Context context, String license);

    private native void jniFree();

    public native void setLogLevel(int logLevel);

    public native void setHWAccelEnable(boolean enable);

    public native void setDenoiseEnable(boolean enable);

    public native void setVideoFrontMirror(boolean mirror);

    public native void setCameraFrontMirror(boolean mirror);

    public native void setAudioCodecParam(int codec, int profile, int sampleRate, int channels, int bitrate);

    public native void setVideoCodecParam(int codec, int profile, int width, int height, int fps, int bitrate);

    public native void setVideoRateControl(int rc);

    public native void setKeyFrameInterval(int keyFrameInterval);

    /**
     * 设置视频解密密码
     *
     * @param cryptoKey 16字节密码
     */
    public native void setCryptoKey(@NonNull String cryptoKey);

    /**
     * 设置是否使用enhanced-rtmp 标准推流
     *
     * @param enhancedRtmp
     */
    public native void setEnhancedRtmp(boolean enhancedRtmp);

    /**
     * 设置音量
     * 0.0 最小值 麦克风静音
     * 1.0 默认值 原始音量
     * 2.0 最大值 增益音量
     *
     * @param volume 0.0 ~~ 2.0
     */
    public native void setVolume(float volume);

    public native int addOutput(@NonNull String url);

    public native int removeOutputs();

    public native int start(@NonNull String url);

    public native int stop();

    public native void setEffectorTextureType(int type);

    private native int GPUImageCreate(int textureID);

    private native int GPUImageChange(int sw, int sh, int cw, int ch, int so, int co, boolean f);

    private native int GPUImageDraw(int textureID, float[] mtx, int len);

    private native int GPUImageDestroy();

    private native int GPUImageGenOESTextureID();

    private void onViewChange() {
        if (this.cameraWidth == 0 || this.cameraHeight == 0 || this.surfaceWidth == 0 || this.surfaceHeight == 0) {
            return;
        }
        WindowManager wm = (WindowManager) this.ctx.getSystemService(Context.WINDOW_SERVICE);
        int surfaceRotation = wm.getDefaultDisplay().getRotation();
        int sensorRotationDegrees = mCamera.getCameraInfo().getSensorRotationDegrees(this.videoOrientation);
        GPUImageChange(this.surfaceWidth, this.surfaceHeight, this.cameraWidth, this.cameraHeight, surfaceRotation, sensorRotationDegrees, this.isOpenFrontCamera);
    }

    private class GLCameraView extends GLSurfaceView implements GLSurfaceView.Renderer {
        private static final String TAG = "NodeMedia.GLCameraView";

        private SurfaceTexture surfaceTexture;
        private int textureId = -1;
        private Context context;
        private float transformMatrix[] = new float[16];

        protected GLCameraView(Context context) {
            super(context);
            this.context = context;
            setEGLContextClientVersion(2);
            setRenderer(this);
            setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        }

        private Preview.SurfaceProvider getSurfaceProvider() {
            if(surfaceTexture == null) {
                return null;
            }
            return request -> {
                Size resolution = request.getResolution();
                surfaceTexture.setDefaultBufferSize(resolution.getWidth(), resolution.getHeight());
                request.provideSurface(new Surface(surfaceTexture), ContextCompat.getMainExecutor(this.context), result -> {
                    result.getSurface().release();
                });
                this.queueEvent(() -> {
                    NodePublisher.this.cameraWidth = resolution.getWidth();
                    NodePublisher.this.cameraHeight = resolution.getHeight();
                    NodePublisher.this.onViewChange();
                });
            };
        }

        @Override
        public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
            textureId = GPUImageGenOESTextureID();
            surfaceTexture = new SurfaceTexture(textureId);
            surfaceTexture.setOnFrameAvailableListener(surfaceTexture -> requestRender());
            NodePublisher.this.GPUImageCreate(textureId);
        }

        @Override
        public void onSurfaceChanged(GL10 gl10, int w, int h) {
            NodePublisher.this.surfaceWidth = w;
            NodePublisher.this.surfaceHeight = h;
            NodePublisher.this.onViewChange();
        }

        @Override
        public void onDrawFrame(GL10 gl10) {
            surfaceTexture.updateTexImage();
            surfaceTexture.getTransformMatrix(transformMatrix);
            NodePublisher.this.GPUImageDraw(textureId, transformMatrix, transformMatrix.length);
        }
    }

    public interface OnNodePublisherEffectorListener {

        void onCreateEffector(Context context);

        int onProcessEffector(int textureID, int width, int height);

        void onReleaseEffector();

    }

    public interface OnNodePublisherEventListener {
        void onEventCallback(NodePublisher publisher, int event, String msg);
    }
}