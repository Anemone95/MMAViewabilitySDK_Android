package cn.com.mma.mobile.tracking.util;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import android.net.Uri;
import android.text.TextUtils;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;


public class ConnectUtil {

    //private static final String CHARSET = "UTF-8";
    private static final String ALLOWED_URI_CHARS = "@#&=*+-_.,:!?()/~'%";
    private static final int CONNECT_TIMEOUT = 30 * 1000;
    private static final int READ_TIMEOUT = 30 * 1000;

    private static ConnectUtil instance;


    private ConnectUtil() {
        try {

            HttpsURLConnection.setDefaultHostnameVerifier(new NullHostNameVerifier());
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllManager, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public static ConnectUtil getInstance() {
        if (instance == null) {
            synchronized (ConnectUtil.class) {
                if (instance == null) {
                    instance = new ConnectUtil();
                }
            }
        }
        return instance;
    }

//    public HttpURLConnection getHttpURLConnection(String url) {
//        try {
//            String encodedUrl = Uri.encode(url, ALLOWED_URI_CHARS);
//            HttpURLConnection conn = (HttpURLConnection) new URL(encodedUrl).openConnection();
//            conn.setConnectTimeout(CONNECT_TIMEOUT);
//            conn.setReadTimeout(READ_TIMEOUT);
//            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
//            conn.setRequestMethod("GET");
//            // not allow auto controll redirect,仅作用于当前URLConnection对象
//            conn.setInstanceFollowRedirects(false);
//            return conn;
//        } catch (Exception e) {
//        }
//        return null;
//    }
//
//    public String doRequest(String url) {
//        String result = null;
//        InputStream is = null;
//        BufferedReader br = null;
//        HttpURLConnection conn = null;
//        try {
//            conn = getHttpURLConnection(url);
//            if (conn == null) {
//                return null;
//            }
//            is = conn.getInputStream();
//            if (is == null) {
//                return null;
//            }
//            br = new BufferedReader(new InputStreamReader(is, CHARSET));
//            String line;
//            StringBuffer sb = new StringBuffer();
//            while ((line = br.readLine()) != null) {
//                sb.append(line);
//            }
//            result = sb.toString();
//        } catch (Exception e) {
//        } finally {
//            try {
//                if (is != null)
//                    is.close();
//            } catch (IOException e) {
//            }
//            try {
//                if (br != null)
//                    br.close();
//            } catch (IOException e) {
//            }
//            if (conn != null) {
//                conn = null;
//            }
//        }
//        return result;
//    }

    public byte[] performGet(String destURL) {
        //Logger.d("Attempting Get to " + destURL + "\n");
        byte[] response = null;
        HttpURLConnection httpConnection = null;
        InputStream is = null;

        try {
            String encodedUrl = Uri.encode(destURL, ALLOWED_URI_CHARS);

            URL url = new URL(encodedUrl);

            httpConnection = (HttpURLConnection) url.openConnection();

            httpConnection.setConnectTimeout(CONNECT_TIMEOUT);
            httpConnection.setReadTimeout(READ_TIMEOUT);
            httpConnection.setRequestMethod("GET");
            httpConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            int statusCode = httpConnection.getResponseCode();


            if (statusCode == HttpURLConnection.HTTP_OK || statusCode == HttpURLConnection.HTTP_MOVED_PERM || statusCode == HttpURLConnection.HTTP_MOVED_TEMP) {

                try {
                    is = httpConnection.getInputStream();
                    response = writeToArr(is);
                } catch (Exception e) {
                    response = new byte[]{};
                }

                //redirect
                String redirectURL = httpConnection.getHeaderField("Location");
                if (!TextUtils.isEmpty(redirectURL)) {
                    httpConnection = (HttpURLConnection) new URL(redirectURL).openConnection();
                    statusCode = httpConnection.getResponseCode();
                    //Logger.d("redirect statusCode::" + statusCode);
                }
            }
        } catch (Exception e) {
        } finally {
            if (null != is)
                try {
                    is.close();
                } catch (final IOException e) {
                }
            if (null != httpConnection)
                httpConnection.disconnect();
        }

        return response;
    }

    public byte[] performPost(String destURL, String data) {
        //Logger.d("Attempting Post to " + destURL + "\n");

        byte[] response = null;

        OutputStream os = null;
        BufferedOutputStream bos = null;
        HttpURLConnection httpConnection = null;
        InputStream is = null;

        try {
            URL url = new URL(destURL);
            httpConnection = (HttpURLConnection) url.openConnection();

            httpConnection.setConnectTimeout(CONNECT_TIMEOUT);
            httpConnection.setReadTimeout(READ_TIMEOUT);
            httpConnection.setDoOutput(true);
            httpConnection.setDoInput(true);
            httpConnection.setRequestMethod("POST");
            httpConnection.setRequestProperty("Content-Type", "text/plain");
            //httpConnection.setRequestProperty("Accept-Encoding", "");//gzip,deflate

            os = httpConnection.getOutputStream();//upload

            bos = new BufferedOutputStream(os);
            bos.write(data.getBytes("UTF-8"));
            bos.flush();

            int statusCode = httpConnection.getResponseCode();


            if (statusCode == HttpURLConnection.HTTP_OK) {
                // 使用普通流读取
                is = httpConnection.getInputStream();
                response = writeToArr(is);
            }
        } catch (Exception e) {
        } finally {
            if (null != bos) {
                try {
                    bos.close();
                } catch (final IOException e) {
                }
            }
            if (null != os) {
                try {
                    os.close();
                } catch (final IOException e) {
                }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                }
            }

            if (null != httpConnection)
                httpConnection.disconnect();
        }

        return response;
    }

    /**
     * 读取到Buffer内，转换成byte[]数组
     */
    private static byte[] writeToArr(final InputStream is) throws IOException {
        if (is == null) {
            return null;
        }
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int nRead;
        byte[] data = new byte[8192];

        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        // buffer.close();
        return buffer.toByteArray();
    }


    private static TrustManager[] trustAllManager = {new X509TrustManager() {
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

}
