package cn.com.mma.mobile.tracking.api;

import java.util.Timer;
import java.util.TimerTask;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import cn.com.mma.mobile.tracking.bean.SDK;
import cn.com.mma.mobile.tracking.util.Logger;
import cn.com.mma.mobile.tracking.util.SdkConfigUpdateUtil;
import cn.com.mma.mobile.tracking.util.SharedPreferencedUtil;
import cn.com.mma.mobile.tracking.viewability.origin.ViewAbilityEventListener;

/**
 * MMAChinaSDK Android API 入口类
 *
 */
public class Countly {


    private  SendMessageThread sendNormalMessageThread = null;
    private  SendMessageThread sendFailedMessageThread = null;

    private Timer normalTimer = null;
    private Timer failedTimer = null;

    private ViewAbilityHandler viewAbilityHandler = null;

    private volatile boolean sIsInitialized = false;
    private Context mContext;
    private RecordEventMessage mUrildBuilder;

    private static final String EVENT_CLICK = "onClick";
    private static final String EVENT_EXPOSE = "onExpose";
    private static final String EVENT_VIEWABILITY_EXPOSE = "onAdViewExpose";
    private static final String EVENT_VIEWABILITY_VIDEOEXPOSE = "onVideoExpose";

    //[本地测试]控制广播的开关
    public static boolean LOCAL_TEST = true;

    public static String ACTION_STATS_EXPOSE = "ACTION_STATS_EXPOSE";
    public static String ACTION_STATS_VIEWABILITY = "ACTION.STATS_VIEWABILITY";
    public static String ACTION_STATS_SUCCESSED = "ACTION.STATS_SUCCESSED";

    private static Countly mInstance = null;


    public static Countly sharedInstance() {
        if (mInstance == null) {
            synchronized (Countly.class) {
                if (mInstance == null) {
                    mInstance = new Countly();
                }
            }
        }
        return mInstance;
    }


    /**
     * 调试模式,打印Log日志,默认关闭
     * @param isPrintOut 是否开启log输出
     */
    public void setLogState(boolean isPrintOut) {
        Logger.DEBUG_LOG = isPrintOut;
    }


    /**
     * SDK初始化,需要手续
     * @param context 上下文
     * @param configURL 配置文件在线更新地址
     */
    public void init(Context context, String configURL) {
        if (context == null) {
            Logger.e("Countly.init(...) failed:Context can`t be null");
            return;
        }
        if (sIsInitialized) {
            return;
        } else {
            sIsInitialized = true;
        }

        try {
            Context appContext = context.getApplicationContext();
            mContext = appContext;
            normalTimer = new Timer();
            failedTimer = new Timer();
            mUrildBuilder = RecordEventMessage.getInstance(context);

            SdkConfigUpdateUtil.initSdkConfigResult(context, configURL);
            SDK sdk = SdkConfigUpdateUtil.getSdk(context);

            //初始化可视化监测模块,传入SDK配置文件
            viewAbilityHandler = new ViewAbilityHandler(mContext, viewAbilityEventListener, sdk);

            startTask();

        } catch (Exception e) {
            Logger.e("Countly init failed");
        }
    }


    /**
     * 普通点击事件监测接口
     * @param adURL 监测链接
     */
    public  void onClick(String adURL) {

        triggerEvent(EVENT_CLICK, adURL, null);
    }

    /**
     * 普通曝光事件监测接口
     * @param adURL 监测链接
     */
    public  void onExpose(String adURL) {

        triggerEvent(EVENT_EXPOSE, adURL, null);
    }

    /**
     * 可视化曝光事件监测接口
     * @param adURL 监测链接
     * @param adView 监测广告视图对象
     */
    public  void onExpose(String adURL, View adView) {

        triggerEvent(EVENT_VIEWABILITY_EXPOSE, adURL, adView);
    }

