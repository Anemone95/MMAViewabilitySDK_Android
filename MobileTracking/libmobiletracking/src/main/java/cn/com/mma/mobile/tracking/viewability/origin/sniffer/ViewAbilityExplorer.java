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

import cn.com.mma.mobile.tracking.util.CommonUtil;
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

    //mzcommit-中点监测
    private boolean mzMidOver = false;
    private boolean mzEndOver = false;
    private String mzVideoProgress = null;
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
        viewFrameBlock.setIsMZURL(isMZURL(adURL)); //mzcommit
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

        //mzcommit-满足条件进行中点监测，否则把mzEndOver置成true，走普通的数据上报流程
        if (viewAbilityStatsResult.isVideoExpose() && viewAbilityStatsResult.getVideoDuration() > 0) {
            mzMidPointVerifyUpload();
        } else  {
            mzEndOver = true;
        }

        //mzcommit-如果已经上报过可见数据了，则只进行中点监测，不再重复上报数据，等超时或view释放停止监测
        if (isVisibleAbility) {
            if (viewFrameBlock.getMaxDuration() >= config.getMaxDuration() || adView == null) {
                if (abilityCallback != null) {
                    abilityCallback.onFinished(adAreaID);
                }
            }
            return;
        }

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

        boolean isMiaozhen = isMZURL(adURL);    //mzcommit

        try {
            JSONArray jsonArray = new JSONArray(events);
            //[]内可视化数据移除所有的引号,并且整体Encode处理
            String repArr = jsonArray.toString().replace("\"", "");
            String separator = viewAbilityStatsResult.getSeparator();
            String equalizer = viewAbilityStatsResult.getEqualizer();

            String eventsArgument = viewAbilityStatsResult.get(ViewAbilityStatsResult.ADVIEWABILITYEVENTS);
            if (!TextUtils.isEmpty(eventsArgument)) {
                //mzcommit-如果不是miaozhen直接记录，如果是miaozhen需要url中配置va=1才记录
                if( !isMiaozhen || (isMiaozhen && isNeedRecord(adURL))) {
                	sb.append(separator);
                	sb.append(eventsArgument);
                	sb.append(equalizer);
                	sb.append(URLEncoder.encode(repArr, "utf-8"));
				}
            }

            String abilityArgument = viewAbilityStatsResult.get(ViewAbilityStatsResult.ADVIEWABILITY);
            if (!TextUtils.isEmpty(abilityArgument)) {
                sb.append(separator);
                sb.append(abilityArgument);
                sb.append(equalizer);
                sb.append(String.valueOf(isVisibleAbility ? 1 : 0));
            }
            String measureArgument = viewAbilityStatsResult.get(ViewAbilityStatsResult.ADMEASURABILITY);
            if (!TextUtils.isEmpty(measureArgument)) {
                sb.append(separator);
                sb.append(measureArgument);
                sb.append(equalizer);
                //如果ViewAbility监测已经到了组装数据的环节,默认MeasureAbility = true
                sb.append("1");
            }

            //mzcommit-加vx参数
            String mzViewability = viewAbilityStatsResult.get(ViewAbilityStatsResult.MZ_VIEWABILITY);
            if (!TextUtils.isEmpty(mzViewability)) {
                sb.append(separator);
                sb.append(mzViewability);
                sb.append(equalizer);
                sb.append(String.valueOf(isVisibleAbility ? 1 : 4));
            }
            //mzcommit-加ve参数
            String mzViewabilityThreshold = viewAbilityStatsResult.get(ViewAbilityStatsResult.MZ_VIEWABILITY_THRESHOLD);
            if (!TextUtils.isEmpty(mzViewabilityThreshold)) {
                sb.append(separator);
                sb.append(mzViewabilityThreshold);
                sb.append(equalizer);
                sb.append(String.valueOf(viewAbilityStatsResult.isVideoExpose() ? config.getVideoExposeValidDuration() : config.getExposeValidDuration()));
            }
            //mzcommit-加vg参数
            String mzViewabilityVideoPlayType = viewAbilityStatsResult.get(ViewAbilityStatsResult.MZ_VIEWABILITY_VIDEO_PLAYTYPE);
            if (!TextUtils.isEmpty(mzViewabilityVideoPlayType)) {
                sb.append(separator);
                sb.append(mzViewabilityVideoPlayType);
                sb.append(equalizer);
                sb.append(String.valueOf(viewAbilityStatsResult.getVideoPlayType()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //sb.append(viewAbilityStatsResult.getRedirectURL()); //fix：普通请求已经带跳转地址进行过一次跳转，可见请求不应该再进行跳转
        abilityURL = sb.toString();

        KLog.d("最终监测链接:" + abilityURL);

        //TODO 回调给AbilityWorker,使用MMASDK执行曝光操作
        if (abilityCallback != null) {
            //mzcommit-可见请求，中点监测还没结束，先把可见数据上报，但不停止定时器
            if (isVisibleAbility && !mzEndOver) {
                abilityCallback.onViewAbilitySend(abilityURL);
            } else {
                abilityCallback.onViewAbilityFinished(adAreaID, abilityURL);
            }
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

    //mzcommit-判断是不是miaozhen的url
    private boolean isMZURL(String url) {
        try {
            String host = CommonUtil.getHostURL(url);
            return host.endsWith(ViewAbilityStatsResult.MZ_COMPANY_DOMAIN);
        } catch (Exception e) {
            return false;
        }
    }

    //mzcommit-从url中取va，va=1记录状态信息
    private boolean isNeedRecord(String url) {
        String mzViewabilityRecord = viewAbilityStatsResult.get(ViewAbilityStatsResult.MZ_VIEWABILITY_RECORD);
        return !TextUtils.isEmpty(mzViewabilityRecord)
                && url.contains(viewAbilityStatsResult.getSeparator() + mzViewabilityRecord + viewAbilityStatsResult.getEqualizer() + "1");
    }

    //mzcommit-中点监测
    private void mzMidPointVerifyUpload() {
        if (mzEndOver) {
            return;
        }

        //取vc参数
        if (mzVideoProgress == null) {
            mzVideoProgress = viewAbilityStatsResult.get(ViewAbilityStatsResult.MZ_VIEWABILITY_VIDEO_PROGRESS);
            if (TextUtils.isEmpty(mzVideoProgress)) {
                mzEndOver = true;
                return;
            }
        }

        //视频中点发请求
        if (!mzMidOver && (viewFrameBlock.getMaxDuration() >= viewAbilityStatsResult.getVideoDuration()/2)) {

            mzMidOver = true;

            if (abilityCallback != null) {
                String midURL = adURL + viewAbilityStatsResult.getSeparator() + mzVideoProgress + viewAbilityStatsResult.getEqualizer() + "mid";
                abilityCallback.onSend(midURL);
            }
        }

        //视频结束发请求
        if (!mzEndOver && (viewFrameBlock.getMaxDuration() >= viewAbilityStatsResult.getVideoDuration())) {

            mzEndOver = true;

            if (abilityCallback != null) {
                String endURL = adURL + viewAbilityStatsResult.getSeparator() + mzVideoProgress + viewAbilityStatsResult.getEqualizer() + "end";
                abilityCallback.onSend(endURL);

                //如果上报过可见数据，中点监测结束还要停止定时器
                if (isVisibleAbility) {
                    abilityCallback.onFinished(adAreaID);
                }
            }
        }
    }

}
