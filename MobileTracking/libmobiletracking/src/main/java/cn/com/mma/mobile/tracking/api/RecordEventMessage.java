package cn.com.mma.mobile.tracking.api;

import android.content.Context;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.com.mma.mobile.tracking.bean.Argument;
import cn.com.mma.mobile.tracking.bean.Company;
import cn.com.mma.mobile.tracking.bean.SDK;
import cn.com.mma.mobile.tracking.bean.SendEvent;
import cn.com.mma.mobile.tracking.util.CommonUtil;
import cn.com.mma.mobile.tracking.util.DeviceInfoUtil;
import cn.com.mma.mobile.tracking.util.Logger;
import cn.com.mma.mobile.tracking.util.SdkConfigUpdateUtil;
import cn.com.mma.mobile.tracking.util.SharedPreferencedUtil;

/**
 * 记录事件
 */
public class RecordEventMessage {
	private Context context;
	private Map<String, String> params;
	private static RecordEventMessage instance;

	private RecordEventMessage(final Context context) {
        this.context = context;
        params = DeviceInfoUtil.fulfillTrackingInfo(context);
    }

	public synchronized static RecordEventMessage getInstance(Context ctx) {
        if (instance == null) {
            instance = new RecordEventMessage(ctx);
        }
        return instance;
    }

	public synchronized void recordEvent(SendEvent sendEvent) throws Exception{
//		params.put(Constant.TRACKING_LOCATION, LocationUtil
//				.getInstance(context).getLocation());
		StringBuilder builder = new StringBuilder();
		String hostUrl = CommonUtil.getHostURL(sendEvent.getUrl());
		SDK sdk = SdkConfigUpdateUtil.getSdk(context);
		String redirectUrlValue = "";
		if (sdk != null && sdk.companies != null) {
			for (Company company : sdk.companies) {
				if (hostUrl.endsWith(company.domain.url)) {
					List<String> requiredArgmentValue = new ArrayList();
					String separator = "";
					String equalizer = "";
					String redirectURLValue = "";
					for (Argument argument : company.config.arguments) {
						if (argument.isRequired) {
							separator = company.separator;
							equalizer = company.equalizer;
							requiredArgmentValue.add(argument.value);
						}
					}
					Map<String, String> removedExistArgmentAndGetRedirectURL = CommonUtil
							.removeExistArgmentAndGetRedirectURL(
									sendEvent.getUrl(), requiredArgmentValue,
									separator, equalizer, redirectURLValue);
					String url = removedExistArgmentAndGetRedirectURL.get(Constant.TRACKING_URL);
					builder.append(url);
//					String valueAndRedirectURL = "";
					for (Argument argument : company.config.arguments) {
						if (argument.isRequired) {
							if (Constant.TRACKING_TIMESTAMP
									.equals(argument.key)) {
								builder.append(company.separator
										+ argument.value
										+ (company.equalizer != null ? company.equalizer
												: "")
										+ (company.timeStampUseSecond ? sendEvent
												.getTimestamp() / 1000
												: sendEvent.getTimestamp()));
								// true则为秒，false为毫秒
							} else if (Constant.TRACKING_MUDS
									.equals(argument.key)) {
								builder.append(company.separator
										+ argument.value
										+ (company.equalizer != null ? company.equalizer
												: "")
										+ CommonUtil.encodingUTF8(
												sendEvent.muds, argument,
												company));
							} else if (Constant.REDIRECTURL
									.equals(argument.key)) {
								// 获取重定向地址
								String patternString = company.separator
										+ argument.value + ".*";
								Pattern pattern = Pattern
										.compile(patternString);
								Matcher matcher = pattern.matcher(sendEvent
										.getUrl());
								if (matcher.find()) {
									redirectUrlValue= matcher.group(0);
									//Logger.d("mma_redirect_url :"+ redirectUrlValue);
								}
							} else if (Constant.TRACKING_AAID
									.equals(argument.key)) {
								// 新加aaid
								builder.append(company.separator
										+ argument.value
										+ (company.equalizer != null ? company.equalizer
												: "")
										+ CommonUtil.md5(params
												.get(argument.key)));
							}

							else {
								builder.append(company.separator
										+ argument.value
										+ (company.equalizer != null ? company.equalizer
												: "")
										+ CommonUtil.encodingUTF8(
												params.get(argument.key),
												argument, company));
							}
						}
					}
//					List<String> requiredEventValue = new ArrayList<String>();
//					builder = new StringBuilder(CommonUtil.removeExistEvent(
//							builder.toString(), requiredEventValue, separator,
//							equalizer));
//					builder.append(valueAndRedirectURL);
					if (company.signature != null
							&& company.signature.paramKey != null) {
						String signatureString = CommonUtil.getSignature(
								context, builder.toString());
						builder.append(company.separator
								+ company.signature.paramKey
								+ (company.equalizer != null ? company.equalizer
										: "")
								+ CommonUtil.encodingUTF8(signatureString));
					}
					builder.append(redirectUrlValue);
					redirectUrlValue = "";
					String resultUrl = builder.toString();
					/**
					 * 老版本 resultUrl = URLEncoder .encode(builder.toString(),
					 * "UTF-8") .toLowerCase().replaceAll("%2f", "/")
					 * .replaceAll("%3a", ":");
					 */

                    long expiration = getEventExpirationTime(company, sendEvent.getTimestamp());
                    SharedPreferencedUtil.putLong(context, SharedPreferencedUtil.SP_NAME_NORMAL, resultUrl, expiration);
//					Logger.d("sendurl: " + resultUrl);
				} else {
					Logger.d("domain不匹配" + hostUrl + " company.domain.url:"
							+ company.domain.url);
				}

			}
		}
	}

	public void recordEventWithUrl(String url) {
        try {
            SendEvent sendEvent = new SendEvent();
            sendEvent.setTimestamp(System.currentTimeMillis());
            // 去除所有空格
            url = url.replaceAll(" ", "");
            sendEvent.setUrl(url);
            recordEvent(sendEvent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 获取Event超时时间,从当前time+配置的缓存有效期 = n天后的时间
     * 如果修改设备系统时间,会影响正确性
     * @param company
     * @param timestamp event产生时间
     * @return
     */
    private long getEventExpirationTime(Company company,long timestamp) {
        long expiration = 0;
        try {
            if (!TextUtils.isEmpty(company.sswitch.offlineCacheExpiration)) {
                Long cachexpiration = Long.parseLong(company.sswitch.offlineCacheExpiration.trim());
                //秒转化为毫秒
                expiration = cachexpiration * 1000 + timestamp;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (expiration == 0) expiration = Constant.TIME_ONE_DAY + timestamp;

        return expiration;
    }

}
