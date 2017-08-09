package cn.com.mma.mobile.tracking.viewability.webjs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import org.json.JSONObject;

import cn.com.mma.mobile.tracking.viewability.common.ViewHelper;

/**
 * Created by yangxiaolong on 17/7/30.
 */
public class ViewAbilityMessage {


    private static final String ADVIEWABILITY_TIME = "AdviewabilityTime";
    private static final String ADVIEWABILITY_FRAME = "AdviewabilityFrame";
    private static final String ADVIEWABILITY_POINT = "AdviewabilityPoint";
    private static final String ADVIEWABILITY_ALPHA = "AdviewabilityAlpha";
    private static final String ADVIEWABILITY_HIDE = "AdviewabilityHide";
    private static final String ADVIEWABILITY_COVERRATE = "AdviewabilityCoverRate";
    private static final String ADVIEWABILITY_SHOWFRAME = "AdviewabilityShowFrame";
    private static final String ADVIEWABILITY_LIGHT = "AdviewabilityLight";
    public static final String ADVIEWABILITY_TYPE = "AdviewabilityType";


    public static JSONObject getViewAbilityEvents(Context context, View adView) {
        JSONObject jsonObject = new JSONObject();
        //AdView尺寸:宽X高
        int width = adView.getWidth();
        int height = adView.getHeight();
        String adSize = width + "x" + height;

        if (adView == null) return jsonObject;

        String visiblePoint = "";
        Rect visibleRect = ViewHelper.getViewInWindowRect(adView);
        try {

            Point visibleLeftPoint = new Point();
            visibleLeftPoint.x = visibleRect.left;
            visibleLeftPoint.y = visibleRect.top;
            visiblePoint = visibleLeftPoint.x + "x" + visibleLeftPoint.y;

            //目前
            boolean checkFrameBounds = checkFrameBounds(adView);
            if (!checkFrameBounds) {
                Rect rect = traverseParent(adView, visibleRect);
                if (rect != null) visibleRect = rect;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        float alpha = 0.0f;
        if (Build.VERSION.SDK_INT >= 11) {
            alpha = adView.getAlpha();
        }

        //是否被隐藏
        int hidden = (adView.getVisibility() == View.VISIBLE) ? 1 : 0;

        String visibleSize = "";
        float coverRate = 0.0f;
        try {
            Rect screenRect = ViewHelper.getScreenRect(context);
            Rect overlapRect = new Rect();
            boolean isIntersets = overlapRect.setIntersect(visibleRect, screenRect);

            int visbleWidth = Math.abs(overlapRect.right - overlapRect.left);
            int visbleHeight = Math.abs(overlapRect.bottom - overlapRect.top);
            visibleSize = visbleWidth + "x" + visbleHeight;

            //覆盖率(被) 可视区域尺寸/视图原尺寸
            double temp = 1.0f - (visbleWidth * visbleHeight) * 1.0f / (width * height) * 1.0f;
            coverRate = (float) Math.round(temp * 100) / 100;

        } catch (Exception e) {
            e.printStackTrace();
        }

        int screenOn = ViewHelper.isScreenOn(adView) ? 1 : 0;

        try {
            jsonObject.put(ADVIEWABILITY_TIME, String.valueOf(System.currentTimeMillis()));
            jsonObject.put(ADVIEWABILITY_FRAME, adSize);
            jsonObject.put(ADVIEWABILITY_POINT, visiblePoint);
            jsonObject.put(ADVIEWABILITY_ALPHA, String.valueOf(alpha));
            jsonObject.put(ADVIEWABILITY_HIDE, String.valueOf(hidden));
            jsonObject.put(ADVIEWABILITY_SHOWFRAME, visibleSize);
            jsonObject.put(ADVIEWABILITY_COVERRATE, String.valueOf(coverRate));
            jsonObject.put(ADVIEWABILITY_LIGHT, String.valueOf(screenOn));
        } catch (Exception e) {
            e.printStackTrace();
        }


        return jsonObject;
    }


    public static JSONObject getEmptyViewAbilityEvents(Context context) {
        JSONObject jsonObject = new JSONObject();
        try {
            int screenOn = ViewHelper.isScreenOn(context) ? 1 : 0;
            jsonObject.put(ADVIEWABILITY_TIME, String.valueOf(System.currentTimeMillis()));
            jsonObject.put(ADVIEWABILITY_LIGHT, String.valueOf(screenOn));

        } catch (Exception e) {
            e.printStackTrace();
        }
        return jsonObject;
    }


    private static boolean checkFrameBounds(View contentView) {
        try {

            Rect currentViewRect = new Rect();
            Point offsets = new Point();
            // 如果contentView的visibility=gone或width,height都为0，返回为false(Rect=(0,0,0,0))
            boolean currentVisible = contentView.getGlobalVisibleRect(currentViewRect, offsets);// left,top,right,bottom

            boolean heightVisible = (currentViewRect.bottom - currentViewRect.top) >= contentView.getMeasuredHeight();
            boolean widthVisible = (currentViewRect.right - currentViewRect.left) >= contentView.getMeasuredWidth();
            boolean totalViewVisible = currentVisible && heightVisible && widthVisible;

            if (!totalViewVisible)
                return false;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }

    @SuppressLint("NewApi")
	private static Rect traverseParent(View adView, Rect originRect) {

        View currentView = adView;
        Rect overlapRect = originRect;

        try {
            while (currentView.getParent() instanceof ViewGroup) {

                // 父类容器
                ViewGroup currentParent = (ViewGroup) currentView.getParent();
                Rect parentRect = new Rect();
                currentParent.getGlobalVisibleRect(parentRect);

                //父父容器
                ViewGroup superParent = null;
                if (currentParent.getParent() instanceof ViewGroup) {
                    superParent = (ViewGroup) currentParent.getParent();
                }

                //默认为true 裁减子视图边框
                boolean clipChildRen = false;
                if (superParent != null && Build.VERSION.SDK_INT > 18) {
                    clipChildRen = superParent.getClipChildren();
                }

                //如果当前容器超出区域不被剪切
                if (clipChildRen) {

                    Rect rect = new Rect();

                    boolean isIntersets = rect.setIntersect(overlapRect, parentRect);
                    overlapRect = rect;
                }

                currentView = currentParent;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


        return overlapRect;
    }

}
