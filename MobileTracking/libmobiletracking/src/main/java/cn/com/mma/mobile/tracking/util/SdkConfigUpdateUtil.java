package cn.com.mma.mobile.tracking.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import android.content.Context;
import android.net.ConnectivityManager;
import android.text.TextUtils;
import cn.com.mma.mobile.tracking.api.Constant;
import cn.com.mma.mobile.tracking.bean.SDK;

/**
 * config配置文件相关工具类
 */
public class SdkConfigUpdateUtil {
	private static SDK sdk = null;

	/**
	 * 初始化Sdk配置文件
	 */
	public static void initSdkConfigResult(final Context context,
			final String configUrl) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				sdk = getNewestSDK(context, configUrl);
				if (sdk != null) {
					setSdk(sdk);
				}
			}
		}).start();
	}

	/**
	 * 获取最新的sdk文件
	 */
	private static SDK getNewestSDK(Context context, String configUrl) {
		SDK sdkNewest;
		if (JudgeUpdateAccordingDate(context) && !TextUtils.isEmpty(configUrl)) {
            sdkNewest = dealUpdateConfig(context, configUrl);
            if (sdkNewest == null) {
                sdkNewest = getSDKFromPreferences(context);
            }
        } else {
			sdkNewest = getSDKFromPreferences(context);
			if (sdkNewest == null) {
				sdkNewest = dealUpdateConfig(context, configUrl);
			}
		}
		return sdkNewest;
	}

	/**
	 * 如果当前是 wifi 环境，每天更新一次配置文件，如果当前是 2G / 3G 环境，每隔 3 天更新一次配置文件
	 */
	private static boolean JudgeUpdateAccordingDate(Context context) {
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

	/**
	 * 网络更新config.xml文件后的
	 * 
	 * @param configUrl
	 */
	public static SDK dealUpdateConfig(Context context, String configUrl) {
        if (!DeviceInfoUtil.isNetworkAvailable(context)) {
            return null;
        }
        SDK sdk = null;

        try {
            byte[] buffer = ConnectUtil.getInstance().performGet(configUrl);
            if (buffer != null) {
                sdk = XmlUtil.doParser(new ByteArrayInputStream(buffer));
                if (sdk != null && sdk.companies != null && sdk.companies.size() > 0) {
                    String response = new String(buffer);
                    if (!TextUtils.isEmpty(response)) {
                        SharedPreferencedUtil.putString(context,
                                SharedPreferencedUtil.SP_NAME_CONFIG,
                                SharedPreferencedUtil.SP_CONFIG_KEY_FILE, response);

                        SharedPreferencedUtil.putLong(context,
                                SharedPreferencedUtil.SP_NAME_OTHER,
                                SharedPreferencedUtil.SP_OTHER_KEY_UPDATE_TIME,
                                System.currentTimeMillis());
                        Logger.d("mma_网络更新sdkconfig.xml成功");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return sdk;
    }

	/**
	 * 从SharedPreferences中获取sdkconfig.xml文件,转换成SDK对象
	 */
	public static SDK getSDKFromPreferences(Context context) {
		try {
			String valueString = SharedPreferencedUtil.getString(context,
					SharedPreferencedUtil.SP_NAME_CONFIG,
					SharedPreferencedUtil.SP_CONFIG_KEY_FILE);
			InputStream is = null;
			try {
				if (valueString != null && valueString.length() > 0) {
					is = new ByteArrayInputStream(valueString.getBytes());
				} else {
					is = context.getAssets().open(XmlUtil.XMLFILE);
				}
			} catch (Exception e) {
				is = null;
			}
			return is != null ? XmlUtil.doParser(is) : null;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * 网络请求sdkconfig.xml文件
	 */
	private static String getConfigFromNetWork(String configUrl) {
		if (configUrl == null)
			return null;
		try {
			URL url = new URL(configUrl);
			HttpURLConnection urlConnection = (HttpURLConnection) url
					.openConnection();
			urlConnection.setConnectTimeout(10000);
			urlConnection.connect();
			InputStream inputStream = urlConnection.getInputStream();
			BufferedReader in = new BufferedReader(new InputStreamReader(
					inputStream));
			StringBuffer buffer = new StringBuffer();
			String line = "";
			while ((line = in.readLine()) != null) {
				buffer.append(line);
			}
			inputStream.close();
			return buffer.toString();
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * 设置SDK
	 */
	private static void setSdk(SDK sdk) {
		Logger.d("mma_setSdk");
		if (sdk != null) {
			try {
				if(sdk.offlineCache!=null){
				if (sdk.offlineCache.length != null
						&& !"".equals(sdk.offlineCache.length))
					Constant.OFFLINECACHE_LENGTH = Integer
							.parseInt(sdk.offlineCache.length);
				if (sdk.offlineCache.queueExpirationSecs != null
						&& !"".equals(sdk.offlineCache.queueExpirationSecs))
					Constant.OFFLINECACHE_QUEUEEXPIRATIONSECS = Integer
							.parseInt(sdk.offlineCache.queueExpirationSecs);
				if (sdk.offlineCache.timeout != null
						&& !"".equals(sdk.offlineCache.timeout))
					Constant.OFFLINECACHE_TIMEOUT = Integer
							.parseInt(sdk.offlineCache.timeout);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	public static SDK getSdk(final Context context) {
        if (sdk == null || sdk.companies == null) {
            //[子线程操作]没有获取配置文件成功，则使用app内置配置文件
            sdk = getSDKFromPreferences(context);
            setSdk(sdk);
        }
        return sdk;
    }

}
