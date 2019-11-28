package com.sahni.rahul.facedetector.utils;

import android.app.Activity;
import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.sahni.rahul.facedetector.R;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class FileUtils {

    private Context context;

    public FileUtils(Context context) {
        this.context = context;
    }

    public File getOutputDirectory() {
        File galleryFolder = new File(
                context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                context.getResources().getString(R.string.app_name)
        );
        if (!galleryFolder.exists()) {
            boolean wasCreated = galleryFolder.mkdirs();
            if (!wasCreated) {
                Log.e("CapturedImages", "Failed to create directory");
            }
        }
        return galleryFolder;
    }

    public File createFile(String format, String extension) {

        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.US);

        return new File(getOutputDirectory(),
                sdf.format(System.currentTimeMillis()) + extension);

    }
}
