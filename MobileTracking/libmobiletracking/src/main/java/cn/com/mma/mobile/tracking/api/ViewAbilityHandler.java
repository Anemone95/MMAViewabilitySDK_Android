package cn.com.mma.mobile.tracking.api;

import android.content.Context;
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
import cn.com.mma.mobile.tracking.viewability.origin.ViewAbilityStatsResult;
import cn.com.mma.mobile.tracking.viewability.origin.sniffer.ViewAbilityConfig;
import cn.com.mma.mobile.tracking.viewability.webjs.ViewAbilityJsService;


/**
 * Countly 和 ViewAbilityService中间件:负责控制View Ability的初始化和监测
 * Created by admaster on 17/6/22.
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
    /* ViewAbility native 方式监测服务 */
    private ViewAbilityService viewAbilityService;
     /* ViewAbility JS 方式监测服务 */
    private ViewAbilityJsService viewAbilityJsService;

    public ViewAbilityHandler(Context context, ViewAbilityEventListener mmaSdkCallback, SDK sdk) {
        this.context = context;
        this.mmaSdkCallback = mmaSdkCallback;
        impressions = new HashMap<>();
        sdkConfig = sdk;

        if (sdkConfig == null) sdkConfig = SdkConfigUpdateUtil.getSdk(context);

        viewAbilityService = new ViewAbilityService(context, mmaSdkCallback, initViewAbilityGlobalConfig());

        viewAbilityJsService = new ViewAbilityJsService(context, sdkConfig);
    }



    public void onJSExpose(String adURL,View adView,boolean isVideo) {

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
     *
     * @param originUrl
     */
    public void onClick(String originUrl) {
        handlerOriginURL(originUrl, MonitorType.CLICK, null);
    }

    /**
     * 普通曝光监测
     *
     * @param originUrl
     */
    public void onExpose(String originUrl) {
        //this.onExpose(originUrl, null);
        handlerOriginURL(originUrl, MonitorType.EXPOSE, null);
    }

    /**
     * 带可视化的曝光监测
     *
     * @param originUrl
     * @param adView
     */
    public void onExpose(String originUrl, View adView) {
        handlerOriginURL(originUrl, MonitorType.EXPOSEWITHABILITY, adView);
    }

    /**
     * 带可视化的视频曝光监测
     *
     * @param originUrl
     * @param videoView
     */
    public void onVideoExpose(String originUrl, View videoView) {
        handlerOriginURL(originUrl, MonitorType.VIDEOEXPOSEWITHABILITY, videoView);
    }


    private ViewAbilityConfig initViewAbilityGlobalConfig() {

        ViewAbilityConfig config = new ViewAbilityConfig();
        try {
            if (sdkConfig != null && sdkConfig.viewAbility != null) {
                //viewability曝光监测时间间隔
                config.setInspectInterval(sdkConfig.viewAbility.intervalTime);
                //满足viewability曝光持续时长
                config.setExposeValidDuration(sdkConfig.viewAbility.viewabilityTime);
                //满足viewability可见区域占总区域的百分比
                config.setCoverRateScale(sdkConfig.viewAbility.viewabilityFrame);
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


    private void handlerOriginURL(String originUrl, MonitorType monitorType, View adView) {


        //Company为空,不监测
        Company company = getCompany(originUrl);
        if (company == null) {
            Logger.w("监测链接:" + originUrl + " 没有对应的配置项,请检查sdkconfig.xml是否存在链接域名对应的Company配置!");
            return;
        }

        //u=
        String redirectPrefix = getRedirectIdentifier(company) + company.equalizer;
        //[1] 截取出原监测链接u参数之后所有的内容 REDIRECTURL
        String redirectStr = "";

        //,u=.*
        String patternStr = company.separator + redirectPrefix + ".*";

        Pattern pattern = Pattern.compile(patternStr);

        Matcher matcher = pattern.matcher(originUrl);
        if (matcher.find()) {
            redirectStr = matcher.group(0);
        }
        //redirectStr e.g. ,uhttp://wwww.admaster.com.cn/conf2jaaa,2fbbb,2hccc


        //[2] 监测链接截取掉REDIRECTURL字段之后的所有内容,重新组装为withoutRedirectURL
        String withoutRedirectURL;
        StringBuilder sb = new StringBuilder();
        String[] splits = originUrl.split(company.separator);
        try {
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
        //withoutRedirectURL e.g. http://vxyz.admaster.com.cn/w/a86218,b1729679,c3259,i0,m202,8a2,8b2,h


        try {

            //ViewAbilityStatsResult初始化,将配置文件的arguments映射到HashMap
            ViewAbilityStatsResult viewAbilityStatsResult = new ViewAbilityStatsResult();
            viewAbilityStatsResult.setSeparator(company.separator);
            viewAbilityStatsResult.setViewabilityarguments(company.config.viewabilityarguments);

            //[3]  降噪处理:如果是onclick和onexpose使用arg[1]只清除原链接的2g属性;如果是url/video的ViewAbility使用arg[2]清除2g,2j,2f,2h
            String[] args = filterOriginIdentifiers(viewAbilityStatsResult, company, withoutRedirectURL);


            //[4] 判断原剔除U字段后的URL是否是ViewAbility监测链接:带有2j字段
            StringBuffer exposeURL = new StringBuffer();
            StringBuffer viewabilityURL;

            exposeURL.append(args[0]);


            boolean isViewAbility = false;
            if (monitorType == MonitorType.EXPOSEWITHABILITY || monitorType == MonitorType.VIDEOEXPOSEWITHABILITY) {
                isViewAbility = checkViewAbilityEnabled(company, withoutRedirectURL);//使用原URL判断是否具有参数
                if (monitorType == MonitorType.VIDEOEXPOSEWITHABILITY)
                    viewAbilityStatsResult.setVideoExpose(true); //标记当前可视化监测来自视频的ViewAbility
            }


            //[5] 获取广告位ID,然后通过广告位ID生成ImpressionID:配置文件的value.(adplacements)和company.separator提取URL中的广告位标识符
            String adAreaID = getAdAreaID(company, withoutRedirectURL);
            String impressionID = "";

            if (TextUtils.isEmpty(adAreaID)) {//如果广告位ID为空,则不进行ViewAbility监测,并且不生成impressionID,以普通click/expose方式上报
                isViewAbility = false;
                Logger.w("没有找到广告位ID,请确认Company是否有配置:adplacements");
            } else {
                impressionID = getImpressionID(company, monitorType, adAreaID);
            }


            //[6] 使用剔除2g/2j/2f/2h后的URL,追加2g字段(impressionID)以及value值,为空的情况下,key和value都不添加
            String impressionValue = "";
            if (!TextUtils.isEmpty(impressionID)) {
                String impressionIdentifier = viewAbilityStatsResult.get(ViewAbilityStatsResult.IMPRESSIONID);
                if (!TextUtils.isEmpty(impressionIdentifier)) {
                    sb = new StringBuilder();
                    sb.append(company.separator);
                    sb.append(impressionIdentifier);
                    sb.append(company.equalizer);
                    sb.append(impressionID);
                    impressionValue = sb.toString();
                }
            }

            exposeURL.append(impressionValue);


            //[7] 如果是viewability监测,view为空时2j/2f/2h使用默认value拼装到URL后,以正常曝光链接上报
            exposeURL.append(redirectStr);
            String trackURL = exposeURL.toString();

            if (isViewAbility) {

                viewabilityURL = new StringBuffer();
                viewabilityURL.append(args[1]);
                viewabilityURL.append(impressionValue);

                if (adView == null || !(adView instanceof View)) {
                    Logger.w("监测链接传入的AdView为空,以正常曝光方式监测.");
                    String defaultAttr = viewAbilityStatsResult.getDefaultViewAbilityData();
                    viewabilityURL.append(defaultAttr);
                    viewabilityURL.append(redirectStr);
                    trackURL = viewabilityURL.toString();
                } else {
                    viewAbilityStatsResult.setRedirectURL(redirectStr);
                    //开启线程执行ViewAbility可视化监测
                    viewAbilityService.addViewAbilityMonitor(viewabilityURL.toString(), adView, impressionID, adAreaID, viewAbilityStatsResult);
                }
            }


            //[8] 最终将REDIRECTURL重新拼装到URL末尾,并回调原有Countly线程单独发送监测事件
            mmaSdkCallback.onEventPresent(trackURL);


            KLog.i("********************************************");
            KLog.i("originURL:" + originUrl);
            KLog.i("monitiorType:" + monitorType);
            KLog.i("REDIRECT_STR:" + redirectStr);
            KLog.i("withoutRedirectURL:" + withoutRedirectURL);
            KLog.i("adAreaID:" + adAreaID);
            KLog.i("imressionID:" + impressionID);
            KLog.i("isViewability:" + isViewAbility);
            KLog.d("exposeURL origin:" + args[0]);
            KLog.d("viewabilityURL origin :" + args[1]);
            KLog.i("trackURL:" + trackURL);
            KLog.i("adView:" + adView);
            KLog.i("eventListener:" + mmaSdkCallback);
            KLog.i("********************************************");

        } catch (Exception e) {
            e.printStackTrace();
        }

        //TODO [TEST] 测试计数:普通曝光事件产生计数
//        Intent intent = new Intent(Countly.ACTION_STATS_EXPOSE);
//        context.sendBroadcast(intent);
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
            Argument argument = adplacements.get(ViewAbilityStatsResult.ADPLACEMENT);
            if (argument != null) adAreaIdentifier = argument.value;
        }
        return adAreaIdentifier;
    }


    /**
     * 获取配置文件config.arguments标签内的REDIRECTURL的值
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
     * 判断是否是可视化监测专用链接
     * 链接里是否包含2j(ADVIEWABILITYEVENTS)
     * e.g. http://v.admaster.com.cn/w/a86218,b1729679,c3259,i0,m202,8a2,8b2,h,2j
     *
     * @param originUrl
     * @return
     */
    private boolean checkViewAbilityEnabled(Company company,String originUrl) throws Exception {

        String[] splits = originUrl.split(company.separator);

        String viewabilityIdentifier = "";
        HashMap<String, Argument> viewabilityarguments = company.config.viewabilityarguments;
        if (viewabilityarguments != null) {
            Argument argument = viewabilityarguments.get(ViewAbilityStatsResult.ADVIEWABILITYEVENTS);
            if (null != argument) viewabilityIdentifier = argument.value;
        }

        if (!TextUtils.isEmpty(viewabilityIdentifier)) {
            //2g+equalizer
            String prefix = viewabilityIdentifier + company.equalizer;
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
     * 获取监测
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
        }

        return null;
    }


    /**
     * 从URL里提取出广告位ID
     *
     * @param adUrl
     * @return
     */
    private String getAdAreaID(Company company,String adUrl) throws Exception {
        String adAreaId = "";
        String adAreaIdentifier = getAdAreaIdentifier(company);
        String[] splits = adUrl.split(company.separator);
        for (String item : splits) {
            if (item.startsWith(adAreaIdentifier)) {
                adAreaId = item.substring(1, item.length());
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


    /**
     * 原始链接:http://v.admaster.com.cn/w/a86218,b1729679,c3259,i0,m202,8a2,8b2,h,2j
     * 拼装2g字段
     *
     * @param company
     * @param originUrl
     * @return
     * @throws Exception
     */
    private String[] filterOriginIdentifiers(ViewAbilityStatsResult result, Company company, String originUrl) {


        //TODO 检查URL内是否有包含u字段,如果存在切割出来,等impressionID或者监测URL都赋值完毕再copy到链接最末端
        StringBuffer exposeURL = new StringBuffer();
        StringBuffer viewabilityURL = new StringBuffer();

        String impressionIdentifier = "";
        String eventsIdentifier = "";
        String abilityIdentifier = "";
        String measureIdentifier = "";
        String[] args = new String[2];

        try {
            String impressionArgument = result.get(ViewAbilityStatsResult.IMPRESSIONID);
            if (!TextUtils.isEmpty(impressionArgument))
                impressionIdentifier = impressionArgument + company.equalizer;
            String eventArgument = result.get(ViewAbilityStatsResult.ADVIEWABILITYEVENTS);
            if (!TextUtils.isEmpty(eventArgument))
                eventsIdentifier = eventArgument + company.equalizer;
            String abilityArgument = result.get(ViewAbilityStatsResult.ADVIEWABILITY);
            if (!TextUtils.isEmpty(abilityArgument))
                abilityIdentifier = abilityArgument + company.equalizer;
            String measureArgument = result.get(ViewAbilityStatsResult.ADMEASURABILITY);
            if (!TextUtils.isEmpty(measureArgument))
                measureIdentifier = measureArgument + company.equalizer;

        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            String[] splits = originUrl.split(company.separator);

            //如果原url里包含impression配置,替换默认value
            for (String item : splits) {

                //如果是2j,2f,2h,则不再组装到url内
                if (!item.startsWith(eventsIdentifier) && !item.startsWith(abilityIdentifier) && !item.startsWith(measureIdentifier) && !item.startsWith(impressionIdentifier)) {
                    viewabilityURL.append(item);
                    viewabilityURL.append(company.separator);
                }

                if (!item.startsWith(impressionIdentifier)) {
                    exposeURL.append(item);
                    exposeURL.append(company.separator);
                }
            }

            exposeURL.deleteCharAt(exposeURL.length() - 1);
            viewabilityURL.deleteCharAt(viewabilityURL.length() - 1);


            args[0] = exposeURL.toString();
            args[1] = viewabilityURL.toString();


        } catch (Exception e) {
            e.printStackTrace();
        }

        return args;
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


}
