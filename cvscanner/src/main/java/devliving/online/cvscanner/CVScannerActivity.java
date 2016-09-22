package devliving.online.cvscanner;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.shapes.PathShape;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Created by Mehedi on 9/19/16.
 */
public class CVScannerActivity extends AppCompatActivity implements SurfaceHolder.Callback, Camera.PreviewCallback, Camera.PictureCallback {

    final int CAMERA_REQ_CODE = 1;
    final int STORAGE_REQ_CODE = 2;

    ImageButton flashButton, shutterButton;
    SurfaceView mSurfaceView;
    CVCanvas mShapeCanvas;

    SurfaceHolder mSurfaceHolder;
    CVCamera mCamera;

    boolean isProcessing = false;

    BaseLoaderCallback mCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            if(status == SUCCESS){
                if(mSurfaceView != null){
                    mSurfaceView.setVisibility(View.VISIBLE);
                }
            }

            super.onManagerConnected(status);
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN |
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        if(getSupportActionBar() != null){
            getSupportActionBar().hide();
        }

        setContentView(R.layout.scanner_layout);
        mSurfaceView= (SurfaceView) findViewById(R.id.cam_surface_view);
        mShapeCanvas = (CVCanvas) findViewById(R.id.cv_canvas);
        shutterButton = (ImageButton) findViewById(R.id.shutter);
        flashButton = (ImageButton) findViewById(R.id.flash_toggle);

        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);
    }


    @Override
    protected void onResume() {
        super.onResume();

        initialize();
    }

    void initialize(){
        if(checkCameraPermission()){
            if(checkStoragePermission()){
                loadOpenCV();
            }
        }
    }

    boolean checkCameraPermission(){
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            Toast.makeText(this, R.string.camera_permission_msg, Toast.LENGTH_SHORT).show();
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_REQ_CODE);
            return false;
        }

        return true;
    }

    boolean checkStoragePermission(){
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            Toast.makeText(this, R.string.storage_permission_msg, Toast.LENGTH_SHORT).show();
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_REQ_CODE);
            return false;
        }

        return true;
    }

    void loadOpenCV(){
        if(!OpenCVLoader.initDebug()){
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, getApplicationContext(), mCallback);
        }
        else{
            mCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        //TODO disable preview
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //TODO disable preview
        disableCamera();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == CAMERA_REQ_CODE){
            initialize();
        }
        else if(requestCode == STORAGE_REQ_CODE){
            loadOpenCV();
        }
    }

    /**
     * This is called immediately after the surface is first created.
     * Implementations of this should start up whatever rendering code
     * they desire.  Note that only one thread can ever draw into
     * a {@link android.view.Surface}, so you should not draw into the Surface here
     * if your normal rendering will be in another thread.
     *
     * @param holder The SurfaceHolder whose surface is being created.
     */
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mCamera = CVCamera.open(this);

        mSurfaceView.getLayoutParams().height = mCamera.previewHeight;
    }

    /**
     * This is called immediately after any structural changes (format or
     * size) have been made to the surface.  You should at this point update
     * the imagery in the surface.  This method is always called at least
     * once, after {@link #surfaceCreated}.
     *
     * @param holder The SurfaceHolder whose surface has changed.
     * @param format The new PixelFormat of the surface.
     * @param width  The new width of the surface.
     * @param height The new height of the surface.
     */
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if(mCamera != null){
            mCamera.stopPreview();

            try {
                mCamera.setPreviewDisplay(mSurfaceHolder);
                mCamera.startPreview();
                mCamera.setPreviewCallback(this);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * This is called immediately before a surface is being destroyed. After
     * returning from this call, you should no longer try to access this
     * surface.  If you have a rendering thread that directly accesses
     * the surface, you must ensure that thread is no longer touching the
     * Surface before returning from this function.
     *
     * @param holder The SurfaceHolder whose surface is being destroyed.
     */
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        disableCamera();
    }

    void disableCamera(){
        if(mCamera != null){
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
    }

    /**
     * Called as preview frames are displayed.  This callback is invoked
     * on the event thread {@link Camera#open(int)} was called from.
     * <p/>
     * <p>If using the {@link ImageFormat#YV12} format,
     * refer to the equations in {@link Camera.Parameters#setPreviewFormat}
     * for the arrangement of the pixel data in the preview callback
     * buffers.
     *
     * @param data   the contents of the preview frame in the format defined
     *               by {@link ImageFormat}, which can be queried
     *               with {@link Camera.Parameters#getPreviewFormat()}.
     *               If {@link Camera.Parameters#setPreviewFormat(int)}
     *               is never called, the default will be the YCbCr_420_SP
     *               (NV21) format.
     * @param camera the Camera service object.
     */
    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        Log.d("CV-SCANNER", "got preview frame");
        if(mCamera.isFocused && !isProcessing){
            Log.d("CV-SCANNER", "start processing preview frame");
            isProcessing = true;
            final Camera.Size picSize = camera.getParameters().getPreviewSize();
            Mat yuv = new Mat(new Size(picSize.width, picSize.height * 1.5), CvType.CV_8UC1);
            yuv.put(0, 0, data);

            final Mat rgba = new Mat(new Size(picSize.width, picSize.height), CvType.CV_8UC4);
            Imgproc.cvtColor(yuv, rgba, Imgproc.COLOR_YUV2RGBA_NV21, 4);

            yuv.release();

            processPreviewFrame(rgba, picSize);
        }
    }

    void processPreviewFrame(final Mat src, final Camera.Size picSize){
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Rect rect = CVProcessor.detectBorder(src);

                Log.d("CV-SCANNER", "detected rect: " + rect.toString());
                Path path = new Path();
                path.moveTo(rect.x, rect.y);
                path.lineTo(rect.x + rect.width, rect.y);
                path.lineTo(rect.x + rect.width, rect.y + rect.height);
                path.lineTo(rect.x, rect.y + rect.height);
                path.close();

                final PathShape shape = new PathShape(path, picSize.width, picSize.height);
                final Paint body = new Paint();
                body.setColor(getResources().getColor(R.color.box_body));
                final Paint border = new Paint();
                border.setColor(getResources().getColor(R.color.box_border));
                border.setStrokeWidth(5);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mShapeCanvas.clear();
                        mShapeCanvas.addShape(shape, body, border);
                        mShapeCanvas.invalidate();
                        isProcessing = false;
                        Log.d("CV-SCANNER", "done processing preview frame");
                    }
                });
            }
        });

        thread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                Log.e("CV-SCANNER", "Thread interrupted: " + e.getLocalizedMessage());
                e.printStackTrace();
                if(t.getStackTrace() != null){
                    Log.e("CV-SCANNER", "thread stack trace:\n" + t.getStackTrace());
                }
            }
        });

        thread.setPriority(Thread.NORM_PRIORITY);
        thread.start();
    }

    void onFlashClick(){
        if(mCamera.toggleFlash()){
            DrawableCompat.setTint(flashButton.getDrawable(),
                    getResources().getColor(mCamera.isFlashOn? R.color.torch_yellow:R.color.dark_gray));
        }
    }

    void onShutterClick(){
        if(!mCamera.takePicture(null, null, this)){
            Toast.makeText(this, R.string.camera_busy, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Called when image data is available after a picture is taken.
     * The format of the data depends on the context of the callback
     * and {@link Camera.Parameters} settings.
     *
     * @param data   a byte array of the picture data
     * @param camera the Camera service object
     */
    @Override
    public void onPictureTaken(byte[] data, Camera camera) {

    }
}
