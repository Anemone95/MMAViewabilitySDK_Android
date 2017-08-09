package cn.com.mma.mobile.tracking.util;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import cn.com.mma.mobile.tracking.api.Constant;

public class LocationUtil {

	private LocationManager locationManager;

	private static LocationUtil locationUtil = null;
	private StringBuilder locationBuilder = new StringBuilder();

	public String getLocation() {
		return locationBuilder.toString();
	}

	private LocationUtil(Context context) {
		locationManager = (LocationManager) context
				.getSystemService(Context.LOCATION_SERVICE);
	}

	public static LocationUtil getInstance(Context context) {
		if (locationUtil == null) {
			locationUtil = new LocationUtil(context);
		}
		return locationUtil;
	}

	/**
	 * 位置监听
	 */
	public LocationListener listener = new LocationListener() {

		public void onStatusChanged(String s, int i, Bundle bundle) {
			Logger.d("onStatusChanged:" + s);

		}

		public void onProviderEnabled(String s) {
			Logger.d("onProviderEnabled:" + s);

		}

		public void onProviderDisabled(String s) {
			Logger.d("onProviderDisabled:" + s);
		}

		public void onLocationChanged(Location location) {
			double longitude = location.getLongitude();
			double latitude = location.getLatitude();
			Logger.d(latitude + "x" + longitude);
			locationBuilder = locationBuilder.append(latitude).append("x")
					.append(longitude);
		}
	};;

	/**
	 * 获得当前地理位置信息及准确程度
	 * 
	 * @param context
	 * @param requestUpdate
	 * @return
	 */
	public void startLocationListener() {
		try {
			Criteria criteria = new Criteria();
			criteria.setAccuracy(Criteria.ACCURACY_COARSE);
			criteria.setAltitudeRequired(false);
			criteria.setBearingRequired(false);
			criteria.setCostAllowed(true);
			criteria.setPowerRequirement(Criteria.POWER_LOW);
			String provider = locationManager.getBestProvider(criteria, true);
			locationManager.requestLocationUpdates(provider,
					Constant.LOCATIOON_UPDATE_INTERVAL, 0, listener);
		} catch (Exception e) {
			Logger.d("mma_Error data LBS" + e);
			e.printStackTrace();
		}
	}

	/**
	 * 停止监听位置信息
	 * 
	 * @param context
	 */
	public void stopListenLocation() {
		if (locationManager != null) {
			locationManager.removeUpdates(listener);
			Logger.d("停止位置监听");
		}
	}
}
