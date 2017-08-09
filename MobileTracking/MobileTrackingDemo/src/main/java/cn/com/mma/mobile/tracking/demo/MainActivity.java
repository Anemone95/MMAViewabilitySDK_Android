package cn.com.mma.mobile.tracking.demo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import cn.com.mma.mobile.tracking.api.Countly;


/**
 * MMAChinaSDK Example
 */
public class MainActivity extends AppCompatActivity {

    //广告位监测链接
    public static final String TRACKING_URL = "http://vxyz.admaster.com.cn/w/a86218,b1778712,c2343,i0,m202,8a2,8b2,2j,h";
    //sdkconfig.xml配置文件服务器存放地址
    public static final String CONFIG_URL = "";

    private static final String TAG = MainActivity.class.getSimpleName();
    private TextView adView;
    private TextView urlView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        adView = (TextView) findViewById(R.id.adview);
        urlView = (TextView) findViewById(R.id.ad_url);
        urlView.setText(TRACKING_URL);

        // MMASDK 初始化
        Countly.sharedInstance().setLogState(true);
        Countly.sharedInstance().init(this, CONFIG_URL);
    }

    /**
     * 点击监测
     *
     * @param view
     */
    public void doClick(View view) {
        Countly.sharedInstance().onClick(TRACKING_URL);
        Log.d(TAG, "[click]：" + TRACKING_URL);
    }

    /**
     * 曝光监测
     *
     * @param view
     */
    public void doExpose(View view) {
        Countly.sharedInstance().onExpose(TRACKING_URL);
        Log.d(TAG, "[expose]：" + TRACKING_URL);
    }

    /**
     * 可视化曝光监测
     *
     * @param view
     */
    public void doViewAbilityExpose(View view) {
        Countly.sharedInstance().onExpose(TRACKING_URL, adView);
        Log.d(TAG, "[ViewAbilityExpose]：" + TRACKING_URL);
    }

    /**
     * 可视化曝光JS监测
     *
     * @param view
     */
    public void doViewAbilityJSExpose(View view) {
        Countly.sharedInstance().onJSExpose(TRACKING_URL, adView);
        Log.d(TAG, "[ViewAbilityJSExpose]：" + TRACKING_URL);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Countly.sharedInstance().terminateSDK();
    }
}
