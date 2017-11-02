package cn.com.mma.mobile.tracking.util;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import org.xmlpull.v1.XmlPullParser;
import android.util.Xml;
import cn.com.mma.mobile.tracking.bean.Argument;
import cn.com.mma.mobile.tracking.bean.Company;
import cn.com.mma.mobile.tracking.bean.Config;
import cn.com.mma.mobile.tracking.bean.Domain;
import cn.com.mma.mobile.tracking.bean.Event;
import cn.com.mma.mobile.tracking.bean.OfflineCache;
import cn.com.mma.mobile.tracking.bean.SDK;
import cn.com.mma.mobile.tracking.bean.Signature;
import cn.com.mma.mobile.tracking.bean.Switch;
import cn.com.mma.mobile.tracking.bean.ViewAbility;

/**
 * 做一些xml解析的工作
 * 
 * @author lincoln
 */
public class XmlUtil {

	public static final String XMLFILE = "sdkconfig.xml";

	/**
	 * 把sdkconfig.xml文件解析成SDK实体类
	 * 
	 * @param inputStream
	 * @return
	 */
	public static SDK doParser(InputStream inputStream) {
		SDK sdk = null;
		Argument argument = null;
		Company company = null;
		Event event = null;
        //<Adplacement>和<viewabilityarguments>标签的子元素都是<Argument>
        //使用局部变量控制该标签的遍历起止,以便在END_TAG时区分
        boolean isAdplacements = false;
        boolean isviewabilityarguments = false;

		try {
			XmlPullParser parser = Xml.newPullParser();
			parser.setInput(inputStream, "UTF-8");
			int _event = parser.getEventType();
			while (_event != XmlPullParser.END_DOCUMENT) {
				switch (_event) {
				case XmlPullParser.START_DOCUMENT:
					sdk = new SDK();
					break;
				case XmlPullParser.START_TAG:
					String elementName = parser.getName();
					/* offlineCache */
					if ("offlineCache".equals(elementName))
						sdk.offlineCache = new OfflineCache();
					if (sdk.offlineCache != null) {
						if ("length".equals(elementName))
							sdk.offlineCache.length = parser.nextText();
						if ("queueExpirationSecs".equals(elementName))
							sdk.offlineCache.queueExpirationSecs = parser
									.nextText();
						if ("timeout".equals(elementName))
							sdk.offlineCache.timeout = parser.nextText();
					}
                    /*ViewAbility*/
                    if (elementName.equals("viewability"))
                        sdk.viewAbility = new ViewAbility();
                    if (elementName.equals("intervalTime"))
                        sdk.viewAbility.intervalTime = Integer.valueOf(parser.nextText());
                    if (elementName.equals("viewabilityFrame"))
                        sdk.viewAbility.viewabilityFrame = Integer.valueOf(parser.nextText());
                    if (elementName.equals("viewabilityTime"))
                        sdk.viewAbility.viewabilityTime = Integer.valueOf(parser.nextText());
                    if (elementName.equals("viewabilityVideoTime"))
                        sdk.viewAbility.viewabilityVideoTime = Integer.valueOf(parser.nextText());
                    if (elementName.equals("maxExpirationSecs"))
                        sdk.viewAbility.maxExpirationSecs = Integer.valueOf(parser.nextText());
                    if (elementName.equals("maxAmount"))
                        sdk.viewAbility.maxAmount = Integer.valueOf(parser.nextText());
                    /* company */
					if ("companies".equals(elementName))
						sdk.companies = new ArrayList<Company>();
					if (sdk.companies != null && "company".equals(elementName))
						company = new Company();
					if (company != null) {

						if ("name".equals(elementName) && company.name == null)
							company.name = parser.nextText();
                        if ("jsurl".equals(elementName) && company.jsurl == null)
                            company.jsurl = parser.nextText();
                        if ("jsname".equals(elementName) && company.jsname == null)
                            company.jsname = parser.nextText();

						if ("domain".equals(elementName))
							company.domain = new Domain();
						if (company.domain != null) {
							if ("url".equals(elementName))
								company.domain.url = parser.nextText();
						}
						if ("signature".equals(elementName))
							company.signature = new Signature();
						if (company.signature != null) {
							if ("publicKey".equals(elementName))
								company.signature.publicKey = parser.nextText();
							if ("paramKey".equals(elementName))
								company.signature.paramKey = parser.nextText();
						}
						if ("switch".equals(elementName))
							company.sswitch = new Switch();
						if (company.sswitch != null) {
                            if ("isTrackLocation".equals(elementName))
                                company.sswitch.isTrackLocation = Boolean.parseBoolean(parser.nextText());
                            if ("offlineCacheExpiration".equals(elementName))
                                company.sswitch.offlineCacheExpiration = parser.nextText();
                            if ("viewabilityTrackPolicy".equals(elementName))
                                company.sswitch.viewabilityTrackPolicy = Integer.parseInt(parser.nextText());
                            if ("encrypt".equals(elementName))
                                company.sswitch.encrypt = new HashMap<>();
                            if (company.sswitch.encrypt != null) {
                                if ("MAC".equals(elementName) || "IDA".equals(elementName) || "IMEI".equals(elementName) || "ANDROID".equals(elementName))
                                    company.sswitch.encrypt.put(elementName, parser.nextText());

                            }
                        }

						if ("config".equals(elementName))
							company.config = new Config();
						if (company.config != null) {
                            if ("arguments".equals(elementName))
                                company.config.arguments = new ArrayList<Argument>();
                            if ("argument".equals(elementName))
                                argument = new Argument();
                            if (argument != null) {
                                if ("key".equals(elementName))
                                    argument.key = parser.nextText();
                                if ("value".equals(elementName))
                                    argument.value = parser.nextText();
                                if ("urlEncode".equals(elementName))
                                    argument.urlEncode = Boolean
                                            .parseBoolean(parser.nextText());
                                if ("isRequired".equals(elementName))
                                    argument.isRequired = Boolean
                                            .parseBoolean(parser.nextText());
                            }


                            if ("events".equals(elementName))
                                // company.config.events = new HashMap<String,
                                // Event>();
                                company.config.events = new ArrayList<Event>();
                            if (company.config.events != null
                                    && "event".equals(elementName))
                                event = new Event();
                            if (event != null) {

                                if ("key".equals(elementName))
                                    event.key = parser.nextText();
                                if ("value".equals(elementName))
                                    event.value = parser.nextText();
                                if ("urlEncode".equals(elementName))
                                    event.urlEncode = Boolean
                                            .parseBoolean(parser.nextText());
                            }

                            if (elementName.equals("Adplacement")) {
                                company.config.adplacements = new HashMap<>();
                                isAdplacements = true;
                            }

                            if (elementName.equals("viewabilityarguments")) {
                                company.config.viewabilityarguments = new HashMap<>();
                                isviewabilityarguments = true;
                            }

                        }
                        if ("separator".equals(elementName))
							company.separator = parser.nextText();
						if ("equalizer".equals(elementName))
							company.equalizer = parser.nextText();
						if ("timeStampUseSecond".equals(elementName))
							company.timeStampUseSecond = Boolean
									.parseBoolean(parser.nextText());

					}
					break;

				case XmlPullParser.END_TAG:
					String endElement = parser.getName();
					if ("company".equals(endElement)) {
						sdk.companies.add(company);
						company = null;
					}
                    if ("argument".equals(endElement)) {
                        //如果是Adplacement标签的元素
                        if (isAdplacements) {
                            company.config.adplacements.put(argument.key, argument);
                        } else if (isviewabilityarguments) {//如果是viewabilityarguments标签的元素
                            company.config.viewabilityarguments.put(argument.key, argument);
                        } else {//其余都放入arguments list
                            company.config.arguments.add(argument);
                        }
                        argument = null;
                    }
                    if (endElement.equals("Adplacement")) {
                        isAdplacements = false;
                        argument = null;
                    }
                    if (endElement.equals("viewabilityarguments")) {
                        isviewabilityarguments = false;
                        argument = null;
                    }
                    if ("event".equals(endElement)) {
						company.config.events.add(event);
						event = null;
					}

					break;
				}
				_event = parser.next();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return sdk;
	}
}
