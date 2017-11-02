package cn.com.mma.mobile.tracking.bean;

import java.util.Map;

/**
 * 消息实体类，对应sdkconfig.xml中的<switch>标签
 */
public class Switch {
    /* 是否监测定位 */
    public boolean isTrackLocation;
    /* 监测数据缓存有效期 */
    public String offlineCacheExpiration;
    /* 设备参数是否加密 */
    public Map<String, String> encrypt;
    /* 可视化监测采集数据策略 */
    public int viewabilityTrackPolicy;
}
