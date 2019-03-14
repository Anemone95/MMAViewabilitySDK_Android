package cn.com.mma.mobile.tracking.api;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import cn.com.mma.mobile.tracking.bean.Argument;
import cn.com.mma.mobile.tracking.bean.Company;
import cn.com.mma.mobile.tracking.bean.SDK;
import cn.com.mma.mobile.tracking.util.CommonUtil;
import cn.com.mma.mobile.tracking.util.DeviceInfoUtil;
import cn.com.mma.mobile.tracking.util.Logger;
import cn.com.mma.mobile.tracking.util.SdkConfigUpdateUtil;
import cn.com.mma.mobile.tracking.util.klog.KLog;
import cn.com.mma.mobile.tracking.viewability.origin.ViewAbilityEventListener;
import cn.com.mma.mobile.tracking.viewability.origin.ViewAbilityService;
import cn.com.mma.mobile.tracking.viewability.origin.ViewAbilityStats;
import cn.com.mma.mobile.tracking.viewability.origin.sniffer.ViewAbilityConfig;
import cn.com.mma.mobile.tracking.viewability.webjs.ViewAbilityJsService;


/**
 * Countly 和 ViewAbilityService中间件:负责控制View Ability的初始化和监测
 * Created by mma on 17/6/22.
 */
public class ViewAbilityHandler {

    enum MonitorType {
        CLICK,
        EXPOSE,
        EXPOSEWITHABILITY,
        VIDEOEXPOSEWITHABILITY
    }

    private Context context;
    /* 到MMASDK执行事件监测和上报的回调函数 */
    private ViewAbilityEventListener mmaSdkCallback;
    /* 用于存储impressionID的字典 */
    private HashMap<String, String> impressions = null;

    /* sdkconfig.xml配置文件实体映射类 */
    private SDK sdkConfig;

    private ViewAbilityConfig abilityConfig;

    /* ViewAbility native 方式监测服务 */
    private ViewAbilityService viewAbilityService;
    /* ViewAbility JS 方式监测服务 */
    private ViewAbilityJsService viewAbilityJsService;

    public ViewAbilityHandler(Context context, ViewAbilityEventListener mmaSdkCallback, SDK sdk) {
        this.context = context;
        this.mmaSdkCallback = mmaSdkCallback;
        impressions = new HashMap<>();
        sdkConfig = sdk;
        abilityConfig = initViewAbilityGlobalConfig();
        viewAbilityService = new ViewAbilityService(context, mmaSdkCallback, abilityConfig);
        viewAbilityJsService = new ViewAbilityJsService(context);
    }

