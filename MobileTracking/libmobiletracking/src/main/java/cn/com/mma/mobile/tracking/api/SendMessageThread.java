package cn.com.mma.mobile.tracking.api;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import cn.com.mma.mobile.tracking.util.CommonUtil;
import cn.com.mma.mobile.tracking.util.ConnectUtil;
import cn.com.mma.mobile.tracking.util.DeviceInfoUtil;
import cn.com.mma.mobile.tracking.util.Logger;
import cn.com.mma.mobile.tracking.util.SharedPreferencedUtil;

/**
 * 发送消息的线程
 *
 * @author lincoln
 */
public class SendMessageThread extends Thread {

    private String spName;
    private Context context;
    private boolean isNormalList;
    private boolean isInterruptThread = false;// 是否中断线程
    private ConnectUtil connectUtil = null;
    private Object object = null;
    private HashSet<String> requestList = null;

    @Override
    public void interrupt() {
        isInterruptThread = true;
        super.interrupt();
    }

    public SendMessageThread(String spFileName, Context context, boolean isNormalList) {
        this.spName = spFileName;
        this.context = context;
        this.isNormalList = isNormalList;
        requestList = new HashSet<>();
        connectUtil = ConnectUtil.getInstance();
        object = new Object();
    }

    @Override
    public void run() {
        sendData();
    }

    private synchronized void sendData() {
        synchronized (object) {
            Set eventSets = SharedPreferencedUtil.getSharedPreferences(context, spName).getAll().keySet();
            Iterator iterator = eventSets.iterator();
            while (iterator.hasNext()) {
                //没有网络或者线程中断停止发送
                if (isInterruptThread || !DeviceInfoUtil.isNetworkAvailable(context))
                    return;
                try {
                    String eventData = (String) iterator.next();
                    if (!TextUtils.isEmpty(eventData)) {
                        long eventExpireTime = SharedPreferencedUtil.getLong(context, spName, eventData);
                        if (eventExpireTime > System.currentTimeMillis()) {

                            if (requestList.contains(eventData)) {
                                // 包含正在请求的，则不再请求
                                return;
                            } else {
                                requestList.add(eventData);
                            }

                            byte[] response = connectUtil.performGet(eventData);
                            if (response == null) {
                                handleFailedResult(eventData, eventExpireTime);
                                return;
                            } else {
                                Logger.i("record [" + CommonUtil.md5(eventData) + "] upload succeed.");
                                handleSuccessResult(spName, eventData);
                                //[LOCALTEST] 测试计数:记录发送成功
                                if (Countly.LOCAL_TEST) {
                                    Intent intent = new Intent(Countly.ACTION_STATS_SUCCESSED);
                                    context.sendBroadcast(intent);
                                }
                            }
                        } else {//超出有效期则删除
                            SharedPreferencedUtil.removeFromSharedPreferences(context, spName, eventData);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }


    /**
     * 消息发送失败处理逻辑
     */
    private void handleFailedResult(String key, long valueExpire) {
        if (isNormalList) {
            // 从正常列表删除，存储到失败列表
            SharedPreferencedUtil.removeFromSharedPreferences(context,
                    SharedPreferencedUtil.SP_NAME_NORMAL, key);
            SharedPreferencedUtil.putLong(context,
                    SharedPreferencedUtil.SP_NAME_FAILED, key, valueExpire);
            SharedPreferencedUtil.putLong(context,
                    SharedPreferencedUtil.SP_NAME_OTHER, key, 1);
        } else {
            // 错误列表发送失败超过3次，则删除
            long failedTime = SharedPreferencedUtil.getLong(context,
                    SharedPreferencedUtil.SP_NAME_OTHER, key);
            failedTime += 1;
            if (failedTime > 3) {
                SharedPreferencedUtil.removeFromSharedPreferences(context,
                        SharedPreferencedUtil.SP_NAME_FAILED, key);
                boolean result = SharedPreferencedUtil
                        .removeFromSharedPreferences(context,
                                SharedPreferencedUtil.SP_NAME_OTHER, key);
                //Logger.d("Failure more than three times, and delete records:" + key);
            } else {
                SharedPreferencedUtil.putLong(context,
                        SharedPreferencedUtil.SP_NAME_OTHER, key, failedTime);
            }
        }

        requestList.remove(key);

    }

    /**
     * 消息发送成功处理逻辑
     */
    private void handleSuccessResult(String sharedPreferencedName, String key) {
        SharedPreferencedUtil.removeFromSharedPreferences(context,
                sharedPreferencedName, key);
        if (!isNormalList) {
            // 删除错误日志中的失败次数
            boolean result = SharedPreferencedUtil.removeFromSharedPreferences(
                    context, SharedPreferencedUtil.SP_NAME_OTHER, key);
            //Logger.d("mma_failed数据发送成功，删除other中记录" + result);
        }

        requestList.remove(key);

    }

}
