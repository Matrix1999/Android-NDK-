package com.a4455jkjh.ndk;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

import org.b.a.a.c;
import org.b.a.a.d;

public class InstallNdk extends AsyncTask<String, CharSequence, Boolean> {
    private final Activity activity;
    private ProgressDialog progressDialog;

    static {
        System.loadLibrary("link");
    }

    public InstallNdk(Activity activity) {
        this.activity = activity;
    }

    public static void showInstallDialog(final Activity activity) {
        LinearLayout layout = new LinearLayout(activity);
        final EditText editText = new EditText(activity);
        editText.setHint("Enter the path of the NDK compressed package");
        layout.addView(editText);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Install NDK");
        builder.setView(layout);
        builder.setCancelable(false);
        builder.setPositiveButton("Install", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					String inputPath = editText.getText().toString().trim();
					if (inputPath.length() > 0) {
						new InstallNdk(activity).execute(inputPath);
					}
				}
			});
        builder.setNegativeButton("Cancel", null);
        builder.show();

        ViewGroup.LayoutParams params = editText.getLayoutParams();
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        editText.setLayoutParams(params);
    }


    private void extractNdk(String path, File outputDir) throws IOException {
        FileInputStream fis = new FileInputStream(path);
        GZIPInputStream gzip = new GZIPInputStream(fis);
        d archive = new d(gzip);

        try {
            outputDir.mkdirs();
            byte[] buffer = new byte[4096];

            while (true) {
                c entry = archive.a();
                if (entry == null) {
                    new File(outputDir, ".installed").createNewFile();
                    break;
                }

                publishProgress(entry.a());
                File outFile = new File(outputDir, entry.a());

                if (entry.j()) {
                    outFile.mkdirs();
                } else if (entry.k()) {
                    symlink(entry.b(), outFile.getAbsolutePath());
                } else if (entry.l()) {
                    link(entry.b(), outFile.getAbsolutePath());
                } else {
                    FileOutputStream fos = new FileOutputStream(outFile);
                    int read;
                    while ((read = archive.read(buffer)) > 0) {
                        fos.write(buffer, 0, read);
                    }
                    fos.close();

                    if ((entry.c() & 292) > 0) {
                        outFile.setExecutable(true);
                    }
                }
            }
        } finally {
            archive.close();
            gzip.close();
            fis.close();
        }
    }


    public static void start(Activity activity, String path) {
        new InstallNdk(activity).execute(path);
    }

    private static native void link(String src, String dest);
    private static native void symlink(String src, String dest);

    @Override
    protected Boolean doInBackground(String... params) {
        try {
            // params[0] = path from execute()
            String inputPath = params[0];
            File targetDir = Q.getNdkInstallDir(activity);
            extractNdk(inputPath, targetDir);
            return Boolean.TRUE;
        } catch (Exception e) {
            e.printStackTrace();
            return Boolean.FALSE;
        }
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        progressDialog = new ProgressDialog(activity);
        progressDialog.setTitle("Installing...");
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    @Override
    protected void onProgressUpdate(CharSequence... values) {
        if (progressDialog != null && values != null && values.length > 0) {
            progressDialog.setMessage(values[0]);
        }
    }

    @Override
    protected void onPostExecute(Boolean success) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("End of installation");
        builder.setMessage(success.booleanValue()
						   ? "The installation is complete"
						   : "Installation failed");
        builder.setPositiveButton("OK", null);
        builder.show();
    }
}

