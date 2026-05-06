package com.a4455jkjh.ndk;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

public class MainActivity extends Activity {

    private static final int REQUEST_STORAGE = 100;
    private EditText jniPathInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 100, 50, 100);

        jniPathInput = new EditText(this);
        jniPathInput.setHint("example :) /sdcard/app/jni) Fuck You Aide 😂✋");
        layout.addView(jniPathInput);

      /*  Button installBtn = new Button(this);
        installBtn.setText("Install NDK");
        layout.addView(installBtn);*/

  
        Button buildBtn = new Button(this);
        buildBtn.setText("Build JNI");
        layout.addView(buildBtn);

        setContentView(layout);

     /*   installBtn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					checkStoragePermission(false);
				}
			});
*/
        buildBtn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					checkStoragePermission(true);
				}
			});
    }

    private void checkStoragePermission(final boolean isBuild) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
				!= PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
				!= PackageManager.PERMISSION_GRANTED) {

                requestPermissions(new String[]{
									   Manifest.permission.READ_EXTERNAL_STORAGE,
									   Manifest.permission.WRITE_EXTERNAL_STORAGE
								   }, REQUEST_STORAGE);
                return;
            }
        }

        if (isBuild) {
            String path = jniPathInput.getText().toString().trim();
            if (path.isEmpty()) {
                Toast.makeText(this, "Enter JNI path first [<_>]", Toast.LENGTH_SHORT).show();
            } else {
                buildJniLibrary(path);
            }
        } else {
            startNdkInstall();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_STORAGE) {
            if (grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startNdkInstall() {
        InstallNdk.showInstallDialog(MainActivity.this);
    }

    private void buildJniLibrary(String jniPath) {
		new NdkBuild(MainActivity.this, jniPath).execute();
	}
	
}
