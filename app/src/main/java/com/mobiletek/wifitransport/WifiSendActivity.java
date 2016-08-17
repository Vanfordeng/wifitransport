package com.mobiletek.wifitransport;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.mobiletek.wifitransport.hotspot.ApServer;
import com.mobiletek.wifitransport.transport.SocketClient;
import com.mobiletek.wifitransport.utils.UtilsUri;
import com.mobiletek.wifitransport.utils.UtilsPermission;

import java.util.ArrayList;
import java.util.List;

public class WifiSendActivity extends Activity {
    private static final String TAG = "WifiSendActivity";
    private static final int REQUEST_CODE_READ_EXTERNAL_STORAGE = 1;
    private static final int REQUEST_CODE_CONNECT_RESULT = 2;

    private String mFilePath;
    private List<String> mFilePathList;
    private List<String> mConnectedIPList;
    private String mIpAddress;
    private int mCount;

    private ProgressBar mProgressContainer;
    private LinearLayout mListContainer;
    private ListView mListView;
    private LinearLayout mProgressWait;

    private UtilsPermission utilsPermission = new UtilsPermission();
    private ApServer apServer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_send);

        mProgressContainer = (ProgressBar) findViewById(R.id.progressContainer);
        mListContainer = (LinearLayout) findViewById(R.id.listContainer);
        mListView = (ListView) findViewById(R.id.list);
        mProgressWait = (LinearLayout) findViewById(R.id.waitContainer);

        if (apServer == null) {
            apServer = new ApServer(this);
        }

        mFilePath = null;
        mFilePathList = null;
        mCount = 0;
        Intent intent = getIntent();
        if (intent != null) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_SEND)) {
                final Uri streamUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
                Log.i(TAG, streamUri.toString());
                if (!checkUri(streamUri)) {
                    return;
                }

                if (!checkConnectedDevices()) {
                    return;
                }

                showSelectList();
            } else if (action.equals(Intent.ACTION_SEND_MULTIPLE)) {
                final List<Uri> uris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                if (!checkUri(uris)) {
                    return;
                }

                if (!checkConnectedDevices()) {
                    return;
                }

                showSelectList();
            }
        }
    }

    private boolean checkUri(Uri uri) {
        mFilePath = UtilsUri.getFilePathByUri(this, uri);
        if (mFilePath == null) {
            Toast.makeText(this, R.string.not_find_file, Toast.LENGTH_LONG).show();
            finish();
            return false;
        }
        return true;
    }

    private boolean checkUri(List<Uri> uris) {
        if (uris == null) {
            Toast.makeText(this, R.string.not_find_file, Toast.LENGTH_LONG).show();
            finish();
            return false;
        }
        mFilePathList = new ArrayList<String>();
        for (Uri uri : uris) {
            Log.i(TAG, uri.toString());
            String file_path = UtilsUri.getFilePathByUri(this, uri);
            if (file_path != null) {
                mFilePathList.add(file_path);
            }
        }
        if (mFilePathList.isEmpty()) {
            Toast.makeText(this, R.string.not_find_file, Toast.LENGTH_LONG).show();
            finish();
            return false;
        }
        return true;
    }

    private boolean checkConnectedDevices() {
        mConnectedIPList = apServer.getConnectedIPList();
        if (mConnectedIPList == null) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.not_find_device)
                    .setMessage(R.string.message_not_find_device)
                    .setNegativeButton(R.string.quit, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .setPositiveButton(R.string.open, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(WifiSendActivity.this, ConnectActivity.class);
                            startActivityForResult(intent, REQUEST_CODE_CONNECT_RESULT);
                        }
                    })
                    .show();
            return false;
        }
        return true;
    }

    private void prepareSendFile() {
        mProgressContainer.setVisibility(View.VISIBLE);
        mListContainer.setVisibility(View.GONE);
        mProgressWait.setVisibility(View.GONE);

        utilsPermission.requestDangerousPermission(WifiSendActivity.this,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                REQUEST_CODE_READ_EXTERNAL_STORAGE,
                new UtilsPermission.PermissionCallBack() {
                    @Override
                    public void granted() {
                        SocketClient socketClient = new SocketClient();
                        socketClient.setOnSendListener(mOnSendListener);
                        socketClient.sendFile(mIpAddress, mFilePath);
                    }

                    @Override
                    public void denied() {
                        Log.i(TAG, "READ_EXTERNAL_STORAGE denied");
                        finish();
                    }
                });
    }

    private void prepareSendFiles() {
        mProgressContainer.setVisibility(View.VISIBLE);
        mListContainer.setVisibility(View.GONE);
        mProgressWait.setVisibility(View.GONE);

        utilsPermission.requestDangerousPermission(WifiSendActivity.this,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                REQUEST_CODE_READ_EXTERNAL_STORAGE,
                new UtilsPermission.PermissionCallBack() {
                    @Override
                    public void granted() {
                        SocketClient socketClient = new SocketClient();
                        socketClient.setOnSendListener(mOnSendListener);
                        socketClient.startSendFiles(mIpAddress, mFilePathList);
                    }

                    @Override
                    public void denied() {
                        Log.i(TAG, "READ_EXTERNAL_STORAGE denied");
                        finish();
                    }
                });
    }

    private void showSelectList() {
        mProgressContainer.setVisibility(View.GONE);
        mListContainer.setVisibility(View.VISIBLE);
        mProgressWait.setVisibility(View.GONE);

        ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, mConnectedIPList);
        mListView.setAdapter(adapter);
        mListView.setOnItemClickListener(mOnClickListener);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        utilsPermission.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_CONNECT_RESULT:

                new AsyncTask<ApServer, Void, List<String>>() {
                    @Override
                    protected List<String> doInBackground(ApServer... params) {
                        long end_time = System.currentTimeMillis() + 30 * 1000;
                        long current_time;
                        ApServer apServer_tmp = params[0];
                        List<String> connectedIPList;
                        do {
                            connectedIPList = apServer_tmp.getConnectedIPList();
                            current_time = System.currentTimeMillis();
                            if (current_time >= end_time) {
                                return null;
                            }
                        } while (connectedIPList == null);
                        return connectedIPList;
                    }

                    @Override
                    protected void onPostExecute(List<String> connectedIPList) {
                        if (connectedIPList != null) {
                            mConnectedIPList = connectedIPList;
                            showSelectList();
                        } else {
                            Toast.makeText(WifiSendActivity.this, R.string.not_find_device,
                                    Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }
                }.execute(apServer);

                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    private AdapterView.OnItemClickListener mOnClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
            mIpAddress = mConnectedIPList.get(position);
            if (mFilePath != null && mIpAddress != null) {
                mListContainer.setVisibility(View.GONE);
                mProgressContainer.setVisibility(View.VISIBLE);
                prepareSendFile();
            } else if (mFilePathList != null && mIpAddress != null) {
                mListContainer.setVisibility(View.GONE);
                mProgressContainer.setVisibility(View.VISIBLE);
                prepareSendFiles();
            } else {
                finish();
            }
        }
    };

    private SocketClient.OnSendListener mOnSendListener = new SocketClient.OnSendListener() {
        @Override
        public void onSendSuccesful(String file_path) {
            Toast.makeText(WifiSendActivity.this, getString(R.string.success) + ":" + file_path,
                    Toast.LENGTH_SHORT).show();
            if (mFilePathList != null && mConnectedIPList != null && !mConnectedIPList.isEmpty()) {
                mCount++;
                if (mCount >= mFilePathList.size()) {
                    finish();
                } else {
                    mProgressContainer.setProgress(0);
                }
            } else {
                finish();
            }
        }

        @Override
        public void onSendFailed(String file_path) {
            Toast.makeText(WifiSendActivity.this, getString(R.string.failure) + ":" + file_path,
                    Toast.LENGTH_SHORT).show();
            if (mFilePathList != null && mConnectedIPList != null && !mConnectedIPList.isEmpty()) {
                mCount++;
                if (mCount >= mFilePathList.size()) {
                    finish();
                } else {
                    mProgressContainer.setProgress(0);
                }
            } else {
                finish();
            }
        }

        @Override
        public void onSendProgress(int progress) {
            int current_progress = mProgressContainer.getMax() / 100 * progress;
            mProgressContainer.setProgress(current_progress);
        }
    };
}
