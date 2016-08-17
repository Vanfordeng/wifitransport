package com.mobiletek.wifitransport.transport;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.List;

/**
 * Created by linux-server-build8 on 6/22/16.
 */
public class SocketClient {
    private final static String TAG = "SocketClient";
    private final static int SERVER_PORT = 12345;
//    private final static String SERVER_IP = "192.168.43.1";

    public static final int RECEIVE_SUCCESSFUL = 1;
    public static final int RECEIVE_FAILED = 2;
    public static final int RECEIVE_PROGRESS = 3;

    private Socket socket = null;
    private OnSendListener onSendListener = null;
    private Handler mServerThreadHandler = null;

    private String mIPAddress = null;
    private List<String> mFiles = null;
    private int mPosition;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case RECEIVE_SUCCESSFUL:
                    if (onSendListener != null) {
                        if (msg.obj != null && msg.obj instanceof String) {
                            onSendListener.onSendSuccesful((String) msg.obj);
                        }
                    }
                    if (mFiles != null) {
                        continueSendFiles();
                    } else {
                        stopSend();
                    }
                    break;

                case RECEIVE_FAILED:
                    if (onSendListener != null) {
                        if (msg.obj != null && msg.obj instanceof String) {
                            onSendListener.onSendFailed((String) msg.obj);
                        }
                    }
                    if (mFiles != null) {
                        continueSendFiles();
                    } else {
                        stopSend();
                    }
                    break;

                case RECEIVE_PROGRESS:
                    if (onSendListener != null) {
                        if (msg.obj != null && msg.obj instanceof Integer) {
                            onSendListener.onSendProgress((Integer) msg.obj);
                        }
                    }
                    break;
            }
        }
    };

    public void setOnSendListener(OnSendListener onSendListener) {
        this.onSendListener = onSendListener;
    }

    public void startSendFiles(String ip, List<String> files) {
        mFiles = files;
        if (files != null && files.size() > 0) {
            mIPAddress = ip;
            mPosition = 0;

            HandlerThread serverThread = new HandlerThread("SocketClient");
            serverThread.start();
            mServerThreadHandler = new Handler(serverThread.getLooper());
            mServerThreadHandler.post(new SendFileRunable(ip, files.get(mPosition)));
        }
    }

    public void sendFile(String ip, String file_path) {
        if (file_path != null) {
            HandlerThread serverThread = new HandlerThread("SocketClient");
            serverThread.start();
            mServerThreadHandler = new Handler(serverThread.getLooper());
            mServerThreadHandler.post(new SendFileRunable(ip, file_path));
        }
    }

    public void stopSend() {
        if (mServerThreadHandler != null) {
            mServerThreadHandler.getLooper().quit();
        }
    }

    private void continueSendFiles() {
            mPosition++;
            if (mPosition < mFiles.size()) {
                mServerThreadHandler.post(new SendFileRunable(mIPAddress, mFiles.get(mPosition)));
            } else {
                stopSend();
            }
    }


    public interface OnSendListener {
        void onSendSuccesful(String file_path);

        void onSendFailed(String file_path);

        void onSendProgress(int progress);
    }

    private class SendFileRunable implements Runnable {
        private String ip;
        private String file_path;

        public SendFileRunable(String ip, String file_path) {
            this.ip = ip;
            this.file_path = file_path;
        }

        @Override
        public void run() {
            long write_size;
            byte[] buffer = new byte[1024];
            FileInputStream fileInputStream = null;
            DataOutputStream outputStream = null;

            try {
                Log.d(TAG, "Client：Connecting");
                socket = new Socket(ip, SERVER_PORT);
                socket.setReuseAddress(true);

                Log.d(TAG, "Client Sending: '" + file_path + "'");
                File file = new File(file_path);
                fileInputStream = new FileInputStream(file);
                outputStream = new DataOutputStream(socket.getOutputStream());

                long size = file.length();
                outputStream.writeLong(size);//file size
                outputStream.writeUTF(file.getName());//file name

                write_size = 0;
                while (write_size < size) {
                    int tmp_size = fileInputStream.read(buffer);
                    outputStream.write(buffer, 0, tmp_size);
                    write_size += tmp_size;
                    Integer progress = (int) (write_size * 100 / size);
//                    publishProgress(progress);
                    mHandler.obtainMessage(RECEIVE_PROGRESS, progress).sendToTarget();
                }

                outputStream.flush();

                Log.i(TAG, "send successful");
                mHandler.obtainMessage(RECEIVE_SUCCESSFUL, file_path).sendToTarget();
            } catch (Exception e) {
                Log.i(TAG, "send faild");
                e.printStackTrace();
                mHandler.obtainMessage(RECEIVE_FAILED, file_path).sendToTarget();
            } finally {
                try {
                    if (fileInputStream != null) {
                        fileInputStream.close();
                    }
                    if (outputStream != null) {
                        outputStream.close();
                    }
                    if (socket != null) {
                        socket.close();
                        Log.d(TAG, "Client:Socket closed");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
