package cn.com.mma.mobile.tracking.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import android.content.Context;

public class Reflection {

	public static String getPlayAdId(Context context) {
		try {
			boolean isGooglePlayServicesAvailable = Reflection.isGooglePlayServicesAvailable(context);
			if(isGooglePlayServicesAvailable){
				Object AdvertisingInfoObject = getAdvertisingInfoObject(context);
				String playAdid = (String) invokeInstanceMethod(
						AdvertisingInfoObject, "getId", null);
				return playAdid;
			}else{
				Logger.e("google play service is missing!!!");
				return "";
			}
			
			
		} catch (Throwable t) {
			return "";
		}
	}

	public static boolean isGooglePlayServicesAvailable(Context context) {
		try {
			Integer isGooglePlayServicesAvailableStatusCode = (Integer) invokeStaticMethod(
					"com.google.android.gms.common.GooglePlayServicesUtil",
					"isGooglePlayServicesAvailable",
					new Class[] { Context.class }, context);

			boolean isGooglePlayServicesAvailable = (Boolean) isConnectionResultSuccess(isGooglePlayServicesAvailableStatusCode);

			return isGooglePlayServicesAvailable;
		} catch (Throwable t) {
			return false;
		}
	}

	public static String getMacAddress(Context context) {
		try {
			String macSha1 = (String) invokeStaticMethod(
					"com.adjust.sdk.plugin.MacAddressUtil", "getMacAddress",
					new Class[] { Context.class }, context);

			return macSha1;
		} catch (Throwable t) {
			return null;
		}
	}

	private static Object getAdvertisingInfoObject(Context context)
			throws Exception {
		return invokeStaticMethod(
				"com.google.android.gms.ads.identifier.AdvertisingIdClient",
				"getAdvertisingIdInfo", new Class[] { Context.class }, context);
	}

	private static boolean isConnectionResultSuccess(Integer statusCode) {
		if (statusCode == null) {
			return false;
		}

		try {
			Class ConnectionResultClass = Class
					.forName("com.google.android.gms.common.ConnectionResult");

			Field SuccessField = ConnectionResultClass.getField("SUCCESS");

			int successStatusCode = SuccessField.getInt(null);

			return successStatusCode == statusCode;
		} catch (Throwable t) {
			return false;
		}
	}

	public static Class forName(String className) {
		try {
			Class classObject = Class.forName(className);
			return classObject;
		} catch (Throwable t) {
			return null;
		}
	}

	public static Object createDefaultInstance(String className) {
		Class classObject = forName(className);
		Object instance = createDefaultInstance(classObject);
		return instance;
	}

	public static Object createDefaultInstance(Class classObject) {
		try {
			Object instance = classObject.newInstance();
			return instance;
		} catch (Throwable t) {
			return null;
		}
	}

	public static Object createInstance(String className, Class[] cArgs,
			Object... args) {
		try {
			Class classObject = Class.forName(className);
			@SuppressWarnings("unchecked")
			Constructor constructor = classObject.getConstructor(cArgs);
			Object instance = constructor.newInstance(args);
			return instance;
		} catch (Throwable t) {
			return null;
		}
	}

	public static Object invokeStaticMethod(String className,
			String methodName, Class[] cArgs, Object... args) throws Exception {
		Class classObject = Class.forName(className);

		return invokeMethod(classObject, methodName, null, cArgs, args);
	}

	public static Object invokeInstanceMethod(Object instance,
			String methodName, Class[] cArgs, Object... args) throws Exception {
		Class classObject = instance.getClass();

		return invokeMethod(classObject, methodName, instance, cArgs, args);
	}

	public static Object invokeMethod(Class classObject, String methodName,
			Object instance, Class[] cArgs, Object... args) throws Exception {
		@SuppressWarnings("unchecked")
		Method methodObject = classObject.getMethod(methodName, cArgs);

		Object resultObject = methodObject.invoke(instance, args);

		return resultObject;
	}

}
