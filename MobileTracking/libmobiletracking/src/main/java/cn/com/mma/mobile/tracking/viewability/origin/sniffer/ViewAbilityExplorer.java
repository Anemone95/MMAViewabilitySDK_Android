package cn.com.mma.mobile.tracking.viewability.origin.sniffer;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;

import cn.com.mma.mobile.tracking.api.Countly;
import cn.com.mma.mobile.tracking.util.klog.KLog;
import cn.com.mma.mobile.tracking.viewability.origin.ViewAbilityStats;


/**
 * View Ability 探测者 每一次独立的曝光链接会生成一个实体
 * Created by yangxiaolong on 17/6/16.
 * ViewAbilityRanger
 */
public class ViewAbilityExplorer implements Serializable {


    private static final long serialVersionUID = 1L;

    private String explorerID;
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

    private boolean viewabilityState = false;//是否满足可视

    /* 是否要进行视频可视化中点监测 */
    private boolean isVideoProcessMonitor = false;
    /* 是否上报可视化数据 */
    private boolean isNeedRecord = true;
    /* 视频可视监测进度列表*/
    private List<AbilityVideoProgress> videoProgressList;
    /* 视频可视进度监测标识符*/
    private String videoProcessIdentifier = null;
    /* 是否正在视频进度监测*/
    private boolean isVideoProcessTracking = false;

    /* 可视监测是否完成*/
    private boolean isViewabilityTrackFinished = false;

    /*结合所有参数判断adView是否可视化 0=不可见 1=可见*/
    //private boolean isMeasureAbility;

    private transient AbilityCallback abilityCallback = null;

    private ViewAbilityStats viewAbilityStats;

    /* 当前监测满足可视曝光时长 从URL中获取,缺省使用config */
    private int exposeValidDuration;


    public ViewAbilityExplorer(String explorerID, String adURL, View adView, String impressionID, ViewAbilityConfig config, ViewAbilityStats result) {
        this.explorerID = explorerID;
        this.adURL = adURL;
        this.adView = adView;
        this.impressionID = impressionID;
        this.abilityStatus = AbilityStatus.EXPLORERING;
        this.config = config;
        this.viewAbilityStats = result;

        //如果监测链接没有动态配置满足可视覆盖比率,使用默认Config的配置
        float coverRate;
        if (viewAbilityStats.getURLShowCoverRate() > 0.0f) {
            coverRate = 1 - viewAbilityStats.getURLShowCoverRate(); //URL动态配置的是可见覆盖比率,计算时使用的是被覆盖比率,为保持统一,在使用时统一使用后者
        } else {
            coverRate = config.getCoverRateScale();
        }

        this.viewFrameBlock = new ViewFrameBlock(result.getViewabilityTrackPolicy(), config.getMaxUploadAmount(), coverRate);
        initConfigParams();
    }

