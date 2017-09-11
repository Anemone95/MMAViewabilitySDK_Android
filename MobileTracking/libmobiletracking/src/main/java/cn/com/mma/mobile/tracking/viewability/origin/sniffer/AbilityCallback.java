package cn.com.mma.mobile.tracking.viewability.origin.sniffer;


/**
 * Created by admaster on 17/6/19.
 */
public interface AbilityCallback {

    void onViewAbilityFinished(String adAreaID,String abilityUrl);
    void onSend(String url);    //mzcommit-上报请求
    void onViewAbilitySend(String url);    //mzcommit-上报可见请求
    void onFinished(String adAreaID);   //mzcommit-停止定时器

}
