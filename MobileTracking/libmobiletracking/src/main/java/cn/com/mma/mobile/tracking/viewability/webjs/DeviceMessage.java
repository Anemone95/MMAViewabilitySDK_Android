package cn.com.mma.mobile.tracking.viewability.webjs;

import android.content.Context;
import android.text.TextUtils;
import org.json.JSONObject;
import cn.com.mma.mobile.tracking.api.Constant;
import cn.com.mma.mobile.tracking.util.CommonUtil;
import cn.com.mma.mobile.tracking.util.DeviceInfoUtil;

/**
 * Created by mma on 17/7/30.
 */
public class DeviceMessage {


    private static JSONObject deviceMessage;
    private static final String JSON_OS = "os";//0: Android; 1: iOS; 2: WP; 3: other
    public static final String JSON_TS = "ts";
    private static final String JSON_MAC = "mac";
    private static final String JSON_IMEI = "imei";
    private static final String JSON_ANDROIDID = "androidid";
    private static final String JSON_WIFI = "wifi";
    private static final String JSON_AKEY = "akey";//包名
    private static final String JSON_ANAME = "aname";//app名称
    private static final String JSON_SCWH = "scwh";
    private static final String JSON_TERM = "term";
    private static final String JSON_OSVS = "osvs";
    //private static final String JSON_LBS = "lbs";
    private static final String JSON_SDKV = "sdkv";


    public static JSONObject getDeviceMessage(Context context) {

        if (deviceMessage == null) {
            deviceMessage = new JSONObject();
            try {

                deviceMessage.put(JSON_OS, "0");

                String mac = DeviceInfoUtil.getMacAddress(context);
                if (!TextUtils.isEmpty(mac)) mac = mac.replaceAll(":", "").toUpperCase();

                deviceMessage.put(JSON_MAC, CommonUtil.md5(mac));

                deviceMessage.put(JSON_IMEI, DeviceInfoUtil.getImei(context));

                deviceMessage.put(JSON_ANDROIDID, DeviceInfoUtil.getAndroidId(context));

                deviceMessage.put(JSON_WIFI, DeviceInfoUtil.isWifi(context));

                deviceMessage.put(JSON_AKEY, DeviceInfoUtil.getPackageName(context));//AKEY=packagename
                deviceMessage.put(JSON_ANAME, DeviceInfoUtil.getAppName(context));//ANAME=appname

                deviceMessage.put(JSON_SCWH, DeviceInfoUtil.getResolution(context));

                deviceMessage.put(JSON_TERM, DeviceInfoUtil.getDevice());

                deviceMessage.put(JSON_OSVS, DeviceInfoUtil.getOSVersion());

                //deviceMessage.put(JSON_LBS, "");

                deviceMessage.put(JSON_SDKV, Constant.TRACKING_SDKVS_VALUE);//跟随SDK

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return deviceMessage;
    }




}
