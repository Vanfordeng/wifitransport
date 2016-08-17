package com.mobiletek.wifitransport;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import com.mobiletek.wifitransport.hotspot.WifiStatus;

public class TransportActivity extends Activity {
    public static final int REQUEST_CODE_READ_EXTERNAL_STORAGE = 1;
    public static final int REQUEST_CODE_WRITE_EXTERNAL_STORAGE = 2;
    public static final int REQUEST_CODE_GET_CONTENT = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transport);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Fragment fragment = getFragmentManager().findFragmentById(R.id.send_fragment);
            if (fragment != null) {
                fragment.onRequestPermissionsResult(requestCode, permissions, grantResults);
            }
            fragment = getFragmentManager().findFragmentById(R.id.receive_fragment);
            if (fragment != null) {
                fragment.onRequestPermissionsResult(requestCode, permissions, grantResults);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Fragment fragment = getFragmentManager().findFragmentById(R.id.send_fragment);
        if (fragment != null) {
            fragment.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        WifiStatus.getInstance().restore(this);
    }
}
