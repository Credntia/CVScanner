package devliving.online.cvscannersample;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created by user on 9/22/16.
 */
public class StepByStepTestActivity extends AppCompatActivity{

    RecyclerView contentView;
    ImageAdapter mAdapter;

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

        contentView = new RecyclerView(this);
        contentView.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        setContentView(contentView);

        contentView.setLayoutManager(new LinearLayoutManager(this));
        contentView.setHasFixedSize(true);

        mAdapter = new ImageAdapter();
        contentView.setAdapter(mAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(!OpenCVLoader.initDebug()){
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, getApplicationContext(), mCallback);
        }
        else mCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mAdapter.clear();
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

        try {
            Message msg = new Message();
            Mat src = Utils.loadResource(this, R.drawable.tough_sample_1);
            msg.obj = new CVTestMessage(CVCommand.START_BORDER_DETECTION, src);
            testRunner.sendMessage(msg);
        } catch (IOException e) {
            e.printStackTrace();
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

    class ImageAdapter extends RecyclerView.Adapter<ImageViewHolder>{
        List<String> imagePaths = new ArrayList<>();

        /**
         * Called when RecyclerView needs a new {@link ViewHolder} of the given type to represent
         * an item.
         * <p/>
         * This new ViewHolder should be constructed with a new View that can represent the items
         * of the given type. You can either create a new View manually or inflate it from an XML
         * layout file.
         * <p/>
         * The new ViewHolder will be used to display items of the adapter using
         * {@link #onBindViewHolder(ViewHolder, int, List)}. Since it will be re-used to display
         * different items in the data set, it is a good idea to cache references to sub views of
         * the View to avoid unnecessary {@link View#findViewById(int)} calls.
         *
         * @param parent   The ViewGroup into which the new View will be added after it is bound to
         *                 an adapter position.
         * @param viewType The view type of the new View.
         * @return A new ViewHolder that holds a View of the given view type.
         * @see #getItemViewType(int)
         * @see #onBindViewHolder(ViewHolder, int)
         */
        @Override
        public ImageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            ImageView view = new ImageView(parent.getContext());
            ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            view.setLayoutParams(params);
            view.setScaleType(ImageView.ScaleType.FIT_XY);
            view.setBackgroundColor(Color.TRANSPARENT);
            return new ImageViewHolder(view);
        }

        /**
         * Called by RecyclerView to display the data at the specified position. This method should
         * update the contents of the {@link ViewHolder#itemView} to reflect the item at the given
         * position.
         * <p/>
         * Note that unlike {@link ListView}, RecyclerView will not call this method
         * again if the position of the item changes in the data set unless the item itself is
         * invalidated or the new position cannot be determined. For this reason, you should only
         * use the <code>position</code> parameter while acquiring the related data item inside
         * this method and should not keep a copy of it. If you need the position of an item later
         * on (e.g. in a click listener), use {@link ViewHolder#getAdapterPosition()} which will
         * have the updated adapter position.
         * <p/>
         * Override {@link #onBindViewHolder(ViewHolder, int, List)} instead if Adapter can
         * handle efficient partial bind.
         *
         * @param holder   The ViewHolder which should be updated to represent the contents of the
         *                 item at the given position in the data set.
         * @param position The position of the item within the adapter's data set.
         */
        @Override
        public void onBindViewHolder(ImageViewHolder holder, int position) {
            if(position < imagePaths.size()){
                String path = imagePaths.get(position);

                holder.view.setImageBitmap(Utility.getBitmapFromPath(path, holder.view.getWidth(), 0));
            }
        }

        /**
         * Returns the total number of items in the data set held by the adapter.
         *
         * @return The total number of items in this adapter.
         */
        @Override
        public int getItemCount() {
            return imagePaths.size();
        }

        public void add(String path){
            int pos = imagePaths.size();
            imagePaths.add(path);
            notifyItemInserted(pos);
        }

        public void clear(){
            if(imagePaths.size() > 0){
                List<String> paths = new ArrayList(imagePaths);
                imagePaths.clear();
                notifyDataSetChanged();

                for(String path:paths){
                    Utility.deleteFilePermanently(path);
                }
            }
        }
    }

    class ImageViewHolder extends RecyclerView.ViewHolder{
        ImageView view;
        public ImageViewHolder(View itemView) {
            super(itemView);
            if(itemView instanceof ImageView) {
                this.view = (ImageView) itemView;
            }
        }
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
