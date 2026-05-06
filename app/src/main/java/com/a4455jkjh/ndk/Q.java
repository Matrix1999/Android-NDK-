package com.a4455jkjh.ndk;

import android.content.Context;
import android.os.Build;
import android.os.Environment;

import java.io.File;


public class Q {


    public static File getNdkInstallDir(Context context) {
        File baseDir = getBaseDir(context);
        return new File(baseDir, getNdkFolderName());
    }


    public static File getNdkInstallDir(Context context, String customSuffix) {
        File baseDir = getBaseDir(context);
        return new File(baseDir, getNdkFolderName() + "-" + sanitize(customSuffix));
    }

    private static File getBaseDir(Context context) {
        File base = context.getFilesDir();
        try {
            if (Build.VERSION.SDK_INT < 29 && Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                File ext = context.getExternalFilesDir(null);
                if (ext != null && ext.canWrite()) {
                    base = ext;
                }
            }
        } catch (Exception ignored) {
        }
        return new File(base, "ndk");
    }


    private static String getNdkFolderName() {
        String arch = getArchName();
        int code = getNdkVersionCode();
        return "ndksupport-" + arch + "-" + code;
    }

    private static int getNdkVersionCode() {
        int base = isX86() ? 1710240001 : 1710240000;
        if (Build.VERSION.SDK_INT >= 34) {
            base += 4; 
        } else if (Build.VERSION.SDK_INT >= 31) {
            base += 3; 
        } else if (Build.VERSION.SDK_INT >= 26) {
            base += 2;
        }
        return base;
    }

    private static String getArchName() {
        String arch = System.getProperty("os.arch", "");
        if (arch.contains("aarch64") || arch.contains("arm64")) {
            return "arm64-v8a";
        } else if (arch.contains("arm")) {
            return "armeabi-v7a";
        } else if (arch.contains("86_64")) {
            return "x86_64";
        } else if (arch.contains("86")) {
            return "x86";
        }
        return "unknown";
    }


    private static boolean isX86() {
        String arch = System.getProperty("os.arch", "");
        return arch.contains("86");
    }

    private static String sanitize(String name) {
        return name == null ? "" : name.replaceAll("[^a-zA-Z0-9-_]", "_");
    }
}

