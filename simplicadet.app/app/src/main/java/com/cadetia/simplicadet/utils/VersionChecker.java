package com.cadetia.simplicadet.utils;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import com.cadetia.simplicadet.database.DbQuery;
import com.cadetia.simplicadet.entities.DialogConfirm;
import com.cadetia.simplicadet.utils.NetworkUtils;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.Map;

public class VersionChecker {
    private static final String TAG = "VersionChecker";

    public interface VersionCheckCallback {
        void onVersionSupported();
        void onVersionUnsupported();
        void onMaintenanceMode();
        void onOfflineMode();
    }

    public static void checkVersion(Context context, VersionCheckCallback callback) {
        if (!NetworkUtils.isNetworkAvailable(context)) {
            callback.onOfflineMode();
            return;
        }

        String currentVersion = getCurrentVersion(context);
        Log.d(TAG, "Current app version: " + currentVersion);

        DbQuery.g_firestore.collection("ADMIN").document("VERSION")
                .get()
                .addOnCompleteListener(task -> {
                    if (context instanceof android.app.Activity) {
                        android.app.Activity activity = (android.app.Activity) context;
                        if (activity.isFinishing() || activity.isDestroyed()) {
                            return;
                        }
                    }

                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            Log.d(TAG, "Document exists. All fields: " + document.getData());

                            Object maintenanceValue = document.get("maintenance");
                            boolean maintenanceMode = false;
                            if (maintenanceValue instanceof Boolean) {
                                maintenanceMode = (Boolean) maintenanceValue;
                            } else if (maintenanceValue instanceof String) {
                                maintenanceMode = "true".equalsIgnoreCase((String) maintenanceValue);
                            } else if (maintenanceValue instanceof Number) {
                                maintenanceMode = ((Number) maintenanceValue).intValue() == 1;
                            }

                            if (maintenanceMode) {
                                callback.onMaintenanceMode();
                                return;
                            }

                            Object rawValue = document.get(currentVersion);
                            Log.d(TAG, "Direct access for '" + currentVersion + "': " + rawValue);

                            boolean versionSupported = false;
                            Map<String, Object> data = document.getData();
                            if (data != null) {
                                for (Map.Entry<String, Object> entry : data.entrySet()) {
                                    String fieldName = entry.getKey();
                                    Object fieldValue = entry.getValue();

                                    Log.d(TAG, "Field: '" + fieldName + "' = " + fieldValue +
                                            " (length: " + fieldName.length() +
                                            ", bytes: " + java.util.Arrays.toString(fieldName.getBytes()) + ")");

                                    if (fieldName.trim().equals(currentVersion.trim())) {
                                        Log.d(TAG, "Found matching field!");
                                        if (fieldValue instanceof Boolean) {
                                            versionSupported = (Boolean) fieldValue;
                                        } else if (fieldValue instanceof String) {
                                            versionSupported = "true".equalsIgnoreCase((String) fieldValue);
                                        } else if (fieldValue instanceof Number) {
                                            versionSupported = ((Number) fieldValue).intValue() == 1;
                                        }
                                        break;
                                    }
                                }
                            }

                            Log.d(TAG, "Final decision - Version supported: " + versionSupported);

                            if (versionSupported) {
                                callback.onVersionSupported();
                            } else {
                                callback.onVersionUnsupported();
                            }
                        } else {
                            Log.e(TAG, "VERSION document does not exist");
                            callback.onVersionUnsupported();
                        }
                    } else {
                        Log.e(TAG, "Failed to get VERSION document", task.getException());
                        callback.onOfflineMode();
                    }
                });
    }

    public static void checkVersionOnMainActivity(Context context, VersionCheckCallback callback) {
        checkVersion(context, callback);
    }

    public static void checkVersionOnHomeActivity(Context context, VersionCheckCallback callback) {
        checkVersion(context, callback);
    }

    public static void showUnsupportedVersionDialogForMain(Context context, Runnable onDismiss) {
        if (context instanceof android.app.Activity) {
            android.app.Activity activity = (android.app.Activity) context;
            if (activity.isFinishing() || activity.isDestroyed()) {
                return;
            }
        }

        String currentVersion = getCurrentVersion(context);
        String message = "This version (" + currentVersion + ") is no longer supported. Please update to continue.";

        DialogConfirm.show(context, "Update Required", message, () -> {
            if (context instanceof android.app.Activity) {
                android.app.Activity activity = (android.app.Activity) context;
                activity.finishAffinity();
                System.exit(0);
            }
            if (onDismiss != null) onDismiss.run();
        }, false);
    }

    public static void showUnsupportedVersionDialogForHome(Context context, Runnable onDismiss) {
        if (context instanceof android.app.Activity) {
            android.app.Activity activity = (android.app.Activity) context;
            if (activity.isFinishing() || activity.isDestroyed()) {
                return;
            }
        }

        String currentVersion = getCurrentVersion(context);
        String message = "Version " + currentVersion + " is no longer supported. Please update to continue.";

        DialogConfirm.show(context, "Update Required", message, () -> {
            if (context instanceof android.app.Activity) {
                android.app.Activity activity = (android.app.Activity) context;
                activity.finishAffinity();
                System.exit(0);
            }
            if (onDismiss != null) onDismiss.run();
        }, false);
    }

    public static void showMaintenanceDialog(Context context, Runnable onDismiss) {
        if (context instanceof android.app.Activity) {
            android.app.Activity activity = (android.app.Activity) context;
            if (activity.isFinishing() || activity.isDestroyed()) {
                return;
            }
        }

        String message = "The app is currently under maintenance. Please try again later.";

        DialogConfirm.show(context, "Maintenance Mode", message, () -> {
            if (context instanceof android.app.Activity) {
                android.app.Activity activity = (android.app.Activity) context;
                activity.finishAffinity();
                System.exit(0);
            }
            if (onDismiss != null) onDismiss.run();
        }, false);
    }

    public static void showUnsupportedVersionDialog(Context context, Runnable onDismiss) {
        if (context instanceof android.app.Activity) {
            android.app.Activity activity = (android.app.Activity) context;
            if (activity.isFinishing() || activity.isDestroyed()) {
                return;
            }
        }

        String currentVersion = getCurrentVersion(context);
        String message = "This version (" + currentVersion + ") is no longer supported. Please update to the latest version.";

        DialogConfirm.show(context, "Version Not Supported", message, onDismiss, false);
    }

    private static String getCurrentVersion(Context context) {
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Error getting version", e);
            return "unknown";
        }
    }
}