    /**
     * 可视化视频曝光事件监测接口
     *
     * @param adURL     监测链接
     * @param videoView 监测广告视频对象
     */
    public void onVideoExpose(String adURL, View videoView) {

        triggerEvent(EVENT_VIEWABILITY_VIDEOEXPOSE, adURL, videoView);
    }

    /**
     * 可视化曝光JS监测接口
     * @param adURL 监测链接
     * @param adView 监测广告视图对象
     */
    public void onJSExpose(String adURL, View adView) {
        viewAbilityHandler.onJSExpose(adURL, adView, false);
    }

    /**
     * 可视化视频曝光JS监测接口
     * @param adURL 监测链接
     * @param adView 监测广告视图对象
     */
    public void onJSVideoExpose(String adURL, View adView) {
        viewAbilityHandler.onJSExpose(adURL, adView, true);
    }

    /**
     * 释放SDK接口,在程序完全退出前调用
     */
    public  void terminateSDK() {
        try {
            if (normalTimer != null) {
                normalTimer.cancel();
                normalTimer.purge();
            }
            if (failedTimer == null) {
                failedTimer.cancel();
                failedTimer.purge();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            normalTimer = null;
            failedTimer = null;
            sendNormalMessageThread = null;
            sendFailedMessageThread = null;
            mUrildBuilder = null;

            if (viewAbilityHandler != null) viewAbilityHandler = null;

        }
    }



    private  void triggerEvent(String eventName, String adURL, View adView) {

        if (sIsInitialized == false || mUrildBuilder == null) {
            Logger.e("The static method " + eventName + "(...) should be called before calling Countly.init(...)");
            return;
        }

        switch (eventName) {
            case EVENT_CLICK:
                viewAbilityHandler.onClick(adURL);
                break;
            case EVENT_EXPOSE:
                viewAbilityHandler.onExpose(adURL);
                break;
            case EVENT_VIEWABILITY_EXPOSE:
                viewAbilityHandler.onExpose(adURL, adView);
                break;
            case EVENT_VIEWABILITY_VIDEOEXPOSE:
                viewAbilityHandler.onVideoExpose(adURL, adView);
                break;

        }
    }



    private  void startTask() {
        try {
            normalTimer.schedule(new TimerTask() {
                public void run() {
                    startNormalRun();
                }
            }, 0, Constant.ONLINECACHE_QUEUEEXPIRATIONSECS);

            failedTimer.schedule(new TimerTask() {
                public void run() {
                    startFailedRun();
                }
            }, 0, Constant.OFFLINECACHE_QUEUEEXPIRATIONSECS * 1000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private  void startNormalRun() {
        try {
            if (sendNormalMessageThread != null && sendNormalMessageThread.isAlive()) return;

            SharedPreferences sp = SharedPreferencedUtil.getSharedPreferences(mContext, SharedPreferencedUtil.SP_NAME_NORMAL);
            if ((sp == null) || (sp.getAll().isEmpty())) return;

            sendNormalMessageThread = new SendMessageThread(SharedPreferencedUtil.SP_NAME_NORMAL, mContext, true);
            sendNormalMessageThread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private  void startFailedRun() {
        try {
            if (sendFailedMessageThread != null && sendFailedMessageThread.isAlive()) return;

            SharedPreferences sp = SharedPreferencedUtil.getSharedPreferences(mContext, SharedPreferencedUtil.SP_NAME_FAILED);
            if ((sp == null) || (sp.getAll().isEmpty())) return;

            sendFailedMessageThread = new SendMessageThread(SharedPreferencedUtil.SP_NAME_FAILED, mContext, false);
            sendFailedMessageThread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 接收来自ViewAbilityService的回调,触发监测事件
     *
     * @param adURL 带ViewAbility监测结果的链接
     */
    private  ViewAbilityEventListener viewAbilityEventListener = new ViewAbilityEventListener() {
        @Override
        public void onEventPresent(String adURL) {
            if (sIsInitialized && mUrildBuilder != null) {
                mUrildBuilder.recordEventWithUrl(adURL);
            }
        }
    };

}
