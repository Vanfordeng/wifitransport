package com.mobiletek.wifitransport.hotspot;

import android.content.Context;

/**
 * Created by linux-server-build8 on 7/1/16.
 */
public class WifiStatus {
    private static WifiStatus instance = new WifiStatus();
    private boolean wifiApEnabled = false;
    private boolean wifiEnabled = false;

    private WifiStatus(){}
    public static WifiStatus getInstance() {
        return instance;
    }

    public void save(Context context) {
        WifiManagerAdapter wifiManagerAdapter = new WifiManagerAdapter(context);
        wifiApEnabled = wifiManagerAdapter.isWifiApEnabled();
        wifiEnabled = wifiManagerAdapter.getWifiManager().isWifiEnabled();
    }

    public void restore(Context context) {
        WifiManagerAdapter wifiManagerAdapter = new WifiManagerAdapter(context);
        if (wifiApEnabled) {
            if(wifiManagerAdapter.getWifiManager().isWifiEnabled()) {
                wifiManagerAdapter.getWifiManager().setWifiEnabled(false);
            }
            if(!wifiManagerAdapter.isWifiApEnabled()) {
                wifiManagerAdapter.setWifiApEnabled(null, true);
            }
        } else if (wifiEnabled) {
            if (wifiManagerAdapter.isWifiApEnabled()) {
                wifiManagerAdapter.setWifiApEnabled(null, false);
            }
            if (!wifiManagerAdapter.getWifiManager().isWifiEnabled()) {
                wifiManagerAdapter.getWifiManager().setWifiEnabled(true);
            }
        } else {
            if (wifiManagerAdapter.isWifiApEnabled()) {
                wifiManagerAdapter.setWifiApEnabled(null, false);
            }
            if (wifiManagerAdapter.getWifiManager().isWifiEnabled()) {
                wifiManagerAdapter.getWifiManager().setWifiEnabled(false);
            }
        }
    }
}
