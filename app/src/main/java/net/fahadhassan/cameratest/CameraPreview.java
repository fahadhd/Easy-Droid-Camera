package net.fahadhassan.cameratest;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

/** A basic Camera preview class */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private SurfaceHolder mHolder;
    private Camera mCamera;
    CameraActivity activity;
    int cameraID;
    private static final String TAG = CameraPreview.class.getSimpleName();

    /**
     * Initializing Camera and SurfaceView for preview
     * @param activity
     */
    public CameraPreview(CameraActivity activity) {
        super(activity);
        this.activity = activity;
        attachCamera();
        setUpHolder();
    }

    public void setUpHolder(){
        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);

        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    /**
     * Surface is now created so start camera preview.
     * @param holder
     */
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG,"SURFACE CREATED CALLED");
        attachCamera();
    }

    /**
     * Called when switching between apps or if onDestroy is called.
     * Releases camera and its preview if it hasn't been done already.
     * Destroys the area the preview was display in.
     * @param holder
     */

    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG,"SURFACE DESTROYED CALLED");
        releaseCameraAndPreview();
        destroyDrawingCache();
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        Log.d(TAG,"SURFACE CHANGED CALLED");
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        if (mHolder.getSurface() == null){
            // preview surface does not exist
            return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e){
            // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here
        setCameraDisplayOrientation(activity,0,mCamera);

        // start preview with new settings
        startCameraPreview();
    }

    /**
     * Releases camera and sets it to null
     */
    public void releaseCameraAndPreview() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    /**
     * Gets the current instance of the camera.
     */
    public void attachCamera(){
        if(mCamera == null){
            mCamera = getCameraInstance();
        }
    }

    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    /************* Setting the camera orientation based on display ***********/
    public void setCameraDisplayOrientation(Activity activity,
                                                   int cameraId, android.hardware.Camera camera) {

        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();

        android.hardware.Camera.getCameraInfo(cameraId, info);

        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();

        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        int result;

        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    /**
     * Starts the camera preview. Sets up camera and surface-holder for the preview if they are null.
     */
    public void startCameraPreview(){
        if(mCamera == null) attachCamera();
        if(mHolder == null || mHolder.getSurface() == null) setUpHolder();
        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();
        }
        catch (Exception e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }
    }



}
