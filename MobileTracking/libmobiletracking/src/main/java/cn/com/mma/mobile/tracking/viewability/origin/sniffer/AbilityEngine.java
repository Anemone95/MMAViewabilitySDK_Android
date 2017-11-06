package cn.com.mma.mobile.tracking.viewability.origin.sniffer;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.view.View;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import cn.com.mma.mobile.tracking.util.klog.KLog;
import cn.com.mma.mobile.tracking.viewability.origin.ViewAbilityEventListener;
import cn.com.mma.mobile.tracking.viewability.origin.ViewAbilityPresenter;
import cn.com.mma.mobile.tracking.viewability.origin.ViewAbilityService;
import cn.com.mma.mobile.tracking.viewability.origin.ViewAbilityStats;


/**
 * Created by yangxiaolong on 17/6/16.
 */
public class AbilityEngine implements ViewAbilityPresenter {
    private Context mContext;
    private AbilityHandler engineHandler = null;
    private static final int MESSAGE_ONEXPOSE = 0x102;

    public AbilityEngine(Context context, ViewAbilityEventListener eventListener, ViewAbilityConfig viewAbilityConfig) {
        mContext = context;
        HandlerThread handlerThread = new HandlerThread(AbilityEngine.class.getCanonicalName());
        handlerThread.setPriority(Process.THREAD_PRIORITY_BACKGROUND);
        handlerThread.start();

        engineHandler = new AbilityHandler(handlerThread.getLooper(), viewAbilityConfig, eventListener);
    }

    @Override
    public void addViewAbilityMonitor(Bundle bundle, View adView) {
        Message message = engineHandler.obtainMessage(MESSAGE_ONEXPOSE);
        message.obj = adView;
        message.setData(bundle);
        engineHandler.sendMessage(message);
    }


    private class AbilityHandler extends Handler {

        private final Lock mStartLock;
        private AbilityWorker abilityWorker;
        //private ViewAbilityConfig config;

        public AbilityHandler(Looper looper, ViewAbilityConfig sdkConfig, ViewAbilityEventListener eventListener) {
            super(looper);
            mStartLock = new ReentrantLock();

            //SDK的配置文件内容映射
            abilityWorker = new AbilityWorker(mContext, eventListener, sdkConfig);
        }

        @Override
        public void handleMessage(Message msg) {
            mStartLock.lock();
            try {
                final int what = msg.what;
                switch (what) {
                    case MESSAGE_ONEXPOSE:
                        View adView = (View) msg.obj;
                        Bundle bundle = msg.getData();
                        handlerViewAbilityMonitor(adView, bundle);
                        break;
                    default:
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                mStartLock.unlock();
            }
        }

        /**
         * 添加一个工作项目
         */
        private void handlerViewAbilityMonitor(View adView, Bundle bundle) {
            if (adView == null) {
                KLog.w("adView 已经被释放...");
                return;
            }
            String adURL = bundle.getString(ViewAbilityService.BUNDLE_ADURL);
            String impressionID = bundle.getString(ViewAbilityService.BUNDLE_IMPRESSIONID);
            String explorerID = bundle.getString(ViewAbilityService.BUNDLE_EXPLORERID);
            ViewAbilityStats result = (ViewAbilityStats) bundle.getSerializable(ViewAbilityService.BUNDLE_VBRESULT);

            abilityWorker.addWorker(adURL, adView, impressionID, explorerID, result);
        }
    }


}
