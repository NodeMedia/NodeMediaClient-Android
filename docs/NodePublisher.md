# NodePublisher API 文档

> NodeMedia Android 客户端核心推流器 API 参考文档  
> © 2024 NodeMedia.cn

---

## 概述

`NodePublisher` 是 NodeMediaClient 的核心推流器类，负责摄像头采集、音视频编码、推流发布等功能。底层通过 JNI 调用原生 C++ 库 `NodeMediaClient` 实现。内置 OpenGL 渲染管线，支持实时美颜滤镜、对焦测光等高级功能。

**包名：** `cn.nodemedia`  
**依赖：** AndroidX CameraX、OpenGL ES 2.0、Guava ListenableFuture

---

## 构造函数

### `NodePublisher(Context context, String license)`

创建推流器实例。

| 参数 | 类型 | 说明 |
|------|------|------|
| `context` | `Context` | Android 上下文（需实现 `LifecycleOwner`） |
| `license` | `String` | 授权码 |

---

## 常量

### 日志级别

| 常量 | 值 | 说明 |
|------|----|------|
| `LOG_LEVEL_ERROR` | `0` | 仅输出错误日志 |
| `LOG_LEVEL_INFO` | `1` | 输出信息日志 |
| `LOG_LEVEL_DEBUG` | `2` | 输出调试日志 |

### 摄像头

| 常量 | 值 | 说明 |
|------|----|------|
| `NMC_CAMERA_FRONT` | `0` | 前置摄像头 |
| `NMC_CAMERA_BACK` | `1` | 后置摄像头 |

### 编码器 ID

| 常量 | 值 | 说明 |
|------|----|------|
| `NMC_CODEC_ID_H264` | `27` | H.264 / AVC 视频编码 |
| `NMC_CODEC_ID_H265` | `173` | H.265 / HEVC 视频编码 |
| `NMC_CODEC_ID_AAC` | `86018` | AAC 音频编码 |
| `NMC_CODEC_ID_OPUS` | `86076` | Opus 音频编码 |
| `NMC_CODEC_ID_PCMA` | `65543` | PCMA (G.711 A-law) 音频编码 |
| `NMC_CODEC_ID_PCMU` | `65542` | PCMU (G.711 μ-law) 音频编码 |

### 编码 Profile

| 常量 | 值 | 说明 |
|------|----|------|
| `NMC_PROFILE_AUTO` | `0` | 自动选择 |
| `NMC_PROFILE_H264_BASELINE` | `66` | H.264 Baseline |
| `NMC_PROFILE_H264_MAIN` | `77` | H.264 Main |
| `NMC_PROFILE_H264_HIGH` | `100` | H.264 High |
| `NMC_PROFILE_H265_MAIN` | `1` | H.265 Main |
| `NMC_PROFILE_AAC_LC` | `1` | AAC-LC（低复杂度） |
| `NMC_PROFILE_AAC_HE` | `4` | AAC HE（高效率 v1） |
| `NMC_PROFILE_AAC_HE_V2` | `28` | AAC HE v2（高效率 v2） |

### 视频方向

| 常量 | 值 | 说明 |
|------|----|------|
| `VIDEO_ORIENTATION_PORTRAIT` | `0` | 竖屏（默认） |
| `VIDEO_ORIENTATION_LANDSCAPE_RIGHT` | `90` | 横屏，顺时针旋转 90° |
| `VIDEO_ORIENTATION_LANDSCAPE_LEFT` | `270` | 横屏，顺时针旋转 270° |

### 特效参数键名

| 常量 | 值 | 说明 |
|------|----|------|
| `EFFECTOR_BRIGHTNESS` | `"brightness"` | 亮度 |
| `EFFECTOR_CONTRAST` | `"contrast"` | 对比度 |
| `EFFECTOR_SATURATION` | `"saturation"` | 饱和度 |
| `EFFECTOR_SHARPEN` | `"sharpen"` | 锐度 |
| `EFFECTOR_SMOOTHSKIN` | `"smoothskin"` | 磨皮 |
| `EFFECTOR_STYLE` | `"style"` | 滤镜风格 |

### 滤镜风格 ID

| 常量 | 值 | 说明 |
|------|----|------|
| `EFFECTOR_STYLE_ID_ORIGINAL` | `0` | 原图 |
| `EFFECTOR_STYLE_ID_ENHANCED` | `1` | 增强 |
| `EFFECTOR_STYLE_ID_FAIRSKIN` | `2` | 白皙 |
| `EFFECTOR_STYLE_ID_COOL` | `3` | 冷色 |
| `EFFECTOR_STYLE_ID_FILM` | `4` | 胶片 |
| `EFFECTOR_STYLE_ID_BOOST` | `5` | 明亮 |

### 对焦/测光标志位

