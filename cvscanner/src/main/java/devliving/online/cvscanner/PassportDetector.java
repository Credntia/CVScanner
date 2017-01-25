package devliving.online.cvscanner;

import android.support.annotation.Nullable;
import android.util.SparseArray;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Size;

/**
 * Created by Mehedi Hasan Khan <mehedi.mailing@gmail.com> on 12/23/16.
 */

public class PassportDetector extends Detector<Document> {

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

        CVProcessor.Quadrilateral quad = CVProcessor.getQuadForPassport(src);
        src.release();

        if(quad != null && quad.points != null){
            quad.points = CVProcessor.getUpscaledPoints(quad.points, CVProcessor.getScaleRatio(imageSize));
            return new Document(frame, quad);
        }

        return null;
    }
}
