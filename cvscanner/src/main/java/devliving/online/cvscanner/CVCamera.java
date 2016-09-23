package devliving.online.cvscanner;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;

import java.io.IOException;
import java.util.List;

/**
 * Created by user on 9/20/16.
 */
public class CVCamera {
    final static String TAG = "CV-CAMERA";

    private Camera mCamera; //TODO: remove deprecated code

    int previewHeight, cameraId;

    boolean isAutofocusAvailable, isFlashAvailable;
    boolean isFlashOn = false, isFocused = false;
    boolean isTakingPicture = true;

    private CVCamera(){}

    public static CVCamera open(Activity context){
        CVCamera camera = new CVCamera();

        camera.cameraId = findBestCamera();
        camera.mCamera = Camera.open(camera.cameraId);

        Camera.Parameters param;
        param = camera.mCamera.getParameters();

        Camera.Size pSize = camera.getMaxPreviewResolution();
        param.setPreviewSize(pSize.width, pSize.height);

        float previewRatio = (float) pSize.width / pSize.height;

        Display display = context.getWindowManager().getDefaultDisplay();
        android.graphics.Point size = new android.graphics.Point();
        display.getRealSize(size);

        int displayWidth = Math.min(size.y, size.x);
        int displayHeight = Math.max(size.y, size.x);

        float displayRatio =  (float) displayHeight / displayWidth;

        camera.previewHeight = displayHeight;

        if ( displayRatio > previewRatio ) {
            camera.previewHeight = (int) ( (float) size.y/displayRatio*previewRatio);
        }

        Camera.Size maxRes = camera.getMaxPictureResolution(previewRatio);

        if ( maxRes != null) {
            param.setPictureSize(maxRes.width, maxRes.height);
            Log.d(TAG,"max supported picture resolution: " + maxRes.width + "x" + maxRes.height);
        }

        PackageManager pm = context.getPackageManager();
        if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_AUTOFOCUS)) {
            param.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            camera.isAutofocusAvailable = true;
            Log.d(TAG, "enabling autofocus");
        } else {
            camera.isAutofocusAvailable = false;
            Log.d(TAG, "autofocus not available");
        }
        if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            camera.isFlashAvailable = true;
            param.setFlashMode(camera.isFlashOn ? Camera.Parameters.FLASH_MODE_TORCH : Camera.Parameters.FLASH_MODE_OFF);
        }
        else{
            camera.isFlashAvailable = false;
        }

        camera.mCamera.setParameters(param);

        camera.updateCameraDisplayOrientation(context);
        /*mBugRotate = mSharedPref.getBoolean("bug_rotate", false);

        if (mBugRotate) {
            mCamera.setDisplayOrientation(270);
        } else {
            mCamera.setDisplayOrientation(90);
        }

        if (mImageProcessor != null) {
            mImageProcessor.setBugRotate(mBugRotate);
        }*/

        camera.lookForFocusedState();
        camera.isTakingPicture = false;
        camera.isFocused = true;
        return camera;
    }

    public void updateCameraDisplayOrientation(Activity context){
        Camera.Parameters parameters = mCamera.getParameters();

        Camera.CameraInfo camInfo = new android.hardware.Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, camInfo);

        Display display = context.getWindowManager().getDefaultDisplay();
        int rotation = display.getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (camInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (camInfo.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (camInfo.orientation - degrees + 360) % 360;
        }
        mCamera.setDisplayOrientation(result);
    }

    public void stopPreview(){
        mCamera.stopPreview();
    }

    public void setPreviewDisplay(SurfaceHolder surface) throws IOException {
        mCamera.setPreviewDisplay(surface);
    }

    public void startPreview(){
        mCamera.startPreview();
    }

    public void setPreviewCallback(Camera.PreviewCallback callback){
        mCamera.setPreviewCallback(callback);
    }

    public void release(){
        mCamera.release();
        mCamera = null;
    }

    public boolean takePicture(Camera.ShutterCallback shutterCallback, Camera.PictureCallback rawCallback,
                               final Camera.PictureCallback jpegCallback){
        if(!isTakingPicture) {
            isTakingPicture = true;
            mCamera.takePicture(shutterCallback, rawCallback, new Camera.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] data, Camera camera) {
                    isTakingPicture = false;
                    jpegCallback.onPictureTaken(data, camera);
                }
            });
            return true;
        }

        return false;
    }

    public boolean toggleFlash(){
        if(isFlashAvailable){
            isFlashOn = !isFlashOn;
            mCamera.getParameters().setFlashMode(isFlashOn? Camera.Parameters.FLASH_MODE_TORCH:Camera.Parameters.FLASH_MODE_OFF);
            return true;
        }

        return false;
    }

    public Camera.Size getPreviewSize(){
        return mCamera.getParameters().getPreviewSize();
    }

    public Camera.Size getPictureSize(){
        return mCamera.getParameters().getPictureSize();
    }

    void lookForFocusedState(){
        mCamera.setAutoFocusMoveCallback(new Camera.AutoFocusMoveCallback() {
            @Override
            public void onAutoFocusMoving(boolean start, Camera camera) {
                isFocused = !start;
                Log.d("CV-CAMERA", "camera focus change, focused? - " + isFocused);
            }
        });
    }

    public List<Camera.Size> getResolutionList() {
        return mCamera.getParameters().getSupportedPreviewSizes();
    }

    public Camera.Size getMaxPreviewResolution() {
        int maxWidth=0;
        Camera.Size curRes=null;

        mCamera.lock();

        for ( Camera.Size r: getResolutionList() ) {
            if (r.width>maxWidth) {
                Log.d(TAG,"supported preview resolution: "+r.width+"x"+r.height);
                maxWidth=r.width;
                curRes=r;
            }
        }

        return curRes;
    }


    public List<Camera.Size> getPictureResolutionList() {
        return mCamera.getParameters().getSupportedPictureSizes();
    }

    public Camera.Size getMaxPictureResolution(float previewRatio) {
        int maxPixels=0;
        int ratioMaxPixels=0;
        Camera.Size currentMaxRes=null;
        Camera.Size ratioCurrentMaxRes=null;
        for ( Camera.Size r: getPictureResolutionList() ) {
            float pictureRatio = (float) r.width / r.height;
            Log.d(TAG,"supported picture resolution: "+r.width+"x"+r.height+" ratio: "+pictureRatio);
            int resolutionPixels = r.width * r.height;

            if (resolutionPixels>ratioMaxPixels && pictureRatio == previewRatio) {
                ratioMaxPixels=resolutionPixels;
                ratioCurrentMaxRes=r;
            }

            if (resolutionPixels>maxPixels) {
                maxPixels=resolutionPixels;
                currentMaxRes=r;
            }
        }

        if (ratioCurrentMaxRes!=null) {

            Log.d(TAG,"Max supported picture resolution with preview aspect ratio: "
                    + ratioCurrentMaxRes.width+"x"+ratioCurrentMaxRes.height);
            return ratioCurrentMaxRes;

        }

        return currentMaxRes;
    }


    private static int findBestCamera() {
        int cameraId = -1;
        //Search for the back facing camera
        //get the number of cameras
        int numberOfCameras = Camera.getNumberOfCameras();
        //for every camera check
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                cameraId = i;
                break;
            }
            cameraId = i;
        }
        return cameraId;
    }
}
