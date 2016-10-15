package devliving.online.cvscanner;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.shapes.PathShape;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.List;

/**
 * Created by Mehedi on 9/19/16.
 */
public class CVScannerActivity extends AppCompatActivity implements SurfaceHolder.Callback, Camera.PreviewCallback, Camera.PictureCallback {

    final int CAMERA_REQ_CODE = 1;
    final int STORAGE_REQ_CODE = 2;

    ImageButton flashButton;
    SurfaceView mSurfaceView;
    CVCanvas mShapeCanvas;

    SurfaceHolder mSurfaceHolder;
    CVCamera mCamera;

    boolean isProcessing = false;

    GestureDetector tapDetector;

    HandlerThread processorThread;
    ImageProcessor mProcessor;

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
        flashButton = (ImageButton) findViewById(R.id.flash_toggle);

        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);

        tapDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener(){
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if(mCamera != null && mCamera.isFocused){
                    takePicture();
                }

                return super.onDoubleTap(e);
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                Toast.makeText(CVScannerActivity.this, "Double tap to force detection", Toast.LENGTH_SHORT).show();
                return super.onSingleTapConfirmed(e);
            }
        });
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
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if(mCamera != null){
            mCamera.updateCameraDisplayOrientation(this);
        }
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

        mShapeCanvas.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return tapDetector.onTouchEvent(event);
            }
        });

        processorThread = new HandlerThread("Image Processor");
        processorThread.start();
        mProcessor = new ImageProcessor(processorThread.getLooper());
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

            processPreviewFrame(rgba);
            isProcessing = false;
        }
    }

    void processPreviewFrame(final Mat src){
        ImgMsg msg = new ImgMsg(MsgType.PROCESS_PREVIEW, src);
        Message message = new Message();
        message.obj = msg;
        mProcessor.handleMessage(message);
    }

    void onFlashClick(){
        if(mCamera.toggleFlash()){
            DrawableCompat.setTint(flashButton.getDrawable(),
                    getResources().getColor(mCamera.isFlashOn? R.color.torch_yellow:R.color.dark_gray));
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

    void drawFrame(Point[] points, Size size){
        Path path = new Path();

        // ATTENTION: axis are swapped

        float previewWidth = (float) size.height;
        float previewHeight = (float) size.width;

        path.moveTo( previewWidth - (float) points[0].y, (float) points[0].x );
        path.lineTo( previewWidth - (float) points[1].y, (float) points[1].x );
        path.lineTo( previewWidth - (float) points[2].y, (float) points[2].x );
        path.lineTo( previewWidth - (float) points[3].y, (float) points[3].x );
        path.close();

        PathShape newBox = new PathShape(path , previewWidth , previewHeight);

        Paint paint = new Paint();
        paint.setColor(Color.argb(64, 0, 255, 0));

        Paint border = new Paint();
        border.setColor(Color.rgb(0, 255, 0));
        border.setStrokeWidth(5);

        mShapeCanvas.clear();
        mShapeCanvas.addShape(newBox, paint, border);
        mShapeCanvas.invalidate();
    }

    void takePicture(){

    }

    enum MsgType{
        PROCESS_PREVIEW,
        PROCESS_PHOTO
    }

    class ImgMsg{
        MsgType type;
        Mat src;

        public ImgMsg(MsgType type, Mat src) {
            this.type = type;
            this.src = src;
        }
    }

    class ImageProcessor extends Handler{

        ImageProcessor(Looper looper){
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            if(msg.obj != null && msg.obj instanceof ImgMsg){
                ImgMsg message = (ImgMsg) msg.obj;
                switch (message.type){
                    case PROCESS_PREVIEW:
                        Size size = message.src.size();
                        List<MatOfPoint> contours = CVProcessor.findContours(message.src);
                        if(!contours.isEmpty()){
                            CVProcessor.Quadrilateral quad = CVProcessor.getQuadrilateral(contours, size);

                            if(quad != null){
                                Point[] rescaledPoints = new Point[4];

                                double ratio = CVProcessor.getScaleRatio(size);

                                for ( int i=0; i<4 ; i++ ) {
                                    int x = Double.valueOf(quad.points[i].x*ratio).intValue();
                                    int y = Double.valueOf(quad.points[i].y*ratio).intValue();
                                    rescaledPoints[i] = new Point(x, y);
                                }

                                drawFrame(rescaledPoints, size);
                                takePicture();
                            }
                        }
                        break;

                    case PROCESS_PHOTO:
                        break;
                }
            }
        }
    }
}
