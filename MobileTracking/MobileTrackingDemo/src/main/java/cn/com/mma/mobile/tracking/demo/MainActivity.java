package cn.com.mma.mobile.tracking.demo;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import cn.com.mma.mobile.tracking.api.Countly;


/**
 * MMAChinaSDK Example
 */
public class MainActivity extends AppCompatActivity {

    //广告位监测链接
//    public static final String TRACKING_URL = "http://vxyz.admaster.com.cn/w/a86218,b1778712,c2343,i0,m202,8a2,8b2,2j,h";
    public static final String TRACKING_URL = "http://test.m.cn.miaozhen.com/x/k=test123&p=test456&va=1&vb=8&vj=1111&vi=10&vh=90&o=www.baidu.com";

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
        Countly.sharedInstance().onVideoExpose(TRACKING_URL, adView, 2);
        Log.d(TAG, "[ViewAbilityExpose]：" + TRACKING_URL);
        new Handler().postDelayed(new Runnable(){
            public void run() {
                //execute the task
                Log.d(TAG, "[StopViewAbilityExpose]：" + TRACKING_URL);
                Countly.sharedInstance().stop(TRACKING_URL);
            }
        }, 5000);
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
