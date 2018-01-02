package cn.com.mma.mobile.tracking.util;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.TextUtils;

import cn.com.mma.mobile.tracking.api.Constant;
import cn.com.mma.mobile.tracking.bean.Argument;
import cn.com.mma.mobile.tracking.bean.Company;
import cn.mmachina.mobile.SignUtils;

/**
 *
 */
public class CommonUtil {
	/**
	 * 去掉传入的event参数
	 * 
	 * @param url
	 * @param requiredEventValue
	 * @param separator
	 * @param equalizer
	 * @return
	 */
	public static String removeExistEvent(String url,
			List<String> requiredEventValue, String separator, String equalizer) {
		for (String value : requiredEventValue) {
			if (url.contains(separator + value)) {
				Logger.d("mma_" + separator + value + equalizer + "[^"
						+ separator + "]*");
				url = url.replaceAll(separator + value + equalizer + "[^"
						+ separator + "]*", "");
			}
		}
		return url;
	}

	/**
	 * 获得track分配的track url，主要是去燥：去掉跟拼接参数一样的字段
	 * 
	 * @param url
	 * @param requiredArgmentValue
	 * @param separator
	 * @param equalizer
	 * @param redirectURLValue
	 * @return
	 */
	public static Map<String,String> removeExistArgmentAndGetRedirectURL(String url,
			List<String> requiredArgmentValue, String separator,
			String equalizer, String redirectURLValue) {
		requiredArgmentValue = sortByLength(requiredArgmentValue);
		String redirectUrl = "";
		Map<String, String> URLAndRedirectURL = new HashMap<String, String>();
		for (String value : requiredArgmentValue) {
			if (url.contains(separator + value)) {
				if (value.equals(redirectURLValue)) {
					Pattern pattern = Pattern.compile(separator + value + "[^"
							+ separator + "]*");
					Matcher matcher = pattern.matcher(url);
					if (matcher.find())
						redirectUrl = matcher.group(0).replace(
								separator + value, "");
				}
				url = url.replaceAll(separator + value + equalizer + "[^"
						+ separator + "]*", "");
			}
		}
		URLAndRedirectURL.put(Constant.TRACKING_URL, url);
		return URLAndRedirectURL;
	}

	/**
	 * 对集合的内容根据长度进行排序
	 * 
	 * @param list
	 * @return
	 */
	private static List<String> sortByLength(List list) {
		Collections.sort(list, new Comparator<String>() {
			@Override
			public int compare(String s1, String s2) {
				return s1.length() > s2.length() ? -1 : 1;
			}
		});
		return list;
	}

	/**
	 * 根据正则表达式匹配字符串，并返回结果。匹配格式如下：
	 *
	 * 原网址：http://192.168.2.192:8080/untitled3/test/test/a123,c123,b123
	 * 返回值：http://192.168.2.192
	 *
	 * @param url
	 * @return
	 */
    public static String getHostURL(String url) {
        String hostURL = "";
        try {
            Pattern pattern = Pattern
                    .compile("^([\\w\\d]+):\\/\\/([\\w\\d\\-_]+(?:\\.[\\w\\d\\-_]+)*)");
            Matcher matcher = pattern.matcher(url);
            if (matcher.find())
                hostURL = matcher.group(0);
        } catch (Exception e) {
            e.printStackTrace();
            hostURL = url;
        }
        return hostURL;
    }

//	/**
//	 * 获得签名
//	 *
//	 * @param context
//	 * @param url
//	 * @return
//	 */
//	public static String getSignature(Context context, String url) {
//		// 检测URL转小写
//		try {
//			url.toLowerCase();
//			String signature = JniClient.MDString("", context, url);
//			return signature;
//		} catch (Exception e) {
//			e.printStackTrace();
//			return url;
//		}
//	}

    public static String getSignature(String sdkVersion, long timestamp, String originURL) {
        // 检测URL转小写
        try {
            String signature = SignUtils.mmaSdkSign(sdkVersion, timestamp, originURL);
            return signature;
        } catch (Exception e) {
        }
        return "";
    }


	/**
	 * 对String 进行md5加密
	 * 
	 * @param input
	 * @return
	 */
	public static String md5(String input) {
        String result = input;
        if (!TextUtils.isEmpty(input)) {
            MessageDigest messageDigest;
            try {
                messageDigest = MessageDigest.getInstance("MD5");
                messageDigest.update(input.getBytes());
                BigInteger hash = new BigInteger(1, messageDigest.digest());
                result = hash.toString(16);
                while (result.length() < 32) {
                    result = "0" + result;
                }
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

	public static boolean isWifiConnected(Context context) {
		boolean result = false;
		if (context != null) {
			ConnectivityManager mConnectivityManager = (ConnectivityManager) context
					.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo mWiFiNetworkInfo = mConnectivityManager
					.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
			if (mWiFiNetworkInfo != null) {
				result = mWiFiNetworkInfo.isAvailable();
			}
		}
		return result;
	}
	
	/**
	 * 判断某种网络类型是否连接
	 * @param context
	 * @param networkType ConnectivityManager.TYPE_WIFI ConnectivityManager.TYPE_MOBILE
	 * @return
	 */
	public static boolean isConnected(Context context, int networkType) {
		boolean state = false;
		try {
			ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
			if (cm != null) {
				NetworkInfo netInfo = cm.getNetworkInfo(networkType);
				if (netInfo != null) {
					state = netInfo.isAvailable();
				}
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
		return state;
	}


	/**
	 * 判断字符串是否为null，是则返回“”，否则进行utf－8编码后，返回
	 * 
	 * @param str
	 * @return
	 */
	public static String encodingUTF8(String str) {
		try {
			return str == null ? "" : URLEncoder.encode(str, "utf-8");
		} catch (UnsupportedEncodingException e) {
			return "";
		}
	}

	/**
	 * 判断拼接的参数是否需要Encoding
	 * 
	 * @param str
	 * @param argument
	 * @param company
	 * @return
	 */
	public static String encodingUTF8(String str, Argument argument, Company company) {
        try {

            if (company.sswitch.encrypt.containsKey(argument.key)) {
                if ("md5".equalsIgnoreCase(company.sswitch.encrypt.get(argument.key)))
                    str = CommonUtil.md5(str);
            }

            if (!argument.urlEncode)
                return str == null ? "" : str;
            else
                return str == null ? "" : URLEncoder.encode(str, "utf-8");
        } catch (Exception e) {
            return "";
        }
    }
}
