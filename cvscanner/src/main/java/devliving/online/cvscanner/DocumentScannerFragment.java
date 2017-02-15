package devliving.online.cvscanner;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.support.v4.app.Fragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.Camera;
import android.media.MediaActionSound;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.Log;
import android.util.SparseArray;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.MultiProcessor;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.io.IOException;

import devliving.online.cvscanner.camera.CameraSource;
import devliving.online.cvscanner.camera.CameraSourcePreview;
import devliving.online.cvscanner.camera.GraphicOverlay;

/**
 * Created by Mehedi on 10/23/16.
 */
public class DocumentScannerFragment extends Fragment implements DocumentTracker.DocumentDetectionListener, View.OnTouchListener {
    final Object mLock = new Object();
    boolean isDocumentSaverBusy = false;

    private ImageButton flashToggle;

    private CameraSource mCameraSource;
    private CameraSourcePreview mPreview;
    private GraphicOverlay<DocumentGraphic> mGraphicOverlay;

    // helper objects for detecting taps and pinches.
    private GestureDetector gestureDetector;

    private Detector<Document> IDDetector;
    private MediaActionSound sound = new MediaActionSound();

    private DocumentScannerCallback mListener;
    private boolean isPassport = false;

    public static DocumentScannerFragment instantiate(boolean isPassport){
        DocumentScannerFragment fragment = new DocumentScannerFragment();
        Bundle args = new Bundle();
        args.putBoolean(DocumentScannerActivity.IsScanningPassport, isPassport);
        fragment.setArguments(args);

        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.scanner_content, container, false);

        mPreview = (CameraSourcePreview) view.findViewById(R.id.preview);
        mGraphicOverlay = (GraphicOverlay<DocumentGraphic>) view.findViewById(R.id.graphicOverlay);
        flashToggle = (ImageButton) view.findViewById(R.id.flash);

        gestureDetector = new GestureDetector(getActivity(), new CaptureGestureListener());

        flashToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mCameraSource != null){
                    if(mCameraSource.getFlashMode() == Camera.Parameters.FLASH_MODE_TORCH){
                        mCameraSource.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                    }
                    else mCameraSource.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);

                    updateFlashButtonColor();
                }
            }
        });

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        isPassport = getArguments() != null && getArguments().getBoolean(DocumentScannerActivity.IsScanningPassport, false);
        mGraphicOverlay.addFrame(new FrameGraphic(mGraphicOverlay, isPassport));
        view.setOnTouchListener(this);

        loadOpenCV();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if(context instanceof DocumentScannerCallback){
            mListener = (DocumentScannerCallback) context;
        }
    }

    void updateFlashButtonColor(){
        if(mCameraSource != null){
            int tintColor = Color.LTGRAY;

            if(mCameraSource.getFlashMode() == Camera.Parameters.FLASH_MODE_TORCH){
                tintColor = Color.YELLOW;
            }

            DrawableCompat.setTint(flashToggle.getDrawable(), tintColor);
        }
    }

    void loadOpenCV(){
        if(!OpenCVLoader.initDebug()){
            //OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, getActivity().getApplicationContext(), mLoaderCallback);
        }
        else{
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(getActivity()) {
        @Override
        public void onManagerConnected(int status) {
            if(status == LoaderCallbackInterface.SUCCESS){
                createCameraSource();
                startCameraSource();
            }
            else{
                if(mListener != null) mListener.onScannerFailed("Could not load OpenCV");
                else Toast.makeText(getActivity(), "Could not load OpenCV", Toast.LENGTH_SHORT).show();
            }
        }
    };

    /**
     * Creates and starts the camera.  Note that this uses a higher resolution in comparison
     * to other detection examples to enable the barcode detector to detect small barcodes
     * at long distances.
     *
     * Suppressing InlinedApi since there is a check that the minimum version is met before using
     * the constant.
     */
    @SuppressLint("InlinedApi")
    private void createCameraSource() {
        if(isPassport){
            IDDetector = new PassportDetector();
        }
        else IDDetector = new DocumentDetector(getContext());

        DocumentTrackerFactory factory = new DocumentTrackerFactory(mGraphicOverlay, this);
        IDDetector.setProcessor(
                new MultiProcessor.Builder<>(factory).build());

        // Creates and starts the camera.  Note that this uses a higher resolution in comparison
        // to other detection examples to enable the barcode detector to detect small barcodes
        // at long distances.
        CameraSource.Builder builder = new CameraSource.Builder(getActivity().getApplicationContext(), IDDetector)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedFps(15.0f);

        // make sure that auto focus is an available option
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            builder = builder.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }

        mCameraSource = builder
                .setFlashMode(Camera.Parameters.FLASH_MODE_AUTO)
                .build();
    }

    /**
     * Restarts the camera.
     */
    @Override
    public void onResume() {
        super.onResume();
        startCameraSource();
    }

    /**
     * Stops the camera.
     */
    @Override
    public void onPause() {
        super.onPause();
        if (mPreview != null) {
            mPreview.stop();
        }
    }

    /**
     * Releases the resources associated with the camera source, the associated detectors, and the
     * rest of the processing pipeline.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mPreview != null) {
            mPreview.release();
        }

        if(sound != null) sound.release();
    }

    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private void startCameraSource() throws SecurityException {
        if (mCameraSource != null) {
            try {
                mPreview.start(mCameraSource, mGraphicOverlay);
            } catch (IOException e) {
                Log.e("SCANNER", "Unable to start camera source.", e);
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }

    void processDocument(Document document){
        synchronized (mLock) {
            if(!isDocumentSaverBusy) {
                isDocumentSaverBusy = true;
                document.saveDocument(getActivity(), new Document.DocumentSaveCallback() {
                    @Override
                    public void onStartTask() {
                        Toast toast = Toast.makeText(getActivity(), "Saving...", Toast.LENGTH_SHORT);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();
                    }

                    @Override
                    public void onSaved(String path) {
                        if(path != null && mListener != null) {
                            mListener.onDocumentScanned(path);
                        }
                        isDocumentSaverBusy = false;
                    }
                });
            }
        }
    }

    @Override
    public void onDocumentDetected(final Document document) {
        Log.d("Scanner", "document detected");
        if(document != null){
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    processDocument(document);
                }
            });
        }
    }

    void detectDocumentManually(final byte[] data){
        Log.d("Scanner", "detecting document manually");
        new Thread(new Runnable() {
            @Override
            public void run() {
                Bitmap image = BitmapFactory.decodeByteArray(data, 0, data.length);
                if(image != null){
                    final SparseArray<Document> docs = IDDetector.detect(new Frame.Builder()
                            .setBitmap(image)
                            .build());

                    if(docs != null && docs.size() > 0){
                        Log.d("Scanner", "detected document manually");
                        final Document doc = docs.get(0);

                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                processDocument(doc);
                            }
                        });
                    }
                    else{
                        getActivity().finish();
                    }
                }
            }
        }).start();
    }

    void takePicture(){
        if(mCameraSource != null){
            mCameraSource.takePicture(new CameraSource.ShutterCallback() {
                @Override
                public void onShutter() {
                    sound.play(MediaActionSound.SHUTTER_CLICK);
                }
            }, new CameraSource.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] data) {
                    detectDocumentManually(data);
                }
            });
        }
    }

    /**
     * Called when a touch event is dispatched to a view. This allows listeners to
     * get a chance to respond before the target view.
     *
     * @param v     The view the touch event has been dispatched to.
     * @param event The MotionEvent object containing full information about
     *              the event.
     * @return True if the listener has consumed the event, false otherwise.
     */
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Log.d("SCANNER", "fragment got touch");
        boolean g = gestureDetector.onTouchEvent(event);

        return g || v.onTouchEvent(event);
    }

    private class CaptureGestureListener extends GestureDetector.SimpleOnGestureListener {
        boolean hasShownMsg = false;
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            Log.d("SCANNER", "fragment got tap");
            if(!hasShownMsg){
                Toast.makeText(getActivity(), "Double tap to take a picture and force detection", Toast.LENGTH_SHORT).show();
                hasShownMsg = true;
            }
            return false;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            takePicture();
            return true;
        }
    }

    public interface DocumentScannerCallback{
        void onScannerFailed(String reason);
        void onDocumentScanned(String path);
    }
}
