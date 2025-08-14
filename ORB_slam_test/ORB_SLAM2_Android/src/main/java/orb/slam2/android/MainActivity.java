package orb.slam2.android;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View.OnTouchListener;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class MainActivity extends Activity implements View.OnClickListener {
    private String VOCPath = "/storage/emulated/0/SLAM/VOC/ORBvoc.txt";
    private String TUMPath = "/storage/emulated/0/SLAM/Calibration/List.yaml";
    Button datasetMode, testMode;
    Button ChooseCalibration, ChooseVOC;
    TextView CalibrationTxt, VOCPathText;
    private static final int REQUEST_CODE_2 = 2;   // TUM file request
    private static final int REQUEST_CODE_3 = 3;   // VOC file request
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private Intent fileChooserIntent;
    LinearLayout loading, origin;
    GestureDetector mGestureDetector;

    // On-screen log TextView
    private TextView logTextView;
    // Log file
    private File logFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        // Initialize log TextView
        logTextView = findViewById(R.id.logTextView);
        initLogFile();
        appendLog("MainActivity started");

        testMode = findViewById(R.id.test_mode);
        testMode.setOnClickListener(this);

        ChooseCalibration = findViewById(R.id.choose_calibration);
        ChooseVOC = findViewById(R.id.choose_voc);
        ChooseCalibration.setOnClickListener(this);
        ChooseVOC.setOnClickListener(this);
        CalibrationTxt = findViewById(R.id.cal_path_txt);
        VOCPathText = findViewById(R.id.voc_path_txt);

        CalibrationTxt.setText("calibration path is " + TUMPath);
        VOCPathText.setText("VOC path is " + VOCPath);

        fileChooserIntent = new Intent(this, FileChooserActivity.class);

        loading = findViewById(R.id.loading);
        origin = findViewById(R.id.origin);

        mGestureDetector = new GestureDetector(this, new MyGestureListener());
        OnTouchListener rootListener = (v, event) -> {
            mGestureDetector.onTouchEvent(event);
            return true;
        };
        View rootView = findViewById(R.id.FrameLayout1);
        rootView.setOnTouchListener(rootListener);
    }

    /** Initialize log file in internal storage */
    private void initLogFile() {
        try {
            logFile = new File(getFilesDir(), "orb_slam_log.txt");
            if (!logFile.exists()) logFile.createNewFile();
            appendLog("[LOG] Log file initialized: " + logFile.getAbsolutePath());
        } catch (IOException e) {
            appendLog("[LOG ERROR] Failed to create log file: " + e.getMessage());
        }
    }

    /** Append log to screen and file */
    public void appendLog(String message) {
        final String timestampedMessage = "[" + System.currentTimeMillis() + "] " + message;

        // On-screen
        runOnUiThread(() -> {
            logTextView.append(timestampedMessage + "\n");
            final int scrollAmount = logTextView.getLayout().getLineTop(logTextView.getLineCount()) - logTextView.getHeight();
            if (scrollAmount > 0) logTextView.scrollTo(0, scrollAmount);
            else logTextView.scrollTo(0, 0);
        });

        // File
        if (logFile != null) {
            new Thread(() -> {
                try (FileWriter fw = new FileWriter(logFile, true)) {
                    fw.write(timestampedMessage + "\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    // Camera permission check
    private boolean checkCameraPermission() {
        int result = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        boolean granted = result == PackageManager.PERMISSION_GRANTED;
        appendLog("[CAMERA] Permission granted? " + granted);
        return granted;
    }

    private void requestCameraPermission() {
        appendLog("[CAMERA] Requesting camera permission...");
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                appendLog("[CAMERA] Permission granted by user");
                Toast.makeText(this, "Camera permission granted", Toast.LENGTH_SHORT).show();
                startSLAMActivity();
            } else {
                appendLog("[CAMERA ERROR] Permission denied by user");
                Toast.makeText(this, "Camera permission is required for SLAM", Toast.LENGTH_LONG).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    /** Start ORB SLAM activity with logging */
    private void startSLAMActivity() {
        appendLog("===== SLAM Initialization Started =====");

        File calibFile = new File(TUMPath);
        if (calibFile.exists()) {
            appendLog("[OK] Calibration file found: " + TUMPath);
        } else {
            appendLog("[ERROR] Calibration file NOT found: " + TUMPath);
        }

        File vocFile = new File(VOCPath);
        if (vocFile.exists()) {
            appendLog("[OK] Vocabulary file found: " + VOCPath);
        } else {
            appendLog("[ERROR] Vocabulary file NOT found: " + VOCPath);
        }

        try {
            Bundle bundle = new Bundle();
            bundle.putString("voc", VOCPath);
            bundle.putString("calibration", TUMPath);
            Intent intent = new Intent(MainActivity.this, ORBSLAMForTestActivity.class);
            intent.putExtras(bundle);

            appendLog("Launching ORBSLAMForTestActivity...");
            startActivity(intent);
            appendLog("===== SLAM Initialization Triggered Successfully =====");
        } catch (Exception e) {
            appendLog("[SLAM ERROR] Failed to launch ORBSLAMForTestActivity: " + e.getMessage());
            Toast.makeText(this, "Error launching SLAM activity: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.dataset_mode:
                break;
            case R.id.test_mode:
                if (checkCameraPermission()) startSLAMActivity();
                else requestCameraPermission();
                break;
            case R.id.choose_calibration:
                if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
                    startActivityForResult(fileChooserIntent, REQUEST_CODE_2);
                else appendLog("[ERROR] SD card not available");
                break;
            case R.id.choose_voc:
                if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
                    startActivityForResult(fileChooserIntent, REQUEST_CODE_3);
                else appendLog("[ERROR] SD card not available");
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_CANCELED) {
            appendLog("[INFO] File chooser canceled");
            return;
        }
        if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_2) {
            TUMPath = data.getStringExtra("file_chooser");
            CalibrationTxt.setText("calibration path is " + TUMPath);
            appendLog("[UPDATE] Calibration path set to: " + TUMPath);
        }
        if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_3) {
            VOCPath = data.getStringExtra("file_chooser");
            VOCPathText.setText("VOC path is " + VOCPath);
            appendLog("[UPDATE] VOC path set to: " + VOCPath);
        }
    }

    private class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if ((e1.getX() - e2.getX() > 50) && Math.abs(velocityX) > 50) {
                loading.setVisibility(View.VISIBLE);
                origin.setVisibility(View.INVISIBLE);
            } else if ((e2.getX() - e1.getX() > 50) && Math.abs(velocityX) > 50) {
                origin.setVisibility(View.VISIBLE);
                loading.setVisibility(View.INVISIBLE);
            }
            return super.onFling(e1, e2, velocityX, velocityY);
        }
    }
			}
