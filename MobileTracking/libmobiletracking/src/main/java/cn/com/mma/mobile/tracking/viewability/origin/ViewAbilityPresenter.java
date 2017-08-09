package cn.com.mma.mobile.tracking.viewability.origin;

import android.os.Bundle;
import android.view.View;

/**
 * Created by yangxiaolong on 17/6/16.
 */
public interface ViewAbilityPresenter {

    //void addViewAbilityMonitor(String adURL, View adView,String impressionID,String adAreaID);
    void addViewAbilityMonitor(Bundle bundle, View adView);

}
