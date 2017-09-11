package cn.com.mma.mobile.tracking.viewability.origin.sniffer;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import cn.com.mma.mobile.tracking.util.klog.KLog;
import cn.com.mma.mobile.tracking.viewability.origin.ViewAbilityStatsResult;


/**
 * 每一个曝光链接的可视化周期内,会产生一个ViewFrameBlock,存放时间轴内所有符合的点
 * Created by yangxiaolong on 17/6/17.
 */
public class ViewFrameBlock implements Serializable {

    private static final long serialVersionUID = 1L;

    /*时间轴可见有效时间片累计时间 单位ms*/
    private long exposeDuration;
    /*整个监测时间轴累计时长*/
    private long maxDuration;
    /*时间轴上初始时间片*/
    private ViewFrameSlice startSlice;
    /*时间轴上最后一个时间片*/
    private ViewFrameSlice lastSlice;
    /*时间轴上可见有效时间片*/
    private ViewFrameSlice visibleSlice;
    /*时间轴保存有效时间片的数组序列*/
    private List<ViewFrameSlice> framesList;
    /*从配置文件读取全局配置参数*/
    private ViewAbilityConfig config;

    //mzcommit-记录上一次slice的可见判定结果
    private boolean prevIsVisibleSlice = false;
    private boolean isMZURL = false;

    //mzcommit
    public void setIsMZURL(boolean isMZURL) {
        this.isMZURL = isMZURL;
    }

    public ViewFrameBlock(ViewAbilityConfig config) {
        this.config = config;
        exposeDuration = 0;
        maxDuration = 0;
        framesList = new ArrayList<>();
        startSlice = null;
        lastSlice = null;
    }

    public long getExposeDuration() {
        return exposeDuration;
    }

    public long getMaxDuration() {
        return maxDuration;
    }

    /**
     * 每次监测时都将压入栈
     *
     * @param slice
     */
    public void onPush(ViewFrameSlice slice) throws Exception {
        if (slice == null) return;
        int len = framesList.size();
        //第一次监测点使用全局变量start
        if (len == 0) {
            startSlice = slice;
        }

        boolean isValided = isValidedSlice(slice);

        //新的有效点,添加到时间轴序列内
        if (isValided) {

            framesList.add(slice);
            KLog.d("当前帧压入时间轴序列:" + slice.toString());

            //TODO 每次超过max uploadAmount时,移除数组内初始元素,保持数组长度为Max之内
            int count = framesList.size();
            if (count > config.getMaxUploadAmount()) {
                framesList.remove(0);
            }
        }

        //序列最后一条数据赋值给全局变量next
        lastSlice = slice;

        //当前是可见有效帧,统计曝光时长(非累加时长:上一次可见---到本次可见为有效时间)
        boolean visible = slice.validateAdVisible(config.getCoverRateScale());
        if (visible) {
            //如果时间轴内可见有效时间片不存在
            if (visibleSlice == null) {
                visibleSlice = slice;
            }
            //持续可见有效间隔 = 本次有效可见时间片-上次可见有效时间片
            exposeDuration = slice.getCaptureTime() - visibleSlice.getCaptureTime();

        } else {//不可见或无效帧,清除曝光时长统计数据
            visibleSlice = null;
            exposeDuration = 0;
        }

        maxDuration = lastSlice.getCaptureTime() - startSlice.getCaptureTime();

        KLog.v("[collectAndPush] frames`s len:" + framesList.size() + "  needRecord:" + isValided + "  is visible:" + visible + "   持续曝光时长:" + exposeDuration + "    持续监测时长:" + maxDuration + "[" + Thread.currentThread().getId() + "]");

        //mzcommit-更新可见状态
        prevIsVisibleSlice = visible;
    }


    /**
     * 判断是否可压入可见有效帧序列:nextSlice不存在 || currentSlice和nextSlice不一样
     *
     * @param currentSlice
     * @return
     */
    private boolean isValidedSlice(ViewFrameSlice currentSlice) {
        //如果nextPoint不存在.则直接验证通过
        if (lastSlice == null) return true;

        if (isMZURL) {
            //mzcommit-如果是miaozhen的url，把状态变化时的slice压入时间轴
            return prevIsVisibleSlice != currentSlice.validateAdVisible(config.getCoverRateScale());
        } else {
            //如果与时间轴内最后一条数据相同,则验证不通过(本次不需要记录)
            if (lastSlice.isSameAs(currentSlice)) return false;
            return true;
        }
    }


    public int blockLength() {
        return framesList.size();
    }


    /**
     * 帧序列数组截取合适长度,并以HashMap方式输出为数组
     *
     * @return
     */
    public List<HashMap<String, Object>> generateUploadEvents(ViewAbilityStatsResult vaResult) {

        List<HashMap<String, Object>> arrs = new ArrayList<>();
        try {
            int len = framesList.size();
            ViewFrameSlice endSlice = framesList.get(len - 1);
            if (endSlice != null && !endSlice.equals(lastSlice)) {
                KLog.w("将lastSlice装入帧序列末尾");
                framesList.add(lastSlice);
            }

            int maxLen = framesList.size();
            int startIndex = 0;
            //如果采集点长度大于max,则取采集点最后max长度的内容组装
            if (maxLen > config.getMaxUploadAmount()) {
                startIndex = maxLen - config.getMaxUploadAmount();
            }

            for (; startIndex < maxLen; startIndex++) {
                ViewFrameSlice itemSlice = framesList.get(startIndex);
                HashMap<String, Object> dict = vaResult.getViewAbilityEvents(itemSlice);
                arrs.add(dict);
            }

            KLog.v("原始帧长度:" + framesList.size() + "  MaxAmount:" + config.getMaxUploadAmount() + "  截取点:" + startIndex + "  上传长度:" + arrs.size());

        } catch (Exception e) {
            e.printStackTrace();
        }
        return arrs;
    }

    @Override
    public String toString() {
        return "[ exposeDuration=" + exposeDuration + ",maxDuration=" + maxDuration + ",framesList`len=" + framesList.size();
    }

}
