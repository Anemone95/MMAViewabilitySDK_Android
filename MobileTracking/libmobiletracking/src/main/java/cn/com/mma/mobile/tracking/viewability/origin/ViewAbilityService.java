package cn.com.mma.mobile.tracking.viewability.origin;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import java.lang.ref.WeakReference;
import cn.com.mma.mobile.tracking.util.Logger;
import cn.com.mma.mobile.tracking.util.klog.KLog;
import cn.com.mma.mobile.tracking.viewability.origin.sniffer.AbilityEngine;
import cn.com.mma.mobile.tracking.viewability.origin.sniffer.ViewAbilityConfig;


/**
 * Created by mma on 17/6/15.
 */
public class ViewAbilityService {


    /*用于存储impressionID的字典*/
    private ViewAbilityPresenter presenter = null;

    public static final String BUNDLE_ADURL = "adurl";
    public static final String BUNDLE_IMPRESSIONID = "impressionId";
    public static final String BUNDLE_EXPLORERID = "explorerID";
    public static final String BUNDLE_VBRESULT = "vbresult";

    //内部流程输出开关 release模式设置为FALSE
    public static boolean LOCAL_DEBUG = true;

    /**
     * 内部日志输出开关，0为线上环境，1为测试环境
     */
    private static int LOG_SWITCH = 1;

    static {
        switch (LOG_SWITCH) {
            case 0:
                LOCAL_DEBUG = false;
                break;
            case 1:
                LOCAL_DEBUG = true;
                break;
        }
    }

    public static final String LOCAL_TAG = "ViewAbilityService";


    public ViewAbilityService(Context context, ViewAbilityEventListener viewAbilityEventListener, ViewAbilityConfig viewAbilityConfig) {
        //设置内部LOG输出
        KLog.init(LOCAL_DEBUG, LOCAL_TAG);
        presenter = new AbilityEngine(context, viewAbilityEventListener, viewAbilityConfig);
    }

    public void addViewAbilityMonitor(String adURL, View view, String impressionID, String explorerID, ViewAbilityStats result) {
        WeakReference<View> weakReference = new WeakReference<>(view);
        View adView = weakReference.get();
        if (adView != null) {
            Bundle bundle = new Bundle();
            bundle.putString(BUNDLE_ADURL, adURL);
            bundle.putString(BUNDLE_IMPRESSIONID, impressionID);
            bundle.putString(BUNDLE_EXPLORERID, explorerID);
            bundle.putSerializable(BUNDLE_VBRESULT, result);
            presenter.addViewAbilityMonitor(bundle, adView);
            Logger.d("URL:" + adURL + " 开启View Ability 监测->");
        }
    }

}
