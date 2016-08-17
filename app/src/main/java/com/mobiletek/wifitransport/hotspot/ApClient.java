package com.mobiletek.wifitransport.hotspot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by linux-server-build8 on 6/15/16.
 */
public class ApClient {
    private final static String TAG = "ApClient";
    public static final int WIFICIPHER_NOPASS = 1;
    public static final int WIFICIPHER_WEP = 2;
    public static final int WIFICIPHER_WPA = 3;

    private final static int DURATION_TIME = 15 * 1000;
    private final static String SSID = ApServer.SSID;
    private final static String Password = ApServer.Password;

    private Context mContext;
    private WifiManagerAdapter mWifiManagerAdapter;
    private Timer mTimer;
    private OnConnectListener mOnConnectListener;
    private boolean isRegister = false;

    private static final int CONNECT_SUCCESSFULLY = 1;
    private static final int CONNECT_FAILED = 2;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case CONNECT_SUCCESSFULLY:
                    if (mOnConnectListener != null) {
                        mOnConnectListener.onConnectSuccessfully();
                    }
                    break;

                case CONNECT_FAILED:
                    if (mOnConnectListener != null) {
                        mOnConnectListener.onConnectFailed();
                    }
                    break;

                default:
                    super.handleMessage(msg);
                    break;
            }
        }
    };

    public ApClient(Context context) {
        this.mContext = context;
        mWifiManagerAdapter = new WifiManagerAdapter(context);
    }

    public void setmOnConnectListener(OnConnectListener onConnectListener) {
        this.mOnConnectListener = onConnectListener;
    }

    public void release() {
        if (mHandler != null) {
            mHandler.removeMessages(CONNECT_SUCCESSFULLY);
            mHandler.removeMessages(CONNECT_FAILED);
        }

        if (mTimer != null) {
            mTimer.cancel();
        }
    }

    public void addNetwork() {
        WifiInfo connectionInfo = mWifiManagerAdapter.getWifiManager().getConnectionInfo();
        String ssid = connectionInfo.getSSID();
        Log.i(TAG, "has connected " + ssid);
        if (ssid.equals(convertToQuotedString(SSID))) {
            if (mTimer != null) {
                mTimer.cancel();
            }
            mHandler.obtainMessage(CONNECT_SUCCESSFULLY).sendToTarget();
            return;
        }

        if (!mWifiManagerAdapter.getWifiManager().isWifiEnabled()) {
            mWifiManagerAdapter.getWifiManager().setWifiEnabled(true);
            Log.d(TAG, "open wifi switch");
        }

        if (mWifiManagerAdapter.isWifiApEnabled()) {
            mWifiManagerAdapter.setWifiApEnabled(null, false);
            Log.d(TAG, "close ap switch");
        }

        if (mTimer != null) {
            mTimer.cancel();
        }
        mTimer = new Timer(true);
        registerReceiver(mContext);
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                mWifiManagerAdapter.getWifiManager().startScan();
            }
        }, 0, 1000);
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                Log.d(TAG, "connect timeout");
                unregisterReceiver(mContext);
                mHandler.obtainMessage(CONNECT_FAILED).sendToTarget();
                mTimer.cancel();
            }
        }, DURATION_TIME);
    }

    private void registerReceiver(Context context) {
        if (!isRegister) {
            context.registerReceiver(mWifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
            isRegister = true;
        }
    }

    private void unregisterReceiver(Context context) {
        if (isRegister) {
            context.unregisterReceiver(mWifiReceiver);
            isRegister = false;
        }
    }

    private String convertToQuotedString(String string) {
        return "\"" + string + "\"";
    }

    public WifiConfiguration createWifiInfo(String SSID, String Password, int type)
    {
        WifiConfiguration config = new WifiConfiguration();
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();
        config.SSID = "\"" + SSID + "\"";

        WifiConfiguration tempConfig = isExsits(SSID);
        if(tempConfig != null) {
            mWifiManagerAdapter.getWifiManager().removeNetwork(tempConfig.networkId);
        }

        if(type == WIFICIPHER_NOPASS)
        {
            config.hiddenSSID = true;
            config.wepKeys[0] = "\"" + "\"";
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
        } else if(type == WIFICIPHER_WEP) {
            config.hiddenSSID = true;
            config.wepKeys[0]= "\"" + Password + "\"";
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
        } else if(type == WIFICIPHER_WPA) {
            config.preSharedKey = "\"" + Password + "\"";
            config.hiddenSSID = true;
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            //config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            config.status = WifiConfiguration.Status.ENABLED;
        }

        return config;
    }

    private WifiConfiguration isExsits(String SSID)
    {
        List<WifiConfiguration> existingConfigs = mWifiManagerAdapter.getWifiManager().getConfiguredNetworks();
        if (existingConfigs != null) {
            for (WifiConfiguration existingConfig : existingConfigs) {
                if (existingConfig.SSID.equals("\"" + SSID + "\"")) {
                    return existingConfig;
                }
            }
        }
        return null;
    }

    public String getLocalIP() {
        if (mWifiManagerAdapter.getWifiManager().isWifiEnabled()) {
            WifiInfo connectionInfo = mWifiManagerAdapter.getWifiManager().getConnectionInfo();
            int ipAddress = connectionInfo.getIpAddress();
            if (ipAddress != 0) {
                return (ipAddress & 0xFF) + "." +
                        ((ipAddress >> 8) & 0xFF) + "." +
                        ((ipAddress >> 16) & 0xFF) + "." +
                        (ipAddress >> 24 & 0xFF);
            }
        } else if (mWifiManagerAdapter.isWifiApEnabled()) {
            return "192.168.43.1";
        }
        return null;
    }

    public WifiManagerAdapter getWifiManagerAdapter() {
        return this.mWifiManagerAdapter;
    }

    public interface OnConnectListener {
        void onConnectSuccessfully();
        void onConnectFailed();
    }

    private class CheckWifiConnectTask extends TimerTask {
        @Override
        public void run() {
            Log.d(TAG, "check connect " + SSID);
            if (mWifiManagerAdapter.isWifiConnect()) {
                WifiInfo connectionInfo = mWifiManagerAdapter.getWifiManager().getConnectionInfo();
                if (connectionInfo.getSSID().equals(convertToQuotedString(SSID))) {
                    Log.d(TAG, SSID + " is connected");
                    mTimer.cancel();
                    mHandler.obtainMessage(CONNECT_SUCCESSFULLY).sendToTarget();
                }
            }
        }
    }
    private BroadcastReceiver mWifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            List<ScanResult> scanResults = mWifiManagerAdapter.getWifiManager().getScanResults();
            if (scanResults != null) {
                for (ScanResult scanResult : scanResults) {
                    Log.i(TAG, scanResult.SSID);
                    if (scanResult.SSID.equals(SSID)) {
                        WifiConfiguration config = createWifiInfo(scanResult.SSID, null, WIFICIPHER_NOPASS);
                        int netid = mWifiManagerAdapter.getWifiManager().addNetwork(config);
                        Log.d(TAG, "net_id=" + netid);
                        mWifiManagerAdapter.getWifiManager().enableNetwork(netid, true);
                        try {
                            mTimer.schedule(new CheckWifiConnectTask(), 0, 1000);
                        } catch (IllegalStateException e) {
                            e.printStackTrace();
                        }
                        unregisterReceiver(mContext);
                        break;
                    }
                }
            }
            Log.i(TAG, "************************");
        }
    };
}
