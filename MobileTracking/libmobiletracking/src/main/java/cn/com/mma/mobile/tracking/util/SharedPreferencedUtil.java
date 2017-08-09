package cn.com.mma.mobile.tracking.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

/**
 * Sp的工具类：存、取
 * 
 * @author lincoln
 * 
 */
public class SharedPreferencedUtil {
	/**
	 * 存储SP的文件名字
	 */
	public static final String SP_NAME_CONFIG = "cn.com.mma.mobile.tracking.sdkconfig";
	public static final String SP_NAME_NORMAL = "cn.com.mma.mobile.tracking.normal";
	public static final String SP_NAME_FAILED = "cn.com.mma.mobile.tracking.falied";
	/**
	 * SP_NAME_CONFIG 保存文件的key
	 */
	public static final String SP_CONFIG_KEY_FILE = "trackingConfig";

	/**
	 * 保存android_id ，配置文件上次更新时间、等字段
	 */
	public static final String SP_NAME_OTHER = "cn.com.mma.mobile.tracking.other";
	/**
	 * SP_NAME_OTHER 上次更新时间key
	 */
	public static final String SP_OTHER_KEY_UPDATE_TIME = "updateTime";
	/**
	 * SP_NAME_OTHER Android_Id
	 */
	public final static String SP_OTHER_KEY_ANDROID_ID = "android_id";

	/**
	 * 向SP中写数据
	 * 
	 * @param context
	 * @param spName
	 * @param key
	 * @param value
	 */
	public synchronized static void putString(Context context, String spName,
			String key, String value) {
		SharedPreferences sPreferences = context.getSharedPreferences(spName,
				Context.MODE_PRIVATE);
		Editor editor = sPreferences.edit();
		editor.putString(key, value);
		editor.commit();
	}

	/**
	 * 从SP中读数据
	 * 
	 * @param context
	 * @param spName
	 * @param key
	 * @return
	 */
	public synchronized static String getString(Context context, String spName,
			String key) {
		SharedPreferences sPreferences = context.getSharedPreferences(spName,
				Context.MODE_PRIVATE);
		return sPreferences.getString(key, "");
	}

	/**
	 * 向SP中写数据
	 * 
	 * @param context
	 * @param spName
	 * @param key
	 * @param value
	 */
	public synchronized static void putLong(Context context, String spName,
			String key, long value) {
		SharedPreferences sPreferences = context.getSharedPreferences(spName,
				Context.MODE_PRIVATE);
		Editor editor = sPreferences.edit();
		editor.putLong(key, value);
		editor.commit();
	}

	/**
	 * 从SP中读数据
	 * 
	 * @param context
	 * @param spName
	 * @param key
	 * @return
	 */
	public synchronized static long getLong(Context context, String spName,
			String key) {
		SharedPreferences sPreferences = context.getSharedPreferences(spName,
				Context.MODE_PRIVATE);
		return sPreferences.getLong(key, 0);
	}

	/**
	 * 获得SP对象
	 * 
	 * @param context
	 * @param spName
	 * @return
	 */
	public synchronized static SharedPreferences getSharedPreferences(
			Context context, String spName) {
		SharedPreferences sharedPreferences = context.getSharedPreferences(
				spName, Context.MODE_PRIVATE);
		return sharedPreferences;
	}

	/**
	 * 从SP中删除数据
	 * 
	 */
	public synchronized static boolean removeFromSharedPreferences(
			Context context, String spName, String key) {
		SharedPreferences sPreferences = context.getSharedPreferences(spName,
				Context.MODE_PRIVATE);
		boolean result = sPreferences.edit().remove(key).commit();
		return result;
	}

	/**
	 * 删除Sp中所有数据
	 * 
	 * @param context
	 * @param spName
	 */
	public static void clearAllDataInSP(Context context, String spName) {
		SharedPreferences sharedPreferences = context.getSharedPreferences(
				spName, Context.MODE_PRIVATE);
		sharedPreferences.edit().clear().commit();
	}
}
