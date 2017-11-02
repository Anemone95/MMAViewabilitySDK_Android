package cn.com.mma.mobile.tracking.viewability.origin.sniffer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import java.io.Serializable;
import java.util.ArrayList;
import cn.com.mma.mobile.tracking.util.klog.KLog;
import cn.com.mma.mobile.tracking.viewability.common.ViewHelper;


/**
 * Created by mma on 17/6/16.
 */
public class ViewFrameSlice implements Serializable {

    private static final long serialVersionUID = 1L;

    //Rect(left,top,right,bottom) left,top = 坐标系左上坐标x点,y点 right,bottom =坐标系右下x点,y点
//    private Rect adWindowRect;
//    private Rect screenRect;
//    private Rect adLocalRect;

    //2t 当前监测点时间戳ms
    private long captureTime;
    //2k 广告实际尺寸 width*height
    private String adSize;
    //2d window坐标系可视区域左上角位置(x*y)
    private String visiblePoint;
    //2o window坐标系可视尺寸
    private String visibleSize;

    //2l 透明度 1.0=完全不透明 0.0=完全透明
    private float alpha;
    //2m 是否隐藏 0=不隐藏 1=隐藏
    private int hidden;
    //2r 屏幕是否点亮 1=开屏 0 = 熄灭
    private int screenOn;
    //是否聚焦 0-1
    private boolean focus;
    //2n 覆盖比例 0.00 - 1.00
    private float coverRate;

    // 结合所有参数判断adView是否可视化 0=不可见 1=可见
    private int visibleAbility;
    // 当前广告是否可测量 0为不可见 1为可见 //默认可见
    //private int measureAbility = 0;

