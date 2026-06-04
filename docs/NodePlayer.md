# NodePlayer API 文档

NodeMediaClient Android 客户端核心播放器 API 参考文档  

---

## 概述

`NodePlayer` 是 NodeMediaClient 的核心播放器类，负责音视频流的播放控制、视图管理、录制截图等功能。底层通过 JNI 调用原生 C++ 库 `NodeMediaClient` 实现。

**包名：** `cn.nodemedia`  
**继承关系：** `Object` → `NodePlayer` implements `TextureView.SurfaceTextureListener`

---

## 构造函数

### `NodePlayer(Context context, String license)`

创建播放器实例。

| 参数 | 类型 | 说明 |
|------|------|------|
| `context` | `Context` | Android 上下文 |
| `license` | `String` | 授权码 |

---

## 常量

### 日志级别

| 常量 | 值 | 说明 |
|------|----|------|
| `LOG_LEVEL_ERROR` | `0` | 仅输出错误日志 |
| `LOG_LEVEL_INFO` | `1` | 输出信息日志 |
| `LOG_LEVEL_DEBUG` | `2` | 输出调试日志 |

### RTSP 传输协议

| 常量 | 值 | 说明 |
|------|----|------|
| `RTSP_TRANSPORT_UDP` | `"udp"` | UDP 传输（默认） |
| `RTSP_TRANSPORT_TCP` | `"tcp"` | TCP 传输 |
| `RTSP_TRANSPORT_UDP_MULTICAST` | `"udp_multicast"` | UDP 组播传输 |
| `RTSP_TRANSPORT_HTTP` | `"http"` | HTTP 隧道传输 |

---

## 视图管理

### `void attachView(ViewGroup vg)`

将播放器画面附加到指定的视图容器中。

- 内部创建 `TextureView` 并自动添加到 `vg`
-  TextureView 尺寸为 `MATCH_PARENT`，居中显示
- 自动保持屏幕常亮
- 多次调用不会重复创建

| 参数 | 类型 | 说明 |
|------|------|------|
| `vg` | `ViewGroup` | 目标视图容器（ViewGroup 的子类） |

### `TextureView getTextureView()`

获取当前的 TextureView 实例。

**返回：** 当前附加的 `TextureView`，未附加时返回 `null`。

### `void detachView()`

将播放器画面从当前视图移除。

- 关闭屏幕常亮
- 释放 TextureView 引用

---

## 播放控制

### `int start(String url)`

开始播放指定的媒体流/文件。

| 参数 | 类型 | 说明 |
|------|------|------|
| `url` | `String` | 播放地址，支持 rtmp、rtsp、http、https、本地文件等 |

**返回：** `int` — 启动结果（0 成功，非 0 失败）。

### `int stop()`

停止播放。

**返回：** `int` — 操作结果。

### `int pause(boolean pause)`

暂停或恢复点播视频播放。

| 参数 | 类型 | 说明 |
|------|------|------|
| `pause` | `boolean` | `true` 暂停，`false` 恢复 |

**返回：** `int` — 操作结果。

### `int seek(long pts)`

时移到指定时间点（仅点播）。

| 参数 | 类型 | 说明 |
|------|------|------|
| `pts` | `long` | 目标时间点，单位**毫秒** |

**返回：** `int` — 操作结果。

---

## 状态查询

| 方法 | 返回类型 | 说明 |
|------|---------|------|
| `isVod()` | `boolean` | 当前播放是否为点播回放 |
| `isPause()` | `boolean` | 当前是否处于暂停状态 |
| `isPlaying()` | `boolean` | 当前是否正在播放 |
| `getDuration()` | `long` | 获取点播视频总时长（毫秒） |
| `getCurrentPosition()` | `long` | 获取当前播放位置（毫秒） |
| `getBufferPosition()` | `long` | 获取缓冲位置（毫秒） |
| `getBufferPercentage()` | `int` | 获取缓冲进度百分比（0-100） |

---

## 录制

### `int startRecord(String filename)`

开始录制当前播放的音视频流。

| 参数 | 类型 | 说明 |
|------|------|------|
| `filename` | `String` | 录制文件名，支持 `.mp4`、`.flv`、`.mkv`、`.ts` 格式 |

**返回：** `int` — 操作结果。

### `int stopRecord()`

停止录制。

**返回：** `int` — 操作结果。

---

## 截图

### `int screenshot(String filename)`

对当前视频画面进行截图。

| 参数 | 类型 | 说明 |
|------|------|------|
| `filename` | `String` | 保存的文件名，格式为 **JPEG** |

**返回：** `int` — 操作结果。

---

## 配置方法

### `void setLogLevel(int logLevel)`

设置日志输出等级。

| 参数 | 类型 | 说明 |
|------|------|------|
| `logLevel` | `int` | `LOG_LEVEL_ERROR` / `LOG_LEVEL_INFO` / `LOG_LEVEL_DEBUG` |

### `void setBufferTime(int bufferTime)`

设置缓存时长。

| 参数 | 类型 | 说明 |
|------|------|------|
| `bufferTime` | `int` | 缓存时长，单位**毫秒** |

### `void setScaleMode(int mode)`

设置视频缩放模式。

| 参数 | 类型 | 说明 |
|------|------|------|
| `mode` | `int` | 缩放模式（具体 mode 值参见底层实现） |

### `void setRTSPTransport(String rtspTransport)`

