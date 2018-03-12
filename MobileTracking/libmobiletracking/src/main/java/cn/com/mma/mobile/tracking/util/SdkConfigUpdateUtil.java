package cn.com.mma.mobile.tracking.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import android.content.Context;
import android.net.ConnectivityManager;
import android.text.TextUtils;
import cn.com.mma.mobile.tracking.api.Constant;
import cn.com.mma.mobile.tracking.bean.SDK;

/**
 * config配置文件相关工具类
 */
public class SdkConfigUpdateUtil {

	private static SDK sdkConfig = null;
    private static String configURL = null;
    private static boolean isUpdating = false;


    /**
     * 在线更新SDKCOFIG配置
     * @param context
     * @param configUrl
     */
    public synchronized static void sync(final Context context, final String configUrl) {

        if (isUpdating) return;

        //如果达到更新周期且传入ConfigURL不为空,开启在线更新
        if (!TextUtils.isEmpty(configUrl) && checkNeedUpdate(context)) {

            configURL = configUrl;
            isUpdating = true;

            new Thread(new Runnable() {
                @Override
                public void run() {

                    try {
                        // natwork unavailable return
                        if (DeviceInfoUtil.isNetworkAvailable(context)) {
                            byte[] buffer = ConnectUtil.getInstance().performGet(configUrl);
                            if (buffer != null) {
                                sdkConfig = XmlUtil.doParser(new ByteArrayInputStream(buffer));
                                //如果可以成功解析为SDK实体类,并且存在Company配置,缓存原始XML数据
                                if (sdkConfig != null && sdkConfig.companies != null && sdkConfig.companies.size() > 0) {
                                    String response = new String(buffer);
                                    if (!TextUtils.isEmpty(response)) {
                                        SharedPreferencedUtil.putString(context,
                                                SharedPreferencedUtil.SP_NAME_CONFIG,
                                                SharedPreferencedUtil.SP_CONFIG_KEY_FILE, response);

                                        SharedPreferencedUtil.putLong(context,
                                                SharedPreferencedUtil.SP_NAME_OTHER,
                                                SharedPreferencedUtil.SP_OTHER_KEY_UPDATE_TIME,
                                                System.currentTimeMillis());
                                        Logger.d("Successful update sdkconfig files");
                                    }
                                    initOffLineCache(sdkConfig);
                                }
                            }

                        }

                    } catch (Exception e) {
                        Logger.w("Online update sdkconfig failed!:" + e.getMessage());
                    } finally {
                        isUpdating = false;
                    }

                }
            }).start();
        }

    }


