package cn.com.mma.mobile.tracking.bean;

import java.util.Map;

/**
 * 消息实体类，对应sdkconfig.xml中的<switch>标签
 * 
 * @author lincoln
 */
public class Switch {
	public boolean isTrackLocation;
	public String offlineCacheExpiration;
	public Map<String, String> encrypt;
}
