package cn.com.mma.mobile.tracking.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import android.content.Context;
import android.content.pm.PackageManager;

public class Reflection {




    public static boolean checkPermission(Context context, String permission) {
        // 使用反射，以免因没有导入新的v4包找不到ContextCompat而崩溃
//        int result = 0;
//        if (getContextCompat()) {
//            result = getContextCompatValue(context, permission);
//        }
//        return result == PackageManager.PERMISSION_GRANTED;
        return getContextCompat() ? getContextCompatValue(context, permission) == PackageManager.PERMISSION_GRANTED : false;

    }


    public static String getPlayAdId(Context context) {

        boolean isGooglePlayServicesAvailable = isGooglePlayServicesAvailable(context);
        if (!isGooglePlayServicesAvailable) Logger.w("googlePlay service is unavailable!");

        String playAdid = null;
        try {
            Object advertisingObject = getAdvertisingInfoObject(context);
            if (advertisingObject != null) {
                playAdid = (String) invokeInstanceMethod(advertisingObject, "getId", null);
            }
        } catch (Throwable t) {

        }
        return playAdid == null ? "" : playAdid;
    }




    private static boolean getContextCompat() {
        try {
            Class<?> classObject = Class.forName("android.support.v4.content.ContextCompat");
            if (classObject != null) {
                return true;
            }
        } catch (Exception e) {
        }
        return false;
    }

    private static int getContextCompatValue(Context ctx, String permissions) {
        try {
            Class<?> classObject = Class.forName("android.support.v4.content.ContextCompat");
            Method staticMethod = classObject.getDeclaredMethod("checkSelfPermission", Context.class, String.class);
            return (Integer) staticMethod.invoke(classObject, ctx, permissions);
        } catch (Exception e) {
            return 0;
        }
    }


    private static boolean isGooglePlayServicesAvailable(Context context) {
        try {
            Integer isGooglePlayServicesAvailableStatusCode = (Integer) invokeStaticMethod(
                    "com.google.android.gms.common.GooglePlayServicesUtil", "isGooglePlayServicesAvailable",
                    new Class[]{Context.class}, context);

            boolean isGooglePlayServicesAvailable = isConnectionResultSuccess(isGooglePlayServicesAvailableStatusCode);

            return isGooglePlayServicesAvailable;
        } catch (Throwable t) {
            return false;
        }
    }


    private static Object getAdvertisingInfoObject(Context context) throws Exception {
        return invokeStaticMethod("com.google.android.gms.ads.identifier.AdvertisingIdClient",
                "getAdvertisingIdInfo",
                new Class[]{Context.class}, context);
    }

    private static boolean isConnectionResultSuccess(Integer statusCode) {
        if (statusCode == null) {
            return false;
        }

        try {
            Class ConnectionResultClass = Class.forName("com.google.android.gms.common.ConnectionResult");

            Field SuccessField = ConnectionResultClass.getField("SUCCESS");

            int successStatusCode = SuccessField.getInt(null);

            return successStatusCode == statusCode;
        } catch (Throwable t) {
            return false;
        }
    }

//    public static Class forName(String className) {
//        try {
//            Class classObject = Class.forName(className);
//            return classObject;
//        } catch (Throwable t) {
//            return null;
//        }
//    }

//    public static Object createDefaultInstance(String className) {
//        Class classObject = forName(className);
//        Object instance = createDefaultInstance(classObject);
//        return instance;
//    }
//
//    public static Object createDefaultInstance(Class classObject) {
//        try {
//            Object instance = classObject.newInstance();
//            return instance;
//        } catch (Throwable t) {
//            return null;
//        }
//    }

//    public static Object createInstance(String className, Class[] cArgs, Object... args) {
//        try {
//            Class classObject = Class.forName(className);
//            Constructor constructor = classObject.getConstructor(cArgs);
//            Object instance = constructor.newInstance(args);
//            return instance;
//        } catch (Throwable t) {
//            return null;
//        }
//    }

    private static Object invokeStaticMethod(String className, String methodName, Class[] cArgs, Object... args) throws Exception {
        Class classObject = Class.forName(className);

        return invokeMethod(classObject, methodName, null, cArgs, args);
    }

    private static Object invokeInstanceMethod(Object instance, String methodName, Class[] cArgs, Object... args) throws Exception {
        Class classObject = instance.getClass();

        return invokeMethod(classObject, methodName, instance, cArgs, args);
    }

    private static Object invokeMethod(Class classObject, String methodName, Object instance, Class[] cArgs, Object... args) throws Exception {

        Method methodObject = classObject.getMethod(methodName, cArgs);

        Object resultObject = methodObject.invoke(instance, args);

        return resultObject;
    }

}