    /**
     * 检查是否要远程更新配置文件
     * 当前 WIFI 环境，每天更新一次,2G/3G/4G 环境，每隔 3 天更新一次配置文件
     * @param context
     * @return
     */
	private static boolean checkNeedUpdate(Context context) {
        boolean result = true;
        try {
            long currentTime = System.currentTimeMillis();
            long lastUpdateTime = SharedPreferencedUtil.getLong(context, SharedPreferencedUtil.SP_NAME_OTHER,
                    SharedPreferencedUtil.SP_OTHER_KEY_UPDATE_TIME);

            if (currentTime < lastUpdateTime) {
                SharedPreferencedUtil.putLong(context, SharedPreferencedUtil.SP_NAME_OTHER,
                        SharedPreferencedUtil.SP_OTHER_KEY_UPDATE_TIME, currentTime);
                return false;
            }

            // 每天更新一次:时间间隔大于1天则更新
            boolean isWifiUpdate = (CommonUtil.isConnected(context, ConnectivityManager.TYPE_WIFI)
                    && (currentTime - lastUpdateTime >= Constant.TIME_ONE_DAY));

            // 每三天更新一次：时间间隔大于3天则更新
            boolean isMobileUpdate = (CommonUtil.isConnected(context, ConnectivityManager.TYPE_MOBILE)
                    && (currentTime - lastUpdateTime >= Constant.TIME_THREE_DAY));

            if (isWifiUpdate || isMobileUpdate) {
                result = true;
            } else {
                result = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //Logger.d("is need Update：" + result);
        return result;
    }

//	/**
//	 * 网络更新config.xml文件
//	 *
//	 * @param configUrl
//	 */
//	private static SDK doUpdate(Context context, String configUrl) {
//        // natwork unavailable return
//        if (!DeviceInfoUtil.isNetworkAvailable(context)) {
//            return null;
//        }
//        SDK sdk = null;
//
//        try {
//            byte[] buffer = ConnectUtil.getInstance().performGet(configUrl);
//            if (buffer != null) {
//                sdk = XmlUtil.doParser(new ByteArrayInputStream(buffer));
//                //如果可以成功解析为SDK实体类,并且存在Company配置,缓存原始XML数据
//                if (sdk != null && sdk.companies != null && sdk.companies.size() > 0) {
//                    String response = new String(buffer);
//                    if (!TextUtils.isEmpty(response)) {
//                        SharedPreferencedUtil.putString(context,
//                                SharedPreferencedUtil.SP_NAME_CONFIG,
//                                SharedPreferencedUtil.SP_CONFIG_KEY_FILE, response);
//
//                        SharedPreferencedUtil.putLong(context,
//                                SharedPreferencedUtil.SP_NAME_OTHER,
//                                SharedPreferencedUtil.SP_OTHER_KEY_UPDATE_TIME,
//                                System.currentTimeMillis());
//                        Logger.d("Successful update sdk_config files");
//                    }
//                }
//            }
//        } catch (Exception e) {
//            Logger.w("Online update sdk_config failed!:" + e.getMessage());
//        }
//
//        return sdk;
//    }

	/**
	 * 从SharedPreferences中获取sdkconfig.xml文件,转换成SDK对象
	 */
	private static SDK getSDKFromCache(Context context) {
        try {
            String cacheConfig = SharedPreferencedUtil.getString(context, SharedPreferencedUtil.SP_NAME_CONFIG, SharedPreferencedUtil.SP_CONFIG_KEY_FILE);
            InputStream is = null;
            SDK sdkcofig = null;
            try {
                //SP有缓存优先使用缓存
                if (!TextUtils.isEmpty(cacheConfig)) {
                    is = new ByteArrayInputStream(cacheConfig.getBytes());
                } else {//无缓存直接读取离线配置
                    is = context.getAssets().open(XmlUtil.XMLFILE);
                }
                if (is != null){
                    sdkcofig = XmlUtil.doParser(is);
                    initOffLineCache(sdkcofig);
                }
            } catch (Exception e) {
            } finally {
                if (is != null) {
                    is.close();
                }
            }
            return sdkcofig;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * 初始化缓存队列配置项:<offlineCache>
     * @param sdk
     */
    private static void initOffLineCache(SDK sdk) {
        try {
            if (sdk != null && sdk.offlineCache != null) {
                if (!TextUtils.isEmpty(sdk.offlineCache.length))
                    Constant.OFFLINECACHE_LENGTH = Integer.parseInt(sdk.offlineCache.length);
                if (!TextUtils.isEmpty(sdk.offlineCache.queueExpirationSecs))
                    Constant.OFFLINECACHE_QUEUEEXPIRATIONSECS = Integer.parseInt(sdk.offlineCache.queueExpirationSecs);
                if (!TextUtils.isEmpty(sdk.offlineCache.timeout))
                    Constant.OFFLINECACHE_TIMEOUT = Integer.parseInt(sdk.offlineCache.timeout);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 立即获取sdkconfig,如果因在线获取延迟,优先使用本地缓存配置数据
     * @param context
     * @return
     */
	public static SDK getSDKConfig(Context context) {
        if (sdkConfig == null || sdkConfig.companies == null) {
            //没有获取配置文件成功，则使用app内置配置文件
            sdkConfig = getSDKFromCache(context);
            //如果本地没有缓存,且有配远程更新地址,则立即在线获取
            if (sdkConfig == null && !TextUtils.isEmpty(configURL)) {
                sync(context, configURL);
            }
        }
        return sdkConfig;
    }

}
