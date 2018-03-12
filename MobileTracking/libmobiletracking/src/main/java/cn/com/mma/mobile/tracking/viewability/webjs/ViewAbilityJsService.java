package cn.com.mma.mobile.tracking.viewability.webjs;

import android.content.Context;
import android.view.View;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import cn.com.mma.mobile.tracking.bean.Company;
import cn.com.mma.mobile.tracking.bean.SDK;
import cn.com.mma.mobile.tracking.util.Logger;


/**
 * Created by yangxiaolong on 17/7/27.
 */
public class ViewAbilityJsService {


    private Context mContext;
    //private SDK mSdkConfig;
    private ScheduledExecutorService scheduledExecutorService = null;
    private boolean isTaskStarted = false;

    // String = CompanyName,ViewAbilityJsExplorer = WebView
    private HashMap<String, ViewAbilityJsExplorer> viewabilityWorkers;
    private static final int monitorInterval = 200;

    public ViewAbilityJsService(Context context) {
        mContext = context;
        //mSdkConfig = sdk;
        //KLog.init(true, "ViewAbilityJS");
        viewabilityWorkers = new HashMap<>();
        scheduledExecutorService = Executors.newScheduledThreadPool(1);
        //每次初始化清理3天前来自js存储的数据
        DataCacheManager.getInstance(mContext).clearnExpiredData();
        isTaskStarted = false;

    }


    public void addTrack(String adURL, View view, Company company, boolean isVideo) {

        synchronized (ViewAbilityJsService.class) {

            //延迟启动定时器
            if (!isTaskStarted) {
                scheduledExecutorService.scheduleWithFixedDelay(new MonitorTask(), 0, monitorInterval, TimeUnit.MILLISECONDS);
                isTaskStarted = true;
            }

            //使用弱引用adView
            View adView = null;
            if (view != null) {
                WeakReference<View> weakReference = new WeakReference<>(view);
                adView = weakReference.get();
            }
            if (adView == null) Logger.w("the adView is null.");

            ViewAbilityJsExplorer existExplorer = viewabilityWorkers.get(company.name);

            //KLog.d("addTrack->Company.name:" + company.name + "  adURL:" + adURL + "  existExplorer:" + existExplorer + "  adView" + adView);

            if (existExplorer == null) {
                existExplorer = new ViewAbilityJsExplorer(mContext, company);
                viewabilityWorkers.put(company.name, existExplorer);
            }

            //如果存在该公司的监测,则直接添加到子Task内
            existExplorer.addExplorerTask(adURL, adView, isVideo);

        }
    }


//    public void removeTrack(String adURL, Company company) {
//        viewabilityWorkers.remove(adURL);
//    }


    /**
     * 全局定时器,无论多少Company多少ADURL,全局使用一个定时器
     */
    private final class MonitorTask extends TimerTask {

        @Override
        public void run() {

            try {
                for (String companyName : viewabilityWorkers.keySet()) {

                    ViewAbilityJsExplorer explorer = viewabilityWorkers.get(companyName);
                    explorer.onExplore();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }


}
