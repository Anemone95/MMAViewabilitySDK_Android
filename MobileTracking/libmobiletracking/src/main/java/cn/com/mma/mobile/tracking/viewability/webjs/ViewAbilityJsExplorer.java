package cn.com.mma.mobile.tracking.viewability.webjs;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.SslErrorHandler;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import cn.com.mma.mobile.tracking.api.Countly;
import cn.com.mma.mobile.tracking.bean.Company;
import cn.com.mma.mobile.tracking.util.Logger;
import cn.com.mma.mobile.tracking.util.klog.KLog;


/**
 * 每一个ViewAbilityJsExplorer实体相当于一个WebView对象,同一个公司检测下只存在一个,KEY = Company.NAME
 * 同一Company下如果同时存在多个监测,在各自Explorer内循环执行
 * Created by admaster on 17/7/28.
 */
public class ViewAbilityJsExplorer {


    private Context mContext;
    private WebView mWebView;
    private String bridgeJs;
    private JSBridgeLoader jsBridgeLoader;
    private HashMap<String, ViewAbilityJsBean> monitorWorkers;
    private Handler mainHandler;
    private DataCacheManager dataCacheManager;
    private String companyName;

    private static final String JS_INTERFACE_SENDMESSAGE = "javascript:sendViewabilityMessage";
    private static final String JS_INTERFACE_SENDCACHEMESSAGE = "javascript:sendCacheMessage";


    private static final String JS_SCHEME = "mmaViewabilitySDK";
    private static final String JS_AUTHORTY_STOPVIEWABILITY = "stopViewability";
    private static final String JS_AUTHORTY_SAVEJSCACHEDATA = "saveJSCacheData";
    private static final String JS_AUTHORTY_GETJSCACHEDATA = "getJSCacheData";
    private static final String JS_PARAMS_ADVIEWABILITYID = "AdviewabilityID";
    private static final String JS_PARAMS_DATA = "data";
    private static final String JS_PARAMS_CLEAR = "clear";
    private boolean isJavaScriptEnabled = false;

    public ViewAbilityJsExplorer(Context context, Company company) {
        mContext = context;
        monitorWorkers = new HashMap<>();
        mainHandler = new Handler(Looper.getMainLooper());
        dataCacheManager = DataCacheManager.getInstance(mContext);
        companyName = company.name;
        jsBridgeLoader = new JSBridgeLoader(context, company);
        //每次初始化从CACHE中取JS配置,同时JSBridgeLoader初始化时会自动从线上获取最新的JS配置,下次启动生效
        bridgeJs = jsBridgeLoader.getBridgeJs();
        jsBridgeLoader.doUpdate();

        initWebViews();
    }


