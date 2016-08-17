package com.mobiletek.wifitransport;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.mobiletek.wifitransport.hotspot.ApClient;
import com.mobiletek.wifitransport.hotspot.ApServer;
import com.mobiletek.wifitransport.hotspot.WifiStatus;
import com.mobiletek.wifitransport.utils.UtilsPermission;

import java.util.Set;

public class ConnectActivity extends Activity {
    private static final String TAG = "ConnectActivity";
    public static final int REQUEST_CODE_CONNECT_WIFI = 1;
    public static final int REQUEST_CODE_OPEN_AP = 2;
    public static final int REQUEST_CODE_WRITE_SETTINGS = 3;
    public static final int REQUEST_CODE_ACCESS_COARSE_LOCATION = 4;

    private RadioGroup radio_group;
    private RadioButton radioAsAp;
    private RadioButton radioConnectAp;
    private ProgressBar progressBar_Connect;

    private ApServer apServer;
    private ApClient apClient;

    private UtilsPermission utilsPermission = new UtilsPermission();
    private boolean mDirectFinishEnable = false;
    private boolean mHasSaved = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);

        radio_group = (RadioGroup) findViewById(R.id.radio_group);
        radioAsAp = (RadioButton) findViewById(R.id.radioAsAp);
        radioConnectAp = (RadioButton) findViewById(R.id.radioConnectAp);
        progressBar_Connect = (ProgressBar) findViewById(R.id.progressBar_Connect);

        mDirectFinishEnable = true;
        Intent intent = getIntent();
        if (intent != null) {
            String action = intent.getAction();
            Set<String> categories = intent.getCategories();
            if (action != null && action.equals(Intent.ACTION_MAIN) &&
                    categories != null && categories.contains(Intent.CATEGORY_LAUNCHER)) {
                mDirectFinishEnable = false;
            }
        }

        if (savedInstanceState == null) {
            radio_group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    if (checkedId == R.id.radioAsAp && radioAsAp.isChecked()) {

                        if (apServer == null) {
                            apServer = new ApServer(ConnectActivity.this);
                            apServer.setmOnAPOpenListener(new ApServer.OnAPOpenListener() {
                                @Override
                                public void onOpenSuccessfully() {
                                    Toast.makeText(ConnectActivity.this, R.string.open_hotspot_successfully, Toast.LENGTH_SHORT).show();
                                    progressBar_Connect.setVisibility(View.GONE);
                                    lunchTransportActivity();
                                }

                                @Override
                                public void onOpenFailed() {
                                    progressBar_Connect.setVisibility(View.GONE);
                                    new AlertDialog.Builder(ConnectActivity.this)
                                            .setTitle(R.string.open_hotspot_failed)
                                            .setMessage(R.string.message_open_hotspot_failed)
                                            .setCancelable(false)
                                            .setNeutralButton(R.string.quit, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    finish();
                                                }
                                            })
                                            .setNegativeButton(R.string.manual_operation, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    Intent intent = new Intent();
                                                    intent.setComponent(new ComponentName("com.android.settings", "com.android.settings.TetherSettings"));
                                                    startActivityForResult(intent, REQUEST_CODE_OPEN_AP);
                                                }
                                            })
                                            .setPositiveButton(R.string.retry, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    openAp();
                                                }
                                            })
                                            .show();
                                }
                            });
                        }
                        openAp();
                    } else if (checkedId == R.id.radioConnectAp && radioConnectAp.isChecked()) {

                        if (apClient == null) {
                            apClient = new ApClient(ConnectActivity.this);
                            apClient.setmOnConnectListener(new ApClient.OnConnectListener() {
                                @Override
                                public void onConnectSuccessfully() {
                                    Toast.makeText(ConnectActivity.this, R.string.connect_successfully, Toast.LENGTH_SHORT).show();
                                    progressBar_Connect.setVisibility(View.GONE);
                                    lunchTransportActivity();
                                }

                                @Override
                                public void onConnectFailed() {
                                    progressBar_Connect.setVisibility(View.GONE);
                                    new AlertDialog.Builder(ConnectActivity.this)
                                            .setTitle(R.string.connect_failed)
                                            .setMessage(R.string.message_connect_failed)
                                            .setCancelable(false)
                                            .setNeutralButton(R.string.quit, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    finish();
                                                }
                                            })
                                            .setNegativeButton(R.string.manual_operation, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    Intent intent = new Intent("android.settings.WIFI_SETTINGS");
                                                    startActivityForResult(intent, REQUEST_CODE_CONNECT_WIFI);
                                                }
                                            })
                                            .setPositiveButton(R.string.retry, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    openWifi();
                                                }
                                            })
                                            .show();
                                }
                            });
                        }
                        openWifi();
                    }
                }
            });
        }
    }

    private void lunchTransportActivity() {
        if (mDirectFinishEnable) {
            finish();
        } else {
            Intent intent = new Intent(ConnectActivity.this, TransportActivity.class);
            startActivity(intent);
            finish();
        }
    }

    private void openAp() {
        utilsPermission.request_WRITE_SETTINGS(ConnectActivity.this,
                REQUEST_CODE_WRITE_SETTINGS, new UtilsPermission.PermissionCallBack() {
            @Override
            public void granted() {
//                if (apClient != null) {
//                    apClient.release();
//                }
                if (!mHasSaved) {
                    WifiStatus.getInstance().save(ConnectActivity.this);
                    mHasSaved = true;
                }

                apServer.CreateAP();
                progressBar_Connect.setVisibility(View.VISIBLE);
            }

            @Override
            public void denied() {
                Log.i(TAG, "WRITE_SETTINGS denied");
                radio_group.clearCheck();
            }
        });
    }

    private void openWifi() {
        utilsPermission.request_WRITE_SETTINGS(ConnectActivity.this,
                REQUEST_CODE_WRITE_SETTINGS, new UtilsPermission.PermissionCallBack() {
            @Override
            public void granted() {
//                if (apServer != null) {
//                    apServer.release();
//                }
                if (!mHasSaved) {
                    WifiStatus.getInstance().save(ConnectActivity.this);
                    mHasSaved = true;
                }

                utilsPermission.requestDangerousPermission(ConnectActivity.this,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        REQUEST_CODE_ACCESS_COARSE_LOCATION,
                        new UtilsPermission.PermissionCallBack() {
                            @Override
                            public void granted() {
                                progressBar_Connect.setVisibility(View.VISIBLE);
                                apClient.addNetwork();
                            }

                            @Override
                            public void denied() {
                                Log.i(TAG, "ACCESS_COARSE_LOCATION denied");
                                radio_group.clearCheck();
                            }
                        });
            }

            @Override
            public void denied() {
                Log.i(TAG, "WRITE_SETTINGS denied");
                radio_group.clearCheck();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (apServer != null) {
            Log.i(TAG, "release apServer");
            apServer.release();
        }
        if (apClient != null) {
            Log.i(TAG, "release apClient");
            apClient.release();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_OPEN_AP:
                if (apServer != null) {
                    if (apServer.getWifiManagerAdapter().isWifiApEnabled()) {
                        Toast.makeText(ConnectActivity.this, R.string.open_hotspot_successfully, Toast.LENGTH_SHORT).show();
                        lunchTransportActivity();
                    } else {
                        Toast.makeText(ConnectActivity.this, R.string.open_hotspot_failed, Toast.LENGTH_SHORT).show();
                        radio_group.clearCheck();
                    }
                }
                break;
            case REQUEST_CODE_CONNECT_WIFI:
                if (apClient != null) {
                    if (apClient.getWifiManagerAdapter().isWifiConnect()) {
                        Toast.makeText(ConnectActivity.this, R.string.connect_successfully, Toast.LENGTH_SHORT).show();
                        lunchTransportActivity();
                    } else {
                        Toast.makeText(ConnectActivity.this, R.string.connect_failed, Toast.LENGTH_SHORT).show();
                        radio_group.clearCheck();
                    }
                }
                break;
            default:
                utilsPermission.onActivityResult(requestCode, requestCode, data);
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        utilsPermission.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
