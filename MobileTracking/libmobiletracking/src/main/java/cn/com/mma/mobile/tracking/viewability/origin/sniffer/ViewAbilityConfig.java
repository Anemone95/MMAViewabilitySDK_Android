package cn.com.mma.mobile.tracking.viewability.origin.sniffer;

import java.io.Serializable;

/**
 * Created by yangxiaolong on 17/6/17.
 */
public class ViewAbilityConfig implements Serializable {

    private static final long serialVersionUID = 1L;


    /* 可视化监测最大时长 单位 ms [配置文件为s]*/
    private int maxDuration;
    /*普通曝光满足viewability有效时长 单位 ms [配置文件为s]*/
    private int exposeValidDuration;
    /*视频曝光满足viewability有效时长 单位 ms [配置文件为s]*/
    private int videoExposeValidDuration;
    /* 探测周期间隔时长 单位 ms */
    private int inspectInterval;
    /* 数据帧最大上报数量 范围0-999*/
    private int maxUploadAmount;
    /*AD视图被覆盖率 范围 0.0-1.0*/
    private float coverRateScale;


    public ViewAbilityConfig() {

        maxDuration = 2 * 60 * 1000;//默认两分钟
        exposeValidDuration = 1 * 1000;//默认1s
        videoExposeValidDuration = 2 * 1000;//视频最大曝光默认2s
        inspectInterval = 100;//默认间隔100ms
        maxUploadAmount = 10;//默认最大上报数量为10
        coverRateScale = 0.5f;//被覆盖率默认50%

    }


    public float getCoverRateScale() {
        return coverRateScale;
    }

    /**
     * 视图被遮盖率,监测过程中使用被覆盖率计算
     *
     * @param coverRateScale 被覆盖比例
     */
    public void setCoverRateScale(float coverRateScale) {
        this.coverRateScale = coverRateScale;
    }

    /**
     * 当前广告位最大上报数量
     *
     * @param maxUploadAmount
     */
    public void setMaxUploadAmount(int maxUploadAmount) {
        this.maxUploadAmount = maxUploadAmount;
    }

    public int getMaxUploadAmount() {
        return maxUploadAmount;
    }


    public int getMaxDuration() {
        return maxDuration;
    }

    /**
     * 当前广告位最大监测时长
     *
     * @param maxDuration 单位为秒
     */
    public void setMaxDuration(int maxDuration) {
        this.maxDuration = maxDuration * 1000;
    }

    public int getExposeValidDuration() {
        return exposeValidDuration;
    }

    /**
     * 满足viewability总时长
     *
     * @param exposeValidDuration 单位为秒
     */
    public void setExposeValidDuration(int exposeValidDuration) {
        this.exposeValidDuration = exposeValidDuration * 1000;
    }

    public int getInspectInterval() {
        return inspectInterval;
    }

    /**
     * viewability监测的时间间隔
     *
     * @param inspectInterval 单位毫秒
     */
    public void setInspectInterval(int inspectInterval) {
        this.inspectInterval = inspectInterval;
    }

    public int getVideoExposeValidDuration() {
        return videoExposeValidDuration;
    }

    public void setVideoExposeValidDuration(int videoExposeValidDuration) {
        this.videoExposeValidDuration = videoExposeValidDuration * 1000;
    }
}
