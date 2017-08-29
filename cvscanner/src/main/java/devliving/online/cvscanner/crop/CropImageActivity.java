/*
 * Copyright (C) 2007 The Android Open Source Project
 * Copyright (C) 2012,2013,2014,2015 Renard Wellnitz
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package devliving.online.cvscanner.crop;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;

import java.io.IOException;

import devliving.online.cvscanner.R;
import devliving.online.cvscanner.util.CVProcessor;
import devliving.online.cvscanner.util.Util;

/**
 * The activity can crop specific region of interest from an image.
 */
public class CropImageActivity extends MonitoredActivity implements HostActivity{
    public static final String EXTRA_IMAGE_URI = "input_image_uri";

    private int mRotation = 0, mScaleFactor = 1;
    boolean mSaving;
    private Bitmap mBitmap;

    protected CropImageView mImageView;
    protected ImageButton mRotateLeft;
    protected ImageButton mRotateRight;
    protected ImageButton mSave;

    private CropHighlightView mCrop;

    Uri sourceUri = null;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
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

        setContentView(R.layout.activity_cropimage);

        findAndInitializeViews();

        loadOpenCV();
    }

    private void findAndInitializeViews(){
        mImageView = findViewById(R.id.cropImageView);
        mRotateLeft = findViewById(R.id.item_rotate_left);
        mRotateRight = findViewById(R.id.item_rotate_right);
        mSave = findViewById(R.id.item_save);

        mRotateRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onRotateRight();
            }
        });

        mRotateLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onRotateLeft();
            }
        });

        mSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onSaveClicked();
            }
        });
    }

    void loadOpenCV(){
        if(!OpenCVLoader.initDebug()){
            //OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, getActivity().getApplicationContext(), mLoaderCallback);
        }
        else{
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            if(status == LoaderCallbackInterface.SUCCESS){
                startCropping();
            }
            else{
                Toast.makeText(CropImageActivity.this, "Could not load OpenCV", Toast.LENGTH_SHORT).show();
                setResult(RESULT_CANCELED);
                CropImageActivity.this.finish();
            }
        }
    };

    public void onRotateLeft() {
        onRotateClicked(-1);
    }

    public void onRotateRight() {
        onRotateClicked(1);
    }

    private void onRotateClicked(int delta) {
        if (mBitmap != null) {
            if (delta < 0) {
                delta = -delta * 3;
            }
            mRotation += delta;
            mRotation = mRotation % 4;
            mImageView.setImageBitmapResetBase(mBitmap, false, mRotation * 90);
            showDefaultCroppingRectangle(mBitmap.getWidth(), mBitmap.getHeight());
        }
    }

    private void startCropping() {
        mImageView.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                Bundle extras = getIntent().getExtras();
                Uri imageUri = null;

                if(extras.containsKey(EXTRA_IMAGE_URI)){
                    imageUri = Uri.parse(extras.getString(EXTRA_IMAGE_URI));
                }

                if(imageUri != null){
                    try {
                        mScaleFactor = Util.calculateBitmapSampleSize(CropImageActivity.this, imageUri);
                        mBitmap = Util.loadBitmapFromUri(CropImageActivity.this, mScaleFactor, imageUri);

                        int rotation = Util.getExifRotation(CropImageActivity.this, imageUri);
                        mRotation = rotation/90;
                    } catch (IOException e) {
                        e.printStackTrace();
                        setResult(RESULT_CANCELED);
                        finish();
                    }
                }

                if(mBitmap != null){
                    sourceUri = imageUri;
                    mImageView.setImageBitmapResetBase(mBitmap, true, mRotation * 90);
                    adjustButtons();
                    showDefaultCroppingRectangle(mBitmap.getWidth(), mBitmap.getHeight());
                }
                else {
                    setResult(RESULT_CANCELED);
                    finish();
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    mImageView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
                else {
                    mImageView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
            }

        });
    }

    private void adjustButtons() {
        if (mBitmap != null) {
            mRotateLeft.setVisibility(View.VISIBLE);
            mRotateRight.setVisibility(View.VISIBLE);
            mSave.setVisibility(View.VISIBLE);
        } else {
            mRotateLeft.setVisibility(View.GONE);
            mRotateRight.setVisibility(View.GONE);
            mSave.setVisibility(View.GONE);
        }
    }

    void clearImages(){
        if(mBitmap != null) mBitmap.recycle();
        mImageView.clear();
    }

    void onSaveClicked() {
        float[] points = mCrop.getTrapezoid();
        Point[] quadPoints = new Point[4];

        for(int i = 0, j = 0; i < 8; i++, j++){
            Log.d("CROP", "i: " + i + ", j: " + j);
            quadPoints[j] = new Point(points[i], points[++i]);
        }

        Point[] sortedPoints = CVProcessor.sortPoints(quadPoints);
        Mat src = new Mat(mBitmap.getWidth(), mBitmap.getHeight(), CvType.CV_8UC4);
        Utils.bitmapToMat(mBitmap, src);
        clearImages();
        Mat cropped = CVProcessor.fourPointTransform(src, sortedPoints);
        src.release();

        Uri savedUri = Util.saveImage(this, System.currentTimeMillis() + "_img.jpg", cropped);
        if(savedUri != null){
            Intent data = getIntent();
            data.setData(savedUri);
            setResult(RESULT_OK, data);
        }else {
            setResult(RESULT_CANCELED);
        }

        finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        setResult(RESULT_CANCELED);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unbindDrawables(findViewById(android.R.id.content));
        mImageView.clear();
    }

    private void unbindDrawables(View view) {
        if (view.getBackground() != null) {
            view.getBackground().setCallback(null);
        }
        if (view instanceof ViewGroup && !(view instanceof AdapterView)) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                unbindDrawables(((ViewGroup) view).getChildAt(i));
            }
            ((ViewGroup) view).removeAllViews();
        }
    }

    private void showDefaultCroppingRectangle(int imageWidth, int imageHeight) {
        Rect imageRect = new Rect(0, 0, imageWidth, imageHeight);

        // make the default size about 4/5 of the width or height
        int cropWidth = Math.min(imageWidth, imageHeight) * 4 / 5;


        int x = (imageWidth - cropWidth) / 2;
        int y = (imageHeight - cropWidth) / 2;

        RectF cropRect = new RectF(x, y, x + cropWidth, y + cropWidth);

        CropHighlightView hv = new CropHighlightView(mImageView, imageRect, cropRect);

        mImageView.resetMaxZoom();
        mImageView.add(hv);
        mCrop = hv;
        mCrop.setFocus(true);
        mImageView.invalidate();
    }

    @Override
    public boolean isBusy() {
        return mSaving;
    }

    @Override
    public Context context() {
        return this;
    }
}

