package devliving.online.cvscanner;

import android.util.SparseArray;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.List;

/**
 * Created by user on 10/15/16.
 */
public class DocumentDetector extends Detector<Document> {

    boolean drawBoundingLine = false;

    DocumentDetector(boolean drawTopBorder){
        drawBoundingLine = drawTopBorder;
    }

    @Override
    public SparseArray<Document> detect(Frame frame) {
        SparseArray<Document> detections = new SparseArray<>();
        Document doc = detectDocument(frame);

        if(doc != null) detections.append(frame.getMetadata().getId(), doc);

        return detections;
    }

    Document detectDocument(Frame frame){
        Size imageSize = new Size(frame.getMetadata().getWidth(), frame.getMetadata().getHeight());
        Mat src = new Mat();
        Utils.bitmapToMat(frame.getBitmap(), src);

        List<MatOfPoint> contours = drawBoundingLine? CVProcessor.findContoursAfterClosing(src):CVProcessor.findContours(src);
        src.release();

        if(!contours.isEmpty()){
            CVProcessor.Quadrilateral quad = CVProcessor.getQuadrilateral(contours, imageSize);

            if(quad != null){
                quad.points = CVProcessor.getUpscaledPoints(quad.points, CVProcessor.getScaleRatio(imageSize));
                return new Document(frame, quad);
            }
        }

        return null;
    }
}
