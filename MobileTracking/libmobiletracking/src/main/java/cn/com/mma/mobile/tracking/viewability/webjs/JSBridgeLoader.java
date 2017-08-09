package cn.com.mma.mobile.tracking.viewability.webjs;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.zip.GZIPInputStream;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import cn.com.mma.mobile.tracking.bean.Company;
import cn.com.mma.mobile.tracking.util.DeviceInfoUtil;
import cn.com.mma.mobile.tracking.util.Logger;

/**
 * Created by yangxiaolong on 17/7/28.
 */
public class JSBridgeLoader {

    private static final String SHAREDPREFERENCES_NAME = "mma.viewabilityjs.bridgejs";
    private static final String SHAREDPREFERENCES_BRIDGEJS = "bridgejs";

    private Context context;
    private String jsURL;//在线下载JS的地址
    private String jsName;//JS离线文件名称
    private boolean isUpdating;
    private String bridgeJs;
    private String jsKey;//存储js内容的SP_VALUE_NAME


    public JSBridgeLoader(Context context, Company company) {
        this.context = context;
        this.jsURL = company.jsurl;
        this.jsName = company.jsname;
        this.jsKey = SHAREDPREFERENCES_BRIDGEJS + "_" + company.name;
        isUpdating = false;
    }


    /**
     * 获取JS配置,优先从CACHE内读取,如果不存在,则尝试工程读取离线配置文件,如果不存在,则等待在线获取
     *
     * @return
     */
    public String getBridgeJs() {
        if (TextUtils.isEmpty(bridgeJs)) {
            try {
                SharedPreferences sp = context.getSharedPreferences(SHAREDPREFERENCES_NAME, Context.MODE_PRIVATE);

                //先从SP缓存内取出js配置
                bridgeJs = sp.getString(jsKey, "");

                //KLog.v("sp bridgeJS:" + bridgeJs);
                //如果不存在js缓存,尝试从本地XML内获取
                if (TextUtils.isEmpty(bridgeJs)) {
                    bridgeJs = getJsFromAssets();
                    //KLog.v("Assets bridgeJS:" + bridgeJs);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return bridgeJs;
    }

    private String getJsFromAssets() {
        InputStream is = null;
        String jsdata = null;
        try {
            is = context.getAssets().open(jsName);
            if (is != null) {
                byte[] buffer = writeToArr(is);
                if (buffer != null) jsdata = new String(buffer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return jsdata;
    }


    public void doUpdate() {

        if (!isUpdating) {
            new Thread(new Runnable() {
                @Override
                public void run() {

                    //网络不好
                    if (!DeviceInfoUtil.isNetworkAvailable(context)) {
                        Logger.w("Network unavailable.");
                        return;
                    }

                    //下载地址为NULL
                    if (TextUtils.isEmpty(jsURL)) {
                        Logger.w(jsName + " <jsurl> is Empty,Online updates are unavailable.");
                        return;
                    }

                    isUpdating = true;

                    try {
                        byte[] response = HttpURLRequest.performGet(jsURL);

                        String jsData = null;
                        if (response != null) jsData = new String(response, "UTF-8");

                        //每次升级后的数据直接覆盖到缓存内,下一次启动时生效
                        if (!TextUtils.isEmpty(jsData) && !jsData.equals(bridgeJs)) {
                            SharedPreferences sp = context.getSharedPreferences(SHAREDPREFERENCES_NAME, Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = sp.edit();
                            editor.putString(jsKey, jsData);
                            editor.apply();
                            //如果初始化时缓存和ASSETS都没有获取到js数据,则本次在线获取的数据立即赋值
                            if (TextUtils.isEmpty(bridgeJs)) bridgeJs = jsData;
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    isUpdating = false;

                }
            }).start();
        }


    }


    private static class HttpURLRequest {

        private static boolean isUsedHttps = false;
        private static final boolean usedGzip = false;

        private static SSLSocketFactory getSSLSocketFactory() {
            SSLSocketFactory foundSSLFactory;
            try {
                final SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, X509TrustAllManager, new SecureRandom());
                foundSSLFactory = sslContext.getSocketFactory();
            } catch (final GeneralSecurityException e) {
                foundSSLFactory = null;
            }
            return foundSSLFactory;
        }


        private static TrustManager[] X509TrustAllManager = {new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            public void checkServerTrusted(X509Certificate[] certs, String authType) {

            }
        }};

        private static class NullHostNameVerifier implements HostnameVerifier {
            public NullHostNameVerifier() {

            }

            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        }

        /**
         * GET请求
         * 获取推送活动中的Image素材
         */

        public static byte[] performGet(String endpointUrl) throws IOException {
            Logger.d("Attempting Get to " + endpointUrl + "\n");
            byte[] response = null;
            HttpURLConnection httpConnection = null;
            InputStream is = null;

            try {
                final URL url = new URL(endpointUrl);
                httpConnection = (HttpURLConnection) url.openConnection();

                // support https
                if (isUsedHttps) {
                    ((HttpsURLConnection) httpConnection).setSSLSocketFactory(getSSLSocketFactory());
                    ((HttpsURLConnection) httpConnection).setHostnameVerifier(new NullHostNameVerifier());
                }

                httpConnection.setConnectTimeout(3000);
                httpConnection.setReadTimeout(3000);
                httpConnection.setRequestMethod("GET");

                if (usedGzip) {
                    //httpConnection.setRequestProperty("Content-Type", "application/json");
                    httpConnection.setRequestProperty("AcceptEncoding", "gzip,deflate");
                    httpConnection.setRequestProperty("Content-Encoding", "gzip");
                }


                int statusCode = httpConnection.getResponseCode();


                if (statusCode == 200) {

                    String hasGzip = httpConnection.getContentEncoding();

                    if (!TextUtils.isEmpty(hasGzip) && hasGzip.contains("gzip")) {
                        // 使用GZIP解压
                        is = new GZIPInputStream(httpConnection.getInputStream());
                    } else {
                        // 使用普通流读取
                        is = httpConnection.getInputStream();
                    }
                    response = writeToArr(is);
                }

            } catch (Throwable e) {
                e.printStackTrace();
            } finally {
                if (null != is)
                    try {
                        is.close();
                    } catch (final IOException e) {
                        //throw e;
                    }
                if (null != httpConnection)
                    httpConnection.disconnect();
            }

            return response;
        }

    }

    /**
     * 读取到Buffer内，转换成byte[]数组
     */
    public static byte[] writeToArr(InputStream is) {
        if (is == null) {
            return null;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] byteArr = null;
        try {
            int len;
            byte[] buffer = new byte[1024];

            while ((len = is.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            //baos.flush();
            // buffer.close();
            byteArr = baos.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (baos != null) {
                try {
                    baos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
        return byteArr;
    }


}
