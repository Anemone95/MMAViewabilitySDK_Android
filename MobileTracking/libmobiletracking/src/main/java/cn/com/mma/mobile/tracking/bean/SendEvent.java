package cn.com.mma.mobile.tracking.bean;

import java.util.Map;

/**
 * 发送消息时的实体类
 * 
 * @author lincoln
 * 
 */
public class SendEvent {

	private String url = null;
	public String muds = null;
	public Map<String, String> segmentation = null;
	private long timestamp = 0;

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public long getTimestamp() {
		return timestamp;
	}

}
