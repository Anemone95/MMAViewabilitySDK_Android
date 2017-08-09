package cn.com.mma.mobile.tracking.bean;

/**
 * 消息实体类，对应sdkconfig.xml中的<offlineCache>标签
 * 
 * @author lincoln
 */
public class OfflineCache {
	public String length;
	public String queueExpirationSecs;
	public String timeout;

	public String toString() {
		String result = "<offlineCache>\r\n<length>" + length
				+ "</length>\r\n<queueExpirationSecs>" + queueExpirationSecs
				+ "</queueExpirationSecs>\r\n<timeout>" + timeout
				+ "</timeout></offlineCache>\r\n";
		return result;
	}
}
