package com.mobiletek.wifitransport;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.mobiletek.wifitransport.hotspot.ApClient;
import com.mobiletek.wifitransport.transport.SocketServer;
import com.mobiletek.wifitransport.utils.UtilsPermission;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by linux-server-build8 on 6/29/16.
 */
public class ReceiveFragment extends Fragment {
    private static final String TAG = "ReceiveFragment";
    public static final String PROGRESS = "progress";
    public static final String LOG = "log";

    private TextView txt_local_ip;
    private ProgressBar progressbar_receive;
    private TextView receive_log;

    private Timer timer;
    private SocketServer socketServer;
    private UtilsPermission utilsPermission = new UtilsPermission();

    private SocketServer.OnAcceptFileListener onAcceptFileListener = new SocketServer.OnAcceptFileListener() {
        @Override
        public void onReceiveSuccesful(String ip, String file) {
            String log = receive_log.getText().toString();
            String new_message = getString(R.string.success) + ":" + file + "\n";
            receive_log.setText(log + new_message);
            Toast.makeText(getActivity(), new_message, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onReceiveFailed(String file) {
            String log = receive_log.getText().toString();
            String new_message = getString(R.string.failure) + ":" + file + "\n";
            receive_log.setText(log + new_message);
            Toast.makeText(getActivity(), new_message, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onReceiveProgress(String file, int progress) {
            int current_progress = progressbar_receive.getMax() / 100 * progress;
            progressbar_receive.setProgress(current_progress);
        }
    };

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root_view = inflater.inflate(R.layout.fragment_receive, container, false);

        txt_local_ip = (TextView) root_view.findViewById(R.id.txt_local_ip);
        progressbar_receive = (ProgressBar) root_view.findViewById(R.id.progressbar_receive);
        receive_log = (TextView) root_view.findViewById(R.id.receive_log);

        ApClient apClient = new ApClient(getActivity());
        String localIP = apClient.getLocalIP();
        if (localIP != null) {
            txt_local_ip.setText(getString(R.string.local_ip) + localIP);
        }

        return root_view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (socketServer == null) {
                socketServer = new SocketServer();
                socketServer.setOnAcceptFileListener(onAcceptFileListener);
            }

            if (timer != null) {
                timer.cancel();
            }
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    utilsPermission.requestDangerousPermission(getActivity(),
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            TransportActivity.REQUEST_CODE_WRITE_EXTERNAL_STORAGE,
                            new UtilsPermission.PermissionCallBack() {
                                @Override
                                public void granted() {
                                    socketServer.startAcceptFile();
                                    timer.cancel();
                                }

                                @Override
                                public void denied() {
                                    Log.i(TAG, "WRITE_EXTERNAL_STORAGE denied");
                                }
                            });
                }
            }, 0, 10 * 1000);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            if (socketServer == null) {
                socketServer = new SocketServer();
                socketServer.setOnAcceptFileListener(onAcceptFileListener);
            }
            socketServer.startAcceptFile();
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (timer != null) {
            timer.cancel();
        }
        socketServer.stopAcceptFile();
        socketServer.release();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (outState != null) {
            outState.putInt(PROGRESS, progressbar_receive.getProgress());
            outState.putString(LOG, receive_log.getText().toString());
        }
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState != null) {
            int progress = savedInstanceState.getInt(PROGRESS);
            String log = savedInstanceState.getString(LOG);
            progressbar_receive.setProgress(progress);
            receive_log.setText(log);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        utilsPermission.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
