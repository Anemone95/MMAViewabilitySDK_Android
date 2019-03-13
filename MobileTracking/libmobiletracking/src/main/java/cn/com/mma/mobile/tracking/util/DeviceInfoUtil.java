package cn.com.mma.mobile.tracking.util;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import android.Manifest;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.provider.Settings.Secure;
import android.support.annotation.RequiresPermission;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import org.json.JSONArray;
import cn.com.mma.mobile.tracking.api.Constant;

/**
 * 获得设备信息
 *
 * @author lincoln
 *
 */
public class DeviceInfoUtil {

	private final static String SHA1_ALGORITHM = "SHA-1";
	private final static String CHAR_SET = "iso-8859-1";

	/**
	 * 获得系统版本
	 * 
	 * @return
	 */
	public static String getOSVersion() {
		try {
			return Build.VERSION.RELEASE;
		} catch (Exception e) {
			return "";
		}
	}

	/**
	 * 设备的名字
	 * 
	 * @return
	 */
	public static String getDevice() {
		try {
			return Build.MODEL;
		} catch (Exception e) {
			return "";
		}
	}

	/**
	 * wifiSSID
	 *
	 * @return
	 */
    @RequiresPermission(Manifest.permission.ACCESS_WIFI_STATE)
	public static String getWifiSSID(Context context) {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = cm.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

                WifiInfo wifiInfo = wm.getConnectionInfo();

                if (wifiInfo != null && !TextUtils.isEmpty(wifiInfo.getSSID())) {
                    String ssid = wifiInfo.getSSID().trim();
                    if (ssid.startsWith("\"")) ssid = ssid.substring(1);
                    if (ssid.endsWith("\"")) ssid = ssid.substring(0, ssid.length() - 1);
                    return ssid;
                }
            }
        } catch (Exception e) {

        }
        return "";
    }


    /**
     *
     * @param context
     * @return
     */
    @RequiresPermission(Manifest.permission.ACCESS_WIFI_STATE)
    public static String getWiFiBSSID(Context context) {
        try {

            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = cm.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

                WifiInfo wifiInfo = wm.getConnectionInfo();

                if (null != wifiInfo && !TextUtils.isEmpty(wifiInfo.getBSSID())) {
                    return wifiInfo.getBSSID();
                }
            }

        } catch (Exception e) {
        }
        return "";
    }