设置 RTSP 传输协议。默认值为 `RTSP_TRANSPORT_UDP`。

| 参数 | 类型 | 说明 |
|------|------|------|
| `rtspTransport` | `String` | 可选值：`"udp"`、`"tcp"`、`"udp_multicast"`、`"http"` |

### `void setHTTPReferer(String httpReferer)`

设置 HTTP Referer 头信息。

| 参数 | 类型 | 说明 |
|------|------|------|
| `httpReferer` | `String` | HTTP Referer 值 |

### `void setHTTPUserAgent(String httpUserAgent)`

设置 HTTP User-Agent 头信息。

| 参数 | 类型 | 说明 |
|------|------|------|
| `httpUserAgent` | `String` | User-Agent 值 |

### `void setCryptoKey(String cryptoKey)`

设置视频解密密码。

| 参数 | 类型 | 说明 |
|------|------|------|
| `cryptoKey` | `String` | 16 字节解密密码 |

### `void setVolume(float volume)`

设置播放音量。

| 参数 | 类型 | 说明 |
|------|------|------|
| `volume` | `float` | `0.0`（静音）~ `1.0`（原始音量） |

### `void setHWAccelEnable(boolean enable)`

设置是否开启硬件加速解码。

| 参数 | 类型 | 说明 |
|------|------|------|
| `enable` | `boolean` | `true` 开启，`false` 关闭 |

---

## 视频画面控制

### `void setVideoSurface(Surface surface)`

设置视频渲染的 Surface。当 `TextureView` 可用时自动调用，也可手动设置自定义 Surface。

| 参数 | 类型 | 说明 |
|------|------|------|
| `surface` | `Surface` | 视频 Surface |

### `void resizeVideoSurface()`

通知视频 Surface 大小已改变，触发重新适配渲染尺寸。

### `void rotateVideo(int rotate)`

旋转视频画面。

| 参数 | 类型 | 说明 |
|------|------|------|
| `rotate` | `int` | 旋转角度 |

---

## 事件监听

### `void setOnNodePlayerEventListener(OnNodePlayerEventListener listener)`

设置播放器事件回调监听器。

| 参数 | 类型 | 说明 |
|------|------|------|
| `listener` | `OnNodePlayerEventListener` | 事件回调监听器 |

### `OnNodePlayerEventListener` 接口

```java
public interface OnNodePlayerEventListener {
    void onEventCallback(NodePlayer player, int event, String msg);
}
```

| 参数 | 类型 | 说明 |
|------|------|------|
| `player` | `NodePlayer` | 触发事件的播放器实例 |
| `event` | `int` | 事件类型码 |
| `msg` | `String` | 事件描述信息 |

---

## 事件码参考

> `onEventCallback(NodePlayer player, int event, String msg)` 回调中的 `event` 取值。

### 播放相关事件

| Code | 名称 | 说明 |
|------|------|------|
| `1000` | 连接中 | 正在连接视频源 |
| `1001` | 连接成功 | 视频连接成功，开始播放 |
| `1002` | 连接失败 | 视频连接失败（自动重连中） |
| `1003` | 重连中 | 视频开始重新连接 |
| `1004` | 播放结束 | 视频播放正常结束 |
| `1005` | 网络异常 | 网络异常（自动重连中） |
| `1006` | 连接超时 | 网络连接超时（自动重连中） |

### 缓冲相关事件

| Code | 名称 | 说明 |
|------|------|------|
| `1100` | 缓冲区空 | 播放缓冲区为空 |
| `1101` | 缓冲中 | 播放缓冲区正在缓冲数据 |
| `1102` | 缓冲完成 | 缓冲完成，开始播放 |
| `1103` | 流统计信息 | 流统计信息（每秒回调一次） |
| `1104` | 视频分辨率 | 解码后得到视频宽高（`msg` 格式为 `width x height`） |

---

## 生命周期

### `finalize()`

播放器被垃圾回收时自动调用，释放原生层资源（JNI free）。一般情况下无需手动调用。

---

## 使用示例

```java
// 1. 创建播放器
NodePlayer player = new NodePlayer(context, "your-license-key");

// 2. 附加到视图
player.attachView(videoContainer);

// 3. 设置事件监听
player.setOnNodePlayerEventListener((player1, event, msg) -> {
    Log.d("PlayerEvent", "Event: " + event + ", Msg: " + msg);
});

// 4. 开始播放
player.start("rtmp://example.com/live/stream");

// 5. 控制播放
player.pause(true);    // 暂停
player.seek(30000);    // 跳转到 30 秒
player.stop();         // 停止播放

// 6. 截图
player.screenshot("/sdcard/screenshot.jpeg");

// 7. 录制
player.startRecord("/sdcard/record.mp4");
player.stopRecord();

// 8. 移除视图
player.detachView();
```

---

## 注意事项

1. **License 授权码**：构造时必须传入有效授权码，若传入空字符串则进入7天试用模式
2. **视图生命周期**：建议在 Activity/Fragment 的 `onDestroy` 中调用 `detachView()` 和 `stop()` 释放资源。
3. **RTSP 传输协议**：默认使用 UDP，若网络环境不稳定可切换为 TCP。
4. **硬件加速**：`setHWAccelEnable(true)` 可能不兼容所有设备或编码格式。
5. **录制格式**：支持 `mp4`、`flv`、`mkv`、`ts` 四种容器格式。
6. **截图格式**：仅支持 JPEG 格式保存。
