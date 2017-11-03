package cn.com.mma.mobile.tracking.viewability.origin;



import android.text.TextUtils;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import cn.com.mma.mobile.tracking.bean.Argument;
import cn.com.mma.mobile.tracking.viewability.origin.sniffer.AbilityVideoProgress;
import cn.com.mma.mobile.tracking.viewability.origin.sniffer.ViewFrameSlice;


/**
 * Created by mma on 17/6/26.
 */
public class ViewAbilityStats implements Serializable {

    private static final long serialVersionUID = 1L;

    /* 对应配置项<Adplacement>标签 广告位标识符 d */
    public static final String ADPLACEMENT = "Adplacement";

    //对应配置项viewabilityarguments内的<argument>标签 start

    /* 可视监测唯一标识符 2g */
    public static final String IMPRESSIONID = "ImpressionID";
    /* 轨迹帧数组 2j */
    public static final String ADVIEWABILITYEVENTS = "AdviewabilityEvents";
    /* 轨迹帧时间戳 2t */
    public static final String ADVIEWABILITY_TIME = "AdviewabilityTime";
    /* 原始广告视图尺寸 2k */
    public static final String ADVIEWABILITY_FRAME = "AdviewabilityFrame";
    /* 可视视图左上角坐标 2d */
    public static final String ADVIEWABILITY_POINT = "AdviewabilityPoint";
    /* 可视视图透明度 2l */
    public static final String ADVIEWABILITY_ALPHA = "AdviewabilityAlpha";
    /* 可视视图是否隐藏 2m */
    public static final String ADVIEWABILITY_SHOWN = "AdviewabilityShown";
    /* 可视视图覆盖率 2n */
    public static final String ADVIEWABILITY_COVERRATE = "AdviewabilityCoverRate";
    /* 可视视图可视尺寸 2o */
    public static final String ADVIEWABILITY_SHOWFRAME = "AdviewabilityShowFrame";
    /* 屏幕是否点亮 2r */
    public static final String ADVIEWABILITY_LIGHT = "AdviewabilityLight";
    /* 监测可视性 2f */
    public static final String ADVIEWABILITY = "Adviewability";
    /* 监测可测量性 2h */
    public static final String ADMEASURABILITY = "AdMeasurability";
    /* 监测轨迹数据是否上报 va */
    public static final String ADVIEWABILITY_RECORD = "AdviewabilityRecord";
    /* 是否开启可视监测 2j */
    public static final String ADVIEWABILITY_ENABLE = "AdviewabilityEnable";
    /* 满足可视时长 2u */
    public static final String ADVIEWABILITY_CONFIG_THRESHOLD = "AdviewabilityConfigThreshold";
    /* 满足可视覆盖比率 2v */
    public static final String ADVIEWABILITY_CONFIG_AREA = "AdviewabilityConfigArea";
    /* 视频进度监测标识符 2a */
    public static final String ADVIEWABILITY_VIDEO_PROGRESS = "AdviewabilityVideoProgress";
    /* 视频播放类型 1a */
    public static final String ADVIEWABILITY_VIDEO_PLAYTYPE = "AdviewabilityVideoPlayType";
    /* 视频播放时长 2w */
    public static final String ADVIEWABILITY_VIDEO_DURATION = "AdviewabilityVideoDuration";
    /* 视频进度监测配置 2x */
    public static final String ADVIEWABILITY_VIDEO_PROGRESSPOINT = "AdviewabilityVideoProgressPoint";

    //对应配置项viewabilityarguments内的<argument>标签 end

    /* 存储<viewabilityarguments>标签内所有的属性 */
    private HashMap<String, String> viewabilityarguments;
    /* 对应配置项<separator>标签 属性分隔符 default=, */
    private String separator;
    /* 对应配置项<equalizer>标签 属性链接符 default=空字符*/
    private String equalizer;



    /* 是否是视频可视化监测*/
    private boolean isVideoExpose;

    private int videoPlayType;  //mzcommit-加播放类型：0-无法识别，1-自动，2-手动

    private int urlVideoDuration;  //mzcommit-视频广告时长，vb，从url中获取

    private int urlExposeDuration;

    private float urlShowCoverRate;

    /* 可视化采集收集策略: 0=TrackPositionChanged,1=TrackVisibleChanged*/
    private int viewabilityTrackPolicy;

    private List<AbilityVideoProgress> videoTrackList;


