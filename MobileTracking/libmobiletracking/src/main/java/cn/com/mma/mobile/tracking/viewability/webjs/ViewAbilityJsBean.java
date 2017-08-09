package cn.com.mma.mobile.tracking.viewability.webjs;

import android.content.Context;
import android.view.View;
import org.json.JSONObject;

import cn.com.mma.mobile.tracking.util.CommonUtil;

/**
 * Created by admaster on 17/7/28.
 */
public class ViewAbilityJsBean {

    private String adURL;
    private String adviewabilityId;
    private long timestamp;
    private View adView;
    private boolean isVideo;
    private boolean isCompleted;

    private static final String JSON_ADURL = "adurl";
    private static final String JSON_DEVICEMESSAGE = "deviceMessage";
    private static final String JSON_VIEWABILITYMESSAGE = "viewabilityMessage";
    private static final String JSON_ADVIEWABILITYID = "AdviewabilityID";

    public ViewAbilityJsBean(String adURL, View adView) {
        this.adURL = adURL;
        this.adView = adView;
        timestamp = System.currentTimeMillis();
        adviewabilityId = CommonUtil.md5(adURL + timestamp);
        isVideo = false;
        isCompleted = false;
    }

    public String getAdviewabilityId() {
        return adviewabilityId;
    }

    public View getAdView() {
        return adView;
    }


    public void setVideo(boolean video) {
        isVideo = video;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public JSONObject generateUploadEvents(Context context, boolean isEmpty) {

        JSONObject json = new JSONObject();

        try {
            json.put(JSON_ADURL, adURL);
            json.put(JSON_ADVIEWABILITYID, adviewabilityId);

            JSONObject viewability;
            //如果监测过程中断或者View被释放时,本次ViewAbility数据为空
            if (isEmpty || adView == null) {
                viewability = ViewAbilityMessage.getEmptyViewAbilityEvents(context);
            } else {
                viewability = ViewAbilityMessage.getViewAbilityEvents(context, adView);
            }
            //每次动态传入是否是Video视图
            viewability.put(ViewAbilityMessage.ADVIEWABILITY_TYPE, isVideo ? "1" : "0");

            json.put(JSON_VIEWABILITYMESSAGE, viewability);

            JSONObject device = DeviceMessage.getDeviceMessage(context);
            device.put(DeviceMessage.JSON_TS, String.valueOf(timestamp));
            json.put(JSON_DEVICEMESSAGE, device);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return json;
    }

    @Override
    public String toString() {
        return "[" + adviewabilityId + ",URL: " + adURL + " ,isVideo:" + isVideo + ",isCompleted:" + isCompleted + ",view:" + adView + "]";
    }
}
