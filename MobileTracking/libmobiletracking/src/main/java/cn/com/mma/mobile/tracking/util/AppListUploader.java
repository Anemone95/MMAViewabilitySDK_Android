package cn.com.mma.mobile.tracking.util;

import android.content.Context;
import android.text.TextUtils;
import android.util.Base64;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.UnsupportedEncodingException;
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
    private static Map<String, String> deviceParams = null;
    private static final String JSON_MAC = "mac";
    private static final String JSON_IMEI = "imei";
    private static final String JSON_ANDROIDID = "androidid";
    private static final String JSON_TIME = "time";
    private static final String JSON_APPLIST = "applist";



    private AppListUploader(Context context, Map<String, String> params) {
        mContext = context;
        deviceParams = params;
    }

    public static AppListUploader getInstance(Context context, Map<String, String> params) {
        if (mInstance == null) {
            synchronized (AppListUploader.class) {
                if (mInstance == null) {
                    mInstance = new AppListUploader(context, params);
                }
            }
        }
        return mInstance;
    }


    public synchronized void sync(Company company) {
        if (isUploading) return;
        checkApplistIsUploaded(company);
    }


    /**
     * 每次触发监测事件时都会检查一下是否需要上报APPLIST
     * 上报条件:sdkconfig有配,且 uploadTime > 0
     *
     * @param company
     */
    private void checkApplistIsUploaded(Company company) {

        Applist applistConfig = company.applist;
        if (applistConfig == null) return;

        isUploading = true;

        if (!TextUtils.isEmpty(applistConfig.uploadUrl) && applistConfig.uploadTime > 0) {

            //上一次上报时间=company+identifier
            final String spName = company.domain.url + SharedPreferencedUtil.SP_OTHER_KEY_LASTUPLOADTIME_SUFFIX;

            long lastuploadTime = SharedPreferencedUtil.getLong(mContext, SharedPreferencedUtil.SP_NAME_OTHER, spName);

            long freq = applistConfig.uploadTime * 60 * 60;//转化成秒

            final long currentTime = System.currentTimeMillis() / 1000;

            if (currentTime > (lastuploadTime + freq)) {

                final String uploadURL;
                if (applistConfig.uploadUrl.startsWith("https://") || applistConfig.uploadUrl.startsWith("http://")) {
                    uploadURL = applistConfig.uploadUrl;
                } else {
                    uploadURL = "http://" + company.domain.url + applistConfig.uploadUrl;
                }

                new Thread(new Runnable() {
                    @Override
                    public void run() {

                        try {

                            JSONObject json = new JSONObject();
                            JSONArray applist = DeviceInfoUtil.getApplist(mContext);
                            json.put(JSON_TIME, String.valueOf(System.currentTimeMillis()));
                            json.put(JSON_MAC, CommonUtil.md5(deviceParams.get(Constant.TRACKING_MAC)));
                            json.put(JSON_IMEI, CommonUtil.md5(deviceParams.get(Constant.TRACKING_IMEI)));
                            json.put(JSON_ANDROIDID, CommonUtil.md5(deviceParams.get(Constant.TRACKING_ANDROIDID)));
                            json.put(JSON_APPLIST, applist);

                            String encodeData;
                            try {
                                encodeData = Base64.encodeToString(json.toString().getBytes("utf-8"), Base64.NO_WRAP);
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                                encodeData = applist.toString();
                            }
                            if (DeviceInfoUtil.isNetworkAvailable(mContext)) {
                                byte[] response = ConnectUtil.getInstance().performPost(uploadURL, encodeData);
                                if (response != null) {
                                    //update lastuploadtime
                                    SharedPreferencedUtil.putLong(mContext, SharedPreferencedUtil.SP_NAME_OTHER, spName, currentTime);
                                }
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }finally {
                            isUploading = false;
                        }

                    }
                }).start();


            }

        }

    }

}
