package com.mobiletek.wifitransport.hotspot;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by linux-server-build8 on 6/15/16.
 */
public class ApServer {
    private final static String TAG = "ApServer";
    public final static String SSID = "M1503_AP";
    public final static String Password = "mobiletek";
    private final static int DURATION_TIME = 15 * 1000;

    public static final int WIFICIPHER_NOPASS = 1;
    public static final int WIFICIPHER_WPA2_PSK = 3;

    public static final int AP_OPEN_SUCCESSFULLY = 1;
    public static final int AP_OPEN_FAILED = 2;

    private WifiManagerAdapter mWifiManagerAdapter;
    private Timer mTimer;
    private OnAPOpenListener mOnAPOpenListener;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case AP_OPEN_SUCCESSFULLY:
                    if (mOnAPOpenListener != null) {
                        mOnAPOpenListener.onOpenSuccessfully();
                    }
                    break;

                case AP_OPEN_FAILED:
                    if (mOnAPOpenListener != null) {
                        mOnAPOpenListener.onOpenFailed();
                    }
                    break;

                default:
                    super.handleMessage(msg);
                    break;
            }
        }
    };

    public ApServer(Context context) {
        mWifiManagerAdapter = new WifiManagerAdapter(context);
    }

    public void setmOnAPOpenListener(OnAPOpenListener onAPOpenListener) {
        this.mOnAPOpenListener = onAPOpenListener;
    }

    public void CreateAP() {
        if (mWifiManagerAdapter.getWifiManager().isWifiEnabled()) {
            mWifiManagerAdapter.getWifiManager().setWifiEnabled(false);
            Log.d(TAG, "close wifi switch");
        }

        if (mWifiManagerAdapter.isWifiApEnabled()) {
            mWifiManagerAdapter.setWifiApEnabled(null, false);
            Log.d(TAG, "close ap switch");
        }

//        WifiConfiguration config = createWifiInfo(WIFICIPHER_WPA2_PSK);
        WifiConfiguration config = createWifiInfo(WIFICIPHER_NOPASS);
        CreateAP(config);

        startCheckAP();
    }

    private void CreateAP(WifiConfiguration wifiConfiguration) {
        if (mWifiManagerAdapter.setWifiApEnabled(wifiConfiguration, true)) {
            Log.i(TAG, "setWifiApEnabled call successfully");
        } else {
            Log.i(TAG, "setWifiApEnabled call unsuccessfully");
        }
    }

    private WifiConfiguration createWifiInfo(int type) {
        WifiConfiguration config = new WifiConfiguration();
        config.SSID = SSID;

        if(type == WIFICIPHER_NOPASS) {
            config.allowedAuthAlgorithms.set(WifiConfiguration.KeyMgmt.NONE);
        } else if (type == WIFICIPHER_WPA2_PSK) {
            config.preSharedKey = Password;

            config.hiddenSSID = true;
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedKeyManagement.set(4);//WifiConfiguration.WPA2_PSK
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            config.status = WifiConfiguration.Status.ENABLED;
        }

        return config;
    }

    public List<String> getConnectedIPList() {
        List<String> connectedIPList = null;
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader("/proc/net/arp"));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                Log.i(TAG, line);
                Pattern compile = Pattern.compile("(192\\.168\\.43\\.[0-9]+)(.*)");
                Matcher matcher = compile.matcher(line);
                if (matcher.matches()) {
                    if (matcher.groupCount() >= 2) {
                        if (connectedIPList == null) {
                            connectedIPList = new ArrayList<String>();
                        }
                        connectedIPList.add(matcher.group(1));
                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return connectedIPList;
    }

    public WifiManagerAdapter getWifiManagerAdapter() {
        return this.mWifiManagerAdapter;
    }

    private void startCheckAP() {
        if (mTimer != null) {
            mTimer.cancel();
        }

        mTimer = new Timer(true);
        mTimer.schedule(new CheckAPTask(), 0, 1000);
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                Log.d(TAG, "check timeout");
                mHandler.obtainMessage(AP_OPEN_FAILED).sendToTarget();
                mTimer.cancel();
            }
        }, DURATION_TIME);
    }

    public void release() {
        if (mHandler != null) {
            mHandler.removeMessages(AP_OPEN_SUCCESSFULLY);
            mHandler.removeMessages(AP_OPEN_FAILED);
        }

        if (mTimer != null) {
            mTimer.cancel();
        }
    }

    public interface OnAPOpenListener {
        void onOpenSuccessfully();
        void onOpenFailed();
    }

    private class CheckAPTask extends TimerTask {
        @Override
        public void run() {
            Log.d(TAG, "check " + SSID);
            if (mWifiManagerAdapter.isWifiApEnabled()) {
                Log.d(TAG, SSID + " is available");
                mHandler.obtainMessage(AP_OPEN_SUCCESSFULLY).sendToTarget();
                mTimer.cancel();
            }
        }
    }
}
