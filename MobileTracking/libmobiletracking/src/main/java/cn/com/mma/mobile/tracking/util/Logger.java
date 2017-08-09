package cn.com.mma.mobile.tracking.util;

import android.util.Log;


/**
 * LOG Utils
 */
public final class Logger {

    public static boolean DEBUG_LOG = false;

    public static String TAG = "MMAChinaSDK";

    public static void d(String tag, String msg) {
        if (DEBUG_LOG)
            Log.d(tag, msg);
    }

    public static void d(String msg) {
        if (DEBUG_LOG)
            Log.d(TAG, msg);
    }

    public static void e(String tag, String msg) {
        if (DEBUG_LOG)
            Log.e(tag, msg);
    }

    public static void e(String msg) {
        if (DEBUG_LOG)
            Log.e(TAG, msg);
    }

    public static void w(String msg) {
        if (DEBUG_LOG)
            Log.w(TAG, msg);
    }
}
