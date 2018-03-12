package cn.com.mma.mobile.tracking.api;

/**
 * 供全局使用的静态变量或者静态方法
 */
public class Constant {
	/**
	 * 对应<offlineCache>中的<length>字段,表示：大于该值时立即发送消息
	 */
	public static int OFFLINECACHE_LENGTH = 0;
	/**
	 * 对应<offlineCache>中的<queueExpirationSecs>字段
	 */
	public static int OFFLINECACHE_QUEUEEXPIRATIONSECS = 15;
	/**
	 * 对应<offlineCache>中的<timeout>字段
	 */
	public static int OFFLINECACHE_TIMEOUT = 15;

    /* 正常队列间隔周期 10s */
    public static int ONLINECACHE_QUEUEEXPIRATIONSECS = 10;

	/**
	 * 先获得位置信息，如需要才发送给服务器，这样能减少延迟。
	 */
	public static String location = "";

	public static final String TRACKING_MAC = "MAC";
	public static final String TRACKING_LOCATION = "LBS";
	public static final String TRACKING_OS = "OS";
	public static final String TRACKING_OS_VERION = "OSVS";
	public static final String TRACKING_WIFI = "WIFI";
	public static final String TRACKING_NAME = "ANAME";
	public static final String TRACKING_KEY = "AKEY";
	public static final String TRACKING_SCWH = "SCWH";
	public static final String TRACKING_TIMESTAMP = "TS";
	public static final String TRACKING_ANDROIDID = "ANDROIDID";
	public static final String TRACKING_AAID = "AAID";// 新加android google
														// advertising id
	public static final String TRACKING_TERM = "TERM";
	public static final String TRACKING_WIFISSID = "WIFISSID";
	public static final String TRACKING_WIFIBSSID = "WIFIBSSID";
	public static final String TRACKING_IMEI = "IMEI";
	public static final String TRACKING_ODIN = "ODIN";
	public static final String TRACKING_MUID = "MUID";
	public static final String TRACKING_MUDS = "MUDS";
	public static final String TRACKING_URL = "URL";
	public static final String REDIRECTURL = "REDIRECTURL";

	public static final String TRACKING_SDKVS = "SDKVS";
	public static final String TRACKING_SDKVS_VALUE = "V2.0.0";

	/**
	 * 新版常量 Begin
	 */
	public static final int NORMAL_MESSAGE_DEFAULT_PEROID = 30 * 1000;
	public static final int FAILED_MESSAGE_DEFAULT_PEROID = 60 * 60 * 1000;
	public static final int LOCATIOON_UPDATE_INTERVAL = 60 * 60 * 1000;

	public static final long TIME_THREE_DAY = 3 * 24 * 60 * 60 * 1000;
	public static final long TIME_ONE_DAY = 24 * 60 * 60 * 1000;//一天的毫秒数

	// 网络参数配置
	public static int DEFAULT_MAX_CONNECTIONS = 30;
//	public static int DEFAULT_SOCKET_TIMEOUT = 30 * 1000;
	public static int DEFAULT_SOCKET_BUFFER_SIZE = 8192;
	public static int DEFAULT_HTTP_SIZE = 50;
	public static int THREAD_SLEEP_TIME = 500;
	public static String APPLICATION_JSON = "application/json";
	public static String CONTENT_TYPE_TEXT_JSON = "text/json";
}
