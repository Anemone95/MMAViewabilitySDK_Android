package cn.com.mma.mobile.tracking.viewability.origin.sniffer;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.List;


/**
 *
 */
public class StoreManager {


    private static final String SHAREDPREFERENCES_NAME = "viewability.explorer.pref";


    //    private static volatile StoreManager mInstance = null;
    private Context mContext;


    public StoreManager(Context context) {
        if (context == null) {
            throw new NullPointerException("StoreManager context can`t be null!");
        }
        mContext = context;
    }

//    public static StoreManager getInstance(Context context) {
//        if (mInstance == null) {
//            synchronized (StoreManager.class) {
//                if (mInstance == null) {
//                    mInstance = new StoreManager(context);
//                }
//            }
//        }
//        return mInstance;
//    }



    /**
     * 针对复杂类型存储<对象>
     *
     * @param key
     * @param object
     */
    public synchronized void setObject(String key, Object object) {

        SharedPreferences sp = mContext.getSharedPreferences(SHAREDPREFERENCES_NAME, Context.MODE_PRIVATE);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();//字节输出流

        ObjectOutputStream out = null;    //字节对象输出流
        try {
            out = new ObjectOutputStream(baos);
            out.writeObject(object);
            String objectVal = new String(Base64.encode(baos.toByteArray(), Base64.DEFAULT));//将字对象进行64转码
            SharedPreferences.Editor editor = sp.edit();
            editor.putString(key, objectVal);
            editor.commit();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (baos != null) {
                    baos.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public synchronized void removeObject(String impressionId) {
        SharedPreferences sp = mContext.getSharedPreferences(SHAREDPREFERENCES_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.remove(impressionId);
        editor.commit();
    }


    public List<ViewAbilityExplorer> getAll() throws Exception {
        SharedPreferences sp = mContext.getSharedPreferences(SHAREDPREFERENCES_NAME, Context.MODE_PRIVATE);
        List<ViewAbilityExplorer> exploerList = new ArrayList<>();
        for (String impressid : sp.getAll().keySet()) {
            String objectVal = sp.getString(impressid, null);
            ViewAbilityExplorer item = getObject(objectVal, ViewAbilityExplorer.class);
            if (item != null) exploerList.add(item);
        }
        return exploerList;
    }

    public <T> T getObject(String objectVal, Class<T> clazz) {
        byte[] buffer = Base64.decode(objectVal, Base64.DEFAULT);
        //写入对象并作强制转换
        ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
        ObjectInputStream ois = null;
        try {
            ois = new ObjectInputStream(bais);
            T t = (T) ois.readObject();
            return t;
        } catch (StreamCorruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bais != null) {
                    bais.close();
                }
                if (ois != null) {
                    ois.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }


}