| 常量 | 值 | 说明 |
|------|----|------|
| `FLAG_AF` | `1` | 自动对焦 |
| `FLAG_AE` | `1 << 1` | 自动曝光 |
| `FLAG_AWB` | `1 << 2` | 自动白平衡 |

---

## 视图管理

### `void attachView(ViewGroup vg)`

将推流预览画面附加到指定的视图容器中。

- 内部创建 `GLCameraView`（继承自 `GLSurfaceView`）并自动添加到 `vg`
- 尺寸为 `MATCH_PARENT`，居中显示
- 自动保持屏幕常亮
- 多次调用不会重复创建

| 参数 | 类型 | 说明 |
|------|------|------|
| `vg` | `ViewGroup` | 目标视图容器 |

### `void detachView()`

将推流预览画面从当前视图移除。

- 隐藏 `GLCameraView`
- 关闭屏幕常亮
- 释放 `GLCameraView` 引用

---

## 摄像头控制

### `void openCamera(int cameraID)`

打开指定摄像头。

| 参数 | 类型 | 说明 |
|------|------|------|
| `cameraID` | `int` | `NMC_CAMERA_FRONT` 或 `NMC_CAMERA_BACK` |

- 基于 CameraX `ProcessCameraProvider` 实现
- 使用 `attachView` 创建的 `GLCameraView` 作为 Preview SurfaceProvider
- 根据 `setVideoOrientation()` 设置的目标旋转角度
- 自动绑定到 `LifecycleOwner`
- 已打开时重复调用无效；未 `attachView` 时调用无效

### `void closeCamera()`

关闭当前打开的摄像头。

- 调用 `ProcessCameraProvider.unbindAll()`

### `void switchCamera()`

前后摄像头切换。

- 自动关闭当前摄像头，再打开另一个

### `void setVideoOrientation(int orientation)`

设置视频输出方向。

| 参数 | 类型 | 说明 |
|------|------|------|
| `orientation` | `int` | `VIDEO_ORIENTATION_PORTRAIT` / `LANDSCAPE_RIGHT` / `LANDSCAPE_LEFT` |

- 应在 `openCamera` 之前设置

### `void setZoomRatio(float ratio)`

设置摄像头变焦倍数（线性变焦）。

| 参数 | 类型 | 说明 |
|------|------|------|
| `ratio` | `float` | `0.0` ~ `1.0`，值越大放大倍数越大 |

- 基于 CameraX `CameraControl.setLinearZoom()`

### `void setTorchEnable(boolean enable)`

开启或关闭闪光灯（手电筒模式）。

| 参数 | 类型 | 说明 |
|------|------|------|
| `enable` | `boolean` | `true` 开启，`false` 关闭 |

---

## 对焦与测光

### `void startFocusAndMeteringCenter()`

在画面中心点触发自动对焦 + 自动曝光 + 自动白平衡。

- 等价于调用 `startFocusAndMetering(1f, 1f, 0.5f, 0.5f, FLAG_AF\|FLAG_AE\|FLAG_AWB)`

### `void startFocusAndMetering(float w, float h, float x, float y, int mod)`

在指定坐标点触发对焦和测光。

| 参数 | 类型 | 说明 |
|------|------|------|
| `w` | `float` | Surface 宽度（归一化坐标参考） |
| `h` | `float` | Surface 高度（归一化坐标参考） |
| `x` | `float` | 焦点 X 坐标（归一化 `0.0~1.0`） |
| `y` | `float` | 焦点 Y 坐标（归一化 `0.0~1.0`） |
| `mod` | `int` | 模式标志位组合：`FLAG_AF`、`FLAG_AE`、`FLAG_AWB` |

- 自动取消时间 2 秒

---

## 推流控制

### `int start(String url)`

开始推流。

| 参数 | 类型 | 说明 |
|------|------|------|
| `url` | `String` | 推流地址，如 `rtmp://example.com/live/stream` |

**返回：** `int` — 操作结果（0 成功，非 0 失败）。

### `int stop()`

停止推流。

**返回：** `int` — 操作结果。

---

## 多目标输出

### `int addOutput(String url)`

添加额外的输出地址（同时推送到多个服务器）。

| 参数 | 类型 | 说明 |
|------|------|------|
| `url` | `String` | 额外的推流地址 |

**返回：** `int` — 操作结果。

### `int removeOutputs()`

移除所有额外输出地址。

**返回：** `int` — 操作结果。

---

## 编码与采集配置

### `void setVideoCodecParam(int codec, int profile, int width, int height, int fps, int bitrate)`

设置视频编码参数。

