package cn.com.mma.mobile.tracking.util;

import android.content.Context;
import android.text.TextUtils;
import android.util.Base64;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.Map;
import cn.com.mma.mobile.tracking.api.Constant;
import cn.com.mma.mobile.tracking.bean.Applist;
import cn.com.mma.mobile.tracking.bean.Company;



/**
 * Created by mmaChinaSDK on 17/11/22.
 */
public class AppListUploader {


    private static AppListUploader mInstance = null;
    private static boolean isUploading;
    private static Context mContext;
    private static final String JSON_MAC = "mac";
    private static final String JSON_IMEI = "imei";
    private static final String JSON_ANDROIDID = "androidid";
    private static final String JSON_TIME = "time";
    private static final String JSON_APPLIST = "applist";



    private AppListUploader(Context context) {
        mContext = context;
    }

    public static AppListUploader getInstance(Context context) {
        if (mInstance == null) {
            synchronized (AppListUploader.class) {
                if (mInstance == null) {
                    mInstance = new AppListUploader(context);
                }
            }
        }
        return mInstance;
    }


    public synchronized void sync(String adURL,Company company) {
        if (isUploading) return;
        checkIsNeedUpload(adURL, company);
    }


    /**
     * 每次触发监测事件时都会检查一下是否需要上报APPLIST
     * 上报条件:sdkconfig有配,且 uploadTime > 0
     *
     * @param company
     */
    private void checkIsNeedUpload(String adURL,Company company) {

        Applist applistConfig = company.applist;
        if (applistConfig == null) return;

        if (!TextUtils.isEmpty(applistConfig.uploadUrl) && applistConfig.uploadTime > 0) {

            //上一次上报时间=company+identifier
            final String spName = company.domain.url + SharedPreferencedUtil.SP_OTHER_KEY_LASTUPLOADTIME_SUFFIX;

            long lastuploadTime = SharedPreferencedUtil.getLong(mContext, SharedPreferencedUtil.SP_NAME_OTHER, spName);

            long freq = applistConfig.uploadTime * 60 * 60;//转化成秒

            final long currentTime = System.currentTimeMillis() / 1000;

            if (currentTime > (lastuploadTime + freq)) {

                String configURL;

                isUploading = true;

                if (applistConfig.uploadUrl.startsWith("https://") || applistConfig.uploadUrl.startsWith("http://")) {
                    configURL = applistConfig.uploadUrl;
                } else {
                    try {
                        URL exposeURL = new URL(adURL);
                        configURL = exposeURL.getProtocol() + "://" + exposeURL.getHost() + applistConfig.uploadUrl;
                    } catch (Exception e) {
                        configURL = "http://" + company.domain.url + applistConfig.uploadUrl;
                    }
                }

                final String uploadURL = configURL;

                new Thread(new Runnable() {
                    @Override
                    public void run() {

                        try {

                            JSONObject json = new JSONObject();
                            JSONArray applist = DeviceInfoUtil.getApplist(mContext);
                            Map<String, String> deviceParams = DeviceInfoUtil.getDeviceInfo(mContext);

                            json.put(JSON_TIME, String.valueOf(System.currentTimeMillis()));
                            json.put(JSON_MAC, CommonUtil.md5(deviceParams.get(Constant.TRACKING_MAC)));
                            json.put(JSON_IMEI, CommonUtil.md5(deviceParams.get(Constant.TRACKING_IMEI)));
                            json.put(JSON_ANDROIDID, CommonUtil.md5(deviceParams.get(Constant.TRACKING_ANDROIDID)));
                            json.put(JSON_APPLIST, applist);

                            String encodeData;
                            try {
                                encodeData = Base64.encodeToString(json.toString().getBytes("utf-8"), Base64.NO_WRAP);
                            } catch (UnsupportedEncodingException e) {
                                encodeData = json.toString();
                            }
                            if (DeviceInfoUtil.isNetworkAvailable(mContext)) {

                                byte[] response = ConnectUtil.getInstance().performPost(uploadURL, encodeData);
                                if (response != null) {
                                    //update lastuploadtime
                                    SharedPreferencedUtil.putLong(mContext, SharedPreferencedUtil.SP_NAME_OTHER, spName, currentTime);
                                }
                            }

                        } catch (JSONException e) {
                        } finally {
                            isUploading = false;
                        }

                    }
                }).start();


            }

        }

    }

}
