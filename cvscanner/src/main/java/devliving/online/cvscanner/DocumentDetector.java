package devliving.online.cvscanner;

import android.util.SparseArray;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.List;

/**
 * Created by user on 10/15/16.
 */
public class DocumentDetector extends Detector<Document> {

    @Override
    public SparseArray<Document> detect(Frame frame) {
        SparseArray<Document> detections = new SparseArray<>();
        Document doc = detectDocument(frame);

        if(doc != null) detections.append(frame.getMetadata().getId(), doc);

        return detections;
    }

    Document detectDocument(Frame frame){
        Size imageSize = new Size(frame.getMetadata().getWidth(), frame.getMetadata().getHeight());
        Mat src = new Mat(imageSize, CvType.CV_8UC4);
        Utils.bitmapToMat(frame.getBitmap(), src);

        List<MatOfPoint> contours = CVProcessor.findContours(src);

        if(!contours.isEmpty()){
            CVProcessor.Quadrilateral quad = CVProcessor.getQuadrilateral(contours, imageSize);

            if(quad != null){
                quad.points = getUpscaledPoints(quad.points, imageSize);
                return new Document(frame, quad);
            }
        }

        return null;
    }

    Point[] getUpscaledPoints(Point[] points, Size actualSize){
        Point[] rescaledPoints = new Point[4];

        double ratio = CVProcessor.getScaleRatio(actualSize);

        for ( int i=0; i<4 ; i++ ) {
            int x = Double.valueOf(points[i].x*ratio).intValue();
            int y = Double.valueOf(points[i].y*ratio).intValue();
            rescaledPoints[i] = new Point(x, y);
        }

        return rescaledPoints;
    }

    /**
     *
     * @param src - actual image
     * @param pts - points scaled up with respect to actual image
     * @return
     */
    private Mat fourPointTransform( Mat src , Point[] pts ) {
        int height = Double.valueOf(src.size().height).intValue();
        int width = Double.valueOf(src.size().width).intValue();

        Point tl = pts[0];
        Point tr = pts[1];
        Point br = pts[2];
        Point bl = pts[3];

        double widthA = Math.sqrt(Math.pow(br.x - bl.x, 2) + Math.pow(br.y - bl.y, 2));
        double widthB = Math.sqrt(Math.pow(tr.x - tl.x, 2) + Math.pow(tr.y - tl.y, 2));

        double dw = Math.max(widthA, widthB);
        int maxWidth = Double.valueOf(dw).intValue();


        double heightA = Math.sqrt(Math.pow(tr.x - br.x, 2) + Math.pow(tr.y - br.y, 2));
        double heightB = Math.sqrt(Math.pow(tl.x - bl.x, 2) + Math.pow(tl.y - bl.y, 2));

        double dh = Math.max(heightA, heightB);
        int maxHeight = Double.valueOf(dh).intValue();

        Mat doc = new Mat(maxHeight, maxWidth, CvType.CV_8UC4);

        Mat src_mat = new Mat(4, 1, CvType.CV_32FC2);
        Mat dst_mat = new Mat(4, 1, CvType.CV_32FC2);

        src_mat.put(0, 0, tl.x, tl.y, tr.x, tr.y, br.x, br.y, bl.x, bl.y);
        dst_mat.put(0, 0, 0.0, 0.0, dw, 0.0, dw, dh, 0.0, dh);

        Mat m = Imgproc.getPerspectiveTransform(src_mat, dst_mat);

        Imgproc.warpPerspective(src, doc, m, doc.size());

        return doc;
    }
}