| 参数 | 类型 | 说明 |
|------|------|------|
| `codec` | `int` | 编码器 ID（`NMC_CODEC_ID_H264` / `NMC_CODEC_ID_H265`） |
| `profile` | `int` | 编码 Profile（如 `NMC_PROFILE_H264_HIGH`） |
| `width` | `int` | 视频宽度（px） |
| `height` | `int` | 视频高度（px） |
| `fps` | `int` | 帧率 |
| `bitrate` | `int` | 码率（bps） |

### `void setAudioCodecParam(int codec, int profile, int sampleRate, int channels, int bitrate)`

设置音频编码参数。

| 参数 | 类型 | 说明 |
|------|------|------|
| `codec` | `int` | 编码器 ID（`NMC_CODEC_ID_AAC` / `OPUS` / `PCMA` / `PCMU`） |
| `profile` | `int` | 编码 Profile（如 `NMC_PROFILE_AAC_LC`） |
| `sampleRate` | `int` | 采样率（Hz） |
| `channels` | `int` | 声道数 |
| `bitrate` | `int` | 码率（bps） |

### `void setKeyFrameInterval(int keyFrameInterval)`

设置关键帧间隔（GOP 大小）。

| 参数 | 类型 | 说明 |
|------|------|------|
| `keyFrameInterval` | `int` | 关键帧间隔帧数 |

### `void setHWAccelEnable(boolean enable)`

设置是否开启硬件加速编码。

| 参数 | 类型 | 说明 |
|------|------|------|
| `enable` | `boolean` | `true` 开启硬件加速 |

### `void setLogLevel(int logLevel)`

设置日志输出等级。

| 参数 | 类型 | 说明 |
|------|------|------|
| `logLevel` | `int` | `LOG_LEVEL_ERROR` / `LOG_LEVEL_INFO` / `LOG_LEVEL_DEBUG` |

### `void setDenoiseEnable(boolean enable)`

设置是否开启音频降噪。

| 参数 | 类型 | 说明 |
|------|------|------|
| `enable` | `boolean` | `true` 开启降噪 |

### `void setCameraFrontMirror(boolean mirror)`

设置前置摄像头画面是否镜像翻转。

| 参数 | 类型 | 说明 |
|------|------|------|
| `mirror` | `boolean` | `true` 镜像翻转 |

### `void setCryptoKey(String cryptoKey)`

设置推流加密密钥。

| 参数 | 类型 | 说明 |
|------|------|------|
| `cryptoKey` | `String` | 加密密钥 |

### `void setVolume(float volume)`

设置推流采集音量。

| 参数 | 类型 | 说明 |
|------|------|------|
| `volume` | `float` | `0.0`（静音）~ `1.0`（原始音量） |

### `void setFlvIdExt(boolean idExt)`

设置 FLV 是否启用 ID 扩展。

| 参数 | 类型 | 说明 |
|------|------|------|
| `idExt` | `boolean` | 开关 |

---

## 实时美颜与滤镜

### `void setEffectParameter(String parameter, float value)`

设置视频特效参数值。

| 参数 | 类型 | 说明 |
|------|------|------|
| `parameter` | `String` | 特效参数名，使用 `EFFECTOR_*` 常量 |
| `value` | `float` | 参数值（范围由具体特效决定） |

**示例参数：**

| 参数键 | 说明 | 值范围 |
|--------|------|--------|
| `EFFECTOR_BRIGHTNESS` | 亮度调整 | `-1.0` ~ `1.0` |
| `EFFECTOR_CONTRAST` | 对比度调整 | `0.0` ~ `2.0` |
| `EFFECTOR_SATURATION` | 饱和度调整 | `0.0` ~ `2.0` |
| `EFFECTOR_SHARPEN` | 锐度调整 | `0.0` ~ `1.0` |
| `EFFECTOR_SMOOTHSKIN` | 磨皮程度 | `0.0` ~ `1.0` |

### `void setEffectStyle(int style)`

设置滤镜风格。

| 参数 | 类型 | 说明 |
|------|------|------|
| `style` | `int` | `EFFECTOR_STYLE_ID_*` 常量之一 |

---

## 事件监听

### `void setOnNodePublisherEventListener(OnNodePublisherEventListener listener)`

设置推流器事件回调监听器。

| 参数 | 类型 | 说明 |
|------|------|------|
| `listener` | `OnNodePublisherEventListener` | 事件回调监听器 |

### `OnNodePublisherEventListener` 接口

```java
public interface OnNodePublisherEventListener {
    void onEventCallback(NodePublisher publisher, int event, String msg);
}
```

| 参数 | 类型 | 说明 |
|------|------|------|
| `publisher` | `NodePublisher` | 触发事件的推流器实例 |
| `event` | `int` | 事件类型码 |
| `msg` | `String` | 事件描述信息 |

### 事件码参考

> `onEventCallback` 回调中的 `event` 取值。

