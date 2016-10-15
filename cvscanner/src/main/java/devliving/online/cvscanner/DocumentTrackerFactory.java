package devliving.online.cvscanner;

import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;

import devliving.online.cvscanner.camera.GraphicOverlay;

/**
 * Created by user on 10/15/16.
 */
public class DocumentTrackerFactory implements MultiProcessor.Factory<Document> {
    GraphicOverlay<DocumentGraphic> mOverlay;
    DocumentTracker.DocumentDetectionListener mListener;

    public DocumentTrackerFactory(GraphicOverlay<DocumentGraphic> mOverlay, DocumentTracker.DocumentDetectionListener mListener) {
        this.mOverlay = mOverlay;
        this.mListener = mListener;
    }

    @Override
    public Tracker<Document> create(Document document) {
        DocumentGraphic graphic = new DocumentGraphic(mOverlay, document.getImage().getMetadata().getId(), document);
        return new DocumentTracker(mOverlay, graphic, mListener);
    }
}
