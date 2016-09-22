package devliving.online.cvscannersample;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PersistableBundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Range;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;

/**
 * Created by user on 9/22/16.
 */
public class StepByStepTestActivity extends AppCompatActivity{

    LinearLayout contentView;

    BaseLoaderCallback mCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            super.onManagerConnected(status);

            startTests();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView parentView = new ScrollView(this);
        parentView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        contentView = new LinearLayout(this);
        contentView.setOrientation(LinearLayout.VERTICAL);
        contentView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        parentView.addView(contentView);

        setContentView(parentView);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(!OpenCVLoader.initDebug()){
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, getApplicationContext(), mCallback);
        }
        else mCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
    }

    void startTests(){
        if(contentView.getChildCount() > 0) {
            contentView.removeAllViews();
        }

        HandlerThread imgThread = new HandlerThread("Image processor thread");
        imgThread.start();

        CVTestRunner testRunner = new CVTestRunner(imgThread.getLooper());

        try {
            Message msg = new Message();
            Mat src = Utils.loadResource(this, R.drawable.passport);
            msg.obj = new CVTestMessage(CVCommand.START_BORDER_DETECTION, src);
            testRunner.sendMessage(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void onNextStep(Mat img){
        final Bitmap result = Bitmap.createBitmap(img.cols(), img.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(img, result);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ImageView view = new ImageView(StepByStepTestActivity.this);
                view.setScaleType(ImageView.ScaleType.FIT_XY);
                view.setBackgroundColor(Color.TRANSPARENT);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                params.setMargins(8, 8, 8, 8);
                view.setLayoutParams(params);
                contentView.addView(view);

                view.setImageBitmap(result);
            }
        });
    }

    enum CVCommand {
        START_BORDER_DETECTION;
    }

    class CVTestMessage {
        CVCommand command;
        Mat input;

        public CVTestMessage(CVCommand cmd, Mat input){
            command = cmd;
            this.input = input;
        }
    }

    class CVTestRunner extends Handler{
        final String TAG = "CV-TEST";

        public CVTestRunner(Looper looper){
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {

            if(msg.obj != null && msg.obj instanceof CVTestMessage){
                CVTestMessage data = (CVTestMessage) msg.obj;

                switch (data.command){
                    case START_BORDER_DETECTION:
                        Log.d(TAG, "*** --> Processing start.");

                        Mat src = data.input.clone();
                        Log.d(TAG, "1 original: " + src.toString());
                        onNextStep(src);
                        Imgproc.GaussianBlur(src, src, new Size(3, 3), 0);
                        Log.d(TAG, "2.1 --> Gaussian blur done\n blur: " + src.toString());
                        onNextStep(src);
                        Imgproc.cvtColor(src, src, Imgproc.COLOR_RGBA2GRAY);
                        Log.d(TAG, "2.2 --> Grayscaling done\n gray: " + src.toString());
                        onNextStep(src);

                        Mat sobelX = new Mat();
                        Mat sobelY = new Mat();

                        Imgproc.Sobel(src, sobelX, CvType.CV_32FC1, 2, 0, 5, 1, 0);
                        Log.d(TAG, "3.1 --> Sobel done.\n X: " + sobelX.toString());
                        Imgproc.Sobel(src, sobelY, CvType.CV_32FC1, 0, 2, 5, 1, 0);
                        Log.d(TAG, "3.2 --> Sobel done.\n Y: " + sobelY.toString());

                        Mat sum_img = new Mat();
                        Core.addWeighted(sobelX, 0.5, sobelY, 0.5, 0.5, sum_img);
                        //Core.add(sobelX, sobelY, sum_img);
                        Log.d(TAG, "4 --> Addition done. sum: " + sum_img.toString());

                        sobelX.release();
                        sobelY.release();

                        Mat gray = new Mat();
                        Core.normalize(sum_img, gray, 0, 255, Core.NORM_MINMAX, CvType.CV_8UC1);
                        Log.d(TAG, "5 --> Normalization done. gray: " + gray.toString());
                        onNextStep(gray);
                        sum_img.release();

                        Mat row_proj = new Mat();
                        Mat col_proj = new Mat();
                        Core.reduce(gray, row_proj, 1, Core.REDUCE_AVG, CvType.CV_8UC1);
                        Log.d(TAG, "6.1 --> Reduce done. row: " + row_proj.toString());
                        onNextStep(row_proj);
                        Core.reduce(gray, col_proj, 0, Core.REDUCE_AVG, CvType.CV_8UC1);
                        Log.d(TAG, "6.2 --> Reduce done. col: " + col_proj.toString());
                        onNextStep(col_proj);
                        gray.release();

                        Imgproc.Sobel(row_proj, row_proj, CvType.CV_8UC1, 0, 2);
                        Log.d(TAG, "7.1 --> Sobel done. row: " + row_proj.toString());
                        onNextStep(row_proj);
                        Imgproc.Sobel(col_proj, col_proj, CvType.CV_8UC1, 2, 0);
                        Log.d(TAG, "7.2 --> Sobel done. col: " + col_proj.toString());
                        onNextStep(col_proj);

                        Rect result = new Rect();

                        int half_pos = (int) (row_proj.total()/2);
                        Mat row_sub = new Mat(row_proj, new Range(0, half_pos), new Range(0, 1));
                        Log.d(TAG, "8.1 --> Copy sub matrix done. row: " + row_sub.toString());
                        result.y = (int) Core.minMaxLoc(row_sub).maxLoc.y;
                        Log.d(TAG, "8.2 --> Minmax done. Y: " + result.y);
                        row_sub.release();
                        Mat row_sub2 = new Mat(row_proj, new Range(half_pos, (int) row_proj.total()), new Range(0, 1));
                        Log.d(TAG, "8.3 --> Copy sub matrix done. row: " + row_sub2.toString());
                        result.height = (int) (Core.minMaxLoc(row_sub2).maxLoc.y + half_pos - result.y);
                        Log.d(TAG, "8.4 --> Minmax done. Height: " + result.height);
                        row_sub2.release();

                        half_pos = (int) (col_proj.total()/2);
                        Mat col_sub = new Mat(col_proj, new Range(0, 1), new Range(0, half_pos));
                        Log.d(TAG, "9.1 --> Copy sub matrix done. col: " + col_sub.toString());
                        result.x = (int) Core.minMaxLoc(col_sub).maxLoc.x;
                        Log.d(TAG, "9.2 --> Minmax done. X: " + result.x);
                        col_sub.release();
                        Mat col_sub2 = new Mat(col_proj, new Range(0, 1), new Range(half_pos, (int) col_proj.total()));
                        Log.d(TAG, "9.3 --> Copy sub matrix done. col: " + col_sub2.toString());
                        result.width = (int) (Core.minMaxLoc(col_sub2).maxLoc.x + half_pos - result.x);
                        Log.d(TAG, "9.4 --> Minmax done. Width: " + result.width);
                        col_sub2.release();

                        row_proj.release();
                        col_proj.release();
                        src.release();
                        Imgproc.rectangle(data.input, new Point(result.x, result.y),
                                new Point(result.x + result.width, result.y + result.height), new Scalar(0, 255, 200, 255), 8);
                        onNextStep(data.input);
                        data.input.release();
                        Log.d(TAG, "*** --> Processing done.");
                        break;
                }
            }

        }
    }
}
