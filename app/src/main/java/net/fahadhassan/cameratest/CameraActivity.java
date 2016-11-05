package net.fahadhassan.cameratest;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;

public class CameraActivity extends AppCompatActivity {
    private CameraPreview mPreview;
    private static final String TAG = CameraActivity.class.getSimpleName();
    boolean previewShown;
    FrameLayout preview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_preview);

        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this);
        preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);
        previewShown = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG,"ON RESUME CALLED");
        mPreview.attachCamera();

        mPreview.startCameraPreview();

    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG,"ON PAUSE CALLED");
        previewShown = false;
        mPreview.releaseCameraAndPreview();
    }
}