    private void initWebViews() {

        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                mWebView = new WebView(mContext);
                WebSettings ws = mWebView.getSettings();
                try {
                    ws.setJavaScriptEnabled(true);
                    //ws.setPluginState(WebSettings.PluginState.ON);
                    ws.setJavaScriptCanOpenWindowsAutomatically(false);
                    //ws.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);
                    ws.setDomStorageEnabled(false);
                    ws.setCacheMode(WebSettings.LOAD_NO_CACHE);
                    ws.setAllowFileAccess(false);
                    ws.setAppCacheEnabled(false);
                    // 以下两条设置可以使页面适应手机屏幕的分辨率，完整的显示在屏幕上
                    // 设置是否使用WebView推荐使用的窗口
                    ws.setUseWideViewPort(false);
                    // 设置WebView加载页面的模式
                    ws.setLoadWithOverviewMode(false);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                mWebView.clearHistory();
                mWebView.clearCache(true);
                isJavaScriptEnabled = ws.getJavaScriptEnabled();
                //mWebView.setWebChromeClient(new MyWebChromeClient());
                mWebView.setWebViewClient(new MyWebViewClient());

                //初始化时加载空白HTML网页
                initJavaScripts();
            }
        });
    }

    private void initJavaScripts() {

        String content = String.format("<!DOCTYPE html>\n<html>\n<head lang=\"en\">\n    <meta charset=\"UTF-8\">\n  <title></title>\n</head>\n<body style=\"margin:0;padding:0;\">\n  <div id=\"mian\" style=\"width:%dpx;height:%dpx;\">\n <script type=\"text/javascript\">%s</script>\n</div>\n</body>\n</html>", 1, 1, bridgeJs);

        //KLog.d("loadURL:" + content);
        //mWebView.loadUrl(html);
        mWebView.loadData(content, "text/html", null);

    }


    public void addExplorerTask(String adURL, View adView, boolean isVideo) {


//        if (!isJavaScriptEnabled) {
//            Logger.e("JavaScript is not enable in current WebView,the current monitoring scheme is not available.");
//            return;
//        }

        //如果JS为空,则在线获取一次
        if (TextUtils.isEmpty(bridgeJs)) {
            jsBridgeLoader.doUpdate();
        }

        ViewAbilityJsBean abilityJsBean = new ViewAbilityJsBean(adURL, adView);
        abilityJsBean.setVideo(isVideo);
        monitorWorkers.put(abilityJsBean.getAdviewabilityId(), abilityJsBean);

        Logger.d("URL:" + adURL + " 开启View Ability JS 监测,监测ID:" + abilityJsBean.getAdviewabilityId());

        //[LOCALTEST] 测试计数:带ViewAbility曝光事件产生计数
        if (Countly.LOCAL_TEST) {
            Intent intent = new Intent(Countly.ACTION_STATS_VIEWABILITY);
            mContext.sendBroadcast(intent);
        }

    }


    public void onExplore() {

        try {
            if (!isJavaScriptEnabled) return;

            // mainHandler.post(mainThread);

            //TODO 主线程中调用
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (mWebView == null && mWebView.getUrl() == null) {
                            Logger.w("The webview is initializing,and this monitoring frame is discarded.");
                            return;
                        }

                        if (monitorWorkers.isEmpty()) return;

                        //用来存放失效或者已完成的序列数组
                        List<String> completes = new ArrayList<>();

                        JSONArray eventArr = new JSONArray();

                        for (String adviewabilityId : monitorWorkers.keySet()) {

                            ViewAbilityJsBean viewAbilityJsBean = monitorWorkers.get(adviewabilityId);

                            //KLog.v("item viewAbilityJsBean:" + viewAbilityJsBean.toString());

                            //isCompleted 代表JS调用了 STOP 指令,停止监测,从池内移除
                            if (viewAbilityJsBean.isCompleted()) {
                                completes.add(viewAbilityJsBean.getAdviewabilityId());
                            } else {
                                //监测目标视图被释放
                                boolean isRecycled = viewAbilityJsBean.getAdView() == null ? true : false;

                                JSONObject eventItem = viewAbilityJsBean.generateUploadEvents(mContext, isRecycled);
                                eventArr.put(eventItem);
                            }
                        }

                        //遍历完毕移除已经完成或失效的工作者
                        for (String adviewabilityId : completes) {
                            monitorWorkers.remove(adviewabilityId);
                        }


                        if (eventArr.length() > 0) {
                            String fire = String.format(JS_INTERFACE_SENDMESSAGE + "(%s)", eventArr.toString());
                            //String fire = String.format(JS_INTERFACE_SENDMESSAGE + "(JSON.stringify(%s))", eventArr.toString());
                            //KLog.i("onExplore", "fire:" + fire);
                            mWebView.loadUrl(fire);
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    public void onDestory() {
        if (mWebView != null) {
            mWebView.loadDataWithBaseURL(null, "", "text/html", "utf-8", null);
            mWebView.clearHistory();
            mWebView.clearCache(true);

            ((ViewGroup) mWebView.getParent()).removeView(mWebView);
            mWebView.destroy();
            mWebView = null;
        }
    }


    private class MyWebViewClient extends WebViewClient {

//        @Override
//        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
//            return super.shouldOverrideUrlLoading(view, request);
//        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            //System.out.println("shouldOverrideUrlLoading:" + url);
            return handlerWebJsMessage(view, url);
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            //super.onReceivedSslError(view, handler, error);
            handler.proceed();
            //System.out.println("onReceivedSslError:" + error);
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
//            System.out.println("onPageStarted:" + url);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
//            System.out.println("onPageFinished:" + url);
        }


        private boolean handlerWebJsMessage(WebView view, String url) {

            try {
                Uri uri = Uri.parse(url);
                KLog.d("MyWebViewClient:handlerWebJsMessage:" + uri);
                //mmaViewabilitySDK
                if (uri.getScheme().equalsIgnoreCase(JS_SCHEME)) {
                    String authority = uri.getAuthority();

//                    KLog.i("getEncodedPath:" + uri.toString());
//                    KLog.i("authority:" + authority);
//                    KLog.i("JS_PARAMS_ADVIEWABILITYID:" + uri.getQueryParameter(JS_PARAMS_ADVIEWABILITYID));
//                    KLog.i("JS_PARAMS_DATA:" + uri.getQueryParameter(JS_PARAMS_DATA));
//                    KLog.i("JS_PARAMS_CLEAR:" + uri.getQueryParameter(JS_PARAMS_CLEAR));

                    switch (authority) {
                        case JS_AUTHORTY_STOPVIEWABILITY:
                            onStopViewability(uri);
                            break;
                        case JS_AUTHORTY_SAVEJSCACHEDATA:
                            onSaveJSCacheData(uri);
                            break;
                        case JS_AUTHORTY_GETJSCACHEDATA:
                            onGetJSCacheData(uri);
                            break;
                    }
                    return true;
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            return super.shouldOverrideUrlLoading(view, url);
        }


        /**
         * 停止某一广告位的ViewAbility监测
         * mmaViewabilitySDK://stopViewability?adviewabilityid=asd
         *
         * @param uri
         */
        private void onStopViewability(Uri uri) throws Exception {
            String adviewabilityid = uri.getQueryParameter(JS_PARAMS_ADVIEWABILITYID);
            if (TextUtils.isEmpty(adviewabilityid)) {
                Logger.w("stopViewability protocol params adviewabilityid is empty.");
                return;
            }
            ViewAbilityJsBean abilityJsBean = monitorWorkers.get(adviewabilityid);
            if (abilityJsBean != null) abilityJsBean.setCompleted(true);

            //[LOCALTEST] 测试计数:记录发送成功
            if (Countly.LOCAL_TEST) {
                Intent intent = new Intent(Countly.ACTION_STATS_SUCCESSED);
                mContext.sendBroadcast(intent);
            }
        }

        /**
         * 缓存JS数据,以CompanyName为KEY,每次覆盖
         * mmaViewabilitySDK://saveJSCacheData?data=XXXXXX
         *
         * @param uri
         */
        private void onSaveJSCacheData(Uri uri) {
            String data = uri.getQueryParameter(JS_PARAMS_DATA);
            if (TextUtils.isEmpty(data)) {
                Logger.w("saveJSCacheData protocol params data is empty.");
                return;
            }
            //本地化缓存,KEY使用company.Name,直接覆盖
            dataCacheManager.setData(companyName, data);
        }

        /**
         * 读取本地缓存JS数据,发送给JS后本地清除
         * mmaViewabilitySDK://getJSCacheData？clear=true
         *
         * @param uri
         */
        private void onGetJSCacheData(Uri uri) {
            boolean clear;
            if (Build.VERSION.SDK_INT >= 11) {
                clear = uri.getBooleanQueryParameter(JS_PARAMS_CLEAR, false);
            } else {
                clear = Boolean.valueOf(uri.getQueryParameter(JS_PARAMS_CLEAR));
            }

            String data = dataCacheManager.getData(companyName);
            if (!TextUtils.isEmpty(data)) {
                //回传给JS
                String fire = String.format(JS_INTERFACE_SENDCACHEMESSAGE + "(JSON.stringify(%s))", data);
                KLog.d("onGetJSCacheData->clear:" + clear + "  conetent:" + fire);
                mWebView.loadUrl(fire);
            }

            //清除数据
            if (clear) {
                dataCacheManager.clearData(companyName);
            }

        }


    }

}
