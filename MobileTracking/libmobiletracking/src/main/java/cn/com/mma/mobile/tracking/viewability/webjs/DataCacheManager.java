package cn.com.mma.mobile.tracking.viewability.webjs;


import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import org.json.JSONObject;
import java.util.Set;

/**
 * Created by admaster on 17/8/2.
 */
public class DataCacheManager {

    private static final String SHAREDPREFERENCES_NAME = "mma.viewabilityjs.data";
    private static final String JSON_EXPIRED_TIME = "expired_time";
    private static final String JSON_CACHEDATA = "cache_data";
    private static final long THREE_DAY_INTERVIAL = 1000 * 60 * 60 * 24 * 3;


    private static volatile DataCacheManager mInstance = null;
    private Context mContext;

    private DataCacheManager(Context context) {
        if (context == null) {
            throw new NullPointerException("DataCacheManager context can`t be null!");
        }
        mContext = context;
    }

    public static DataCacheManager getInstance(Context context) {
        if (mInstance == null) {
            synchronized (DataCacheManager.class) {
                if (mInstance == null) {
                    mInstance = new DataCacheManager(context);
                }
            }
        }
        return mInstance;
    }


    private SharedPreferences getSharedPreferences() {
        return mContext.getSharedPreferences(SHAREDPREFERENCES_NAME, Context.MODE_PRIVATE);
    }


    /**
     * @param companyName
     * @param events
     */
    public synchronized void setData(String companyName, String events) {
        try {

            JSONObject json = new JSONObject();
            long expired_time = System.currentTimeMillis() + THREE_DAY_INTERVIAL;
            json.put(JSON_EXPIRED_TIME, expired_time);
            json.put(JSON_CACHEDATA, events);

            SharedPreferences.Editor editor = getSharedPreferences().edit();
            editor.putString(companyName, json.toString());
            editor.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * @param companyName
     * @return
     */
    public synchronized String getData(String companyName) {
        String data = "";
        try {
            String cache = getSharedPreferences().getString(companyName, "");
            //WEBVIEW
            if (!TextUtils.isEmpty(cache)) {
                JSONObject json = new JSONObject(cache);
                data = json.getString(JSON_CACHEDATA);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return data;
    }

    public synchronized void clearData(String companyName) {

        try {
            SharedPreferences.Editor editor = getSharedPreferences().edit();
            editor.remove(companyName);
            editor.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public synchronized void clearnExpiredData() {

        try {
            SharedPreferences sharedPreferences = getSharedPreferences();
            Set<String> companys = sharedPreferences.getAll().keySet();
            for (String companyName : companys) {
                String itemData = sharedPreferences.getString(companyName, "");
                if (!TextUtils.isEmpty(itemData)) {
                    JSONObject json = new JSONObject(itemData);
                    long expiredItme = json.getLong(JSON_EXPIRED_TIME);
                    //对于超过3天时间的数据直接清理掉
                    if (expiredItme > System.currentTimeMillis()) {
                        clearData(companyName);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
