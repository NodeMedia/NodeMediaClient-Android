/**
 * ©2024 NodeMedia
 * <p>
 * Copyright © 2015 - 2024 NodeMedia.All Rights Reserved.
 */

package cn.nodemedia;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.MeteringRectangle;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import java.util.Arrays;

public class Camera2Manager {
    private static final String TAG = "Camera2Manager";

    private CameraManager cameraManager;
    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private CaptureRequest.Builder previewRequestBuilder;
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;

    private String cameraId;
    private SurfaceTexture surfaceTexture;
    private Size previewSize;
    private CameraCharacteristics cameraCharacteristics;
    private float currentZoomRatio = 1.0f;
    private boolean isTorchOn = false;

    public interface CameraStateListener {
        void onCameraOpened();
        void onCameraClosed();
        void onCameraError(String error);
    }

    private CameraStateListener stateListener;
    private int sensorOrientation = 0;

    public Camera2Manager(Context context) {
        this.cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
    }

    public void setCameraStateListener(CameraStateListener listener) {
        this.stateListener = listener;
    }

    @SuppressLint("MissingPermission")
    public void openCamera(int facingId, SurfaceTexture surfaceTexture, int width, int height) {
        this.surfaceTexture = surfaceTexture;

        startBackgroundThread();

        try {
            String cameraId = getCameraId(facingId);
            if (cameraId == null) {
                if (stateListener != null) {
                    stateListener.onCameraError("找不到摄像头");
                }
                return;
            }

            cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

            // 获取摄像头传感器方向
            sensorOrientation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            Log.d(TAG, "Camera sensor orientation: " + sensorOrientation);

            if (map != null) {
                Size[] outputSizes = map.getOutputSizes(SurfaceTexture.class);
                previewSize = chooseOptimalSize(outputSizes, width, height);
                surfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            }

            cameraManager.openCamera(cameraId, stateCallback, backgroundHandler);

        } catch (CameraAccessException e) {
            Log.e(TAG, "无法访问摄像头: " + e.getMessage());
            if (stateListener != null) {
                stateListener.onCameraError("无法访问摄像头: " + e.getMessage());
            }
        }
    }

    public void closeCamera() {
        try {
            if (captureSession != null) {
                captureSession.close();
                captureSession = null;
            }
            if (cameraDevice != null) {
                cameraDevice.close();
                cameraDevice = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "关闭摄像头时出错: " + e.getMessage());
        }

        stopBackgroundThread();
    }

    public boolean isFrontCamera() {
        return true;
    }

    public Size getPreviewSize() {
        return previewSize;
    }

    public int getSensorOrientation() {
        return sensorOrientation;
    }