    public ViewFrameSlice(View adView, Context context) {
        try {
            //时间戳
            captureTime = System.currentTimeMillis();

            //AdView尺寸:宽X高
            int width = adView.getWidth();
            int height = adView.getHeight();
            adSize = width + "x" + height;

            //TODO 可视区域是否包括被cliped或是panding出去的部分,目前获取方式是view原始左上角位置,
            //AdView实际在Window上可视区域坐标(view左上角x,y点)
//            Rect visibleRect = new Rect();
//            adView.getGlobalVisibleRect(visibleRect);
            Rect visibleRect = ViewHelper.getViewInWindowRect(adView);

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

            //透明度
            if (Build.VERSION.SDK_INT >= 11) {
                alpha = adView.getAlpha();
            }


            //是否被隐藏
            hidden = (adView.getVisibility() == View.VISIBLE) ? 0 : 1;

            //可视尺寸 在当前屏幕范围内,排除不可见区域后,view的宽和高,滑动时实时变动(和WindowFrame相交运算)
            Rect screenRect = ViewHelper.getScreenRect(context);
            Rect overlapRect = new Rect();
            boolean isIntersets = overlapRect.setIntersect(visibleRect, screenRect);

            int visbleWidth = Math.abs(overlapRect.right - overlapRect.left);
            int visbleHeight = Math.abs(overlapRect.bottom - overlapRect.top);
            //int visbleWidth = visibleRect.right - Math.abs(visibleRect.left);
            //int visbleHeight = visibleRect.bottom - Math.abs(visibleRect.top);
            visibleSize = visbleWidth + "x" + visbleHeight;


            //覆盖率(被) 可视区域尺寸/视图原尺寸
            //float coverRate = 1.0f - (visbleWidth * visbleHeight) / (width * height);
            double temp = 1.0f - (visbleWidth * visbleHeight) * 1.0f / (width * height) * 1.0f;
            coverRate = (float) Math.round(temp * 100) / 100;

            //屏幕是否点亮
            screenOn = (ViewHelper.isScreenOn(adView) && adView.hasWindowFocus()) ? 1:0;

            Rect selfRect = new Rect();
            adView.getLocalVisibleRect(selfRect);

            /**保留字段 view在坐标系的Rect*/
//            adWindowRect = visibleRect;
//            adLocalRect = selfRect;

            KLog.i("=================ViewFrameSlice Constructor begin ======================");
            DisplayMetrics dm = context.getResources().getDisplayMetrics();
            KLog.i("density:" + dm.density + "  api:" + dm.densityDpi);
            KLog.i("screenRect:" + screenRect);
            KLog.i("adView local visible Rect:" + selfRect);
            KLog.i("[2t] captureTime:" + captureTime);
            KLog.i("[2k] adView Size:" + adSize);
            KLog.i("[2d] adView visible left top Point:" + visibleLeftPoint);
            KLog.i("[2l] adView alpha:" + alpha);
            KLog.i("[2m] adView hidden:" + hidden);
            KLog.i("[2o] adView visible Size:" + visibleSize);
            KLog.i("[2n] adView cover rate:" + coverRate);
            KLog.i("[2r] current Screen is Light:" + screenOn);
            KLog.i("[2f] current adView visible ability:" + visibleAbility);
            KLog.i("checkFrameBounds:" + checkFrameBounds);
            KLog.i("adView isIntersets :" + isIntersets + "    overlapRect:" + overlapRect);
            KLog.i("adView window origin Rect:" + visibleRect);

            KLog.i("=================ViewFrameSlice Constructor end ======================");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public long getCaptureTime() {
        return captureTime;
    }

    public String getAdSize() {
        return adSize;
    }

    public String getVisiblePoint() {
        return visiblePoint;
    }

    public String getVisibleSize() {
        return visibleSize;
    }

    public float getAlpha() {
        return alpha;
    }

    public int getHidden() {
        return hidden;
    }

    public int getScreenOn() {
        return screenOn;
    }

    public float getCoverRate() {
        return coverRate;
    }


    /**
     * 结合参数判断AdView是否可视化
     *
     * @return
     */
    public boolean validateAdVisible(float confCoverRate) {
        //被覆盖率 <= 0.5 && 不隐藏 && 不完全透明 && 开屏
        if (coverRate <= confCoverRate && hidden == 0 && alpha > 0.001 && screenOn == 1) {
            visibleAbility = 1;
        } else {
            visibleAbility = 0;
        }
        return (visibleAbility == 1);
    }


    /**
     * 判断两个Slice是否相同
     *
     * @param otherSlice
     * @return
     */
    public boolean isSameAs(ViewFrameSlice otherSlice) {
        try {
            if (adSize.equals(otherSlice.adSize)
                    && visiblePoint.equals(otherSlice.visiblePoint)
                    && visibleSize.equals(otherSlice.visibleSize)
                    && Math.abs(alpha - otherSlice.alpha) < 0.001
                    && hidden == otherSlice.hidden
                    && screenOn == otherSlice.screenOn
                    && coverRate == otherSlice.coverRate) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean checkFrameBounds(View contentView) {
        try {

            Rect currentViewRect = new Rect();
            Point offsets = new Point();
            // 如果contentView的visibility=gone或width,height都为0，返回为false(Rect=(0,0,0,0))
            boolean currentVisible = contentView.getGlobalVisibleRect(currentViewRect, offsets);// left,top,right,bottom

            boolean heightVisible = (currentViewRect.bottom - currentViewRect.top) >= contentView.getMeasuredHeight();
            boolean widthVisible = (currentViewRect.right - currentViewRect.left) >= contentView.getMeasuredWidth();
            boolean totalViewVisible = currentVisible && heightVisible && widthVisible;

            //KLog.i("checkFrameBounds,rect:" + currentViewRect + "  offset:" + offsets + "  height:" + contentView.getMeasuredHeight() + "  width:" + contentView.getMeasuredWidth());
            //KLog.i("checkFrameBounds,current:" + currentVisible + "  heightVisiable:" + heightVisible + "  widthVisiable:" + widthVisible);

            if (!totalViewVisible)
                return false;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return true;
    }

    @SuppressLint("NewApi")
	private Rect traverseParent(View adView, Rect originRect) {

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

                // KLog.w("current:" + currentParent + "  super:" + superParent + "   clipChildRen:" + clipChildRen);

                //如果当前容器超出区域不被剪切
                if (clipChildRen) {

                    Rect rect = new Rect();

                    boolean isIntersets = rect.setIntersect(overlapRect, parentRect);
                    overlapRect = rect;

                    //int vWidth = Math.abs(rect.right - rect.left);
                    //int vHeight = Math.abs(rect.bottom - rect.top);
                    //KLog.v("isIntersets:" + isIntersets + "   overlapRect:" + rect + "  vWidth:" + vWidth + "  vHeight:" + vHeight);
                }

                currentView = currentParent;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


        return overlapRect;
    }


    @SuppressLint("NewApi")
	@SuppressWarnings("unused")
    private Rect traverseRootView(View adView, Rect originRect) {
        ArrayList<ViewGroup> viewGroups = new ArrayList<>();
        View currentView = adView;
        Rect overlapRect = originRect;
        try {
            while (currentView.getParent() instanceof ViewGroup) {
                ViewGroup currentParent = (ViewGroup) currentView.getParent();

                viewGroups.add(currentParent);

                currentView = currentParent;
            }


            int len = viewGroups.size();

            //从最定层开始遍历
            for (int i = len - 1; i >= 0; i--) {
                ViewGroup item = viewGroups.get(i);
                Rect parentRect = new Rect();
                item.getGlobalVisibleRect(parentRect);

                //默认为true 裁减子视图边框
                boolean clipChildRen = true;
                if (Build.VERSION.SDK_INT > 18) {
                    clipChildRen = item.getClipChildren();
                    //KLog.e("current:" + item + "  rect:" + parentRect + "   clipChildRen:" + clipChildRen);
                }

                if (clipChildRen) {
                    if ((i - 1) >= 0) {
                        ViewGroup subItem = viewGroups.get(i - 1);
                        Rect itemRect = new Rect();
                        subItem.getGlobalVisibleRect(itemRect);
                        Rect rect = new Rect();

                        boolean isIntersets = rect.setIntersect(overlapRect, itemRect);

                        int vWidth = Math.abs(rect.right - rect.left);
                        int vHeight = Math.abs(rect.bottom - rect.top);

                        overlapRect = rect;
                        //KLog.v("isIntersets:" + isIntersets + "   overlapRect:" + rect + "  vWidth:" + vWidth + "  vHeight:" + vHeight);
                    }
                }

            }


        } catch (Exception e) {
            e.printStackTrace();
        }

        return overlapRect;

    }

    @Override
    public String toString() {
        return "[ 2t=" + captureTime + ",2k=" + adSize + ",2d=" + visiblePoint + ",2o=" + visibleSize + ",2n=" + coverRate + ",2l=" + alpha + ",2m=" + hidden + ",2r=" + screenOn + "]";
    }
}