    public void setViewabilityTrackPolicy(int trackPolicy) {
        //默认使用TrackPositionChanged
        if (trackPolicy < 0 || trackPolicy > 1) trackPolicy = 0;
        this.viewabilityTrackPolicy = trackPolicy;
    }

    public int getViewabilityTrackPolicy() {
        return viewabilityTrackPolicy;
    }

    public boolean isVideoExpose() {
        return isVideoExpose;
    }

    public void setVideoExpose(boolean videoExpose) {
        isVideoExpose = videoExpose;
    }

    public void setSeparator(String separator) {
        this.separator = separator;
    }

    public String getSeparator() {
        return separator;
    }

    //mzcommit-begin
    public void setEqualizer(String equalizer) {
        this.equalizer = equalizer;
    }

    public String getEqualizer() {
        return equalizer;
    }

    public void setVideoPlayType(int videoPlayType) {
        this.videoPlayType = videoPlayType;
    }

    public int getVideoPlayType() {
        return videoPlayType;
    }




    public int getURLVideoDuration() {
        return urlVideoDuration;
    }

    public int getURLExposeDuration() {
        return urlExposeDuration;
    }


    public float getURLShowCoverRate() {
        return urlShowCoverRate;
    }

    public boolean isVideoProgressTrack() {
        return (videoTrackList != null && videoTrackList.size() > 0);
    }

    public List<AbilityVideoProgress> getURLVideoProcessTrackList() {
        return videoTrackList;
    }