    private String getCameraId(int facingId) {
        try {
            String[] cameraIds = cameraManager.getCameraIdList();
            for (String id : cameraIds) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == facingId) {
                    return id;
                }
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "获取摄像头ID失败: " + e.getMessage());
        }
        return null;
    }

    private Size chooseOptimalSize(Size[] choices, int width, int height) {
        for (Size size : choices) {
            if (size.getWidth() == width && size.getHeight() == height) {
                return size;
            }
        }

        Size bestSize = choices[0];
        int bestArea = bestSize.getWidth() * bestSize.getHeight();
        int targetArea = width * height;

        for (Size size : choices) {
            int area = size.getWidth() * size.getHeight();
            if (Math.abs(area - targetArea) < Math.abs(bestArea - targetArea)) {
                bestSize = size;
                bestArea = area;
            }
        }

        return bestSize;
    }

    private void startBackgroundThread() {
        if (backgroundThread == null) {
            backgroundThread = new HandlerThread("CameraBackground");
            backgroundThread.start();
            backgroundHandler = new Handler(backgroundThread.getLooper());
        }
    }

    private void stopBackgroundThread() {
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
            try {
                backgroundThread.join();
                backgroundThread = null;
                backgroundHandler = null;
            } catch (InterruptedException e) {
                Log.e(TAG, "停止后台线程失败: " + e.getMessage());
            }
        }
    }

    private void createCameraPreviewSession() {
        try {
            Surface surface = new Surface(surfaceTexture);

            previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewRequestBuilder.addTarget(surface);

            cameraDevice.createCaptureSession(Arrays.asList(surface), sessionStateCallback, backgroundHandler);

        } catch (CameraAccessException e) {
            Log.e(TAG, "创建预览会话失败: " + e.getMessage());
            if (stateListener != null) {
                stateListener.onCameraError("创建预览会话失败: " + e.getMessage());
            }
        }
    }

    public void startPreview() {
        if (cameraDevice == null || captureSession == null) {
            return;
        }

        try {
            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);

            captureSession.setRepeatingRequest(previewRequestBuilder.build(), null, backgroundHandler);

        } catch (CameraAccessException e) {
            Log.e(TAG, "开始预览失败: " + e.getMessage());
        }
    }

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@SuppressLint("InvalidAccessToGuardedMember") CameraDevice camera) {
            Log.d(TAG, "摄像头已打开");
            cameraDevice = camera;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@SuppressLint("InvalidAccessToGuardedMember") CameraDevice camera) {
            Log.d(TAG, "摄像头已断开连接");
            camera.close();
            cameraDevice = null;
            if (stateListener != null) {
                stateListener.onCameraClosed();
            }
        }

        @Override
        public void onError(@SuppressLint("InvalidAccessToGuardedMember") CameraDevice camera, int error) {
            Log.e(TAG, "摄像头错误: " + error);
            camera.close();
            cameraDevice = null;
            if (stateListener != null) {
                stateListener.onCameraError("摄像头错误: " + error);
            }
        }
    };

    private final CameraCaptureSession.StateCallback sessionStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(@SuppressLint("InvalidAccessToGuardedMember") CameraCaptureSession session) {
            Log.d(TAG, "摄像头会话已配置");
            captureSession = session;
            startPreview();
            if (stateListener != null) {
                stateListener.onCameraOpened();
            }
        }

        @Override
        public void onConfigureFailed(@SuppressLint("InvalidAccessToGuardedMember") CameraCaptureSession session) {
            Log.e(TAG, "摄像头会话配置失败");
            if (stateListener != null) {
                stateListener.onCameraError("摄像头会话配置失败");
            }
        }
    };

    /**
     * 获取最小变焦比例
     */
    public float getMinZoomRatio() {
        if (cameraCharacteristics == null) {
            return 1.0f;
        }
        Float value = cameraCharacteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);
        if (value == null || value < 1.0f) {
            return 1.0f;
        }
        return 1.0f;
    }

    /**
     * 获取最大变焦比例
     */
    public float getMaxZoomRatio() {
        if (cameraCharacteristics == null) {
            return 1.0f;
        }
        Float value = cameraCharacteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);
        if (value == null || value < 1.0f) {
            return 1.0f;
        }
        return value;
    }

    /**
     * 获取当前变焦比例
     */
    public float getZoomRatio() {
        return currentZoomRatio;
    }

    /**
     * 设置变焦比例
     */
    public void setZoomRatio(float ratio) {
        if (cameraCharacteristics == null || previewRequestBuilder == null) {
            return;
        }

        float maxZoom = getMaxZoomRatio();
        float minZoom = getMinZoomRatio();

        if (ratio < minZoom) {
            ratio = minZoom;
        } else if (ratio > maxZoom) {
            ratio = maxZoom;
        }

        currentZoomRatio = ratio;

        try {
            // 计算变焦区域
            Rect sensorArray = cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
            if (sensorArray != null) {
                int cropW = (int) (sensorArray.width() / currentZoomRatio);
                int cropH = (int) (sensorArray.height() / currentZoomRatio);
                int cropX = (sensorArray.width() - cropW) / 2;
                int cropY = (sensorArray.height() - cropH) / 2;

                Rect zoomRect = new Rect(cropX, cropY, cropX + cropW, cropY + cropH);
                previewRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoomRect);

                // 重新应用预览请求
                if (captureSession != null) {
                    captureSession.setRepeatingRequest(previewRequestBuilder.build(), null, backgroundHandler);
                }
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "设置变焦失败: " + e.getMessage());
        }
    }

    /**
     * 检查是否支持闪光灯
     */
    public boolean isFlashAvailable() {
        if (cameraCharacteristics == null) {
            return false;
        }
        Boolean flashAvailable = cameraCharacteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
        return flashAvailable != null && flashAvailable;
    }

    /**
     * 开启/关闭闪光灯
     */
    public void enableTorch(boolean enable) {
        if (cameraCharacteristics == null || previewRequestBuilder == null) {
            Log.w(TAG, "摄像头未就绪，无法设置闪光灯");
            return;
        }

        // 检查是否支持闪光灯
        if (!isFlashAvailable()) {
            Log.w(TAG, "设备不支持闪光灯");
            return;
        }

        isTorchOn = enable;
        Log.d(TAG, "设置闪光灯: " + (enable ? "开启" : "关闭"));

        try {
            // 设置闪光灯模式
            if (enable) {
                previewRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH);
            } else {
                previewRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
            }

            // 重新应用预览请求
            if (captureSession != null) {
                captureSession.setRepeatingRequest(previewRequestBuilder.build(), null, backgroundHandler);
                Log.d(TAG, "闪光灯设置已应用");
            } else {
                Log.w(TAG, "摄像头会话未建立，无法应用闪光灯设置");
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "设置闪光灯失败: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "设置闪光灯时发生未知错误: " + e.getMessage());
        }
    }

    /**
     * 开始中心对焦和测光
     */
    public void startFocusAndMeteringCenter() {
        if (previewSize == null) {
            return;
        }

        // 使用预览区域的中心点
        float centerX = previewSize.getWidth() / 2.0f;
        float centerY = previewSize.getHeight() / 2.0f;

        startFocusAndMetering(centerX, centerY, 100.0f, 100.0f,
            NodePublisher.FLAG_AF | NodePublisher.FLAG_AE | NodePublisher.FLAG_AWB);
    }

    /**
     * 开始对焦和测光
     * @param x 对焦点X坐标
     * @param y 对焦点Y坐标
     * @param width 对焦区域宽度
     * @param height 对焦区域高度
     * @param mode 对焦模式标志 (FLAG_AF, FLAG_AE, FLAG_AWB)
     */
    public void startFocusAndMetering(float x, float y, float width, float height, int mode) {
        if (cameraCharacteristics == null || previewRequestBuilder == null || previewSize == null) {
            return;
        }

        try {
            // 检查是否支持自动对焦
            int[] afModes = cameraCharacteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
            if (afModes == null) {
                return;
            }

            boolean supportAF = false;
            for (int afMode : afModes) {
                if (afMode == CaptureRequest.CONTROL_AF_MODE_AUTO ||
                    afMode == CaptureRequest.CONTROL_AF_MODE_MACRO) {
                    supportAF = true;
                    break;
                }
            }

            // 创建测光区域
            Rect sensorArray = cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
            if (sensorArray == null) {
                return;
            }

            // 将坐标转换为传感器坐标
            int sensorX = (int) (x * sensorArray.width() / previewSize.getWidth());
            int sensorY = (int) (y * sensorArray.height() / previewSize.getHeight());
            int sensorWidth = (int) (width * sensorArray.width() / previewSize.getWidth());
            int sensorHeight = (int) (height * sensorArray.height() / previewSize.getHeight());

            // 确保测光区域在传感器范围内
            int left = Math.max(0, sensorX - sensorWidth / 2);
            int top = Math.max(0, sensorY - sensorHeight / 2);
            int right = Math.min(sensorArray.width(), sensorX + sensorWidth / 2);
            int bottom = Math.min(sensorArray.height(), sensorY + sensorHeight / 2);

            if (left >= right || top >= bottom) {
                return;
            }

            Rect meteringRect = new Rect(left, top, right, bottom);
            MeteringRectangle meteringRectangle = new MeteringRectangle(meteringRect, MeteringRectangle.METERING_WEIGHT_MAX);

            // 设置自动对焦
            if ((mode & NodePublisher.FLAG_AF) != 0 && supportAF) {
                previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO);
                previewRequestBuilder.set(CaptureRequest.CONTROL_AF_REGIONS, new MeteringRectangle[]{meteringRectangle});
                previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START);
            }

            // 设置自动曝光
            if ((mode & NodePublisher.FLAG_AE) != 0) {
                previewRequestBuilder.set(CaptureRequest.CONTROL_AE_REGIONS, new MeteringRectangle[]{meteringRectangle});
                previewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            }

            // 设置自动白平衡
            if ((mode & NodePublisher.FLAG_AWB) != 0) {
                int[] awbModes = cameraCharacteristics.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES);
                if (awbModes != null) {
                    for (int awbMode : awbModes) {
                        if (awbMode == CaptureRequest.CONTROL_AWB_MODE_AUTO) {
                            previewRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_AUTO);
                            break;
                        }
                    }
                }
            }

            // 应用设置
            if (captureSession != null) {
                captureSession.capture(previewRequestBuilder.build(), null, backgroundHandler);
                // 恢复连续预览
                previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_IDLE);
                previewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER, CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_IDLE);
                captureSession.setRepeatingRequest(previewRequestBuilder.build(), null, backgroundHandler);
            }

        } catch (CameraAccessException e) {
            Log.e(TAG, "设置对焦和测光失败: " + e.getMessage());
        }
    }
}