package devliving.online.cvscanner;

import com.google.android.gms.vision.Frame;

/**
 * Created by Mehedi on 10/15/16.
 *
 * Holds the actual image data. Quad point are also scaled with respect to actual image.
 */
public class Document {
    Frame image;
    CVProcessor.Quadrilateral detectedQuad;

    public Document(Frame image, CVProcessor.Quadrilateral detectedQuad) {
        this.image = image;
        this.detectedQuad = detectedQuad;
    }

    public Frame getImage() {
        return image;
    }

    public void setImage(Frame image) {
        this.image = image;
    }

    public CVProcessor.Quadrilateral getDetectedQuad() {
        return detectedQuad;
    }

    public void setDetectedQuad(CVProcessor.Quadrilateral detectedQuad) {
        this.detectedQuad = detectedQuad;
    }
}
