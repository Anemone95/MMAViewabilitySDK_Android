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
 * API入口类
 * <p>
 * AdmasterViewAbilitySDK 1.0.0
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

//    public static String ACTION_STATS_EXPOSE = "com.admaster.broadcast.STATS_EXPOSE";
//    public static String ACTION_STATS_VIEWABILITY = "com.admaster.broadcast.STATS_VIEWABILITY";
//    public static String ACTION_STATS_SUCCESSED = "com.admaster.broadcast.STATS_SUCCESSED";

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
     * 设置是否打开log输出
     */
    public void setLogState(boolean isPrintOut) {
        Logger.DEBUG_LOG = isPrintOut;
    }

    /**
     * 初始化SDK
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
     * 启动定时器
     */
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
     * 公有方法，点击或爆光
     */
    public  void onClick(String adURL) {

        triggerEvent(EVENT_CLICK, adURL, null);
    }

    public  void onExpose(String adURL) {

        triggerEvent(EVENT_EXPOSE, adURL, null);
    }


    public  void onExpose(String adURL, View adView) {

        triggerEvent(EVENT_VIEWABILITY_EXPOSE, adURL, adView);
    }

    public  void onVideoExpose(String adURL, View videoView) {

        triggerEvent(EVENT_VIEWABILITY_VIDEOEXPOSE, adURL, videoView);
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

    public void onJSExpose(String adURL, View adView) {
        viewAbilityHandler.onJSExpose(adURL, adView, false);
    }

    public void onJSVideoExpose(String adURL, View adView) {
        viewAbilityHandler.onJSExpose(adURL, adView, true);
    }


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


    /**
     * 接收来自ViewAbilityService的回调,触发监测事件
     *
     * @param adURL
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
