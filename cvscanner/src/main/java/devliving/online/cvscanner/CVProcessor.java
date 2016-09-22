package devliving.online.cvscanner;

import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Range;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

/**
 * Created by Mehedi on 9/20/16.
 */
public class CVProcessor {
    final static String TAG = "CV-PROCESSOR";

    public static Rect detectBorder(Mat original){
        Mat src = original.clone();
        Log.d(TAG, "1 original: " + src.toString());

        Imgproc.GaussianBlur(src, src, new Size(3, 3), 0);
        Log.d(TAG, "2.1 --> Gaussian blur done\n blur: " + src.toString());

        Imgproc.cvtColor(src, src, Imgproc.COLOR_RGBA2GRAY);
        Log.d(TAG, "2.2 --> Grayscaling done\n gray: " + src.toString());

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
        sum_img.release();

        Mat row_proj = new Mat();
        Mat col_proj = new Mat();
        Core.reduce(gray, row_proj, 1, Core.REDUCE_AVG, CvType.CV_8UC1);
        Log.d(TAG, "6.1 --> Reduce done. row: " + row_proj.toString());

        Core.reduce(gray, col_proj, 0, Core.REDUCE_AVG, CvType.CV_8UC1);
        Log.d(TAG, "6.2 --> Reduce done. col: " + col_proj.toString());
        gray.release();

        Imgproc.Sobel(row_proj, row_proj, CvType.CV_8UC1, 0, 2);
        Log.d(TAG, "7.1 --> Sobel done. row: " + row_proj.toString());

        Imgproc.Sobel(col_proj, col_proj, CvType.CV_8UC1, 2, 0);
        Log.d(TAG, "7.2 --> Sobel done. col: " + col_proj.toString());

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

        return result;
    }
}
