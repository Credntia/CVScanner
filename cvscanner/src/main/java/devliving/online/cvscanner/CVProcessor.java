package devliving.online.cvscanner;

import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Range;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by Mehedi on 9/20/16.
 */
public class CVProcessor {
    final static String TAG = "CV-PROCESSOR";
    final static int FIXED_HEIGHT = 800;

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

    static double getScaleRatio(Size srcSize){
        return srcSize.height/FIXED_HEIGHT;
    }

    public static List<MatOfPoint> findContours(Mat src){
        Mat img = src.clone();
        src.release();
        //find contours
        double ratio = getScaleRatio(img.size());
        int width = (int) (img.size().width / ratio);
        int height = (int) (img.size().height / ratio);
        Size newSize = new Size(width, height);
        Mat resizedImg = new Mat(newSize, CvType.CV_8UC4);
        Imgproc.resize(img, resizedImg, newSize);

        Imgproc.medianBlur(resizedImg, resizedImg, 5);

        Mat cannedImg = new Mat(newSize, CvType.CV_8UC1);
        Imgproc.Canny(resizedImg, cannedImg, 70, 200, 3, true);
        resizedImg.release();

        Imgproc.threshold(cannedImg, cannedImg, 70, 255, Imgproc.THRESH_OTSU);

        Mat dilatedImg = new Mat(newSize, CvType.CV_8UC1);
        Mat morph = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3));
        Imgproc.dilate(cannedImg, dilatedImg, morph, new Point(-1, -1), 2, 1, new Scalar(1));
        cannedImg.release();
        morph.release();

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

        return contours;
    }

    static public Quadrilateral getQuadrilateral(List<MatOfPoint> contours, Size srcSize){
        double ratio = getScaleRatio(srcSize);
        int height = Double.valueOf(srcSize.height / ratio).intValue();
        int width = Double.valueOf(srcSize.width / ratio).intValue();
        Size size = new Size(width,height);

        for ( MatOfPoint c: contours ) {
            MatOfPoint2f c2f = new MatOfPoint2f(c.toArray());
            double peri = Imgproc.arcLength(c2f, true);
            MatOfPoint2f approx = new MatOfPoint2f();
            Imgproc.approxPolyDP(c2f, approx, 0.02 * peri, true);

            Point[] points = approx.toArray();
            Log.d("SCANNER", "approx size: " + points.length);

            // select biggest 4 angles polygon
            if (points.length == 4) {
                Point[] foundPoints = sortPoints(points);

                if (insideArea(foundPoints, size)) {
                    return new Quadrilateral( c , foundPoints );
                }
            }
        }

        return null;
    }

    public static Point[] sortPoints( Point[] src ) {

        ArrayList<Point> srcPoints = new ArrayList<>(Arrays.asList(src));

        Point[] result = { null , null , null , null };

        Comparator<Point> sumComparator = new Comparator<Point>() {
            @Override
            public int compare(Point lhs, Point rhs) {
                return Double.valueOf(lhs.y + lhs.x).compareTo(rhs.y + rhs.x);
            }
        };

        Comparator<Point> diffComparator = new Comparator<Point>() {

            @Override
            public int compare(Point lhs, Point rhs) {
                return Double.valueOf(lhs.y - lhs.x).compareTo(rhs.y - rhs.x);
            }
        };

        // top-left corner = minimal sum
        result[0] = Collections.min(srcPoints, sumComparator);

        // bottom-right corner = maximal sum
        result[2] = Collections.max(srcPoints, sumComparator);

        // top-right corner = minimal diference
        result[1] = Collections.min(srcPoints, diffComparator);

        // bottom-left corner = maximal diference
        result[3] = Collections.max(srcPoints, diffComparator);

        return result;
    }

    public static boolean insideArea(Point[] rp, Size size) {

        int width = Double.valueOf(size.width).intValue();
        int height = Double.valueOf(size.height).intValue();
        int baseMeasure = height/4;

        int bottomPos = height-baseMeasure;
        int topPos = baseMeasure;
        int leftPos = width/2-baseMeasure;
        int rightPos = width/2+baseMeasure;

        return (
                rp[0].x <= leftPos && rp[0].y <= topPos
                        && rp[1].x >= rightPos && rp[1].y <= topPos
                        && rp[2].x >= rightPos && rp[2].y >= bottomPos
                        && rp[3].x <= leftPos && rp[3].y >= bottomPos

        );
    }

    public static class Quadrilateral {
        public MatOfPoint contour;
        public Point[] points;

        public Quadrilateral(MatOfPoint contour, Point[] points) {
            this.contour = contour;
            this.points = points;
        }
    }
}
