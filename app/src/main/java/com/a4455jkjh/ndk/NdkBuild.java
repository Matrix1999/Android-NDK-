
package com.a4455jkjh.ndk;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.widget.ScrollView;
import android.widget.TextView;
import android.graphics.Typeface;
import android.text.method.ScrollingMovementMethod;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.IOException;

public class NdkBuild extends AsyncTask<Void, String, Boolean> {

    private Activity activity;
    private String jniPath;
    private String ndkPath;
    private ProgressDialog progressDialog;
    private StringBuilder logBuffer = new StringBuilder();
    private String errorMessage = null;

    public NdkBuild(Activity activity, String jniPath) {
        this.activity = activity;
        this.jniPath = jniPath;
        this.ndkPath = activity.getFilesDir().getAbsolutePath() +
			"/ndk/ndksupport-arm64-v8a-1710240004/android-ndk-aide/ndk-build";
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        progressDialog = new ProgressDialog(activity);
        progressDialog.setTitle("Installing...");
        progressDialog.setMessage("Preparing NDK build...");
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        try {
            File ndkFile = new File(ndkPath);
            if (!ndkFile.exists()) {
                errorMessage = "NDK not found please install it first";
                return Boolean.FALSE;
            }
            ndkFile.setExecutable(true);

            File jniDir = new File(jniPath);
            if (!jniDir.exists() || !jniDir.isDirectory()) {
                errorMessage = "JNI directory not found :)\n" + jniPath;
                return Boolean.FALSE;
            }

            publishProgress("Building native code...");

            ProcessBuilder pb = new ProcessBuilder(
				ndkPath,
				"NDK_PROJECT_PATH=.",
				"APP_BUILD_SCRIPT=Android.mk",
				"NDK_APPLICATION_MK=Application.mk",
				"-j4"
            );
            pb.directory(jniDir);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(
				new InputStreamReader(process.getInputStream())
            );

            String line;
            while ((line = reader.readLine()) != null) {
                if (isCancelled()) break;
                publishProgress(line);
                logBuffer.append(line).append("\n");
            }

            int exitCode = process.waitFor();
            reader.close();

            return exitCode == 0;

        } catch (IOException e) {
            errorMessage = e.getMessage();
            return Boolean.FALSE;
        } catch (InterruptedException e) {
            errorMessage = e.getMessage();
            return Boolean.FALSE;
        }
    }

    @Override
    protected void onProgressUpdate(String... values) {
        if (values != null && values.length > 0 && progressDialog != null) {
            progressDialog.setMessage(values[0]);
        }
    }

    @Override
    protected void onPostExecute(Boolean success) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }

        TextView logView = new TextView(activity);
        logView.setTypeface(Typeface.MONOSPACE);
        logView.setPadding(30, 30, 30, 30);
        logView.setTextSize(13f);
        logView.setText(logBuffer.toString());
        logView.setMovementMethod(new ScrollingMovementMethod());
        ScrollView scrollView = new ScrollView(activity);
        scrollView.addView(logView);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("NDK Build Result");

        if (success.booleanValue()) {
            builder.setMessage("Build Completed :)");
        } else {
            builder.setMessage("Build Failed :(\n\n" +
							   (errorMessage != null ? errorMessage : "Unknown error"));
        }

        builder.setView(scrollView);
        builder.setPositiveButton("OK", null);
        builder.show();
    }
}










/*package com.a4455jkjh.ndk;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.widget.Toast;

import java.io.*;

public class NdkBuild extends AsyncTask<Void, String, Boolean> {

    private final Activity activity;
    private final String jniPath;
    private final String ndkPath;
    private final StringBuilder outputLog = new StringBuilder();
    private ProgressDialog progressDialog;
    private String errorMessage = null;

    public NdkBuild(Activity activity, String jniPath) {
        this.activity = activity;
        this.jniPath = jniPath;
        this.ndkPath = activity.getFilesDir().getAbsolutePath() +
			"/ndk/ndksupport-arm64-v8a-1710240004/android-ndk-aide/ndk-build";
    }

    @Override
    protected void onPreExecute() {
        progressDialog = new ProgressDialog(activity);
        progressDialog.setTitle("NDK Build");
        progressDialog.setMessage("Checking NDK environment...");
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        File ndkFile = new File(ndkPath);
        if (!ndkFile.exists()) {
            errorMessage = "NDK not found!\nPlease install NDK first.";
            return false;
        }

        publishProgress("Building native code...\n");

        try {
            ProcessBuilder pb = new ProcessBuilder(
				ndkPath,
				"NDK_PROJECT_PATH=.",
				"APP_BUILD_SCRIPT=Android.mk",
				"NDK_APPLICATION_MK=Application.mk",
				"-j4"
            );
            pb.directory(new File(jniPath));
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(
				new InputStreamReader(process.getInputStream())
            );
            String line;
            while ((line = reader.readLine()) != null) {
                outputLog.append(line).append("\n");
                publishProgress(line);
            }
            reader.close();

            int exitCode = process.waitFor();
            return exitCode == 0;

        } catch (Exception e) {
            errorMessage = e.getMessage();
            return false;
        }
    }

    @Override
    protected void onProgressUpdate(String... values) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.setMessage(values[0]);
        }
    }

    @Override
    protected void onPostExecute(Boolean success) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("NDK Build Result");

        if (success) {
            builder.setMessage("null" +
							   outputLog.toString());
        } else {
            String message = (errorMessage != null ? errorMessage : "null!") +
				"\n\n" + highlightErrorLine(outputLog.toString());
            builder.setMessage("null" + message);
        }

        builder.setPositiveButton("OK", null);
        builder.show();
    }


    private String highlightErrorLine(String log) {
        String[] lines = log.split("\n");
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            if (line.toLowerCase().contains("error")) {
                sb.append("").append(line).append("\n");
            } else {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }
}
*/
