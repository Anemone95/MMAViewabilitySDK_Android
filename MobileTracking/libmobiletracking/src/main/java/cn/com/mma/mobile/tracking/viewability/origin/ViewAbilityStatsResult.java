package cn.com.mma.mobile.tracking.viewability.origin;



import android.text.TextUtils;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import cn.com.mma.mobile.tracking.bean.Argument;
import cn.com.mma.mobile.tracking.viewability.origin.sniffer.ViewFrameSlice;


/**
 * Created by admaster on 17/6/26.
 */
public class ViewAbilityStatsResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /* 对应配置项<Adplacement>标签 广告位标识符 default=d */
    public static final String ADPLACEMENT = "Adplacement";
    /* 对应配置项viewabilityarguments内的<argument>标签 开启ViewAbility监测的唯一标识符 default=2g */
    public static final String IMPRESSIONID = "ImpressionID";

    /* 对应配置项viewabilityarguments内的<argument>标签 开启ViewAbility监测的唯一标识符 default=2j */
    public static final String ADVIEWABILITYEVENTS = "AdviewabilityEvents";

    /* 对应配置项viewabilityarguments内的<argument>标签 ViewAbility监测帧时间戳 default=2t */
    public static final String ADVIEWABILITY_TIME = "AdviewabilityTime";
    /* 对应配置项viewabilityarguments内的<argument>标签 ViewAbility监测原始广告视图尺寸 default=2k */
    public static final String ADVIEWABILITY_FRAME = "AdviewabilityFrame";
    /* 对应配置项viewabilityarguments内的<argument>标签 ViewAbility监测可视左上角坐标 default=2d */
    public static final String ADVIEWABILITY_POINT = "AdviewabilityPoint";
    /* 对应配置项viewabilityarguments内的<argument>标签 ViewAbility监测广告视图透明度 default=2l */
    public static final String ADVIEWABILITY_ALPHA = "AdviewabilityAlpha";
    /* 对应配置项viewabilityarguments内的<argument>标签 ViewAbility监测广告视图是否隐藏 default=2m */
    public static final String ADVIEWABILITY_HIDE = "AdviewabilityHide";
    /* 对应配置项viewabilityarguments内的<argument>标签 ViewAbility监测广告视图覆盖率 default=2n */
    public static final String ADVIEWABILITY_COVERRATE = "AdviewabilityCoverRate";
    /* 对应配置项viewabilityarguments内的<argument>标签 ViewAbility监测广告视图可视尺寸 default=2o */
    public static final String ADVIEWABILITY_SHOWFRAME = "AdviewabilityShowFrame";
    /* 对应配置项viewabilityarguments内的<argument>标签 ViewAbility监测屏幕是否点亮 default=2r */
    public static final String ADVIEWABILITY_LIGHT = "AdviewabilityLight";
    /* 对应配置项viewabilityarguments内的<argument>标签 ViewAbility监测结果是否可见 default=2f */
    public static final String ADVIEWABILITY = "Adviewability";
    /* 对应配置项viewabilityarguments内的<argument>标签 ViewAbility监测结果是否可测量 default=2h */
    public static final String ADMEASURABILITY = "AdMeasurability";

    /* 存储<viewabilityarguments>标签内所有的属性 */
    private HashMap<String, String> viewabilityarguments;
    /* 对应配置项<separator>标签 监测链接QueryString分隔符 default=, */
    private String separator;
    /* 如果监测链接里带有<argument>标签为REDIRECTURL的项,截取出的Value,等待ViewAbility拼装完毕后追加到链接末尾*/
    private String redirectURL;
    /* 是否是视频可视化监测*/
    private boolean isVideoExpose;


    public boolean isVideoExpose() {
        return isVideoExpose;
    }

    public void setVideoExpose(boolean videoExpose) {
        isVideoExpose = videoExpose;
    }

    public String getRedirectURL() {
        return redirectURL == null ? "" : redirectURL;
    }

    public void setRedirectURL(String redirectURL) {
        this.redirectURL = redirectURL;
    }

    public void setSeparator(String separator) {
        this.separator = separator;
    }

    public String getSeparator() {
        return separator;
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

    public String getDefaultViewAbilityData() {

        StringBuilder sb = new StringBuilder();

        String events = viewabilityarguments.get(ADVIEWABILITYEVENTS);
        if (!TextUtils.isEmpty(events)) {
            sb.append(separator);
            String emptyArr = "";
            try {
                emptyArr = URLEncoder.encode("[]", "utf-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            sb.append(events + emptyArr);
        }
        String viewability = viewabilityarguments.get(ADVIEWABILITY);
        if (!TextUtils.isEmpty(viewability)) {
            sb.append(separator);
            sb.append(viewability + "0");
        }
        String measureability = viewabilityarguments.get(ADMEASURABILITY);
        if (!TextUtils.isEmpty(measureability)) {
            sb.append(separator);
            sb.append(measureability + "0");
        }

        return sb.toString();
    }

    public HashMap<String, Object> getViewAbilityEvents(ViewFrameSlice slice) throws Exception {
        HashMap<String, Object> events = new HashMap<>();

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
        String vbhide = viewabilityarguments.get(ADVIEWABILITY_HIDE);
        if (!TextUtils.isEmpty(vbhide)) {
            events.put(vbhide, slice.getHidden());
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

        return events;
    }

}