    /**
     * 通过监测链接设置动态可视时长
     * @param originURL
     */
    public void setURLExposeDuration(String originURL) {
        try {
            String configThresholdArgument = get(ViewAbilityStats.ADVIEWABILITY_CONFIG_THRESHOLD);
            if (!TextUtils.isEmpty(configThresholdArgument)) {
                String exposeDuration = getValueFromURL(configThresholdArgument, originURL);
                this.urlExposeDuration = Integer.valueOf(exposeDuration) * 1000;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 通过监测链接设置动态可视覆盖率
     * @param originURL
     */
    public void setURLShowCoverRate(String originURL) {
        try {
            String configAreaArgument = get(ViewAbilityStats.ADVIEWABILITY_CONFIG_AREA);
            if (!TextUtils.isEmpty(configAreaArgument)) {
                String showCoverRateStr = getValueFromURL(configAreaArgument, originURL);
                float showCoverRate = Integer.valueOf(showCoverRateStr) / 100.0f;
                if (showCoverRate > 0.0f && showCoverRate < 1.0f)
                    this.urlShowCoverRate = showCoverRate;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 通过监测链接设置动态视频播放时长
     * @param originURL
     */
    public void setURLVideoDuration(String originURL) {
        try {
            String videoDurationArgument = get(ViewAbilityStats.ADVIEWABILITY_VIDEO_DURATION);
            if (!TextUtils.isEmpty(videoDurationArgument)) {
                String videoDurationStr = getValueFromURL(videoDurationArgument, originURL);
                int videoDuration = Integer.valueOf(videoDurationStr) * 1000;
                this.urlVideoDuration = videoDuration;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 通过监测链接设置动态视频进度监测点
     * @param originURL
     */
    public void setURLVideoProgressTracks(String originURL) {
        try {
            String videoProgressPointArgument = get(ViewAbilityStats.ADVIEWABILITY_VIDEO_PROGRESSPOINT);
            if (!TextUtils.isEmpty(videoProgressPointArgument)) {
                String rawParams = getValueFromURL(videoProgressPointArgument, originURL);
                if (!TextUtils.isEmpty(rawParams) && rawParams.length() == 4) {
                    videoTrackList = new ArrayList<>();
                    boolean isTrack = Integer.parseInt(rawParams.substring(0, 1)) == 1;
                    if (isTrack) videoTrackList.add(AbilityVideoProgress.TRACK1_4);
                    isTrack = Integer.parseInt(rawParams.substring(1, 2)) == 1;
                    if (isTrack) videoTrackList.add(AbilityVideoProgress.TRACK2_4);
                    isTrack = Integer.parseInt(rawParams.substring(2, 3)) == 1;
                    if (isTrack) videoTrackList.add(AbilityVideoProgress.TRACK3_4);
                    isTrack = Integer.parseInt(rawParams.substring(3, 4)) == 1;
                    if (isTrack) videoTrackList.add(AbilityVideoProgress.TRACK4_4);
                }
            }
        } catch (Exception e) {
            //数据解析出现异常,则清除所有进度监测点
            videoTrackList.clear();
            e.printStackTrace();
        }
    }




    public String get(String key) {
        return viewabilityarguments.get(key);
    }

    public void setViewabilityarguments(HashMap<String, Argument> arguments ) {

        HashMap<String, String> viewabilityarguments = new HashMap<>();

        if (arguments != null && arguments.size() > 0) {
            // Logger.w("监测链接:" + originUrl + " 没有对应的ViewAbility配置项,请检查sdkconfig.xml的Company下config标签是否存在<viewabilityarguments>配置!");
            for (String argumentKey : arguments.keySet()) {
                String key = argumentKey;
                if (!TextUtils.isEmpty(key)) {
                    String value = arguments.get(key).value;
                    if (!TextUtils.isEmpty(value))
                        viewabilityarguments.put(key, value);
                }
            }
        }

        this.viewabilityarguments = viewabilityarguments;
    }


    /**
     * 可视化监测开启失败时,链接串入默认的ViewAbility数据
     * @return
     */
    public String getFailedViewabilityParams() {

        StringBuilder sb = new StringBuilder();

        String eventArgument = viewabilityarguments.get(ADVIEWABILITYEVENTS);
        if (!TextUtils.isEmpty(eventArgument)) {
            sb.append(separator);
            String emptyArr = "";
            try {
                emptyArr = URLEncoder.encode("[]", "utf-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            sb.append(eventArgument);
            sb.append(equalizer);
            sb.append(emptyArr);
        }

        String viewability = viewabilityarguments.get(ADVIEWABILITY);
        if (!TextUtils.isEmpty(viewability)) {
            sb.append(separator);
            sb.append(viewability);
            sb.append(equalizer);
            sb.append("0");
        }


        String measureability = viewabilityarguments.get(ADMEASURABILITY);
        if (!TextUtils.isEmpty(measureability)) {
            sb.append(separator);
            sb.append(measureability);
            sb.append(equalizer);
            sb.append("0");
        }

        return sb.toString();
    }

    /**
     * 返回每一帧的轨迹数据
     * @param slice
     * @return
     */
    public HashMap<String, Object> getAbilitySliceTrackEvents(ViewFrameSlice slice) {
        HashMap<String, Object> events = new HashMap<>();

        try {
            String vbtime = viewabilityarguments.get(ADVIEWABILITY_TIME);
            if (!TextUtils.isEmpty(vbtime)) {
                events.put(vbtime, slice.getCaptureTime());
            }
            String vbframe = viewabilityarguments.get(ADVIEWABILITY_FRAME);
            if (!TextUtils.isEmpty(vbframe)) {
                events.put(vbframe, slice.getAdSize());
            }
            String vbpoint = viewabilityarguments.get(ADVIEWABILITY_POINT);
            if (!TextUtils.isEmpty(vbpoint)) {
                events.put(vbpoint, slice.getVisiblePoint());
            }
            String vbalpha = viewabilityarguments.get(ADVIEWABILITY_ALPHA);
            if (!TextUtils.isEmpty(vbalpha)) {
                events.put(vbalpha, slice.getAlpha());
            }
            String vbshown = viewabilityarguments.get(ADVIEWABILITY_SHOWN);
            if (!TextUtils.isEmpty(vbshown)) {
                events.put(vbshown, slice.getShown());
            }
            String vbrate = viewabilityarguments.get(ADVIEWABILITY_COVERRATE);
            if (!TextUtils.isEmpty(vbrate)) {
                events.put(vbrate, slice.getCoverRate());
            }
            String vbshowframe = viewabilityarguments.get(ADVIEWABILITY_SHOWFRAME);
            if (!TextUtils.isEmpty(vbshowframe)) {
                events.put(vbshowframe, slice.getVisibleSize());
            }
            String vblight = viewabilityarguments.get(ADVIEWABILITY_LIGHT);
            if (!TextUtils.isEmpty(vblight)) {
                events.put(vblight, slice.getScreenOn());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return events;
    }

    /**
     * 从监测链接里获取配置项字段对应的值
     *
     * @param identifier 标识符
     * @param adURL      原始链接
     * @return
     * @throws Exception
     */
    private String getValueFromURL(String identifier, String adURL) throws Exception {
        String objValue = "";
        String[] splits = adURL.split(separator);
        String regex = identifier + equalizer;
        for (String item : splits) {
            if (item.startsWith(regex)) {
                objValue = item.replaceFirst(regex, "");
                break;
            }
        }
        return objValue;
    }

}
