package com.cadetia.simplicadet.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ImageStorageUtils {
    private static final String TAG = "ImageStorageUtils";
    private static final String PREFS_NAME = "ImagePrefs";
    private static final String IMAGE_DIR = "schedule_images";

    public static String saveImageToInternalStorage(Context context, Uri imageUri, String imageName) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();

            File directory = new File(context.getFilesDir(), IMAGE_DIR);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            File imageFile = new File(directory, imageName + ".jpg");
            FileOutputStream fos = new FileOutputStream(imageFile);

            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos);
            fos.close();

            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putString("schedule_image_path", imageFile.getAbsolutePath()).apply();

            Log.d(TAG, "Image saved successfully: " + imageFile.getAbsolutePath());
            return imageFile.getAbsolutePath();

        } catch (IOException e) {
            Log.e(TAG, "Error saving image", e);
            return null;
        }
    }

    public static Bitmap loadImageFromInternalStorage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String imagePath = prefs.getString("schedule_image_path", null);

        if (imagePath != null) {
            try {
                File imageFile = new File(imagePath);
                if (imageFile.exists()) {
                    FileInputStream fis = new FileInputStream(imageFile);
                    Bitmap bitmap = BitmapFactory.decodeStream(fis);
                    fis.close();
                    return bitmap;
                }
            } catch (IOException e) {
                Log.e(TAG, "Error loading image", e);
            }
        }
        return null;
    }

    public static String getStoredImagePath(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString("schedule_image_path", null);
    }

    public static boolean hasStoredImage(Context context) {
        String imagePath = getStoredImagePath(context);
        return imagePath != null && new File(imagePath).exists();
    }

    public static void deleteStoredImage(Context context) {
        String imagePath = getStoredImagePath(context);
        if (imagePath != null) {
            File imageFile = new File(imagePath);
            if (imageFile.exists()) {
                imageFile.delete();
            }
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().remove("schedule_image_path").apply();
        }
    }
}