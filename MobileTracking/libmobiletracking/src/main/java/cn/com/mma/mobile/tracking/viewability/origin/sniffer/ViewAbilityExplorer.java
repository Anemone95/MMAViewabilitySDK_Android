package cn.com.mma.mobile.tracking.viewability.origin.sniffer;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import cn.com.mma.mobile.tracking.util.klog.KLog;
import cn.com.mma.mobile.tracking.viewability.origin.ViewAbilityStatsResult;


/**
 * View Ability 探测者 每一次独立的曝光链接会生成一个实体
 * Created by yangxiaolong on 17/6/16.
 * ViewAbilityRanger
 */
public class ViewAbilityExplorer implements Serializable {


    private static final long serialVersionUID = 1L;

    private String adAreaID;
    /*监测链接*/
    private String adURL;
    /*广告视图*/
    private transient View adView;
    /*标识监测链接的唯一ID*/
    private String impressionID;
    /*Ability状态 */
    private AbilityStatus abilityStatus;
    /*曝光可视周期内时间轴块*/
    public ViewFrameBlock viewFrameBlock = null;
    /*配置*/
    private ViewAbilityConfig config;
    /*结合所有参数判断adView是否可视化 0=不可见 1=可见*/
    private boolean isVisibleAbility;

    /*结合所有参数判断adView是否可视化 0=不可见 1=可见*/
    //private boolean isMeasureAbility;

    private transient AbilityCallback abilityCallback = null;

    private ViewAbilityStatsResult viewAbilityStatsResult;


    public ViewAbilityExplorer(String adAreaID, String adURL, View adView, String impressionID, ViewAbilityConfig config) {
        this.adAreaID = adAreaID;
        this.adURL = adURL;
        this.adView = adView;
        this.impressionID = impressionID;
        abilityStatus = AbilityStatus.EXPLORERING;
        this.config = config;
        viewFrameBlock = new ViewFrameBlock(config);
        isVisibleAbility = false;
        //isMeasureAbility = true;
    }


    public void setAbilityCallback(AbilityCallback abilityCallback) {
        this.abilityCallback = abilityCallback;
    }

    public void setViewAbilityStatsResult(ViewAbilityStatsResult viewAbilityStatsResult) {
        this.viewAbilityStatsResult = viewAbilityStatsResult;
    }

    public AbilityStatus getAbilityStatus() {
        return abilityStatus;
    }


    /**
     * 开启可视化监测
     */
    public void onExplore(Context context) {
        try {
            synchronized (ViewAbilityExplorer.class) {
                //如果继续持有AdView的引用,开启监测并
                if (adView != null) {
                    ViewFrameSlice itemSlice = new ViewFrameSlice(adView, context);
                    viewFrameBlock.onPush(itemSlice);
                }
                //验证是否停需要上报数据
                verifyBreakCondition();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 验证当前 frame block 是否满足上报条件
     */
    private void verifyBreakCondition() throws Exception {

        boolean isBreak = false;


        //条件1: 达到曝光最大时长并且当前广告无曝光
        if (viewFrameBlock.getMaxDuration() >= config.getMaxDuration() && viewFrameBlock.getExposeDuration() < 0.001) {
            KLog.w("ID:" + impressionID + " 已经达到最大监测时长,且当前无曝光,终止定时任务,等待数据上报,max duration:" + viewFrameBlock.getMaxDuration() + "  config duration:" + config.getInspectInterval());
            //isMeasureAbility = true;
            isBreak = true;
        } else if (viewFrameBlock.getExposeDuration() >= (viewAbilityStatsResult.isVideoExpose() ? config.getVideoExposeValidDuration() : config.getExposeValidDuration())) { // 条件2: 当前曝光时长已经满足曝光上报条件阈值
            KLog.w("ID:" + impressionID + " 已满足可视曝光时长,终止定时任务,等待数据上报");
            isVisibleAbility = true;
            isBreak = true;
            //isMeasureAbility = true;
        } else if (adView == null) { // 条件3: AdView 释放满足上报条件
            KLog.w("ID:" + impressionID + " AdView 已被释放,终止定时任务,等待数据上报");
            //isMeasureAbility = false;
            isBreak = true;
        }

        if (isBreak) breakToUpload();
    }


    public void breakToUpload() throws Exception {


        abilityStatus = AbilityStatus.WAITINGUPLOAD;

        //TODO: 生成MMA 监测链接  上传最近十条 小于十条全部上传
        HashMap<String, Object> params = new HashMap<>();
        List<HashMap<String, Object>> events = viewFrameBlock.generateUploadEvents(viewAbilityStatsResult);

        params.put(viewAbilityStatsResult.IMPRESSIONID, impressionID);
        params.put(viewAbilityStatsResult.ADVIEWABILITYEVENTS, events);
        params.put(viewAbilityStatsResult.ADVIEWABILITY, isVisibleAbility ? 1 : 0);
        params.put(viewAbilityStatsResult.ADMEASURABILITY, 1);

        JSONObject jsonObject = new JSONObject(params);
        KLog.e("<-------------------------------------------------------------------------------->");
        KLog.d("ID:" + impressionID + " 原始数据帧长度:" + viewFrameBlock.blockLength() + " 准备生成MMA监测链接");
        KLog.json(jsonObject.toString());

        String abilityURL;
        StringBuilder sb = new StringBuilder();
        sb.append(adURL);
        try {
            JSONArray jsonArray = new JSONArray(events);
            String temp = jsonArray.toString().replace("\"", "");
            String separator = viewAbilityStatsResult.getSeparator();

            String vbevents = viewAbilityStatsResult.get(ViewAbilityStatsResult.ADVIEWABILITYEVENTS);
            if (!TextUtils.isEmpty(vbevents)) {
                sb.append(separator);
                sb.append(vbevents + temp);
            }

            String viewability = viewAbilityStatsResult.get(ViewAbilityStatsResult.ADVIEWABILITY);
            if (!TextUtils.isEmpty(viewability)) {
                sb.append(separator);
                sb.append(viewability + String.valueOf(isVisibleAbility ? 1 : 0));
            }
            String measureability = viewAbilityStatsResult.get(ViewAbilityStatsResult.ADMEASURABILITY);
            if (!TextUtils.isEmpty(measureability)) {
                sb.append(separator);
                sb.append(measureability + "1");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        sb.append(viewAbilityStatsResult.getRedirectURL());
        abilityURL = sb.toString();

        KLog.d("最终监测链接:" + abilityURL);

        //TODO 回调给AbilityWorker,使用MMASDK执行曝光操作
        if (abilityCallback != null) {
            abilityCallback.onViewAbilityFinished(adAreaID, abilityURL);
        }
        KLog.e("<-------------------------------------------------------------------------------->");
        abilityStatus = AbilityStatus.UPLOADED;
    }

    /**
     * 供cache Explorer使用,cache时没有保存callback的状态
     *
     * @param abilityCallback
     * @throws Exception
     */
    public void breakToUpload(AbilityCallback abilityCallback) throws Exception {
        if (this.abilityCallback == null) {
            this.abilityCallback = abilityCallback;
        }
        this.breakToUpload();
    }

    @Override
    public String toString() {
        return "[ impressionID=" + impressionID + ",adAreaID=" + adAreaID + ",adURL=" + adURL + ",view=" + adView + " block=" + viewFrameBlock.toString() + " ]";
    }


}
