
###Android SDK 部署指南
####步骤1：添加  Android SDK 到工程中

* 将  SDK 的 `mma_android_sdk.jar` 拷贝到 libs 目录下并添加为 JAR 包；
* 将libMMASignature.so 拷贝到 libs\armeabi 目录下；
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
<uses-permission android:name="android.permission.READ_PHONE_STATE" />

```

####步骤3：为广告添加监测代码
在有广告监测需求的 Activity 中的 onStart() 方法中调用,进行初始化,其中 CONFIG_URL 为远程配置文件的地址

```
Countly.sharedInstance().init(this, CONFIG_URL); 
```

**普通广告曝光监测**：当广告产生曝光时，控制触发以下代码进行曝光监测。

```
String TEST_TRACKING_URL = "http://example.com/xxxxxx";//TEST_TRACKING_URL 为对应的曝光代码  
Countly.sharedInstance().onExpose(TEST_TRACKING_URL);

```

**可见性广告曝光监测**：当广告产生曝光时，控制触发以下代码进行曝光监测。第一个参数为曝光监测代码，第二个参数为当前广告展示视图对象。  
**注意：对于需要监测广告可视化的广告监测，第二个参数（广告视图对象）为必选项，且媒体需要传入当前广告展示的视图对象，否则可能造成SDK无法监测当前广告可视化的情况出现**

```
String TEST_TRACKING_URL = "http://example.com/xxxxxx";//TEST_TRACKING_URL 为对应的曝光代码  
Countly.sharedInstance().onExpose(TEST_TRACKING_URL,adView);

```

**可见性视频广告曝光监测**：当视频广告产生曝光时，控制触发以下代码进行曝光监测。第一个参数为曝光监测代码，第二个参数为当前广告展示视频视图对象。  
**注意：对于需要监测广告可视化的视频广告监测，第二个参数（视频视图对象）为必选项，且媒体需要传入当前广告展示的视图对象，否则可能造成SDK无法监测当前视频广告可视化的情况出现**

```
String TEST_TRACKING_URL = "http://example.com/xxxxxx";//TEST_TRACKING_URL 为对应的曝光代码  
Countly.sharedInstance().onVideoExpose(TEST_TRACKING_URL,videoView);

```

**可见性广告监测停止**：当广告播放结束时，控制触发以下代码停止监测。参数为曝光监测代码。

```
String TEST_TRACKING_URL = "http://example.com/xxxxxx";//TEST_TRACKING_URL 为对应的曝光代码
Countly.sharedInstance().stop(TEST_TRACKING_URL);

```

**可见性广告曝光JS监测**：当广告产生曝光时，控制触发以下代码使用JS方案进行曝光监测。第一个参数为曝光监测代码，第二个参数为当前广告展示视图对象。  

```
String TEST_TRACKING_URL = "http://example.com/xxxxxx";//TEST_TRACKING_URL 为对应的曝光代码  
Countly.sharedInstance().onJsExpose(TEST_TRACKING_URL,adView);

```



**可见性视频广告曝光JS监测**：当视频广告产生曝光时，控制触发以下代码使用JS方案进行曝光监测，第一个参数为曝光监测代码，第二个参数为当前广告展示视频视图对象。

```
String TEST_TRACKING_URL = "http://example.com/xxxxxx";//TEST_TRACKING_URL 为对应的曝光代码
Countly.sharedInstance().onVideoJsExpose(TEST_TRACKING_URL,videoView); 

```



**广告点击监测**：当广告被点击时，控制触发以下代码

```
String TEST_TRACKING_URL = "http://example.com/xxxxxx";//TEST_TRACKING_URL 为对应的曝光代码
Countly.sharedInstance().onClick(TEST_TRACKING_URL); 

```

代码示例(具体请见 demo 项目源码)

```
package com.mmandroid_demo;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import cn.com.mma.mobile.tracking.api.Countly;

/**
 * 此工程是为使用SDK的用户提供的demo
 * 
 */
public class MainActivity extends Activity {

	/**
	 * sdkconfig.xml配置文件服务器存放地址
	 */
	public static final String CONFIG_URL = "";
	private static final String TAG = MainActivity.class.getSimpleName();
	/**
	 * 点击监测地址
	 */
	public static final String TEST_TRACKING_URL = "http://example.com/xxxxxx";

	private TextView adView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		adView = (TextView) findViewById(R.id.adview);

		// MMA SDK 初始化
		Countly.sharedInstance().setLogState(true);
		Countly.sharedInstance().init(this, CONFIG_URL);

	}

	/**
	 * 点击监测
	 * 
	 * @param view
	 */
	public void doClick(View view) {
		Countly.sharedInstance().onClick(TEST_TRACKING_URL);
		Log.d(TAG, "[click] 目标链接：" + TEST_TRACKING_URL);
	}

	/**
	 * 曝光监测
	 * 
	 * @param view
	 */
	public void doExpose(View view) {
		Countly.sharedInstance().onExpose(TEST_TRACKING_URL);
		Log.d(TAG, "[expose] 目标链接：" + TEST_TRACKING_URL);
	}

	/**
	 * 可视化曝光监测
	 * 
	 * @param view
	 */
	public void doViewAbilityExpose(View view) {
		Countly.sharedInstance().onExpose(TEST_TRACKING_URL, adView);
		Log.d(TAG,  "[ViewAbilityExpose] 目标链接：" + TEST_TRACKING_URL);
	}

	/**
	 * 停止可视化曝光监测，广告播放结束时调用
	 * 
	 * @param tracking_url
	 */
	public void stopViewAbilityExpose() {
		Countly.sharedInstance().stop(TEST_TRACKING_URL);
		Log.d(TAG,  "[Stop ViewAbilityExpose] 目标链接：" + TEST_TRACKING_URL);
	}

	/**
	 * 可视化曝光JS监测
	 * 
	 * @param view
	 */
	public void doViewAbilityJSExpose(View view) {
		Countly.sharedInstance().onJSExpose(TEST_TRACKING_URL, adView);
		Log.d(TAG, "[ViewAbilityJSExpose] 目标链接：" + TEST_TRACKING_URL);
	}

}
```
####步骤4：验证和调试
SDK 的测试有两个方面：

    1. 参数是否齐全，URL 拼接方式是否正确
    2. 请求次数和第三方监测平台是否能对应上

请联系第三方监测平台完成测试。

### 位置信息获取配置

想要获取位置信息，需要如下两步：

* 在AndroidManifest.xml 文件添加相关权限：

```
 <!-- 如果获取位置信息，需要声明以下权限 -->
 <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
 <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
```

* 监测代码对应的sdkconfig内Company标签的<isTrackLocation>项设置为**true**。

  ​

如果不想开启定位服务，需要**sdkconfig内将所有Company标签内的<isTrackLocation>设置为false**。

### 混淆配置

如果APP使用了混淆技术，以防SDK被混淆报错，需要在混淆配置里添加如下代码：

```
# SDK开源，集成的Library(Jar)本身无混淆，二次混淆与否不影响
#-keep class cn.com.mma.** { *; }
#-keep class cn.mmachina.**{*;}
#-dontwarn cn.com.mma.**
#-dontwarn cn.mmachina.**
# SDK用到了v4包里的API，请确保v4相关support包不被混淆
#-keep class android.support.v4.** { *; }
#-dontwarn android.support.v4.**
```

