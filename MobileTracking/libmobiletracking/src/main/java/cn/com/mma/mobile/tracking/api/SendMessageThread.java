package cn.com.mma.mobile.tracking.api;

import java.net.HttpURLConnection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

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
        connectUtil = ConnectUtil.getInstance(context);
        object = new Object();
    }

    @Override
    public synchronized void run() {
        sendData();
    }

    private synchronized void sendData() {
        synchronized (object) {
            Set set = SharedPreferencedUtil
                    .getSharedPreferences(context, spName).getAll().keySet();
            Iterator iterator = set.iterator();
            while (iterator.hasNext()) {
                if (this.isInterruptThread)
                    return;
                if (!DeviceInfoUtil.isNetworkAvailable(context))
                    return;
                Long valueExpire;
                String key;
                try {
                    key = (String) iterator.next();
                    valueExpire = SharedPreferencedUtil.getLong(context,
                            spName, key);
//                    long failedTime = SharedPreferencedUtil.getLong(context,
//                            SharedPreferencedUtil.SP_NAME_OTHER, key);
                    String data = key;
                    if (!TextUtils.isEmpty(data)) {
                        if (valueExpire > System.currentTimeMillis()) {

                            if (requestList.contains(data)) {
                                // 包含正在请求的，则不再请求
                                return;
                            } else {
                                requestList.add(data);
                            }

                            HttpURLConnection httpResponse = connectUtil.getHttpURLConnection(data);
                            if (httpResponse == null) {
                                handleFailedResult(key, valueExpire);
                                return;
                            }
                            int statueCode = httpResponse.getResponseCode();

                            if (statueCode == 200 || statueCode == 301 || statueCode == 302) {

                                //TODO 记录发送成功[for TEST]
//                                Intent intent = new Intent(Countly.ACTION_STATS_SUCCESSED);
//                                context.sendBroadcast(intent);

                                handleSuccessResult(spName, key);
                                if (statueCode == 301 || statueCode == 302) {
                                    // 获取重定向地址
                                    String redirectUrl = httpResponse.getHeaderField("Location");
                                    if (!TextUtils.isEmpty(redirectUrl)) {
                                        connectUtil.doRequest(redirectUrl);
                                    }
                                }
                            } else {
                                handleFailedResult(key, valueExpire);
                            }
                        } else {
                            // 超出有效期则删除
                            SharedPreferencedUtil.removeFromSharedPreferences(
                                    context, spName, key);
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
                Logger.d("mma_failed发送失败超过三次，删除other中记录" + result);
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
            Logger.d("mma_failed数据发送成功，删除other中记录" + result);
        }

        requestList.remove(key);

    }

}