| Code | 名称 | 说明 |
|------|------|------|
| `2000` | 开始连接 | 开始连接推流服务器 |
| `2001` | 连接成功 | 推流连接成功 |
| `2002` | 连接失败 | 连接失败（自动重连中） |
| `2003` | 重连中 | 正在重新连接 |
| `2004` | 推流结束 | 推流正常结束 |
| `2005` | 网络异常 | 网络异常（自动重连中） |
| `2006` | 连接超时 | 网络连接超时（自动重连中） |

---

## 内部类：GLCameraView

`GLCameraView` 是 `NodePublisher` 的内部类，继承自 `GLSurfaceView` 并实现 `GLSurfaceView.Renderer`。

| 方法 | 说明 |
|------|------|
| `getSurfaceProvider()` | 获取 CameraX Preview SurfaceProvider，将摄像头数据桥接到 OpenGL 纹理 |
| `onSurfaceCreated()` | 创建 OpenGL 纹理和 `SurfaceTexture`，触发摄像头打开 |
| `onSurfaceChanged()` | 接收 Surface 尺寸变化并通知原生层 |
| `onDrawFrame()` | 更新 `SurfaceTexture` 图像，将纹理矩阵传入原生层渲染 |
| `onDetachedFromWindow()` | 销毁 GPU 图像资源 |

该内部类封装了完整的 OpenGL 渲染管线：

```
CameraX Preview → SurfaceTexture → GPUImage 原生渲染引擎 → 编码器
```

---

## 使用示例

```java
// 1. 创建推流器（Activity 需实现 LifecycleOwner）
NodePublisher publisher = new NodePublisher(this, "your-license-key");

// 2. 附加到视图
publisher.attachView(previewContainer);

// 3. 设置视频方向（可选，需在 openCamera 前）
publisher.setVideoOrientation(NodePublisher.VIDEO_ORIENTATION_PORTRAIT);

// 4. 配置编码参数（可选）
publisher.setVideoCodecParam(
    NodePublisher.NMC_CODEC_ID_H264,
    NodePublisher.NMC_PROFILE_H264_HIGH,
    720, 1280, 24, 1_500_000
);
publisher.setAudioCodecParam(
    NodePublisher.NMC_CODEC_ID_AAC,
    NodePublisher.NMC_PROFILE_AAC_LC,
    44100, 1, 64_000
);

// 5. 设置美颜（可选）
publisher.setEffectParameter(NodePublisher.EFFECTOR_SMOOTHSKIN, 0.5f);
publisher.setEffectStyle(NodePublisher.EFFECTOR_STYLE_ID_FAIRSKIN);

// 6. 打开摄像头
publisher.openCamera(NodePublisher.NMC_CAMERA_FRONT);

// 7. 设置事件监听
publisher.setOnNodePublisherEventListener((pub, event, msg) -> {
    switch (event) {
        case NodePublisher.EVENT_CONNECTED:
            Log.d("PublisherEvent", "推流连接成功");
            break;
        case NodePublisher.EVENT_CONNECT_FAIL:
            Log.d("PublisherEvent", "连接失败: " + msg);
            break;
        case NodePublisher.EVENT_NETWORK_ERROR:
            Log.d("PublisherEvent", "网络异常: " + msg);
            break;
    }
});

// 8. 开始推流
publisher.start("rtmp://example.com/live/stream");

// 9. 摄像头控制
// publisher.switchCamera();
// publisher.setZoomRatio(0.3f);
// publisher.setTorchEnable(true);
// publisher.startFocusAndMeteringCenter();

// 10. 多目标推流
// publisher.addOutput("rtmp://backup.example.com/live/stream");

// 11. 停止推流
// publisher.stop();
// publisher.closeCamera();
// publisher.detachView();
```

---

## 注意事项

1. **License 授权码**：构造时必须传入有效授权码，否则推流器无法工作。
2. **LifecycleOwner**：context 需实现 `LifecycleOwner`（通常为 Activity），CameraX 依赖生命周期管理。
3. **视图生命周期**：建议在 Activity/Fragment `onDestroy` 中调用 `stop()` → `closeCamera()` → `detachView()` 释放资源。
4. **配置顺序**：视频方向、编码参数、美颜参数等配置应在 `openCamera()` 和 `start()` 之前完成。
5. **硬件加速**：`setHWAccelEnable(true)` 可能不兼容所有设备，请在实际设备上充分测试。
6. **CameraX 兼容性**：`openCamera()` 基于 CameraX API，要求设备支持 Camera2。
7. **前置镜像**：默认前置摄像头画面未镜像，可通过 `setCameraFrontMirror(true)` 开启镜像。
8. **美颜性能**：开启磨皮等特效会增加 GPU 负载，低端设备上可能影响帧率。
