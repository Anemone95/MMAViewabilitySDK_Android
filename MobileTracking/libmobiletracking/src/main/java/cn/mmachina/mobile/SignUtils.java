package cn.mmachina.mobile;

/**
 * Created by Amos on 17/12/12.
 * Copyright (c) 2017 MMAChinaSDK. All rights reserved.
 */
public class SignUtils {


    static {
        System.loadLibrary("MMASignature");
    }


    /**
     * 签名函数
     *
     * @param sdkVersion sdk版本号
     * @param timestamp  事件触发时的时间戳
     * @param originURL  原始监测链接
     * @return 返回签名后的36位字符串
     */
    public static native String mmaSdkSign(String sdkVersion, long timestamp, String originURL);


}
