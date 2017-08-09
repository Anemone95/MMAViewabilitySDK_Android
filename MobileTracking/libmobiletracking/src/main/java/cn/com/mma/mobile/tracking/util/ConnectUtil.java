package cn.com.mma.mobile.tracking.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;


public class ConnectUtil {

    private static final String CHARSET = "UTF-8";
    private static final String ALLOWED_URI_CHARS = "@#&=*+-_.,:!?()/~'%";
    private static final int DEFAULT_TIMEOUT = 30 * 1000;
    private static ConnectUtil instance;
    private Context mContext;


    private TrustManager[] trustAllCerts = {new X509TrustManager() {
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }

        public void checkClientTrusted(X509Certificate[] certs, String authType) {
        }

        public void checkServerTrusted(X509Certificate[] certs, String authType) {

        }
    }};

    /**
     * HOST验证
     */
    private class NullHostNameVerifier implements HostnameVerifier {
        public NullHostNameVerifier() {

        }

        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    }

    private ConnectUtil(Context ctx) {
        try {
            mContext = ctx;
            HttpsURLConnection.setDefaultHostnameVerifier(new NullHostNameVerifier());
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public static ConnectUtil getInstance(Context ctx) {
        if (instance == null) {
            synchronized (ConnectUtil.class) {
                if (instance == null) {
                    instance = new ConnectUtil(ctx);
                }
            }
        }
        return instance;
    }

    public HttpURLConnection getHttpURLConnection(String url) {
        try {
            String encodedUrl = Uri.encode(url, ALLOWED_URI_CHARS);
            HttpURLConnection conn = (HttpURLConnection) new URL(encodedUrl).openConnection();
            conn.setConnectTimeout(DEFAULT_TIMEOUT);
            conn.setReadTimeout(DEFAULT_TIMEOUT);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestMethod("GET");
            // not allow auto controll redirect,仅作用于当前URLConnection对象
            conn.setInstanceFollowRedirects(false);
            return conn;
        } catch (Exception e) {
        }
        return null;
    }

    public String doRequest(String url) {
        String result = null;
        InputStream is = null;
        BufferedReader br = null;
        HttpURLConnection conn = null;
        try {
            conn = getHttpURLConnection(url);
            if (conn == null) {
                return null;
            }
            is = conn.getInputStream();
            if (is == null) {
                return null;
            }
            br = new BufferedReader(new InputStreamReader(is, CHARSET));
            String line;
            StringBuffer sb = new StringBuffer();
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            result = sb.toString();
        } catch (Exception e) {
        } finally {
            try {
                if (is != null)
                    is.close();
            } catch (IOException e) {
            }
            try {
                if (br != null)
                    br.close();
            } catch (IOException e) {
            }
            if (conn != null) {
                conn = null;
            }
        }
        return result;
    }

}