//    public static String getSSID(Context context) {
//        try {
//            WifiManager wfm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
//            WifiInfo wfi = wfm.getConnectionInfo();
//            String mSSID = "";
//            if (null != wfi && !TextUtils.isEmpty(wfi.getSSID())) {
//                // 只允许字母和数字 String regEx = "[^a-zA-Z0-9]";
//                mSSID = wfi.getSSID();
//                String regEx = "[`~!@#$%^&*()+=|{}':;',//[//].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]";
//                Pattern pattern = Pattern.compile(regEx);
//                Matcher matcher = pattern.matcher(mSSID);
//                mSSID = matcher.replaceAll("").trim();
//                mSSID = mSSID.replaceAll("\"", "").replaceAll("\\s+", "").trim();
//                return mSSID;
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return "";
//    }


    /**
	 * 获得手机的：宽＊density + x + 高＊density
	 * 
	 * @param context
	 * @return
	 */

	public static String getResolution(Context context) {
		try {
			WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
			DisplayMetrics metrics = new DisplayMetrics();
			wm.getDefaultDisplay().getMetrics(metrics);
			return metrics.widthPixels + "x" + metrics.heightPixels;
		} catch (Exception e) {
			return "";
		}
	}

	/**
	 * 获得注册运营商的名字
	 * 
	 * @param context
	 * @return
	 */
	public static String getCarrier(Context context) {
		TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		return manager.getNetworkOperatorName();
	}

	/**
	 * 获得设备IMEI标识
	 * 
	 * @param context
	 * @return
	 */
	public static String getImei(Context context) {
		try {
			TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
			return manager.getDeviceId();
		} catch (Throwable e) {
		}
        return "";
	}

	/**
	 * 获得本地语言和国家
	 * 
	 * @return
	 */
	public static String getLocale() {
		Locale locale = Locale.getDefault();
		return locale.getLanguage() + "_" + locale.getCountry();
	}

	/**
	 * 获得当前应用的版本号
	 * 
	 * @param context
	 * @return
	 */
	public static String appVersion(Context context) {
		String result = "1.0";
		try {
			result = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
		}

		return result;
	}

	/**
	 * 获得设备的IP地址
	 * 
	 * @param context
	 * @return
	 */

	public static String getIP(Context context) {
		String ip = null;
		try {
			Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
			NetworkInterface inf = interfaces.nextElement();
			for (Enumeration<InetAddress> enumAddress = inf.getInetAddresses(); enumAddress.hasMoreElements();) {
				InetAddress in = enumAddress.nextElement();
				if (!in.isLinkLocalAddress()) {
					return in.getHostAddress();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ip;
	}

	/**
	 * 获得设备device 、id 、display、product等信息
	 * 
	 * @return
	 */
	public static String getModel() {
		String device = Build.DEVICE;
		String id = Build.ID;
		String display = Build.DISPLAY;
		String product = Build.PRODUCT;
		String board = Build.BOARD;
		String brand = Build.BRAND;
		String model = Build.MODEL;
		return device + "," + id + "," + display + "," + product + "," + board + "," + brand + "," + model;
	}

	/**
	 * 判断是否是wifi连接
	 * 0: 2/3G  1: wifi  2: 无网状态
	 * @param context
	 * @return
	 */
	public static String isWifi(Context context) {
        String state = "2";
        try {
            String nettype = getCurrentNetType(context);
            if (TextUtils.isEmpty(nettype)) {
                state = "2";
            } else if (nettype.equals("wifi")) {
                state = "1";
            } else {
                state = "0";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return state;
    }

    /**
     * 返回当前网络的状态
     * @param context
     * @return
     */
    public static String getCurrentNetType(Context context) {
        String type = "";
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo info = cm.getActiveNetworkInfo();
            if (info == null || !info.isAvailable()) {
                type = "";
            } else if (info.getType() == ConnectivityManager.TYPE_WIFI) {
                type = "wifi";
            } else if (info.getType() == ConnectivityManager.TYPE_MOBILE) {
                int subType = info.getSubtype();
                if (subType == TelephonyManager.NETWORK_TYPE_CDMA || subType == TelephonyManager.NETWORK_TYPE_GPRS
                        || subType == TelephonyManager.NETWORK_TYPE_EDGE) {
                    type = "2g";
                } else if (subType == TelephonyManager.NETWORK_TYPE_UMTS
                        || subType == TelephonyManager.NETWORK_TYPE_HSDPA
                        || subType == TelephonyManager.NETWORK_TYPE_EVDO_A
                        || subType == TelephonyManager.NETWORK_TYPE_EVDO_0
                        || subType == TelephonyManager.NETWORK_TYPE_EVDO_B
                        || subType == TelephonyManager.NETWORK_TYPE_HSPA
                        || subType == TelephonyManager.NETWORK_TYPE_HSUPA
                        || subType == TelephonyManager.NETWORK_TYPE_EHRPD
                        || subType == TelephonyManager.NETWORK_TYPE_HSPAP) {
                    type = "3g";
                } else if (subType == TelephonyManager.NETWORK_TYPE_LTE) {// LTE是3g到4g的过渡，是3.9G的全球标准
                    type = "4g";
                }
            }
        } catch (Exception e) {
            type = "";
        }
        return type;
    }

	/**
	 * 判断当前网络是否可用
	 * 
	 * @param context
	 * @return
	 */
	public static boolean isNetworkAvailable(Context context) {
		try {
			if (context != null) {
				ConnectivityManager connectivity = (ConnectivityManager) context
						.getSystemService(Context.CONNECTIVITY_SERVICE);
				if (connectivity == null) {
					return false;
				} else {
					NetworkInfo[] info = connectivity.getAllNetworkInfo();
					if (info != null) {
						for (int i = 0; i < info.length; i++) {
							if (info[i].getState() == NetworkInfo.State.CONNECTED) {
								return true;
							}
						}
					}
				}
			}
		} catch (Throwable e) {
		}
		return false;
	}

	/**
	 * 获取当前应用的名字
	 * 
	 * @param context
	 * @return
	 */
	public static String getAppName(Context context) {
		try {
			PackageInfo pkg =context.getPackageManager().getPackageInfo(context.getPackageName(), 0);  
			String appName = pkg.applicationInfo.loadLabel(context.getPackageManager()).toString(); 
			return appName;
		} catch (NameNotFoundException e) {
			e.printStackTrace(); // To change body of catch statement use File |
									// Settings | File Templates.
		}
		return "";
	}

	/**
	 * 获得应用的包名
	 * 
	 * @param context
	 * @return
	 */
	public static String getPackageName(Context context) {
		try {
			return context.getPackageName();
		} catch (Exception e) {
			return "";
		}
	}

    /**
     * 获得设备获取MAC地址
     *
     * @param context require Manifest.permission.ACCESS_NETWORK_STATE permission
     * @return e.g. 0c:1d:af:c6:95:a8
     */
    public static String getMacAddress(Context context) {
        String macaddress;
        if (Build.VERSION.SDK_INT >= 23) {
            macaddress = getMacWithNetWorkInterface();
        } else {
            macaddress = getMacWithManager(context);
        }
        return macaddress;
    }

    /**
     * 在android6.0+上通过NetworkInterface获取MAC
     *
     * @return
     */
    private static String getMacWithNetWorkInterface() {
        try {
            String wlan = "wlan0";
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                if (!intf.getName().equalsIgnoreCase(wlan)) {
                    continue;
                }

                byte[] byteMac = intf.getHardwareAddress();
                if (byteMac == null) {
                    return "";
                }

                StringBuilder sb = new StringBuilder();
                for (byte aMac : byteMac) {
                    sb.append(String.format("%02x:", aMac));// %02X
                }
                if (sb.length() > 0) {
                    sb.deleteCharAt(sb.length() - 1);
                }
                return sb.toString();
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 在android6.0以下，通过WifiManager获取MAC
     *
     * @param context
     * @return
     */
    @RequiresPermission(Manifest.permission.ACCESS_WIFI_STATE)
    private static String getMacWithManager(Context context) {
        try {
            WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            if (wm != null) {
                WifiInfo wifiInfo = wm.getConnectionInfo();
                if (wifiInfo != null) {
                    return wifiInfo.getMacAddress();
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return "";
    }


	/**
	 * 获得设备ONIN,这里使用AndroidID
	 * 
	 * @param context
	 * @return
	 */
	public static String getODIN1(Context context) {
		String androidId = "";
		try {
			androidId = Settings.System.getString(context.getContentResolver(), Secure.ANDROID_ID);
			return SHA1(androidId);
		} catch (Exception e) {
			return "";
		}
	}

	private static String convertToHex(byte[] data) {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < data.length; i++) {
			int halfbyte = (data[i] >>> 4) & 0x0F;
			int two_halfs = 0;
			do {
				if ((0 <= halfbyte) && (halfbyte <= 9))
					buf.append((char) ('0' + halfbyte));
				else
					buf.append((char) ('a' + (halfbyte - 10)));
				halfbyte = data[i] & 0x0F;
			} while (two_halfs++ < 1);
		}
		return buf.toString();
	}

	/**
	 * 对字符串进行加密处理
	 * 
	 * @param text
	 * @return
	 */
	private static String SHA1(String text) {
		try {
			MessageDigest md;
			md = MessageDigest.getInstance(SHA1_ALGORITHM);
			byte[] sha1hash;
			md.update(text.getBytes(CHAR_SET), 0, text.length());
			sha1hash = md.digest();
			return convertToHex(sha1hash);
		} catch (Exception e) {
			Logger.e("ODIN Error generating generating SHA-1: " + e);
			return null;
		}
	}

    private static Map<String, String> deviceInfoParams = null;

    /**
     * 获取设备信息
     *
     * @param context
     * @return
     */
    public static Map<String, String> getDeviceInfo(Context context) {

        if (deviceInfoParams == null) {
            deviceInfoParams = new HashMap<>();
            try {
                String mac = getMacAddress(context).replace(":", "").toUpperCase();
                deviceInfoParams.put(Constant.TRACKING_MAC, mac);
                deviceInfoParams.put(Constant.TRACKING_ANDROIDID, getAndroidId(context));
                deviceInfoParams.put(Constant.TRACKING_OS_VERION, getOSVersion());
                deviceInfoParams.put(Constant.TRACKING_TERM, getDevice());
                deviceInfoParams.put(Constant.TRACKING_NAME, getAppName(context));
                deviceInfoParams.put(Constant.TRACKING_KEY, getPackageName(context));
                deviceInfoParams.put(Constant.TRACKING_SCWH, getResolution(context));
                deviceInfoParams.put(Constant.TRACKING_OS, "0");
                deviceInfoParams.put(Constant.TRACKING_SDKVS, Constant.TRACKING_SDKVS_VALUE);
                deviceInfoParams.put(Constant.TRACKING_AAID, Reflection.getPlayAdId(context));
            } catch (Exception e) {
            }
        }
        //参数动态获取
        deviceInfoParams.put(Constant.TRACKING_IMEI, getImei(context));
        deviceInfoParams.put(Constant.TRACKING_RAWIMEI, getImei(context));
        String apMac = getWiFiBSSID(context).replace(":", "").toUpperCase();
        deviceInfoParams.put(Constant.TRACKING_WIFIBSSID, apMac);
        deviceInfoParams.put(Constant.TRACKING_WIFISSID, getWifiSSID(context));
        deviceInfoParams.put(Constant.TRACKING_WIFI, isWifi(context));

        return deviceInfoParams;
    }

	/**
	 * 从Sharedpreferenced中获取android_Id
	 * 
	 * @param mContext
	 * @return
	 */
	public static String getAndroidId(Context mContext) {
        try {
            return Settings.Secure.getString(mContext.getContentResolver(), Settings.Secure.ANDROID_ID);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 获取APP已经安装的应用列表信息
     *
     * @param context
     * @return
     */
    public static JSONArray getApplist(Context context) {

        JSONArray applist = new JSONArray();
        try {
            List<PackageInfo> packages = context.getPackageManager().getInstalledPackages(0);
            if (packages != null) {

                for (PackageInfo packageInfo : packages) {
                    StringBuffer appItem = new StringBuffer();

                    boolean isSystemApp;
                    if ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) > 0) {
                        isSystemApp = true;
                    } else {
                        isSystemApp = false;
                    }

                    //String appname = packageInfo.applicationInfo == null ? "" : packageInfo.applicationInfo.loadLabel(context.getPackageManager()).toString();
                    String packageName = packageInfo.packageName == null ? "" : packageInfo.packageName.trim();
                    String versionName = packageInfo.versionName == null ? "" : packageInfo.versionName.trim();
                    appItem.append(packageName);
                    appItem.append(",");
                    appItem.append(versionName);
                    appItem.append(",");
                    appItem.append(isSystemApp ? "1" : "0");
                    applist.put(appItem);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return applist;
    }

}
