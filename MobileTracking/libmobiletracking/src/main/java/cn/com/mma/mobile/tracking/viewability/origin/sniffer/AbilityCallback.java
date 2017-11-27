package cn.com.mma.mobile.tracking.viewability.origin.sniffer;


/**
 * Created by mma on 17/6/19.
 */
public interface AbilityCallback {


    /* 触发MMA 曝光监测 */
    void onSend(String trackURL);

    /* 移除监测任务 */
    void onFinished(String taskID);

}
