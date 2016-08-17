package com.mobiletek.wifitransport.transport;

import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * Created by linux-server-build8 on 6/16/16.
 */
public class SocketServer {
    private final static String TAG = "SocketServer";
    private static final int SERVER_PORT = 12345;
    private static final int TIMEOUT = 10 * 1000;
    private final Handler mServerThreadHandler;
    private HandlerThread mServerThread;
    private ServerSocket serverSocket = null;
    private boolean mAcceptEnable = false;

    //UI Thread handler flag
    private static final int RECEIVE_FILE_COMPLETED = 3;
    private static final int RECEIVE_FILE_FAILED = 4;
    private static final int RECEIVE_FILE_PROGRESS = 5;
    private static final int RECEIVE_FILE_TIMEOUT = 6;

    private OnAcceptFileListener mOnAcceptFileListener;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "handleMessage() called with: " + "msg = [" + msg + "]");
            switch (msg.what) {
                case RECEIVE_FILE_COMPLETED:
                    if (mAcceptEnable) {
                        if (mOnAcceptFileListener != null && msg.obj != null) {
                            String[] message = (String[]) msg.obj;
                            mOnAcceptFileListener.onReceiveSuccesful(message[0], message[1]);
                        }
                        mServerThreadHandler.post(mReceiveFileRunnable);
                    }
                    break;

                case RECEIVE_FILE_FAILED:
                    if (mAcceptEnable) {
                        if (mOnAcceptFileListener != null) {
                            if (msg.obj != null && msg.obj instanceof String) {
                                mOnAcceptFileListener.onReceiveFailed((String) msg.obj);
                            }
                        }
                        mServerThreadHandler.post(mReceiveFileRunnable);
                    }
                    break;

                case RECEIVE_FILE_PROGRESS:
                    if (mAcceptEnable) {
                        if (mOnAcceptFileListener != null && msg.obj != null) {
                            if (msg.obj instanceof String[]) {
                                String[] message = (String[]) msg.obj;
                                mOnAcceptFileListener.onReceiveProgress(message[0], Integer.parseInt(message[1]));
                            }
                        }
                    }
                    break;

                case RECEIVE_FILE_TIMEOUT:
                    if (mAcceptEnable) {
                        mServerThreadHandler.post(mReceiveFileRunnable);
                    }
                    break;

                default:
                    break;
            }
        }
    };

    private Runnable mReceiveFileRunnable = new Runnable() {
        @Override
        public void run() {
            Socket clientSocket = null;
            DataInputStream inputStream = null;
            FileOutputStream fileOutputStream = null;
            byte[] buffer = new byte[1024];
            long read_size;
            String name = null;
            try {
                Log.d(TAG, "Server: Connecting...");
                clientSocket = serverSocket.accept();

                Log.d(TAG, "Server: Receiving...");
                inputStream = new DataInputStream(clientSocket.getInputStream());
                long size = inputStream.readLong();
                Log.i(TAG, "file size=" + size);
                name = inputStream.readUTF();
                Log.i(TAG, "file name=" + name);

                File file = createFile(name);
                if (file == null) {
                    throw new IOException("create " + name + "failed");
                }
                fileOutputStream = new FileOutputStream(file);

                read_size = 0;
                while (read_size < size) {
                    int tmp_size = inputStream.read(buffer);
                    fileOutputStream.write(buffer, 0, tmp_size);
                    read_size += tmp_size;
                    Integer progress = (int) (read_size * 100 / size);
                    mHandler.obtainMessage(RECEIVE_FILE_PROGRESS, new String[]{name, String.valueOf(progress)})
                            .sendToTarget();
                }
                fileOutputStream.flush();

                String client_ip = clientSocket.getInetAddress().getHostAddress();
                Log.d(TAG, "Server: Received " + client_ip + " " + file);

                String[] msg = new String[]{client_ip, file.getPath()};
                mHandler.obtainMessage(RECEIVE_FILE_COMPLETED, msg).sendToTarget();
            } catch (SocketTimeoutException e) {
                e.printStackTrace();
                mHandler.obtainMessage(RECEIVE_FILE_TIMEOUT).sendToTarget();
            } catch (Exception e) {
                e.printStackTrace();
                mHandler.obtainMessage(RECEIVE_FILE_FAILED, name).sendToTarget();
            } finally {
                try {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                    if (fileOutputStream != null) {
                        fileOutputStream.close();
                    }
                    if (clientSocket != null) {
                        clientSocket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    };

    public SocketServer() {
        mServerThread = new HandlerThread("SocketServer");
        mServerThread.start();
        mServerThreadHandler = new Handler(mServerThread.getLooper());
    }

    public void setOnAcceptFileListener(OnAcceptFileListener onAcceptFileListener) {
        this.mOnAcceptFileListener = onAcceptFileListener;
    }

    private boolean openSocket() {
        try {
            if (serverSocket == null) {
                serverSocket = new ServerSocket();
                serverSocket.setReuseAddress(true);
                serverSocket.bind(new InetSocketAddress(SERVER_PORT));
                serverSocket.setSoTimeout(TIMEOUT);
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            serverSocket = null;
        }
        return false;
    }

    private boolean closeSocket() {
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
            serverSocket = null;
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public synchronized void startAcceptFile() {
        if (!mAcceptEnable) {
            mAcceptEnable = true;
            if (openSocket()) {
                mServerThreadHandler.post(mReceiveFileRunnable);
            }
        }
    }

    public synchronized void stopAcceptFile() {
        if (mAcceptEnable) {
            mAcceptEnable = false;
            mServerThreadHandler.removeCallbacks(mReceiveFileRunnable);
            closeSocket();
        }
    }

    public void release() {
        mServerThreadHandler.getLooper().quit();
    }

    public interface OnAcceptFileListener {
        void onReceiveSuccesful(String ip, String file);
        void onReceiveFailed(String file);
        void onReceiveProgress(String file, int progress);
    }

    private File createFile(String name) {
        if (Environment.isExternalStorageEmulated()) {
            File root = Environment.getExternalStorageDirectory();
            File directory = new File(root, "WifiTransport");
            if (!directory.exists()) {
                if (!directory.mkdirs()) {
                    return null;
                }
            }

            if (directory.isDirectory()) {
                File file = new File(directory, name);

                try {
                    if (!file.createNewFile()) {
                        String extensionName = getExtensionName(name);
                        String fileNameNoEx = getFileNameNoEx(name);

                        for (int i = 1; i < Integer.MAX_VALUE; i++) {
                            file = new File(directory, fileNameNoEx + "(" + i + ")." + extensionName);
                            if (!file.exists()) {
                                break;
                            }
                        }

                        if (file.createNewFile()) {
                            return file;
                        }
                    } else {
                        return file;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return null;
    }

    private String getExtensionName(String filename) {
        if ((filename != null) && (filename.length() > 0)) {
            int dot = filename.lastIndexOf('.');
            if ((dot > -1) && (dot < (filename.length() - 1))) {
                return filename.substring(dot + 1);
            }
        }
        return filename;
    }

    private String getFileNameNoEx(String filename) {
        if ((filename != null) && (filename.length() > 0)) {
            int dot = filename.lastIndexOf('.');
            if ((dot > -1) && (dot < (filename.length()))) {
                return filename.substring(0, dot);
            }
        }
        return filename;
    }
}
