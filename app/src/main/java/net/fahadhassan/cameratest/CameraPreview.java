package net.fahadhassan.cameratest;

import android.app.Activity;
import android.hardware.Camera;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/** A basic Camera preview class */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private static final int MEDIA_TYPE_IMAGE = 1;
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private Button captureButton;

    //In charge of opening the camera in a separate thread
    private CameraHandlerThread mThread = null;

    CameraActivity activity;
    int cameraID;
    private static final String TAG = CameraPreview.class.getSimpleName();

    /**
     * Initializing Camera and SurfaceView for preview
     * @param activity
     */
    public CameraPreview(final CameraActivity activity) {
        super(activity);
        this.activity = activity;
        openCamera();
        setUpHolder();

        captureButton = (Button) activity.findViewById(R.id.button_capture);

        captureButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                //STEP #1: Get rotation degrees
                if(mCamera != null){
                    Camera.CameraInfo info = new Camera.CameraInfo();
                    Camera.getCameraInfo(Camera.CameraInfo.CAMERA_FACING_BACK, info);
                    int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
                    int degrees = 0;
                    switch (rotation) {
                        case Surface.ROTATION_0: degrees = 0; break; //Natural orientation
                        case Surface.ROTATION_90: degrees = 90; break; //Landscape left
                        case Surface.ROTATION_180: degrees = 180; break;//Upside down
                        case Surface.ROTATION_270: degrees = 270; break;//Landscape right
                    }
                    int rotate = (info.orientation - degrees + 360) % 360;

                    //STEP #2: Set the 'rotation' parameter
                    Camera.Parameters params = mCamera.getParameters();
                    params.setRotation(rotate);
                    mCamera.setParameters(params);
                    

                    mCamera.takePicture(null,null,mPicture);
                }
            }
        });
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
        openCamera();
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

    /**
     * Part of surface view lifecycle. Gets called when phone changes views such as portrait to landscape.
     * Figures out what position phone is in and positions the camera accordingly.
     * @param holder
     * @param format
     * @param w
     * @param h
     */
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        Log.d(TAG,"SURFACE CHANGED CALLED");
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        if (mHolder.getSurface() == null){
            // preview surface does not exist
            return;
        }

        // Stopping preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e){
            // ignore: tried to stop a non-existent preview
        }

        //Responsible for repositioning camera and activating preview
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
     * Opens camera in a separate worker thread
     */
    public void openCamera() {
        if (mThread == null) {
            mThread = new CameraHandlerThread();
        }

        synchronized (mThread) {
            mThread.openCamera();
        }
    }

    /** A safe way to get an instance of the Camera object. */
    public  Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }



    /**
     * Starts the camera preview. Sets up camera and surface-holder for the preview if they are null.
     */
    public void startCameraPreview(){
        if(mCamera == null) return;
        if(mHolder == null || mHolder.getSurface() == null) setUpHolder();

        //Repositioning camera
        setCameraDisplayOrientation(activity,0,mCamera);

        //Setting preview
        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();
        }
        catch (Exception e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }
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


    /***************** Opening camera in separate thread *****************/
    private class CameraHandlerThread extends HandlerThread {
        Handler mHandler = null;

        CameraHandlerThread() {
            super("CameraHandlerThread");
            start();
            mHandler = new Handler(getLooper());
        }

        synchronized void notifyCameraOpened() {
            notify();
        }

        void openCamera() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if(mCamera == null) mCamera = getCameraInstance();
                    notifyCameraOpened();
                }
            });
            try {
                wait();
            }
            catch (InterruptedException e) {
                Log.w(TAG, "wait was interrupted");
            }
        }
    }


    /****************** Saving Images ********************/

    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
            if (pictureFile == null){
                Log.d(TAG, "Error creating media file, check storage permissions");

                return;
            }

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
            } catch (FileNotFoundException e) {
                Log.d(TAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(TAG, "Error accessing file: " + e.getMessage());
            }

            startCameraPreview();
        }
    };

    /** Create a File for saving an image **/
    private static File getOutputMediaFile(int type){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "MyCameraApp");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_"+ timeStamp + ".jpg");
        } else {
            return null;
        }

        return mediaFile;
    }

}