    private void initConfigParams() {
        try {
            //如果监测链接没有动态配置满足可视时长,使用默认Config的配置
            if (viewAbilityStats.getURLExposeDuration() > 0) {
                this.exposeValidDuration = viewAbilityStats.getURLExposeDuration();//URL获取到单位为秒,转化为毫秒
            } else {
                this.exposeValidDuration = viewAbilityStats.isVideoExpose() ? config.getVideoExposeValidDuration() : config.getExposeValidDuration();
            }

            String videoProgressArgument = viewAbilityStats.get(ViewAbilityStats.ADVIEWABILITY_VIDEO_PROGRESS);
            //判断是否进行视频播放进度监测 条件1:视频可视化监测 && 条件2:视频播放时长>0 && 条件3:有配置视频进度监测点 && 条件4:有配置视频进度上报参数
            if (viewAbilityStats.isVideoExpose() && viewAbilityStats.getURLVideoDuration() > 0 && viewAbilityStats.isVideoProgressTrack() && !TextUtils.isEmpty(videoProgressArgument)) {
                isVideoProcessMonitor = true;
                videoProgressList = viewAbilityStats.getURLVideoProcessTrackList();
                videoProcessIdentifier = videoProgressArgument;
            } else {
                isVideoProcessMonitor = false;
            }

            //从原始链接动态提取是否上报监测轨迹,默认收集上报
            String needRecord = viewAbilityStats.get(ViewAbilityStats.ADVIEWABILITY_RECORD);
            if (!TextUtils.isEmpty(needRecord)) {
                String key = viewAbilityStats.getSeparator() + needRecord + viewAbilityStats.getEqualizer() + "0";
                if (adURL.contains(key)) {
                    isNeedRecord = false;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void setAbilityCallback(AbilityCallback abilityCallback) {
        this.abilityCallback = abilityCallback;
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

                //开启视频进度监测
                if (isVideoProcessMonitor && videoProgressList.size() > 0) trackVideoProgress();
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

        if (isViewabilityTrackFinished) {
            //abilityCallback.onFinished(adAreaID);
            return;
        }

        boolean isBreak = false;

        //条件1: 达到曝光最大时长并且当前广告无曝光
        if (viewFrameBlock.getMaxDuration() >= config.getMaxDuration() && viewFrameBlock.getExposeDuration() < 0.001) {
            KLog.w("ID:" + impressionID + " 已经达到最大监测时长,且当前无曝光,终止定时任务,等待数据上报,max duration:" + viewFrameBlock.getMaxDuration() + "  config duration:" + config.getInspectInterval());
            //isMeasureAbility = true;
            isBreak = true;
        } else if (viewFrameBlock.getExposeDuration() >= exposeValidDuration) { // 条件2: 当前曝光时长已经满足曝光上报条件阈值
            KLog.w("ID:" + impressionID + " 已满足可视曝光时长,终止定时任务,等待数据上报");
            viewabilityState = true;
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

        List<HashMap<String, Object>> events = viewFrameBlock.generateUploadEvents(viewAbilityStats);

        if (Countly.LOCAL_TEST) {
            HashMap<String, Object> params = new HashMap<>();
            params.put(viewAbilityStats.IMPRESSIONID, impressionID);
            params.put(viewAbilityStats.ADVIEWABILITYEVENTS, events);
            params.put(viewAbilityStats.ADVIEWABILITY, viewabilityState ? 1 : 0);
            params.put(viewAbilityStats.ADMEASURABILITY, 1);

            JSONObject jsonObject = new JSONObject(params);
            KLog.e("<-------------------------------------------------------------------------------->");
            KLog.d("ID:" + impressionID + " 原始数据帧长度:" + viewFrameBlock.blockLength() + " 准备生成MMA监测链接");
            KLog.json(jsonObject.toString());
        }

        StringBuilder sb = new StringBuilder();
        sb.append(adURL);

        try {
            JSONArray jsonArray = new JSONArray(events);
            //[]内可视化数据移除所有的引号,并且整体Encode处理
            String repArr = jsonArray.toString().replace("\"", "");
            String separator = viewAbilityStats.getSeparator();
            String equalizer = viewAbilityStats.getEqualizer();

            //行为轨迹数据
            String eventsArgument = viewAbilityStats.get(ViewAbilityStats.ADVIEWABILITYEVENTS);
            if (!TextUtils.isEmpty(eventsArgument) && isNeedRecord) {
                sb.append(separator);
                sb.append(eventsArgument);
                sb.append(equalizer);
                sb.append(URLEncoder.encode(repArr, "utf-8"));
            }

            //是否可视
            String abilityArgument = viewAbilityStats.get(ViewAbilityStats.ADVIEWABILITY);
            if (!TextUtils.isEmpty(abilityArgument)) {
                sb.append(separator);
                sb.append(abilityArgument);
                sb.append(equalizer);
                sb.append(String.valueOf(viewabilityState ? 1 : 0));
            }

            //是否可测量
            String measureArgument = viewAbilityStats.get(ViewAbilityStats.ADMEASURABILITY);
            if (!TextUtils.isEmpty(measureArgument)) {
                sb.append(separator);
                sb.append(measureArgument);
                sb.append(equalizer);
                sb.append("1");//如果成功开启可视监测,MeasureAbility即为true
            }

            //视频播放类型,只有视频可视监测才会回传
            String videoTypeArgument = viewAbilityStats.get(ViewAbilityStats.ADVIEWABILITY_VIDEO_PLAYTYPE);
            if (!TextUtils.isEmpty(videoTypeArgument) && viewAbilityStats.isVideoExpose()) {
                sb.append(separator);
                sb.append(videoTypeArgument);
                sb.append(equalizer);
                sb.append(String.valueOf(viewAbilityStats.getVideoPlayType()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        String trackURL = sb.toString();

        KLog.d("最终监测链接:" + trackURL);


        isViewabilityTrackFinished = true;

        if (abilityCallback != null) abilityCallback.onSend(trackURL);

        //如果没有视频进度监测,移除任务
        if (!isVideoProcessMonitor || !isVideoProcessTracking) {
            abilityCallback.onFinished(explorerID);
            abilityStatus = AbilityStatus.UPLOADED;
        }
        KLog.e("<-------------------------------------------------------------------------------->");

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
        return "[ impressionID=" + impressionID + ",explorerID=" + explorerID + ",adURL=" + adURL + ",view=" + adView + " block=" + viewFrameBlock.toString() + " ]";
    }


    /**
     * 视频播放轨迹监测
     */
    private void trackVideoProgress() {

        synchronized (ViewAbilityExplorer.class) {

            isVideoProcessTracking = true;
            AbilityVideoProgress trackType = videoProgressList.get(0);

            long processTime = 0;
            int quarter = viewAbilityStats.getURLVideoDuration() / 4;

            if (trackType == AbilityVideoProgress.TRACK1_4) {
                processTime = quarter;
            } else if (trackType == AbilityVideoProgress.TRACK2_4) {
                processTime = quarter * 2;
            } else if (trackType == AbilityVideoProgress.TRACK3_4) {
                processTime = quarter * 3;
            } else if (trackType == AbilityVideoProgress.TRACK4_4) {
                processTime = quarter * 4;
            }


            //使用总监测时长判断Video进度监测
            if (viewFrameBlock.getMaxDuration() >= processTime) {

                //触发视频进度监测
                if (abilityCallback != null) {
                    //adURL + separator + identifier + equalizer + process
                    String processURL = adURL + viewAbilityStats.getSeparator() + videoProcessIdentifier + viewAbilityStats.getEqualizer() + trackType.value();
                    abilityCallback.onSend(processURL);
                }
                videoProgressList.remove(trackType);
            }

            //最后一个点监测完成后移除任务
            if (videoProgressList.size() == 0 || adView == null) {

                isVideoProcessTracking = false;
                if (isViewabilityTrackFinished && abilityCallback != null) {
                    abilityStatus = AbilityStatus.UPLOADED;
                    abilityCallback.onFinished(explorerID);
                }

            }
        }
    }

}
