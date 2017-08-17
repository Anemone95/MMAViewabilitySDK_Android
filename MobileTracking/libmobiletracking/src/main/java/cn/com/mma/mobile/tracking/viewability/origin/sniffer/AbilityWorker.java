package cn.com.mma.mobile.tracking.viewability.origin.sniffer;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import cn.com.mma.mobile.tracking.api.Countly;
import cn.com.mma.mobile.tracking.util.klog.KLog;
import cn.com.mma.mobile.tracking.viewability.origin.ViewAbilityEventListener;
import cn.com.mma.mobile.tracking.viewability.origin.ViewAbilityStatsResult;


/**
 * Created by yangxiaolong on 17/6/16.
 */
public class AbilityWorker implements AbilityCallback {


    private ScheduledExecutorService scheduledExecutorService;
    private Context mContext;
    private long inspectIntervial;//200ms
    private ScheduledFuture<?> scheduledFuture = null;
    private int cacheIndex = 0;
    private static final int CACHEINDEX_AMOUNT = 10;
    private ViewAbilityConfig config;
    private HashMap<String, ViewAbilityExplorer> explorers;
    private ViewAbilityEventListener mmaSdk;
    private StoreManager mCacheManager;


    public AbilityWorker(Context context, ViewAbilityEventListener eventListener, ViewAbilityConfig config) {
        mContext = context;
        this.config = config;
        this.mmaSdk = eventListener;
        scheduledExecutorService = Executors.newScheduledThreadPool(1);
        inspectIntervial = config.getInspectInterval();

        KLog.w("********************************************");
        KLog.i("AbilityWorker Constructor on Thread:" + "[" + Thread.currentThread().getId() + "]");
        KLog.i("inspect interival:" + inspectIntervial);
        KLog.i("exposeValidDuration:" + config.getExposeValidDuration());
        KLog.i("MaxDuration:" + config.getMaxDuration());
        KLog.i("coverRate scale:" + config.getCoverRateScale());
        KLog.i("MaxUploadAmount:" + config.getMaxUploadAmount());
        KLog.w("********************************************");

        explorers = new HashMap<>();
        mCacheManager = new StoreManager(context);

        activeWorkflow();

        loadAndUploadCachedWorks();

    }

    /**
     * 线程池开启定时任务
     */
    private void activeWorkflow() {
        try {
            scheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(new WorkTask(), 0, inspectIntervial, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    private void stopWorkflow() {
//
//        if (scheduledFuture != null) {
//            scheduledFuture.cancel(true);
//        }
//    }

    /**
     * 每次初始化时,从文件读取并上传上次未完成的数据
     */
    private void loadAndUploadCachedWorks() {
        try {
            //读出所有的Explorer对象,直接上报
            List<ViewAbilityExplorer> cachelists = mCacheManager.getAll();
            if (cachelists != null) {
                for (ViewAbilityExplorer cacheExplorer : cachelists) {
                    KLog.i("load cache explore item:" + cacheExplorer.toString());
                    cacheExplorer.breakToUpload(this);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 添加一个工作者
     */
    public void addWorker(String adURL, View adView, String impressionID, String adAreaID, ViewAbilityStatsResult result) {

        try {
            ViewAbilityExplorer existExplore = explorers.get(adAreaID);
            KLog.d("addWorker->ID:" + adAreaID + " existExplore:" + existExplore + "  url:" + adURL + "  adView" + adView);
            //当前Work池内已存在 停止并上报 使用新的监测覆盖
            if (existExplore != null) {
                KLog.w("当前广告位:" + adAreaID + " 已经存在,停止监测并UPLOAD,当前任务重新开启!");
                existExplore.breakToUpload();
                //explorers.remove(existExplore);
                explorers.remove(adAreaID);
            }

            ViewAbilityExplorer normalExplorer = new ViewAbilityExplorer(adAreaID, adURL, adView, impressionID, config);
            //收集完数据回调到本类内使用mmasdk发送最终监测URL
            normalExplorer.setAbilityCallback(this);
            normalExplorer.setViewAbilityStatsResult(result);
            explorers.put(adAreaID, normalExplorer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onViewAbilityFinished(final String adAreaID, final String abilityURL) {

        new Thread(new Runnable() {
            @Override
            public void run() {

                //[LOCALTEST] 测试计数:带ViewAbility曝光事件产生计数
                if (Countly.LOCAL_TEST) {
                    Intent intent = new Intent(Countly.ACTION_STATS_VIEWABILITY);
                    mContext.sendBroadcast(intent);
                }

                mmaSdk.onEventPresent(abilityURL);
                KLog.w(",ID:" + adAreaID + "监测完成,移除对应的数据");
                //T每次成功上报数据后,都删除CACHE内impressionID对应的Data
                mCacheManager.removeObject(adAreaID);

            }
        }).start();
    }


    /**
     * 定时器,每隔
     */
    private final class WorkTask extends TimerTask {

        @Override
        public void run() {
            try {
                long start = System.currentTimeMillis();
                int len = explorers.size();
                if (len == 0) return;

                //用来存放失效或者已完成的序列数组
                List<String> invalidExplorers = new ArrayList<>();
                KLog.v("<-----------------------------Time Line Begin" + " [" + Thread.currentThread().getId() + "]" + "--------------------------------------------------->");
                for (String adAreaID : explorers.keySet()) {

                    ViewAbilityExplorer explorer = explorers.get(adAreaID);

                    AbilityStatus abilityStatus = explorer.getAbilityStatus();

                    if (abilityStatus == AbilityStatus.UPLOADED) {
                        //已经上传过,移除到invidious
                        invalidExplorers.add(adAreaID);
                    } else if (abilityStatus == AbilityStatus.EXPLORERING) {
                        //准备开启
                        explorer.onExplore(mContext);
                    }
                }

                //遍历完毕移除已经完成或失效的工作者
                for (String adAreaID : invalidExplorers) {
                    explorers.remove(adAreaID);
                }

                //缓存当前数据
                if (cacheIndex > CACHEINDEX_AMOUNT) {
                    cacheWorks();
                }

                long end = System.currentTimeMillis();
                long cost = end - start;
                cacheIndex++;

                KLog.d("index:" + cacheIndex + " cost:" + cost + "ms, workExplorers length:" + len);
                KLog.v("<-----------------------------Time Line end" + " [" + Thread.currentThread().getId() + "]" + "--------------------------------------------------->");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        /**
         * 每隔一定时间,把当前ViewAbilityExplorer内的数据以impression标识为ID序列化存储
         */
        private void cacheWorks() {
            try {
                for (String adAreaID : explorers.keySet()) {
                    ViewAbilityExplorer explorer = explorers.get(adAreaID);
                    KLog.i("cache explorer index:" + cacheIndex + "   item:" + explorer.toString());
                    mCacheManager.setObject(adAreaID, explorer);
                }
                cacheIndex = 0;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

}
