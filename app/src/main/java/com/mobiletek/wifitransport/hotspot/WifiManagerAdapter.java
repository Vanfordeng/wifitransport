package com.mobiletek.wifitransport.hotspot;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.mobiletek.wifitransport.utils.UtilsPermission;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by linux-server-build8 on 6/15/16.
 */
public class WifiManagerAdapter {

    private final static String TAG = "WifiManagerAdapter";

    private WifiManager wifiManager;
    private ConnectivityManager connectivityManager;

    public WifiManagerAdapter(Context context) {
        this.wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    public boolean isWifiApEnabled() {
        try {
            Method method = wifiManager.getClass().getMethod("isWifiApEnabled");
            method.setAccessible(true);
            return (Boolean) method.invoke(wifiManager);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            Log.e(TAG, "isWifiApEnabled call error: ", e);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            Log.e(TAG, "isWifiApEnabled call error: ", e);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            Log.e(TAG, "isWifiApEnabled call error: ", e);
        }

        return false;
    }

    public boolean setWifiApEnabled(WifiConfiguration wifiConfig, boolean enabled) {
        try {
            Method setWifiApEnabled = wifiManager.getClass().getMethod("setWifiApEnabled", new Class<?>[]{WifiConfiguration.class, Boolean.TYPE});
            setWifiApEnabled.setAccessible(true);
            return (Boolean) setWifiApEnabled.invoke(wifiManager, new Object[]{wifiConfig, enabled});
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            Log.e(TAG, "setWifiApEnabled: call error: ", e);
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean isWifiConnect() {
        NetworkInfo wifiNetworkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return wifiNetworkInfo.getDetailedState() == NetworkInfo.DetailedState.CONNECTED;
    }

    public WifiManager getWifiManager() {
        return this.wifiManager;
    }
}
