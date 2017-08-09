package cn.com.mma.mobile.tracking.viewability.common;

import android.content.Context;
import android.graphics.Rect;
import android.os.PowerManager;
import android.util.DisplayMetrics;
import android.view.View;

/**
 * Created by admaster on 17/6/20.
 */
public class ViewHelper {

    /**
     * 判断屏幕是否点亮
     *
     * @param adView
     * @return
     */
    public static boolean isScreenOn(View adView) {
        try {
            PowerManager powerManager = (PowerManager) adView.getContext().getSystemService(Context.POWER_SERVICE);
            return ((Boolean) powerManager.getClass().getMethod("isScreenOn", new Class[0]).invoke(powerManager, new Object[0])).booleanValue();
        } catch (Throwable e) {
            e.printStackTrace();
            return true;
        }
    }

    public static boolean isScreenOn(Context context) {
        try {
            PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            return ((Boolean) powerManager.getClass().getMethod("isScreenOn", new Class[0]).invoke(powerManager, new Object[0])).booleanValue();
        } catch (Throwable e) {
            e.printStackTrace();
            return true;
        }
    }


    private static  Rect screenRect;
    /**
     * 获取坐标系屏幕区域的Rect
     *
     * @return
     */
    public static Rect getScreenRect(Context context) {
        try {
            if (null == screenRect) {
                DisplayMetrics dm = context.getResources().getDisplayMetrics();
                if (dm != null) {
                    int width = dm.widthPixels;
                    int height = dm.heightPixels;
                    screenRect = new Rect(0, 0, width, height);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return screenRect;
    }

    /**
     * 获取view相对Window实际Rect,包含不可视部分的原始位置
     *
     * @param adView
     * @return
     */
    public static Rect getViewInWindowRect(View adView) {

        //方法一:通过getLocationInWindow确定实际左上角坐标然后加和view的宽高确定右下角
        int[] location = {Integer.MAX_VALUE, Integer.MAX_VALUE};
        //在onStart和onCreate获取不到
        adView.getLocationInWindow(location);//如果当前载体是Dialog,window就是Dialog,getLocationOnScreen是整个屏幕而言
        int left = location[0];
        int top = location[1];
        int right = left + adView.getWidth();
        int bottom = top + adView.getHeight();

        //方法二,通过GlobalVisibleRect和offsetPoint
//            Rect rect = new Rect();
//            Point offset = new Point();
//            adView.getGlobalVisibleRect(rect,offset);
//            rect.left = offset.x;
//            rect.top = offset.y;

        return new Rect(left, top, right, bottom);
    }

    // 获取可视区域尺寸
//        private Rect getVisibleRect(View adView, Rect screenRect) {
//            Rect visableRect = getMiniRect();
//            if (!adView.getGlobalVisibleRect(visableRect)) {
//                visableRect = getMiniRect();
//            }
//            visableRect.left = Math.min(Math.max(0, visableRect.left), screenRect.right);
//            visableRect.right = Math.min(Math.max(0, visableRect.right), screenRect.right);
//            visableRect.top = Math.min(Math.max(0, visableRect.top), screenRect.bottom);
//            visableRect.bottom = Math.min(Math.max(0, visableRect.bottom), screenRect.bottom);
//            return visableRect;
//        }
//
//        private Rect getMiniRect() {
//            return new Rect(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
//        }

}
