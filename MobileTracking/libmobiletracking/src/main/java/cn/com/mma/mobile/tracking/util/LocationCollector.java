package cn.com.mma.mobile.tracking.util;

import android.Manifest;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import java.util.List;


/**
 * LBS 定位相关数据的采集管理
 */
public class LocationCollector {

    private  LocationManager locationManager;
    private static LocationCollector mInstance = null;
    private  Context mContext;
    private boolean isSynced;
    //private static final String TAG = "LocationCollector";

    /* 是否使用 位置变更监听器 */
    private static final boolean ENABLE_LOCATION_UPDATELISTENER = true;
    /* 位置更新最小时间间隔 单位毫秒 默认300s*/
    private static final long LOCATION_UPDATE_MINTIME = 300*1000l;
    /* 位置更新最小距离 单位m 默认100M*/
    private static final float LOCATION_UPDATE_MINDISTANCE = 0f;

    /* 位置更新时间间隔 单位毫秒 120s*/
    private static final long UPDATES_INTERVAL = 120 * 1000l;
    /* 移除更新时间 单位毫秒 20s*/
    private static final long REMOVE_INTERVAL = 20 * 1000l;


    /* 上一次成功获取位置时的时间*/
    private long lastLocationTime = 0;

    private Location currentLocation;
    private static  Handler mHandler;



    private LocationCollector(Context context) {
        mContext = context;
        isSynced = false;
        currentLocation = null;
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        mHandler = new Handler(Looper.getMainLooper());
    }

    public static LocationCollector getInstance(Context context) {
        if (mInstance == null) {
            synchronized (LocationCollector.class) {
                if (mInstance == null) {
                    mInstance = new LocationCollector(context);
                }
            }
        }
        return mInstance;
    }





    public String getLocation() {
        if (currentLocation == null) {
            //Log.w(TAG, "Location is empty,return null,start get syncLocation");
            syncLocation();
            return "";
        } else {

            long currentTime = System.currentTimeMillis();
            if ((currentTime - lastLocationTime) > UPDATES_INTERVAL) {
                //Log.i(TAG, "Location is expired,start update syncLocation");
                //更新位置
                syncLocation();
            }

            StringBuffer location = new StringBuffer();
            try {
                location.append(currentLocation.getLatitude());
                location.append("x");
                location.append(currentLocation.getLongitude());
                location.append("x");
                location.append(currentLocation.getAccuracy());
            } catch (Exception e) {
            }

            return location.toString();//String.format("%fx%fx%f", latitude, longitude, accuracy);
        }

    }



    /**
     * 注册Location Update
     */
    public void syncLocation() {
        try {

            //require ACCESS_FINE_LOCATION and ACCESS_FINE_LOCATION
            if (Reflection.checkPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) && Reflection.checkPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                //long start = System.currentTimeMillis();
                //获取所有可用的位置提供器
                List<String> providers = locationManager.getProviders(true);
                String locationProvider = null;
                Location location = null;

                if (providers.contains(LocationManager.GPS_PROVIDER)) {
                    //如果是GPS
                    locationProvider = LocationManager.GPS_PROVIDER;
                    location = locationManager.getLastKnownLocation(locationProvider);
                }

                if (location == null && providers.contains(LocationManager.NETWORK_PROVIDER)) {
                    locationProvider = LocationManager.NETWORK_PROVIDER;
                    location = locationManager.getLastKnownLocation(locationProvider);
                }

                if (TextUtils.isEmpty(locationProvider)) {
                    //Log.w(TAG, "no available Location Provider!");
                    return;
                }

               //Log.d(TAG, "locationProvider:" + locationProvider + "  LastKnownLocation is :" + location);

                if (location != null) {
                    //不为空,显示地理位置经纬度
                    //long end = System.currentTimeMillis();
                    //String cost = String.valueOf(end - start) + " ms";
                    //Log.d(TAG, "cost:" + cost + " lat:" + location.getLatitude() + "  lon:" + location.getLongitude() + "  acc:" + location.getAccuracy() + "   time:" + location.getTime());
                    currentLocation = location;
                    lastLocationTime = System.currentTimeMillis();
                }

                if (ENABLE_LOCATION_UPDATELISTENER && !isSynced) {

                    //Logger.e("request Location Updates:" + lastLocationTime + ",mintime:" + LOCATION_UPDATE_MINTIME + ",distance:" + LOCATION_UPDATE_MINDISTANCE);

                    final String provider = locationProvider;

                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            locationManager.requestLocationUpdates(provider, LOCATION_UPDATE_MINTIME, LOCATION_UPDATE_MINDISTANCE, locationListener);
                        }
                    });

                    isSynced = true;
                    //10ms 后停止更新Location
                    mHandler.postDelayed(stopListener, REMOVE_INTERVAL);
                }

            }


        } catch (Exception e) {
            isSynced = false;
        }

    }



    private final Runnable stopListener = new Runnable() {
        @Override
        public void run() {
            //Log.e(TAG, "remove Location Updates:" + lastLocationTime);
            locationManager.removeUpdates(locationListener);
            isSynced = false;
            mHandler.removeCallbacks(stopListener);
        }
    };


    private LocationListener locationListener = new LocationListener() {

        @Override
        public void onLocationChanged(Location location) {
            //Log.i(TAG, "onLocationChanged:" + location + "    t:" + location.getTime());
            //位置发生变化
            lastLocationTime = System.currentTimeMillis();
            currentLocation = location;

        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {
            //Location Provider状态改变
            //Log.i(TAG, "onStatusChanged:" + s + " i:" + i);
        }

        @Override
        public void onProviderEnabled(String s) {
            //Provider可用
            //Log.i(TAG, "onProviderEnabled:" + s);
        }

        @Override
        public void onProviderDisabled(String s) {
            //Provider不可用
            //Log.i(TAG, "onProviderDisabled:" + s);
        }
    };

    /**
     * 停止监听位置信息
     */
    public void stopSyncLocation() {
        if (locationManager != null) {
            locationManager.removeUpdates(locationListener);
            mHandler.removeCallbacks(stopListener);
        }
    }




}
