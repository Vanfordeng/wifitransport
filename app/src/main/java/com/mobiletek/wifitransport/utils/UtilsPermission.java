package com.mobiletek.wifitransport.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by linux-server-build8 on 6/30/16.
 */
public class UtilsPermission {

    /**
     * request {@link Manifest.permission#WRITE_SETTINGS} permission.
     */
    public void request_WRITE_SETTINGS(final Activity activity, final int request_code, final PermissionCallBack callBack) {
        /**
         * An app can use this method to check if it is currently allowed to write or modify system
         * settings. In order to gain write access to the system settings, an app must declare the
         * {@link Manifest.permission#WRITE_SETTINGS} permission in its manifest. If it is
         * currently disallowed, it can prompt the user to grant it this capability through a
         * management UI by sending an Intent with action
         * {@link Settings#ACTION_MANAGE_WRITE_SETTINGS}.
         *
         * @param context A context
         * @return true if the calling app can write to system settings, false otherwise
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(activity)) {
                addOnActivityResultCallBack(request_code, new OnActivityResultCallBack() {
                    @Override
                    public void onActivityResult(int requestCode, int resultCode, Intent data) {
                        if (request_code == requestCode) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                if (Settings.System.canWrite(activity)) {
                                    callBack.granted();
                                } else {
                                    callBack.denied();
                                }
                            }
                        }
                    }
                });

                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(Uri.parse("package:" + activity.getPackageName()));
//                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                activity.startActivityForResult(intent, request_code);
                return;
            }
        }

        callBack.granted();
    }

    /**
     * request dangerous permission.
     */
    public void requestDangerousPermission(Activity activity, final String permission, final int request_code,
                                           final PermissionCallBack callBack) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (activity.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                addOnRequestPermissionsResultCallBack(request_code, new OnRequestPermissionsResultCallBack() {
                    @Override
                    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
                        if (request_code == requestCode) {
                            for (String s : permissions) {
                                if (s.equals(permission)) {
                                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                                        callBack.granted();
                                    } else {
                                        callBack.denied();
                                    }
                                    break;
                                }
                            }
                        }
                    }
                });

                activity.requestPermissions(new String[]{permission}, request_code);
                return;
            }
        }

        callBack.granted();
    }

    /**
     * request dangerous permission.
     */
    public void requestDangerousPermissions(Activity activity, final String[] permissions, final int request_code,
                                           final PermissionCallBack callBack) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (permissions != null && permissions.length > 0) {
                if (activity.checkSelfPermission(permissions[0]) != PackageManager.PERMISSION_GRANTED) {
                    addOnRequestPermissionsResultCallBack(request_code, new OnRequestPermissionsResultCallBack() {
                        @Override
                        public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
                            if (request_code == requestCode) {
                                boolean permissionGranted = true;
                                for (int i = 0; i < permissions.length; i++) {
                                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                                        permissionGranted = false;
                                        break;
                                    }
                                }
                                if (permissionGranted) {
                                    callBack.granted();
                                } else {
                                    callBack.denied();
                                }
                            }
                        }
                    });

                    activity.requestPermissions(permissions, request_code);
                    return;
                }
            }
        }

        callBack.granted();
    }

    /**
     * please call in the method which is derived from {@link Activity#onActivityResult}.
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        List<OnActivityResultCallBack> onActivityResultCallBacks = onActivityResultCallBackMap.get(requestCode);
        if (onActivityResultCallBacks != null) {
            for (OnActivityResultCallBack callBack : onActivityResultCallBacks) {
                callBack.onActivityResult(requestCode, resultCode, data);
            }
            onActivityResultCallBacks.clear();
        }
    }

    /**
     * please call in the method which is derived from {@link Activity#onRequestPermissionsResult}.
     */
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        List<OnRequestPermissionsResultCallBack> onRequestPermissionsResultCallBacks = onRequestPermissionsResultCallBackMap.get(requestCode);
        if (onRequestPermissionsResultCallBacks != null) {
            for (OnRequestPermissionsResultCallBack callBack : onRequestPermissionsResultCallBacks) {
                callBack.onRequestPermissionsResult(requestCode, permissions, grantResults);
            }
            onRequestPermissionsResultCallBacks.clear();
        }
    }

    public interface PermissionCallBack {
        void granted();
        void denied();
    }

    private interface OnActivityResultCallBack {
        void onActivityResult(int requestCode, int resultCode, Intent data);
    }

    private Map<Integer, List<OnActivityResultCallBack>> onActivityResultCallBackMap = new HashMap<>();

    private void addOnActivityResultCallBack(int requestCode, OnActivityResultCallBack callBack) {
        if (onActivityResultCallBackMap.containsKey(requestCode)) {
            List<OnActivityResultCallBack> onActivityResultCallBacks = onActivityResultCallBackMap.get(requestCode);
            if (onActivityResultCallBacks == null) {
                onActivityResultCallBacks = new ArrayList<>();
            }
            onActivityResultCallBacks.add(callBack);
        } else {
            List<OnActivityResultCallBack> onActivityResultCallBacks = new ArrayList<>();
            onActivityResultCallBacks.add(callBack);
            onActivityResultCallBackMap.put(requestCode, onActivityResultCallBacks);
        }
    }

    private void removeOnActivityResultCallBack(int requestCode, OnActivityResultCallBack callBack) {
        List<OnActivityResultCallBack> onActivityResultCallBacks = onActivityResultCallBackMap.get(requestCode);
        if (onActivityResultCallBacks != null) {
            if (onActivityResultCallBacks.contains(callBack)) {
                onActivityResultCallBacks.remove(callBack);
            }
        }
    }

    private interface OnRequestPermissionsResultCallBack {
        void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults);
    }

    private Map<Integer, List<OnRequestPermissionsResultCallBack>> onRequestPermissionsResultCallBackMap = new HashMap<>();

    private void addOnRequestPermissionsResultCallBack(int requestCode, OnRequestPermissionsResultCallBack callBack) {
        if (onRequestPermissionsResultCallBackMap.containsKey(requestCode)) {
            List<OnRequestPermissionsResultCallBack> onRequestPermissionsResultCallBacks = onRequestPermissionsResultCallBackMap.get(requestCode);
            if (onRequestPermissionsResultCallBacks == null) {
                onRequestPermissionsResultCallBacks = new ArrayList<>();
            }
            onRequestPermissionsResultCallBacks.add(callBack);
        } else {
            List<OnRequestPermissionsResultCallBack> onRequestPermissionsResultCallBacks = new ArrayList<>();
            onRequestPermissionsResultCallBacks.add(callBack);
            onRequestPermissionsResultCallBackMap.put(requestCode, onRequestPermissionsResultCallBacks);
        }
    }

    private void removeOnRequestPermissionsResultCallBack(int requestCode, OnRequestPermissionsResultCallBack callBack) {
        List<OnRequestPermissionsResultCallBack> onRequestPermissionsResultCallBacks = onRequestPermissionsResultCallBackMap.get(requestCode);
        if (onRequestPermissionsResultCallBacks != null) {
            if (onRequestPermissionsResultCallBacks.contains(callBack)) {
                onRequestPermissionsResultCallBacks.remove(callBack);
            }
        }
    }
}
