package cn.com.mma.mobile.tracking.bean;

/**
 * Created by yangxiaolong on 17/6/19.
 */
public class ViewAbility {


    /* viewability监测的时间间隔（ms）*/
    public int intervalTime;
    /* 满足viewability可见区域占总区域的百分比 */
    public int viewabilityFrame;
    /* 满足viewability链接曝光总时长（s）*/
    public int viewabilityTime;
    /* 满足viewability视频曝光总时长（s）*/
    public int viewabilityVideoTime;
    /* 当前广告位最大监测时长（s）*/
    /* 当前广告位最大监测时长（s）*/
    public int maxExpirationSecs;
    /* 当前广告位最大上报数量 */
    public int maxAmount;


    @Override
    public String toString() {
        return "[ " + "interval:" + intervalTime + ",framerate:" + viewabilityFrame + ",abilitytime:" + viewabilityTime + ",viewabilityVideoTime:" + viewabilityVideoTime + ",maxtime:" + maxExpirationSecs + ",maxAmount:" + maxAmount + " ]";
    }
}
