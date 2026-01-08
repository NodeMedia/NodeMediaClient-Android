# NodeMediaClient-Android
[![](https://jitpack.io/v/NodeMedia/NodeMediaClient-Android.svg)](https://jitpack.io/#NodeMedia/NodeMediaClient-Android)   
A simple, high-performance, low-latency live streaming SDK.

## Features
### Play
* RTMP/RTSP/HLS/HTTP/KMP/UDP protocols
* FLV/MP4/fMP4/MKV/MPEGTS demuxers
* H264/H265 video decoders
* AAC/OPUS/G711/SPEEX/NELLYMOSER audio decoders
* Hardware Acceleration
* Low latency
* Delay elimination
* Take screenshot while playing
* Take record while playing, support mp4/flv/ts/mkv format 
* Compatible with flv_extension_id and Enhanced-Rtmp standards

### Publish
* RTMP/RTSP/HLS/HTTP/KMP/UDP protocols
* FLV/MPEGTS muxers
* H264/H265 video encoders 
* AAC/OPUS/G711 audio encoder
* Hardware Acceleration
* Arbitrary video resolution
* Multiple output
* Compatible with flv_extension_id and Enhanced-Rtmp standards
* Network congestion packet loss strategy
* Network quality event callback
* Build-in color filters and skin smoothing filter

## Install
### 1. Add the JitPack repository to your build file
```
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

### 2. Add the dependency
```
dependencies {
    implementation 'com.github.NodeMedia:NodeMediaClient-Android:4.1.3'
}
```

## Play Live Streaming

### 1. Add permission INTERNET
```
<uses-permission android:name="android.permission.INTERNET" />
```

### 2. Setting up the layout
```
<FrameLayout
    android:id="@+id/video_view"
    android:layout_width="match_parent"
    android:layout_height="match_parent">
</FrameLayout>
```

### 3. Play the stream
```
private NodePlayer np;
    
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
    setContentView(R.layout.activity_playview);

    FrameLayout vv = findViewById(R.id.video_view);
    np = new NodePlayer(this,"");
    np.attachView(vv);
    np.start("rtmp://192.168.0.2/live/demo");
}
    
@Override
protected void onDestroy() {
    super.onDestroy();
    np.detachView();
    np.stop();
}
```
That's it. Very simple!


## Publish Live Streaming
### 1. Request more permissions
```
<uses-feature android:name="android.hardware.camera.any" />

<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
```

### 2. Permission to apply
```
private static final String[] PERMISSIONS = new String[]{
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO};
private static final int REQUEST_PERMISSION_CODE = 0XFF00;

@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    requestPermission();
    ……………………
}

private void requestPermission() {
    ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS, REQUEST_PERMISSION_CODE);
}

```

### 3. Setting up the layout
```
<FrameLayout
    android:id="@+id/camera_view"
    android:layout_width="match_parent"
    android:layout_height="match_parent">
</FrameLayout>
```

### 4.Start Publish
```
private NodePublisher np;

@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
    setContentView(R.layout.activity_publish_view);
    FrameLayout fl = findViewById(R.id.camera_view);

    np = new NodePublisher(this, "");
    np.setAudioCodecParam(NodePublisher.NMC_CODEC_ID_AAC, NodePublisher.NMC_PROFILE_AUTO, 48000, 1, 64_000);
    np.setVideoOrientation(NodePublisher.VIDEO_ORIENTATION_PORTRAIT);
    np.setVideoCodecParam(NodePublisher.NMC_CODEC_ID_H264, NodePublisher.NMC_PROFILE_AUTO, 480, 854, 30, 1_000_000);
    np.attachView(fl);
    np.openCamera(true);
    Button publishBtn = findViewById(R.id.publish_btn);
    publishBtn.setOnClickListener((v) -> {
        np.start("rtmp://192.168.0.2/live/demo");
    });
}

@Override
protected void onDestroy() {
    super.onDestroy();
    np.detachView();
    np.closeCamera();
    np.stop();
}
```
## Demo
[https://cdn.nodemedia.cn/NodeMediaClient/NodeMediaClient-AndroidDemo.zip](https://cdn.nodemedia.cn/NodeMediaClient/NodeMediaClient-AndroidDemo.zip)

## License
A commercial license is required.  
[https://www.nodemedia.cn/product/nodemediaclient-android/](https://www.nodemedia.cn/product/nodemediaclient-android/)

## Business & Technical service
* QQ: 281269007
* Email: service@nodemedia.cn
