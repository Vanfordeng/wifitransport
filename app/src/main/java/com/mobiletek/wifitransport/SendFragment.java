package com.mobiletek.wifitransport;

import android.Manifest;
import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.mobiletek.wifitransport.utils.UtilsUri;
import com.mobiletek.wifitransport.utils.UtilsPermission;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by linux-server-build8 on 6/29/16.
 */
public class SendFragment extends Fragment {
    private static final String TAG = "SendFragment";
    public static final String SELECTED_FILES = "selected_files";

    private Button button_add;
    private Button button_clear;
    private Button button_send;
    private TextView selected_log;

    private List<Uri> mUriList = null;
    private UtilsPermission utilsPermission = new UtilsPermission();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root_view = inflater.inflate(R.layout.fragment_send, container, false);

        button_add = (Button) root_view.findViewById(R.id.button_add);
        button_clear = (Button) root_view.findViewById(R.id.button_clear);
        button_send = (Button) root_view.findViewById(R.id.button_send);
        selected_log = (TextView) root_view.findViewById(R.id.selected_log);

        updateSelectFilesUI();
        button_add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                utilsPermission.requestDangerousPermission(getActivity(),
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        TransportActivity.REQUEST_CODE_READ_EXTERNAL_STORAGE,
                        new UtilsPermission.PermissionCallBack() {
                            @Override
                            public void granted() {
                                Intent intent = new Intent();
                                intent.setAction(Intent.ACTION_GET_CONTENT);
                                intent.addCategory(Intent.CATEGORY_OPENABLE);
                                intent.setType("*/*");
                                startActivityForResult(intent, TransportActivity.REQUEST_CODE_GET_CONTENT);
                            }

                            @Override
                            public void denied() {
                                Log.i(TAG, "READ_EXTERNAL_STORAGE denied");
                            }
                        });
            }
        });
        button_clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mUriList != null) {
                    mUriList.clear();
                }
                updateSelectFilesUI();
            }
        });
        button_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), WifiSendActivity.class);
                intent.setAction(Intent.ACTION_SEND_MULTIPLE);
                intent.setType("*/*");
                intent.putExtra(Intent.EXTRA_STREAM, (ArrayList<Uri>) mUriList);
                startActivity(intent);
            }
        });
        return root_view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (outState != null) {
            outState.putParcelableArrayList(SELECTED_FILES, (ArrayList) mUriList);
        }
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState != null) {
            mUriList = savedInstanceState.getParcelableArrayList(SELECTED_FILES);
            updateSelectFilesUI();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        utilsPermission.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case TransportActivity.REQUEST_CODE_GET_CONTENT:
                if (data != null) {
                    if (mUriList == null) {
                        mUriList = new ArrayList<Uri>();
                    }
                    Uri uri = data.getData();
                    if (uri == null) {
                        Log.e(TAG, "uri = null, can not find the file");
                        return;
                    }
                    mUriList.add(uri);
                    updateSelectFilesUI();
                }
                break;

            default:
                utilsPermission.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    private void updateSelectFilesUI() {
        String log = getString(R.string.selected_files_log);
        if (mUriList != null && !mUriList.isEmpty()) {
            for (Uri uri : mUriList) {
                String file = UtilsUri.getFilePathByUri(getActivity(), uri);
                if (file != null) {
                    log += file + "\n";
                }
            }
            button_send.setEnabled(true);
        } else {
            button_send.setEnabled(false);
        }
        selected_log.setText(log);
    }
}
