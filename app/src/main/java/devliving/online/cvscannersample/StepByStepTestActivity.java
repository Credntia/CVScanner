package devliving.online.cvscannersample;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TimingLogger;
import android.view.View;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat4;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Range;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.FileNotFoundException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;

import devliving.online.cvscanner.CVProcessor;
import devliving.online.cvscanner.Line;

/**
 * Created by user on 9/22/16.
 */
public class StepByStepTestActivity extends AppCompatActivity{

    final static String EXTRA_SCAN_TYPE = "scan_type";

    final int REQ_PICK_IMAGE = 1;
    final int REQ_STORAGE_PERM = 11;

    CVCommand scanType;

    RecyclerView contentView;
    ImageAdapter mAdapter;
    FloatingActionButton fab;

    Mat mData = null;

    BaseLoaderCallback mCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            super.onManagerConnected(status);

            fab.setScaleX(0.1f);
            fab.setScaleY(0.1f);
            fab.setAlpha(0.4f);
            fab.setVisibility(View.VISIBLE);

            fab.animate()
                    .alpha(1)
                    .scaleX(1)
                    .scaleY(1)
                    .start();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_step_by_step);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if(getIntent().getExtras() != null) scanType = (CVCommand) getIntent().getExtras().getSerializable(EXTRA_SCAN_TYPE);

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent, REQ_PICK_IMAGE);
            }
        });

        contentView = (RecyclerView) findViewById(R.id.image_list);

        contentView.setLayoutManager(new LinearLayoutManager(this));
        //contentView.setHasFixedSize(true);

        mAdapter = new ImageAdapter();
        contentView.setAdapter(mAdapter);

        int result = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if(result != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQ_STORAGE_PERM);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(!OpenCVLoader.initDebug()){
            //OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, getApplicationContext(), mCallback);
        }
        else mCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mAdapter.clear();
        if(mData != null){
            mData.release();
            mData = null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == REQ_PICK_IMAGE && resultCode == RESULT_OK && data != null){
            try {
                Bitmap image = BitmapFactory.decodeStream(getContentResolver().openInputStream(data.getData()));
                if(mData != null){
                    mData.release();
                    mData = null;
                }
                mData = new Mat();
                Utils.bitmapToMat(image, mData);
                image.recycle();

                startTests();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != REQ_STORAGE_PERM) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        if (grantResults.length == 0 || grantResults[0] == PackageManager.PERMISSION_DENIED) {
            finish();
        }
    }

    HandlerThread imgThread;
    CVTestRunner testRunner;

    void startTests(){
        mAdapter.clear();

        if(imgThread == null || testRunner == null) {
            imgThread = new HandlerThread("Image processor thread");
            imgThread.start();

            testRunner = new CVTestRunner(imgThread.getLooper());
        }

        if(mData != null){
            Message msg = new Message();
            msg.obj = new CVTestMessage(scanType, mData);
            testRunner.sendMessage(msg);
        }
    }

    void onNextStep(Mat img){
        Bitmap result = Bitmap.createBitmap(img.cols(), img.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(img, result);
        final String path = Utility.saveBitmapJPG(result, "cvsample_" + Calendar.getInstance().getTimeInMillis() + ".jpg");

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAdapter.add(path);
            }
        });
    }

    enum CVCommand {
        DOCUMENT_SCAN,
        PASSPORT_SCAN_HOUGHLINES,
        PASSPORT_SCAN_MRZ,
        PASSPORT_SCAN_MRZ2
    }

    class CVTestMessage {
        CVCommand command;
        Mat input;

        public CVTestMessage(CVCommand cmd, Mat input){
            command = cmd;
            this.input = input;
        }
    }

    void drawRect(Mat img, Point[] points) {
        if (img == null || points == null || points.length != 4) return;
        Point[] sorted = CVProcessor.sortPoints(points);

        Imgproc.line(img, sorted[0], sorted[1], new Scalar(250, 0, 0), 2);
        Imgproc.line(img, sorted[0], sorted[3], new Scalar(250, 0, 0), 2);
        Imgproc.line(img, sorted[1], sorted[2], new Scalar(250, 0, 0), 2);
        Imgproc.line(img, sorted[3], sorted[2], new Scalar(250, 0, 0), 2);
    }

    double findAngleBetweenLines(Point firstSrc, Point firstDest, Point secSrc, Point secDest){
        double theta = Math.PI;

        if(firstSrc.x == firstDest.x) //infinite slope
        {
            if(secSrc.x != secDest.x){
                double secSlope = (secDest.y - secSrc.y)/(secDest.x - secSrc.x);
                theta = Math.atan2(1, Math.abs(secSlope));
            }
        }
        else if(secSrc.x == secDest.x){
            if(firstSrc.x != firstDest.x){
                double firstSlope = (firstDest.y - firstSrc.y)/(firstDest.x - firstSrc.x);
                theta = Math.atan2(1, Math.abs(firstSlope));
            }
        }
        else{
            double firstSlope = (firstDest.y - firstSrc.y)/(firstDest.x - firstSrc.x);
            double secSlope = (secDest.y - secSrc.y)/(secDest.x - secSrc.x);
            if(firstSlope == 0) theta = Math.atan2(Math.abs(secDest.y - secSrc.y), Math.abs(secDest.x - secSrc.x));
            else if(secSlope == 0) theta = Math.atan2(Math.abs(firstDest.y - firstSrc.y), Math.abs(firstDest.x - firstSrc.x));
            else theta = Math.atan2(Math.abs(firstSlope - secSlope), Math.abs(1 + (firstSlope * secSlope)));
        }

        return Math.abs(theta);
    }

    Mat buildSkeleton(Mat img){
        Mat morph = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_CROSS, new Size(3, 3));
        Mat skel = new Mat(img.size(), CvType.CV_8UC1, Scalar.all(0));
        Mat eroded = new Mat();
        Mat temp = new Mat();

        boolean done = false;

        do{
            Imgproc.morphologyEx(img, eroded, Imgproc.MORPH_ERODE, morph);
            Imgproc.morphologyEx(eroded, temp, Imgproc.MORPH_DILATE, morph);
            Core.subtract(img, temp, temp);
            Core.bitwise_or(skel, temp, skel);
            eroded.copyTo(img);

            done = Core.countNonZero(img) == 0;
        }while (!done);

        return skel;
    }

    class CVTestRunner extends Handler{
        final String TAG = "CV-TEST";

        public CVTestRunner(Looper looper){
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            final int FIXED_HEIGHT = 600;

            if(msg.obj != null && msg.obj instanceof CVTestMessage){
                CVTestMessage data = (CVTestMessage) msg.obj;

                switch (data.command){
                    case DOCUMENT_SCAN:
                        TimingLogger timingLogger = new TimingLogger(TAG, "Detect Document");
                        Mat img = data.input.clone();
                        data.input.release();
                        //find contours
                        double ratio = img.size().height/FIXED_HEIGHT;
                        int width = (int) (img.size().width / ratio);
                        int height = (int) (img.size().height / ratio);
                        Size newSize = new Size(width, height);
                        Mat resizedImg = new Mat(newSize, CvType.CV_8UC4);
                        Imgproc.resize(img, resizedImg, newSize);
                        onNextStep(resizedImg);

                        Imgproc.medianBlur(resizedImg, resizedImg, 5);
                        onNextStep(resizedImg);

                        Mat cannedImg = new Mat(newSize, CvType.CV_8UC1);
                        Imgproc.Canny(resizedImg, cannedImg, 70, 200, 3, true);
                        resizedImg.release();
                        onNextStep(cannedImg);

                        Imgproc.threshold(cannedImg, cannedImg, 70, 255, Imgproc.THRESH_OTSU);
                        onNextStep(cannedImg);

                        Mat dilatedImg = new Mat(newSize, CvType.CV_8UC1);
                        Mat morph = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3));
                        Imgproc.dilate(cannedImg, dilatedImg, morph, new Point(-1, -1), 2, 1, new Scalar(1));
                        cannedImg.release();
                        morph.release();
                        onNextStep(dilatedImg);
                        timingLogger.addSplit("Segmentation");

                        ArrayList<MatOfPoint> contours = new ArrayList<>();
                        Mat hierarchy = new Mat();
                        Imgproc.findContours(dilatedImg, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
                        hierarchy.release();

                        Log.d(TAG, "contours found: " + contours.size());

                        Collections.sort(contours, new Comparator<MatOfPoint>() {
                            @Override
                            public int compare(MatOfPoint o1, MatOfPoint o2) {
                                return Double.valueOf(Imgproc.contourArea(o2)).compareTo(Imgproc.contourArea(o1));
                            }
                        });

                        Imgproc.drawContours(dilatedImg, contours, 0, new Scalar(255, 255, 250));
                        onNextStep(dilatedImg);
                        dilatedImg.release();

                        timingLogger.addSplit("Find contours");

                        MatOfPoint rectContour = null;
                        Point[] foundPoints = null;

                        for(MatOfPoint contour:contours){
                            MatOfPoint2f mat = new MatOfPoint2f(contour.toArray());
                            double peri = Imgproc.arcLength(mat, true);
                            MatOfPoint2f approx = new MatOfPoint2f();
                            Imgproc.approxPolyDP(mat, approx, 0.02 * peri, true);

                            Point[] points = approx.toArray();
                            Log.d("SCANNER", "approx size " + points.length);

                            if (points.length == 4) {
                                Point[] spoints = CVProcessor.sortPoints(points);

                                if (CVProcessor.isInside(spoints, newSize) && CVProcessor.isLargeEnough(spoints, newSize, 0.40)) {
                                    rectContour = contour;
                                    foundPoints = spoints;
                                    break;
                                }
                            }
                        }
                        timingLogger.addSplit("Find points");

                        if(rectContour != null){
                            Point[] scaledPoints = new Point[foundPoints.length];

                            for(int i = 0; i < foundPoints.length; i++){
                                scaledPoints[i] = new Point(foundPoints[i].x * ratio, foundPoints[i].y * ratio);
                            }
                            Log.d("SCANNER", "drawing lines");
                            Imgproc.line(img, scaledPoints[0], scaledPoints[1], new Scalar(250, 20, 20));
                            Imgproc.line(img, scaledPoints[0], scaledPoints[3], new Scalar(250, 20, 20));
                            Imgproc.line(img, scaledPoints[1], scaledPoints[2], new Scalar(250, 20, 20));
                            Imgproc.line(img, scaledPoints[3], scaledPoints[2], new Scalar(250, 20, 20));

                            timingLogger.addSplit("Upscaling points, drawing lines");
                        }

                        onNextStep(img);
                        timingLogger.dumpToLog();
                        img.release();
                        break;

                    case PASSPORT_SCAN_HOUGHLINES:
                        timingLogger = new TimingLogger(TAG, "Detecting Passport - HoughLinesP");
                        img = data.input.clone();
                        data.input.release();
                        //find contours
                        ratio = img.size().height/FIXED_HEIGHT;
                        width = (int) (img.size().width / ratio);
                        height = (int) (img.size().height / ratio);
                        newSize = new Size(width, height);
                        resizedImg = new Mat(newSize, CvType.CV_8UC4);
                        Imgproc.resize(img, resizedImg, newSize);
                        onNextStep(resizedImg);

                        Imgproc.medianBlur(resizedImg, resizedImg, 13);
                        onNextStep(resizedImg);

                        cannedImg = new Mat(newSize, CvType.CV_8UC1);
                        Imgproc.Canny(resizedImg, cannedImg, 70, 200, 3, true);
                        //resizedImg.release();
                        onNextStep(cannedImg);

                        //morph = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_CROSS, new Size(5, 5));
                        Mat morphR = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_RECT, new Size(5, 5));
                        //Mat morphE = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_ELLIPSE, new Size(3, 3));

                        Imgproc.morphologyEx(cannedImg, cannedImg, Imgproc.MORPH_CLOSE, morphR, new Point(-1, -1), 1);
                        //Imgproc.morphologyEx(cannedImg, cannedImg, Imgproc.MORPH_DILATE, morph, new Point(-1, -1), 1);
                        //imgX= buildSkeleton(imgX);
                        //onNextStep(cannedImg);
                        timingLogger.addSplit("Segmentation");

                        MatOfFloat4 lines = new MatOfFloat4();
                        Imgproc.HoughLinesP(cannedImg, lines, 1, Math.PI/180, 30, 30, 150);

                        timingLogger.addSplit("Hough lines");

                        Log.d("SCANNER", "got lines: " + lines.rows());
                        if(lines.rows() >= 3) {
                            ArrayList<Line> hLines = new ArrayList<>();
                            ArrayList<Line> vLines = new ArrayList<>();

                            for (int i = 0; i < lines.rows(); i++) {
                                double[] vec = lines.get(i, 0);
                                Line l = new Line(vec[0], vec[1], vec[2], vec[3]);
                                if(l.isNearHorizontal()) hLines.add(l);
                                else if(l.isNearVertical()) vLines.add(l);
                            }

                            Collections.sort(hLines, new Comparator<Line>() {
                                @Override
                                public int compare(Line o1, Line o2) {
                                    return (int) Math.ceil(o1.start.y - o2.start.y);
                                }
                            });

                            Collections.sort(vLines, new Comparator<Line>() {
                                @Override
                                public int compare(Line o1, Line o2) {
                                    return (int) Math.ceil(o1.start.x - o2.start.x);
                                }
                            });

                            timingLogger.addSplit("Separating horizontal and vertical lines");

                            if(hLines.size() >= 2 && vLines.size() >= 2){
                                List<Line> nhLines = Line.joinSegments(hLines);

                                List<Line> nvLines = Line.joinSegments(vLines);

                                Collections.sort(nhLines, new Comparator<Line>() {
                                    @Override
                                    public int compare(Line o1, Line o2) {
                                        return (int) Math.ceil(o2.length() - o1.length());
                                    }
                                });

                                Collections.sort(nvLines, new Comparator<Line>() {
                                    @Override
                                    public int compare(Line o1, Line o2) {
                                        return (int) Math.ceil(o2.length() - o1.length());
                                    }
                                });

                                timingLogger.addSplit("Joining line segments");

                                if((nvLines.size() > 1 && nhLines.size() > 0) || (nvLines.size() > 0 && nhLines.size() > 1)){
                                    Line left = null, right = null, bottom = null, top = null;

                                    for(Line l:nvLines){
                                        if(l.length()/height < 0.60 || (left != null && right != null)) break;

                                        if(left == null && l.isInleft(width)){
                                            left = l;
                                            continue;
                                        }

                                        if(right == null && !l.isInleft(width)) right = l;
                                    }

                                    for(Line l:nhLines){
                                        if(l.length()/width < 0.60 || (top != null && bottom != null)) break;

                                        if(bottom == null && l.isInBottom(height)){
                                            bottom = l;
                                            continue;
                                        }

                                        if(top == null && !l.isInBottom(height)) top = l;
                                    }

                                    timingLogger.addSplit("Finding edges");

                                    foundPoints = null;

                                    if((left != null && right != null) && (bottom != null || top != null)){
                                        Point vLeft = bottom != null? bottom.intersect(left):top.intersect(left);
                                        Point vRight = bottom != null? bottom.intersect(right):top.intersect(right);

                                        if(vLeft != null && vRight != null) {
                                            double pwidth = new Line(vLeft, vRight).length();
                                            float pRatio = 3.465f / 4.921f;
                                            double pHeight = pRatio * pwidth;

                                            double dxFactor = pHeight / new Line(vLeft, left.end).length();
                                            double tLeftX = ((1 - dxFactor) * vLeft.x) + (dxFactor * left.end.x);
                                            double tLeftY = ((1 - dxFactor) * vLeft.y) + (dxFactor * left.end.y);
                                            Point tLeft = new Point(tLeftX, tLeftY);

                                            dxFactor = pHeight / new Line(vRight, right.end).length();
                                            double tRightX = ((1 - dxFactor) * vRight.x) + (dxFactor * right.end.x);
                                            double tRightY = ((1 - dxFactor) * vRight.y) + (dxFactor * right.end.y);
                                            Point tRight = new Point(tRightX, tRightY);

                                            foundPoints = new Point[]{vLeft, vRight, tLeft, tRight};
                                        }
                                    }
                                    else if((top != null && bottom != null) && (left != null || right != null)){
                                        Point vTop = left != null? left.intersect(top):right.intersect(top);
                                        Point vBottom = left != null? left.intersect(bottom):right.intersect(bottom);

                                        if(vTop != null && vBottom != null) {
                                            double pHeight = new Line(vTop, vBottom).length();
                                            float pRatio = 4.921f / 3.465f;
                                            double pWidth = pRatio * pHeight;

                                            double dxFactor = pWidth / new Line(vTop, top.end).length();
                                            double tTopX = ((1 - dxFactor) * vTop.x) + (dxFactor * top.end.x);
                                            double tTopY = ((1 - dxFactor) * vTop.y) + (dxFactor * top.end.y);
                                            Point tTop = new Point(tTopX, tTopY);

                                            dxFactor = pWidth / new Line(vBottom, bottom.end).length();
                                            double tBottomX = ((1 - dxFactor) * vBottom.x) + (dxFactor * bottom.end.x);
                                            double tBottomY = ((1 - dxFactor) * vBottom.y) + (dxFactor * bottom.end.y);
                                            Point tBottom = new Point(tBottomX, tBottomY);

                                            foundPoints = new Point[]{tTop, tBottom, vTop, vBottom};
                                        }
                                    }
                                    timingLogger.addSplit("Calculating vertices");
                                    timingLogger.dumpToLog();

                                    if(foundPoints != null){
                                        drawRect(resizedImg, foundPoints);
                                        onNextStep(resizedImg);
                                    }
                                }
                            }
                        }
                        resizedImg.release();

                        break;

                    case PASSPORT_SCAN_MRZ:
                        //downscale
                        img = data.input.clone();
                        data.input.release();
                        ratio = img.size().height/FIXED_HEIGHT;
                        width = (int) (img.size().width / ratio);
                        height = (int) (img.size().height / ratio);
                        newSize = new Size(width, height);
                        resizedImg = new Mat(newSize, CvType.CV_8UC4);
                        Imgproc.resize(img, resizedImg, newSize);
                        onNextStep(resizedImg);

                        Mat gray = new Mat();
                        Imgproc.cvtColor(resizedImg, gray, Imgproc.COLOR_BGR2GRAY);
                        Imgproc.medianBlur(gray, gray, 3);
                        onNextStep(gray);

                        morph = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(13, 5));
                        dilatedImg = new Mat();
                        Imgproc.morphologyEx(gray, dilatedImg, Imgproc.MORPH_BLACKHAT, morph);
                        onNextStep(dilatedImg);
                        gray.release();

                        Mat gradX = new Mat();
                        Imgproc.Sobel(dilatedImg, gradX, CvType.CV_32F, 1, 0);
                        dilatedImg.release();
                        Core.convertScaleAbs(gradX, gradX, 1, 0);
                        Core.MinMaxLocResult minMax = Core.minMaxLoc(gradX);
                        Core.convertScaleAbs(gradX, gradX, (255/(minMax.maxVal - minMax.minVal)),
                                - ((minMax.minVal * 255) / (minMax.maxVal - minMax.minVal)));
                        Imgproc.morphologyEx(gradX, gradX, Imgproc.MORPH_CLOSE, morph);

                        Mat thresh = new Mat();
                        Imgproc.threshold(gradX, thresh, 0, 255, Imgproc.THRESH_OTSU);
                        onNextStep(thresh);
                        gradX.release();
                        morph.release();

                        morph = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(21, 21));
                        Imgproc.morphologyEx(thresh, thresh, Imgproc.MORPH_CLOSE, morph);
                        Imgproc.erode(thresh, thresh, new Mat(), new Point(-1, -1), 4);
                        onNextStep(thresh);
                        morph.release();

                        int col = (int) resizedImg.size().width;
                        int p = (int) (resizedImg.size().width * 0.05);
                        int row = (int) resizedImg.size().height;
                        for(int i = 0; i < row; i++)
                        {
                            for(int j = 0; j < p; j++){
                                thresh.put(i, j, 0);
                                thresh.put(i, col-j, 0);
                            }
                        }

                        contours = new ArrayList<>();
                        hierarchy = new Mat();
                        Imgproc.findContours(thresh, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
                        hierarchy.release();

                        Log.d(TAG, "contours found: " + contours.size());

                        Collections.sort(contours, new Comparator<MatOfPoint>() {
                            @Override
                            public int compare(MatOfPoint o1, MatOfPoint o2) {
                                return Double.valueOf(Imgproc.contourArea(o2)).compareTo(Imgproc.contourArea(o1));
                            }
                        });

                        rectContour = null;
                        foundPoints = null;

                        for(MatOfPoint c:contours){
                            Rect bRect = Imgproc.boundingRect(c);
                            float aspectRatio = bRect.width / (float)bRect.height;
                            float coverageRatio = bRect.width/(float)col;

                            Log.d(TAG, "AR: " + aspectRatio + ", CR: " + coverageRatio);

                            if(aspectRatio > 5 && coverageRatio > 0.70){
                                Imgproc.drawContours(resizedImg, Arrays.asList(c), 0, new Scalar(255, 0, 0), 5);
                                onNextStep(resizedImg);

                                MatOfPoint2f c2f = new MatOfPoint2f(c.toArray());
                                double peri = Imgproc.arcLength(c2f, true);
                                MatOfPoint2f approx = new MatOfPoint2f();
                                Imgproc.approxPolyDP(c2f, approx, 0.02 * peri, true);

                                Point[] points = approx.toArray();
                                Log.d("SCANNER", "approx size: " + points.length);

                                // select biggest 4 angles polygon
                                if (points.length == 4){
                                    foundPoints = CVProcessor.sortPoints(points);
                                    break;
                                }
                                else if(points.length == 2){
                                    if(rectContour == null){
                                        rectContour = c;
                                        foundPoints = points;
                                    }
                                    else{
                                        //try to merge
                                        RotatedRect box1 = Imgproc.minAreaRect(new MatOfPoint2f(c.toArray()));
                                        RotatedRect box2 = Imgproc.minAreaRect(new MatOfPoint2f(rectContour.toArray()));

                                        float ar = (float) (box1.size.width/box2.size.width);
                                        if(box1.size.width > 0 && box2.size.width > 0 && 0.5 < ar && ar < 2.0) {
                                            if (Math.abs(box1.angle - box2.angle) <= 0.1 ||
                                                    Math.abs(Math.PI - (box1.angle - box2.angle)) <= 0.1) {
                                                double minAngle = Math.min(box1.angle, box2.angle);
                                                double relX = box1.center.x - box2.center.x;
                                                double rely = box1.center.y - box2.center.y;
                                                double distance = Math.abs((rely * Math.cos(minAngle)) - (relX * Math.sin(minAngle)));
                                                if(distance < (1.5 * (box1.size.height + box2.size.height))){
                                                    Point[] allPoints = Arrays.copyOf(foundPoints, 4);

                                                    System.arraycopy(points, 0, allPoints, 2, 2);
                                                    Log.d("SCANNER", "after merge approx size: " + allPoints.length);
                                                    if (allPoints.length == 4){
                                                        foundPoints = CVProcessor.sortPoints(allPoints);
                                                        break;
                                                    }
                                                }
                                            }
                                        }

                                        rectContour = null;
                                        foundPoints = null;
                                    }
                                }
                            }
                        }

                        if(foundPoints != null && foundPoints.length == 4){
                            Point lowerLeft = foundPoints[3];
                            Point lowerRight = foundPoints[2];
                            Point topLeft = foundPoints[0];
                            Point topRight = foundPoints[1];
                            double w = Math.sqrt(Math.pow(lowerRight.x - lowerLeft.x, 2) + Math.pow(lowerRight.y - lowerLeft.y, 2));
                            double h = Math.sqrt(Math.pow(topLeft.x - lowerLeft.x, 2) + Math.pow(topLeft.y - lowerLeft.y, 2));;
                            int px = (int) ((lowerLeft.x + w) * 0.03);
                            int py = (int) ((lowerLeft.y + h) * 0.03);
                            lowerLeft.x = lowerLeft.x - px;
                            lowerLeft.y = lowerLeft.y + py;

                            px = (int) ((lowerRight.x + w) * 0.03);
                            py = (int) ((lowerRight.y + h) * 0.03);
                            lowerRight.x = lowerRight.x + px;
                            lowerRight.y = lowerRight.y + py;

                            float pRatio = 3.465f/4.921f;
                            w = Math.sqrt(Math.pow(lowerRight.x - lowerLeft.x, 2) + Math.pow(lowerRight.y - lowerLeft.y, 2));

                            h = pRatio * w;
                            h = h - (h * 0.04);

                            Log.d("SCANNER", "topLeft:(" + topLeft.x + ", " + topLeft.y + ")\ntopRight:("
                            + topRight.x + "," + topRight.y + ")\nlowLeft:(" + lowerLeft.x + "," + lowerLeft.y + ")\nlowRight:("
                            + lowerRight.x + "," + lowerRight.y + ")");

                            topLeft.y = lowerLeft.y - h;

                            topRight.y = lowerLeft.y - h;

                            //Imgproc.drawContours(resizedImg, Arrays.asList(new MatOfPoint(foundPoints)), 0, new Scalar(255, 0, 0), 4);
                            //onNextStep(resizedImg);

                            foundPoints = CVProcessor.getUpscaledPoints(foundPoints, ratio);

                            //img = CVProcessor.fourPointTransform(img, foundPoints);
                            drawRect(img, foundPoints);
                            onNextStep(img);
                        }
                        img.release();
                        break;

                    case PASSPORT_SCAN_MRZ2:
                        //downscale
                        img = data.input.clone();
                        data.input.release();
                        ratio = img.size().height/FIXED_HEIGHT;
                        width = (int) (img.size().width / ratio);
                        height = (int) (img.size().height / ratio);
                        newSize = new Size(width, height);
                        resizedImg = new Mat(newSize, CvType.CV_8UC4);
                        Imgproc.resize(img, resizedImg, newSize);
                        onNextStep(resizedImg);

                        gray = new Mat();
                        Imgproc.cvtColor(resizedImg, gray, Imgproc.COLOR_BGR2GRAY);
                        Imgproc.medianBlur(gray, gray, 3);
                        onNextStep(gray);

                        morph = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(13, 5));
                        dilatedImg = new Mat();
                        Imgproc.morphologyEx(gray, dilatedImg, Imgproc.MORPH_BLACKHAT, morph);
                        onNextStep(dilatedImg);
                        gray.release();

                        gradX = new Mat();
                        Imgproc.Sobel(dilatedImg, gradX, CvType.CV_32F, 1, 0);
                        dilatedImg.release();
                        Core.convertScaleAbs(gradX, gradX, 1, 0);
                        minMax = Core.minMaxLoc(gradX);
                        Core.convertScaleAbs(gradX, gradX, (255/(minMax.maxVal - minMax.minVal)),
                                - ((minMax.minVal * 255) / (minMax.maxVal - minMax.minVal)));
                        Imgproc.morphologyEx(gradX, gradX, Imgproc.MORPH_CLOSE, morph);

                        thresh = new Mat();
                        Imgproc.threshold(gradX, thresh, 0, 255, Imgproc.THRESH_OTSU);
                        onNextStep(thresh);
                        gradX.release();
                        morph.release();

                        morph = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(21, 21));
                        Imgproc.morphologyEx(thresh, thresh, Imgproc.MORPH_CLOSE, morph);
                        Imgproc.erode(thresh, thresh, new Mat(), new Point(-1, -1), 4);
                        onNextStep(thresh);
                        morph.release();

                        col = (int) resizedImg.size().width;
                        p = (int) (resizedImg.size().width * 0.05);
                        row = (int) resizedImg.size().height;
                        for(int i = 0; i < row; i++)
                        {
                            for(int j = 0; j < p; j++){
                                thresh.put(i, j, 0);
                                thresh.put(i, col-j, 0);
                            }
                        }

                        contours = new ArrayList<>();
                        hierarchy = new Mat();
                        Imgproc.findContours(thresh, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
                        hierarchy.release();

                        Log.d(TAG, "contours found: " + contours.size());

                        Collections.sort(contours, new Comparator<MatOfPoint>() {
                            @Override
                            public int compare(MatOfPoint o1, MatOfPoint o2) {
                                return Double.valueOf(Imgproc.contourArea(o2)).compareTo(Imgproc.contourArea(o1));
                            }
                        });

                        rectContour = null;
                        foundPoints = null;

                        for(MatOfPoint c:contours){
                            Rect bRect = Imgproc.boundingRect(c);
                            float aspectRatio = bRect.width / (float)bRect.height;
                            float coverageRatio = bRect.width/(float)col;

                            Log.d(TAG, "AR: " + aspectRatio + ", CR: " + coverageRatio);

                            if(aspectRatio > 5 && coverageRatio > 0.70){
                                //Imgproc.drawContours(resizedImg, Arrays.asList(c), 0, new Scalar(255, 0, 0), 5);
                                //onNextStep(resizedImg);

                                MatOfPoint2f c2f = new MatOfPoint2f(c.toArray());
                                double peri = Imgproc.arcLength(c2f, true);
                                MatOfPoint2f approx = new MatOfPoint2f();
                                Imgproc.approxPolyDP(c2f, approx, 0.02 * peri, true);

                                Point[] points = approx.toArray();
                                Log.d("SCANNER", "approx size: " + points.length);

                                // select biggest 4 angles polygon
                                if (points.length == 4){
                                    foundPoints = CVProcessor.sortPoints(points);
                                    break;
                                }
                                else if(points.length == 2){
                                    if(rectContour == null){
                                        rectContour = c;
                                        foundPoints = points;
                                    }
                                    else{
                                        //try to merge
                                        RotatedRect box1 = Imgproc.minAreaRect(new MatOfPoint2f(c.toArray()));
                                        RotatedRect box2 = Imgproc.minAreaRect(new MatOfPoint2f(rectContour.toArray()));

                                        float ar = (float) (box1.size.width/box2.size.width);
                                        if(box1.size.width > 0 && box2.size.width > 0 && 0.5 < ar && ar < 2.0) {
                                            if (Math.abs(box1.angle - box2.angle) <= 0.1 ||
                                                    Math.abs(Math.PI - (box1.angle - box2.angle)) <= 0.1) {
                                                double minAngle = Math.min(box1.angle, box2.angle);
                                                double relX = box1.center.x - box2.center.x;
                                                double rely = box1.center.y - box2.center.y;
                                                double distance = Math.abs((rely * Math.cos(minAngle)) - (relX * Math.sin(minAngle)));
                                                if(distance < (1.5 * (box1.size.height + box2.size.height))){
                                                    Point[] allPoints = Arrays.copyOf(foundPoints, 4);

                                                    System.arraycopy(points, 0, allPoints, 2, 2);
                                                    Log.d("SCANNER", "after merge approx size: " + allPoints.length);
                                                    if (allPoints.length == 4){
                                                        foundPoints = CVProcessor.sortPoints(allPoints);
                                                        break;
                                                    }
                                                }
                                            }
                                        }

                                        rectContour = null;
                                        foundPoints = null;
                                    }
                                }
                            }
                        }

                        if(foundPoints != null && foundPoints.length == 4){
                            Point lowerLeft = foundPoints[3];
                            Point lowerRight = foundPoints[2];
                            Point topLeft = foundPoints[0];
                            Point topRight = foundPoints[1];
                            double w = Math.sqrt(Math.pow(lowerRight.x - lowerLeft.x, 2) + Math.pow(lowerRight.y - lowerLeft.y, 2));
                            double h = Math.sqrt(Math.pow(topLeft.x - lowerLeft.x, 2) + Math.pow(topLeft.y - lowerLeft.y, 2));;
                            int px = (int) ((lowerLeft.x + w) * 0.03);
                            int py = (int) ((lowerLeft.y + h) * 0.03);
                            lowerLeft.x = lowerLeft.x - px;
                            lowerLeft.y = lowerLeft.y + py;

                            px = (int) ((lowerRight.x + w) * 0.03);
                            py = (int) ((lowerRight.y + h) * 0.03);
                            lowerRight.x = lowerRight.x + px;
                            lowerRight.y = lowerRight.y + py;

                            float pRatio = 3.465f/4.921f;
                            w = Math.sqrt(Math.pow(lowerRight.x - lowerLeft.x, 2) + Math.pow(lowerRight.y - lowerLeft.y, 2));

                            h = pRatio * w;
                            h = h - (h * 0.04);

                            Log.d("SCANNER", "topLeft:(" + topLeft.x + ", " + topLeft.y + ")\ntopRight:("
                                    + topRight.x + "," + topRight.y + ")\nlowLeft:(" + lowerLeft.x + "," + lowerLeft.y + ")\nlowRight:("
                                    + lowerRight.x + "," + lowerRight.y + ")");

                            topLeft.y = lowerLeft.y - h;

                            topRight.y = lowerLeft.y - h;

                            topLeft.x = 0;
                            topRight.x = resizedImg.width();
                            Imgproc.line(resizedImg, topLeft, topRight, new Scalar(0, 0, 0), 2);
                            onNextStep(resizedImg);

                            Imgproc.medianBlur(resizedImg, resizedImg, 5);
                            //onNextStep(resizedImg);

                            cannedImg = new Mat(newSize, CvType.CV_8UC1);
                            Imgproc.Canny(resizedImg, cannedImg, 70, 200, 3, true);
                            resizedImg.release();
                            onNextStep(cannedImg);

                            Imgproc.threshold(cannedImg, cannedImg, 70, 255, Imgproc.THRESH_OTSU);
                            onNextStep(cannedImg);

                            dilatedImg = new Mat(newSize, CvType.CV_8UC1);
                            morph = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3));
                            Imgproc.dilate(cannedImg, dilatedImg, morph, new Point(-1, -1), 2, 1, new Scalar(1));
                            cannedImg.release();
                            morph.release();
                            onNextStep(dilatedImg);

                            contours = new ArrayList<>();
                            hierarchy = new Mat();
                            Imgproc.findContours(dilatedImg, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
                            hierarchy.release();

                            Log.d(TAG, "contours found: " + contours.size());

                            Collections.sort(contours, new Comparator<MatOfPoint>() {
                                @Override
                                public int compare(MatOfPoint o1, MatOfPoint o2) {
                                    return Double.valueOf(Imgproc.contourArea(o2)).compareTo(Imgproc.contourArea(o1));
                                }
                            });

                            Imgproc.drawContours(dilatedImg, contours, 0, new Scalar(255, 255, 250));
                            onNextStep(dilatedImg);
                            dilatedImg.release();

                            rectContour = null;
                            foundPoints = null;

                            for(MatOfPoint contour:contours){
                                MatOfPoint2f mat = new MatOfPoint2f(contour.toArray());
                                double peri = Imgproc.arcLength(mat, true);
                                MatOfPoint2f approx = new MatOfPoint2f();
                                Imgproc.approxPolyDP(mat, approx, 0.02 * peri, true);

                                Point[] points = approx.toArray();
                                Log.d("SCANNER", "approx size " + points.length);

                                if (points.length == 4) {
                                    Point[] spoints = CVProcessor.sortPoints(points);

                                    if (CVProcessor.isInside(spoints, newSize) && CVProcessor.isLargeEnough(spoints, newSize, 0.40)) {
                                        rectContour = contour;
                                        foundPoints = spoints;
                                        break;
                                    }
                                }
                            }

                            if(rectContour != null){
                                Point[] scaledPoints = new Point[foundPoints.length];

                                for(int i = 0; i < foundPoints.length; i++){
                                    scaledPoints[i] = new Point(foundPoints[i].x * ratio, foundPoints[i].y * ratio);
                                }
                                drawRect(img, scaledPoints);
                                onNextStep(img);
                            }

                            img.release();
                        }
                        break;

                }
            }

        }
    }
}
