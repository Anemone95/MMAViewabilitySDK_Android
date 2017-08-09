###Android SDK 部署指南
####步骤1：添加  Android SDK 到工程中

* 将  SDK 的 `mma_android_sdk.jar` 拷贝到 libs 目录下并添加为 JAR 包；
* 将libNDKSignature.so 拷贝到 libs\armeabi 目录下；
* 把sdkconfig.xml配置文件拷贝到工程里的assets目录下，同时将 sdkconfig.xml 上传到 web 服务器，使其可以通过 web 方式访问，假设其地址为 CONFIG_URL（其后会用到）。


对于Eclipse工程，请参照下面的步骤添加 JAR 包：

1. 在 “Package Explorer” 窗口中右击你的工程并选择 “Properties”
2. 在左侧面板中选择 “Java Build Path”
3. 在主窗口中选择 “Libraries”
4. 点击“Add JARs…”按钮
5. 选择拷贝到libs目录下的`mma_android_sdk.jar`
6. 点击 “OK” 完成添加。

####步骤2：修改 AndroidManifest.xml 文件
修改 AndroidManifest.xml 文件，新增网络连接请求和读取手机状态的权限：

```
<uses-permission android:name="android.permission.INTERNET" /> 
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" /> 
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" /> 
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" /> 
<uses-permission android:name="android.permission.READ_PHONE_STATE" />

```

####步骤3：为广告添加监测代码
在有广告监测需求的 Activity 中的 onStart() 方法中调用,进行初始化,其中 CONFIG_URL 为远程配置文件的地址

```
Countly.sharedInstance().init(this, CONFIG_URL); 
```

**普通广告曝光监测**：当广告产生曝光时，控制触发以下代码进行曝光监测。

```
String TEST_TRACKING_URL = "http://admaster.mobi/android/log/a222,c123,b132,2g[ImpressionID]";//TEST_TRACKING_URL 为对应的曝光代码  
Countly.sharedInstance().onExpose(TEST_TRACKING_URL);

```

**可见性广告曝光监测**：当广告产生曝光时，控制触发以下代码进行曝光监测。第一个参数为曝光监测代码，第二个参数为当前广告展示视图对象。  
**注意：对于需要监测广告可视化的广告监测，第二个参数（广告视图对象）为必选项，且媒体需要传入当前广告展示的视图对象，否则可能造成SDK无法监测当前广告可视化的情况出现**

```
String TEST_TRACKING_URL = "http://admaster.mobi/android/log/a222,c123,b132,2g[ImpressionID]";//TEST_TRACKING_URL 为对应的曝光代码  
Countly.sharedInstance().onExpose(TEST_TRACKING_URL,adView);

```


**可见性视频广告曝光监测**：当视频广告产生曝光时，控制触发以下代码进行曝光监测。第一个参数为曝光监测代码，第二个参数为当前广告展示视频视图对象。  
**注意：对于需要监测广告可视化的视频广告监测，第二个参数（视频视图对象）为必选项，且媒体需要传入当前广告展示的视图对象，否则可能造成SDK无法监测当前视频广告可视化的情况出现**

```
String TEST_TRACKING_URL = "http://admaster.mobi/android/log/a222,c123,b132,2g[ImpressionID]";//TEST_TRACKING_URL 为对应的曝光代码  
Countly.sharedInstance().onVideoExpose(TEST_TRACKING_URL,videoView);

```

**可见性广告曝光JS监测**：当广告产生曝光时，控制触发以下代码使用JS方案进行曝光监测。第一个参数为曝光监测代码，第二个参数为当前广告展示视图对象。  

```
String TEST_TRACKING_URL = "http://admaster.mobi/android/log/a222,c123,b132,2g[ImpressionID]";//TEST_TRACKING_URL 为对应的曝光代码  
Countly.sharedInstance().onJsExpose(TEST_TRACKING_URL,adView);

```



**可见性视频广告曝光JS监测**：当视频广告产生曝光时，控制触发以下代码使用JS方案进行曝光监测，第一个参数为曝光监测代码，第二个参数为当前广告展示视频视图对象。

```
String TEST_TRACKING_URL = "http://admaster.mobi/android/log/a222,c123,b132,";//TEST_TRACKING_URL 为对应的曝光代码
Countly.sharedInstance().onVideoJsExpose(TEST_TRACKING_URL,videoView); 

```



**广告点击监测**：当广告被点击时，控制触发以下代码

```
String TEST_TRACKING_URL = "http://admaster.mobi/android/log/a222,c123,b132,";//TEST_TRACKING_URL 为对应的曝光代码
Countly.sharedInstance().onClick(TEST_TRACKING_URL); 

```

代码示例(具体请见 demo 项目源码)

```
package com.ndroid_demo;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import cn.com..mobile.tracking.api.Countly;
import cn.com..mobile.tracking.util.Logger;
/**
 * 此工程是为使用SDK的用户提供的demo
 * 
 * @author admaster
 * 
 */
public class MainActivity extends Activity {
 //int count = 0;
/**
 * sdkconfig.xml配置文件服务器存放地址
 */
 public static final String CONFIG_URL = "http://admaster.mobi/sdkconfig.xml";
 /**
 * 点击监测地址
 */
 public static final String TEST_TRACKING_URL = "http://admaster.mobi/android/log/a222,c123,b132,";
@Override
 protected void onCreate(Bundle savedInstanceState) {
 super.onCreate(savedInstanceState);
 setContentView(R.layout.activity_main);
/**
 * 初始化SDK
 */
 Countly.sharedInstance().init(this, CONFIG_URL);
}
/**
 * 点击广告，开启监测
 */
 public void sendClickMsg(View view) {
 Countly.sharedInstance().onClick(TEST_TRACKING_URL);
 }
 /**
 * 曝光广告，开启监测
 */
 public void sendExposeMsg(View adview) {
 Countly.sharedInstance().onExpose(TEST_TRACKING_URL, adview);
 } 
/**
 * 视频曝光广告，开启监测
 */
 public void sendVideoExposeMsg(View videoView) {
 Countly.sharedInstance().onVideoExpose(TEST_TRACKING_URL, videoView);
 } 
}
```
####步骤4：验证和调试
SDK 的测试有两个方面：
 
 1. 参数是否齐全，URL 拼接方式是否正确
 2. 请求次数和第三方监测平台是否能对应上
针对第一点，使用 Admaster SDK 测试平台进行测试和验证，登入 http://developer.admaster.com.cn/, 根据页面上的提示进行调用， 页面会实时显示出服务器接收到的信息，如果和本地的设备相关信息一致，则表示测试通过。

针对第二点，建议使用第三方监测系统的正式环境进行测试，主要对比媒体自身广告系统监测数据和第三方监测数据数量上的差异。