    /**
     * 初始化可视化配置,网络动态更新,需要下次启动时生效
     * @return
     */
    private ViewAbilityConfig initViewAbilityGlobalConfig() {

        ViewAbilityConfig config = new ViewAbilityConfig();
        try {
            if (sdkConfig != null && sdkConfig.viewAbility != null) {
                //viewability曝光监测时间间隔
                config.setInspectInterval(sdkConfig.viewAbility.intervalTime);
                //满足viewability曝光持续时长
                config.setExposeValidDuration(sdkConfig.viewAbility.viewabilityTime);
                //config内配置的是可见比率(int),计算时使用的是被覆盖比率,为保持统一,在使用时统一使用后者
                float showCoverRate = sdkConfig.viewAbility.viewabilityFrame / 100.0f;
                config.setCoverRateScale(1 - showCoverRate);
                //最大监测时长
                config.setMaxDuration(sdkConfig.viewAbility.maxExpirationSecs);
                //最大上报数量
                config.setMaxUploadAmount(sdkConfig.viewAbility.maxAmount);
                //最大满足ViewAbility 视频曝光持续时长
                config.setVideoExposeValidDuration(sdkConfig.viewAbility.viewabilityVideoTime);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return config;
    }


    /**
     * JS可视监测
     * @param adURL
     * @param adView
     * @param isVideo
     */
    public void onJSExpose(String adURL, View adView, boolean isVideo) {

        //Company为空,不监测
        Company company = getCompany(adURL);
        if (company == null || TextUtils.isEmpty(company.name)) {
            Logger.w("监测链接:" + adURL + " 没有对应的配置项,请检查sdkconfig.xml是否存在链接域名对应的Company配置!");
            return;
        }

        viewAbilityJsService.addTrack(adURL, adView, company, isVideo);

    }


    /**
     * 普通点击监测
     * AdView=NULL,VideoType=0
     *
     * @param originUrl
     */
    public void onClick(String originUrl) {
        handlerOriginURL(originUrl, MonitorType.CLICK, null, 0);
    }

    /**
     * 普通曝光监测
     * AdView=NULL,VideoType=0
     *
     * @param originUrl
     */
    public void onExpose(String originUrl) {
        handlerOriginURL(originUrl, MonitorType.EXPOSE, null, 0);
    }

    /**
     * Banner可视化监测
     * VideoType=0
     *
     * @param originUrl
     * @param adView
     */
    public void onExpose(String originUrl, View adView) {
        handlerOriginURL(originUrl, MonitorType.EXPOSEWITHABILITY, adView, 0);
    }


    /**
     * Video可视化监测
     *
     * @param originUrl
     * @param videoView
     * @param videoPlayType 传视频播放类型，1-自动播放，2-手动播放，0-无法识别
     */
    public void onVideoExpose(String originUrl, View videoView, int videoPlayType) {
        handlerOriginURL(originUrl, MonitorType.VIDEOEXPOSEWITHABILITY, videoView, videoPlayType);
    }

    /**
     * 停止可视化监测
     *
     * @param originUrl
     */
    public void stop(String originUrl) {
        //Company为空,无法停止
        Company company = getCompany(originUrl);
        if (company == null) {
            Logger.w("监测链接:" + originUrl + " 没有对应的配置项,请检查sdkconfig.xml是否存在链接域名对应的Company配置!");
            return;
        }

        String adAreaID = null;
        try {
            adAreaID = getAdAreaID(company, originUrl);
        } catch (Exception e) {
            e.printStackTrace();
        }

        String explorerID = company.domain.url + adAreaID;
        viewAbilityService.stopViewAbilityMonitor(explorerID);
    }

    private void handlerOriginURL(String originUrl, MonitorType monitorType, View adView, int videoPlayType) {


        //Company为空,无法监测
        Company company = getCompany(originUrl);
        if (company == null) {
            Logger.w("监测链接:" + originUrl + " 没有对应的配置项,请检查sdkconfig.xml是否存在链接域名对应的Company配置!");
            return;
        }

        //[1] 截取出原监测链接u参数之后所有的内容 REDIRECTURL
        String redirectPrefix = getRedirectIdentifier(company) + company.equalizer;
        String redirectStr = "";
        String patternStr = company.separator + redirectPrefix + ".*"; //,u=.*
        Pattern pattern = Pattern.compile(patternStr);
        Matcher matcher = pattern.matcher(originUrl);
        if (matcher.find()) {
            redirectStr = matcher.group(0);
        }


        //[2] 监测链接截取掉REDIRECTURL字段之后的所有内容,重新组装为withoutRedirectURL
        String withoutRedirectURL;
        StringBuilder sb = new StringBuilder();
        try {
            String[] splits = originUrl.split(company.separator);
            for (String item : splits) {
                //如果遇到redirect标识,之后的内容都移除掉
                if (item.startsWith(redirectPrefix)) {
                    break;
                }
                sb.append(item);
                sb.append(company.separator);
            }
            sb.deleteCharAt(sb.length() - 1);
            withoutRedirectURL = sb.toString();

        } catch (Exception e) {
            e.printStackTrace();
            withoutRedirectURL = originUrl;
        }

        StringBuffer exposeURL = new StringBuffer();
        try {

            //将配置文件的arguments映射到JavaBean<HashMap>
            ViewAbilityStats abilityStats = new ViewAbilityStats();
            abilityStats.setSeparator(company.separator);
            abilityStats.setEqualizer(company.equalizer);
            abilityStats.setViewabilityarguments(company.config.viewabilityarguments);
            // viewAbilityStats.setIsMZURL(company.name.equals(Constant.MZ_COMPANY_NAME));


            //[3] 原始监测链接中获取广告位ID,然后通过广告位ID生成ImpressionID
            String adAreaID = getAdAreaID(company, withoutRedirectURL);

            String impressionAgument = abilityStats.get(ViewAbilityStats.IMPRESSIONID);
            String impressionID = "";
            String impressionValue = "";
            if (!TextUtils.isEmpty(impressionAgument)) {
                impressionID = getImpressionID(company, monitorType, adAreaID);
                sb = new StringBuilder();
                sb.append(company.separator);
                sb.append(impressionAgument);
                sb.append(company.equalizer);
                sb.append(impressionID);
                impressionValue = sb.toString();
            }

            //如果调用普通和视频可视化监测接口,需要从监测链接获取动态配置参数和可视参数过滤
            if (monitorType == MonitorType.EXPOSEWITHABILITY || monitorType == MonitorType.VIDEOEXPOSEWITHABILITY) {

                //从配置中读取采集策略
                abilityStats.setViewabilityTrackPolicy(company.sswitch.viewabilityTrackPolicy);
                //从原始监测链接动态提取满足可视时长
                abilityStats.setURLExposeDuration(withoutRedirectURL);
                //从原始监测链接动态提取可视覆盖比率
                abilityStats.setURLShowCoverRate(withoutRedirectURL);

                if (monitorType == MonitorType.VIDEOEXPOSEWITHABILITY) {
                    //标记当前为视频可视监测
                    abilityStats.setVideoExpose(true);
                    //记录视频播放类型
                    abilityStats.setVideoPlayType(videoPlayType);
                    //从原始链接动态提取视频广告时长，单位s 用于视频进度监测
                    abilityStats.setURLVideoDuration(withoutRedirectURL);
                    //从原始链接动态提取视频广告过程监测配置
                    abilityStats.setURLVideoProgressTracks(withoutRedirectURL);
                }

                //[4] 去u监测链接过滤占用字段:除了某些需要用到的参数会保留,其余在sdkconfig.xml中<viewabilityarguments>出现的关键字都会替换为空
                String filteredURL = filterIdentifiers(company, abilityStats, withoutRedirectURL);

                //[5] 过滤后的URL作为可视监测补发的普通曝光,同时需要串入ImpressionID
                exposeURL.append(filteredURL);
                exposeURL.append(impressionValue);

                //[6] 通过AdviewabilityEnable确定链接是否是可视监测链接
                // boolean isViewAbility = checkViewAbilityEnabled(viewAbilityStats, withoutRedirectURL);
//                if (isViewAbility) {
//                }
                StringBuffer viewabilityURL = new StringBuffer();
                viewabilityURL.append(exposeURL);

                //[7] 如果是可视化监测,普通曝光需要追加标识字段,ad:2f,mz:vx
                String viewAbilityArgument = abilityStats.get(ViewAbilityStats.ADVIEWABILITY);
                if (!TextUtils.isEmpty(viewAbilityArgument)) {
                    sb = new StringBuilder();
                    sb.append(company.separator);
                    sb.append(viewAbilityArgument);
                    String viewability = sb.toString();
                    exposeURL.append(viewability);
                }

                String viewAbilityArgumentResult = abilityStats.get(ViewAbilityStats.ADVIEWABILITY_RESULT);
                if (!TextUtils.isEmpty(viewAbilityArgumentResult)) {
                    sb = new StringBuilder();
                    sb.append(company.separator);
                    sb.append(viewAbilityArgumentResult);
                    sb.append(company.equalizer);
                    sb.append("0");
                    String viewabilityResult = sb.toString();
                    exposeURL.append(viewabilityResult);
                }

                if (adView != null && (adView instanceof View)) {

                    //开启线程执行ViewAbility可视化监测
                    String explorerID = company.domain.url + adAreaID;
                    viewAbilityService.addViewAbilityMonitor(viewabilityURL.toString(), adView, impressionID, explorerID, abilityStats);

                } else {//如果传入View为空或者非View对象,则可视化监测结果为不可见:Adviewability=0,不可测量:AdMeasurability=0
                    Logger.w("监测链接传入的AdView为空,以正常曝光方式监测.");
                    String failedParams = abilityStats.getFailedViewabilityParams();//2j[],2f0,2h0
                    viewabilityURL.append(failedParams);
                    mmaSdkCallback.onEventPresent(viewabilityURL.toString());
                }


            } else { //如果调用普通曝光/点击接口,不需要可视化监测逻辑

                //如果有配置ImpressionID Argument,普通曝光只需要串入impressionID,不串入
                if (!TextUtils.isEmpty(impressionValue)) {
                    String regex = company.separator + impressionAgument + company.equalizer + "[^" + company.separator + "]*";
                    //监测链接替换原有impressionID KEY和VALUE
                    String filteredURL = withoutRedirectURL.replaceAll(regex, "");
                    exposeURL.append(filteredURL);
                    exposeURL.append(impressionValue);
                } else {
                    exposeURL.append(withoutRedirectURL);
                }
            }

            //[7] 普通曝光参数拼装完毕后需要重新追加上REDIRECTURL字段
            exposeURL.append(redirectStr);


            //[LOCALTEST] 测试计数:普通曝光事件产生计数
            if (Countly.LOCAL_TEST) {

                Intent intent = new Intent(Countly.ACTION_STATS_EXPOSE);
                context.sendBroadcast(intent);

                KLog.i("********************************************");
                KLog.i("originURL:" + originUrl);
                KLog.i("monitiorType:" + monitorType);
                KLog.i("REDIRECT_STR:" + redirectStr);
                KLog.i("withoutRedirectURL:" + withoutRedirectURL);
                KLog.i("adAreaID:" + adAreaID);
                KLog.i("imressionID:" + impressionID);
                KLog.i("trackURL:" + exposeURL.toString());
                KLog.i("adView:" + adView);
                KLog.i("eventListener:" + mmaSdkCallback);
                KLog.i("********************************************");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }


        //[8] 最终将REDIRECTURL重新拼装到URL末尾,并回调原有Countly线程单独发送监测事件
        mmaSdkCallback.onEventPresent(exposeURL.toString());

    }


    /**
     * 以配置文件config.adplacements.ADPLACEMENT作为广告位标识符
     *
     * @param company
     * @return
     * @throws Exception
     */
    private String getAdAreaIdentifier(Company company) throws Exception {
        String adAreaIdentifier = "";
        HashMap<String, Argument> adplacements = company.config.adplacements;
        if (adplacements != null) {
            Argument argument = adplacements.get(ViewAbilityStats.ADPLACEMENT);
            if (argument != null) adAreaIdentifier = argument.value;
        }
        return adAreaIdentifier;
    }


    /**
     * 获取配置文件config.arguments标签内的REDIRECTURL的值
     *
     * @param company
     * @return
     * @throws Exception
     */
    private String getRedirectIdentifier(Company company) {
        String redirectIdentifier = "u";
        List<Argument> arguments = company.config.arguments;
        if (arguments != null) {
            for (Argument argument : arguments) {
                if (argument != null && !TextUtils.isEmpty(argument.key)) {
                    if (argument.key.equals("REDIRECTURL")) {
                        redirectIdentifier = argument.value;
                        break;
                    }
                }
            }
        }
        return redirectIdentifier;
    }


    /**
     * 判断监测链接是否包含可视化监测标识
     * 链接里是否包含可视化配置项:AdviewabilityEnable
     *
     * @param originUrl
     * @return
     */
    private boolean checkViewAbilityEnabled(ViewAbilityStats abilityStatsResult, String originUrl) throws Exception {

        String[] splits = originUrl.split(abilityStatsResult.getSeparator());

        String viewabilityIdentifier = abilityStatsResult.get(ViewAbilityStats.ADVIEWABILITY_ENABLE);

        if (!TextUtils.isEmpty(viewabilityIdentifier)) {
            //2g+equalizer
            String prefix = viewabilityIdentifier + abilityStatsResult.getEqualizer();

            for (String item : splits) {
                boolean hasPrefix = item.startsWith(prefix);
                //字符串截取,最后拼装
                if (hasPrefix) {
                    return true;
                }
            }
        }

        return false;
    }


    /**
     * 获取监测代码对应的监测公司配置对象
     * 注意：严格校验模式：如果输入的一个 http://g.cn.miaozhen.ks 类似这样的监测链接，在获取对应监测公司配置对象（.miaozhen.com）时，
     * 会因为找不到匹配的HOST（http://g.cn.miaozhen.ks）而返回NULL，该次监测无法完成，没有数据包上报。
     *
     * @param adURL
     * @return
     */
    private Company getCompany(String adURL) {

        if (sdkConfig != null && sdkConfig.companies != null) {
            String host = CommonUtil.getHostURL(adURL);
            for (Company company : sdkConfig.companies) {
                //广告监测HOST要和配置文件的公司域名相符
                if (host.endsWith(company.domain.url)) {
                    return company;
                }
            }
        } else {//如果配置缺失,重新获取
            sdkConfig = SdkConfigUpdateUtil.getSDKConfig(context);
        }

        return null;
    }


    /**
     * 从URL里提取出广告位ID
     *
     * @param adUrl
     * @return
     */
    private String getAdAreaID(Company company, String adUrl) throws Exception {
        String adAreaId = "";
        String adAreaIdentifier = getAdAreaIdentifier(company);
        String[] splits = adUrl.split(company.separator);
        for (String item : splits) {
            if (item.startsWith(adAreaIdentifier)) {
//                int startPoint = adAreaIdentifier.length() + company.equalizer.length();
//                adAreaId = item.substring(startPoint, item.length());

                //替换广告位标识符+属性连接符=广告位ID
                String regex = adAreaIdentifier + company.equalizer;
                adAreaId = item.replaceFirst(regex, "");
                break;
            }
        }
        return adAreaId;
    }


    /**
     * adAreaID + company.domain[0]
     *
     * @param monitorType
     * @param adAreaId
     * @return
     * @throws Exception
     */
    private String getImpressionID(Company company, MonitorType monitorType, String adAreaId) throws Exception {

        String impressionID = "";

        String adidKey = company.domain.url + adAreaId;

        //点击检测是从已经存在的池子内查找是否有存在的广告位及ID
        if (monitorType == MonitorType.CLICK) {
            for (String adidkey : impressions.keySet()) {
                if (adidKey.equals(adidkey)) {
                    impressionID = impressions.get(adidkey);
                    KLog.i("广告位:" + adidKey + " 存在对应的impressionID:" + impressionID);
                    break;
                }
            }
        } else {//普通曝光或带可视化监测的曝光,每次触发时都生成新的ImpressionID,并存储
            impressionID = generateImpressionID(context, adAreaId);
            KLog.i("广告位:" + adidKey + " 不存在对应的impressionID,即将生成:" + impressionID);
            impressions.put(adidKey, impressionID);
        }

        return impressionID;
    }

    private static String generateImpressionID(Context context, String adAreaId) {
        try {
            String mac = DeviceInfoUtil.getMacAddress(context);
            String imei = DeviceInfoUtil.getImei(context);
            String android = DeviceInfoUtil.getAndroidId(context);
            String at = String.valueOf(System.currentTimeMillis());
            String impressionId = imei + android + mac + adAreaId + at;
            return CommonUtil.md5(impressionId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    //mzcommit-url中包含的配置文件中已有的参数去掉后重新添加
    private String filterIdentifiers(Company company, ViewAbilityStats abilityStats, String adURL) {

        String filteredURL;

        try {
            HashMap<String, Argument> arguments = company.config.viewabilityarguments;
            String separator = company.separator;
            String equalizer = company.equalizer;

            String showCoverRate = "";
            String exposeDuration = "";
            String filterURL = adURL;

            //如果缺失viewabilityargument配置项,直接返回
            if (arguments == null) return filterURL;

            //去掉viewabilityarguments中的参数
            for (String argumentKey : arguments.keySet()) {
                if (!TextUtils.isEmpty(argumentKey)) {
                    String value = arguments.get(argumentKey).value;
                    if (!TextUtils.isEmpty(value)) {

                        //部分参数需要保留
                        if (argumentKey.equals(ViewAbilityStats.ADVIEWABILITY_RECORD)) {//va
                            continue;
                        } else if (argumentKey.equals(ViewAbilityStats.ADVIEWABILITY_VIDEO_DURATION)) {//vb||2w
                            continue;
                        } else if (argumentKey.equals(ViewAbilityStats.ADVIEWABILITY_VIDEO_PROGRESSPOINT)) {//2x
                            continue;
                        } else if (argumentKey.equals(ViewAbilityStats.ADVIEWABILITY_CONFIG_THRESHOLD)) {//vi||2u
                            try {
                                int urlExposeDuration = abilityStats.getURLExposeDuration();
                                if (urlExposeDuration > 0) {
                                    urlExposeDuration = urlExposeDuration / 1000;
                                } else {
                                    urlExposeDuration = abilityStats.isVideoExpose() ? abilityConfig.getVideoExposeValidDuration() : abilityConfig.getExposeValidDuration();
                                    urlExposeDuration = urlExposeDuration / 1000;
                                }
                                exposeDuration = value + equalizer + String.valueOf(urlExposeDuration);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else if (argumentKey.equals(ViewAbilityStats.ADVIEWABILITY_CONFIG_AREA)) {//vh||2v
                            try {
                                int coverRate;
                                if (abilityStats.getURLShowCoverRate() > 0.0f) {
                                    coverRate = (int) (abilityStats.getURLShowCoverRate() * 100);
                                } else {
                                    coverRate = (int) (abilityConfig.getCoverRateScale() * 100);
                                }
                                showCoverRate = value + equalizer + String.valueOf(coverRate);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
//                        else if (argumentKey.equals(ViewAbilityStats.ADVIEWABILITY_ENABLE)) {//2j||vx
//                            continue;
//                        }

                        if (filterURL.contains(separator + value + equalizer)) {
                            String regex = separator + value + equalizer + "[^" + separator + "]*";
                            filterURL = filterURL.replaceAll(regex, "");
                        }
                    }
                }
            }

            StringBuffer sb = new StringBuffer();
            sb.append(filterURL);

            //将实际生效的满足可视时长参数替换到监测链接
            if (!TextUtils.isEmpty(exposeDuration)) {
                sb.append(separator);
                sb.append(exposeDuration);
            }
            //将实际生效的满足可视覆盖尺寸参数替换到监测链接
            if (!TextUtils.isEmpty(showCoverRate)) {
                sb.append(separator);
                sb.append(showCoverRate);
            }
            filteredURL = sb.toString();

        } catch (Exception e) {
            e.printStackTrace();
            filteredURL = adURL;
        }

        return filteredURL;
    }


}
