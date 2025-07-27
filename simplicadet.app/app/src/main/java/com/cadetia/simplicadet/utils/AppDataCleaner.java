package com.cadetia.simplicadet.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.bumptech.glide.Glide;
import com.cadetia.simplicadet.database.DbQuery;
import java.io.File;

public class AppDataCleaner {

    private static final String TAG = "AppDataCleaner";

    public static void clearInstitutionPreference(Context context, Runnable onComplete) {
        try {
            SharedPreferences sharedPreferences = context.getSharedPreferences("UserData", Context.MODE_PRIVATE);
            String email = sharedPreferences.getString("userEmail", "");

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove("userInstitution");
            editor.apply();

            if (!email.isEmpty()) {
                DbQuery.g_firestore.collection("USERS").document(email).get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                String newInstitution = documentSnapshot.getString("INSTITUTION");
                                if (newInstitution != null) {
                                    SharedPreferences.Editor newEditor = context.getSharedPreferences("UserData", Context.MODE_PRIVATE).edit();
                                    newEditor.putString("userInstitution", newInstitution);
                                    newEditor.apply();
                                }
                            }
                            if (onComplete != null) onComplete.run();
                        })
                        .addOnFailureListener(e -> {
                            if (onComplete != null) onComplete.run();
                        });
            } else {
                if (onComplete != null) onComplete.run();
            }

            Log.d(TAG, "Institution preference cleared successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error clearing institution preference", e);
            if (onComplete != null) onComplete.run();
        }
    }

    public static void clearAllAppData(Context context, Runnable onComplete) {
        Log.d(TAG, "Clearing all app data");
        clearInstitutionPreference(context, onComplete);
        clearAllCaches(context);
        clearDatabaseCache();
        clearTemporaryFiles(context);
        clearImageCaches(context);
    }

    private static void clearAllCaches(Context context) {
        try {
            deleteDir(context.getCacheDir());
            if (context.getExternalCacheDir() != null) {
                deleteDir(context.getExternalCacheDir());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error clearing caches", e);
        }
    }

    private static void clearDatabaseCache() {
        try {
            DbQuery.clearAllCache();
        } catch (Exception e) {
            Log.e(TAG, "Error clearing database cache", e);
        }
    }

    private static void clearTemporaryFiles(Context context) {
        try {
            File tempDir = new File(context.getFilesDir(), "temp");
            if (tempDir.exists()) {
                deleteDir(tempDir);
            }
            File internalDir = context.getFilesDir();
            if (internalDir.exists()) {
                File[] files = internalDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.getName().contains("temp") || file.getName().contains("cache")) {
                            deleteDir(file);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error clearing temporary files", e);
        }
    }

    private static void clearImageCaches(Context context) {
        try {
            Glide.get(context).clearMemory();
            new Thread(() -> {
                try {
                    Glide.get(context).clearDiskCache();
                } catch (Exception e) {
                    Log.e(TAG, "Error clearing Glide disk cache", e);
                }
            }).start();
        } catch (Exception e) {
            Log.e(TAG, "Error clearing image caches", e);
        }
    }

    private static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            if (children != null) {
                for (String child : children) {
                    boolean success = deleteDir(new File(dir, child));
                    if (!success) return false;
                }
            }
            return dir.delete();
        } else if (dir != null && dir.isFile()) {
            return dir.delete();
        }
        return false;
    }
}