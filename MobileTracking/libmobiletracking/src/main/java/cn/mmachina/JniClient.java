package cn.mmachina;

import android.content.Context;

public class JniClient {

	public static int version = 1;

	static {
		System.loadLibrary("MMANDKSignature");
	}

	public static native String MDString(String service, Context context, String data);
}
