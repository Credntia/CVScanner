package devliving.online.cvscanner;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.Log;

import devliving.online.cvscanner.camera.GraphicOverlay;

/**
 * Created by user on 10/15/16.
 */
public class DocumentGraphic extends GraphicOverlay.Graphic {
    int Id;
    Document scannedDoc;
    Paint borderPaint, bodyPaint;

    public DocumentGraphic(GraphicOverlay overlay, Document doc) {
        super(overlay);
        scannedDoc = doc;

        borderPaint = new Paint();
        borderPaint.setColor(Color.parseColor("#41fa97"));
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeCap(Paint.Cap.ROUND);
        borderPaint.setStrokeJoin(Paint.Join.ROUND);
        borderPaint.setStrokeWidth(12);

        bodyPaint = new Paint();
        bodyPaint.setColor(Color.parseColor("#69fbad"));
        bodyPaint.setAlpha(180);
        bodyPaint.setStyle(Paint.Style.FILL);
    }

    public int getId() {
        return Id;
    }

    public void setId(int id) {
        Id = id;
    }

    void update(Document doc){
        scannedDoc = doc;
        postInvalidate();
    }

    /**
     * Draw the graphic on the supplied canvas.  Drawing should use the following methods to
     * convert to view coordinates for the graphics that are drawn:
     * <ol>
     * <li>{@link Graphic#scaleX(float)} and {@link Graphic#scaleY(float)} adjust the size of
     * the supplied value from the preview scale to the view scale.</li>
     * <li>{@link Graphic#translateX(float)} and {@link Graphic#translateY(float)} adjust the
     * coordinate from the preview's coordinate system to the view coordinate system.</li>
     * </ol>
     *
     * @param canvas drawing canvas
     */
    @Override
    public void draw(Canvas canvas) {
        if(scannedDoc != null && scannedDoc.detectedQuad != null){
            boolean isPortrait = mOverlay.isPortraitMode();
            Path path = new Path();

            float tlX = isPortrait? translateY((float) scannedDoc.detectedQuad.points[0].y):translateX((float) scannedDoc.detectedQuad.points[0].x);
            float tlY = isPortrait? translateX((float) scannedDoc.detectedQuad.points[0].x):translateY((float) scannedDoc.detectedQuad.points[0].y);

            Log.d("DOC-GRAPHIC", "Top left: x: " + scannedDoc.detectedQuad.points[0].y + ", y: " + scannedDoc.detectedQuad.points[0].x
                    + " -> x: " + tlX + ", y: " + tlY);

            float blX = isPortrait? translateY((float) scannedDoc.detectedQuad.points[1].y):translateX((float) scannedDoc.detectedQuad.points[1].x);
            float blY = isPortrait? translateX((float) scannedDoc.detectedQuad.points[1].x):translateY((float) scannedDoc.detectedQuad.points[1].y);

            Log.d("DOC-GRAPHIC", "Bottom left: x: " + scannedDoc.detectedQuad.points[1].y + ", y: " + scannedDoc.detectedQuad.points[1].x
                    + " -> x: " + blX + ", y: " + blY);

            float brX = isPortrait? translateY((float) scannedDoc.detectedQuad.points[2].y):translateX((float) scannedDoc.detectedQuad.points[2].x);
            float brY = isPortrait? translateX((float) scannedDoc.detectedQuad.points[2].x):translateY((float) scannedDoc.detectedQuad.points[2].y);

            Log.d("DOC-GRAPHIC", "Bottom right: x: " + scannedDoc.detectedQuad.points[2].y + ", y: " + scannedDoc.detectedQuad.points[2].x
                    + " -> x: " + brX + ", y: " + brY);

            float trX = isPortrait? translateY((float) scannedDoc.detectedQuad.points[3].y):translateX((float) scannedDoc.detectedQuad.points[3].x);
            float trY = isPortrait? translateX((float) scannedDoc.detectedQuad.points[3].x):translateY((float) scannedDoc.detectedQuad.points[3].y);

            Log.d("DOC-GRAPHIC", "Top right: x: " + scannedDoc.detectedQuad.points[3].y + ", y: " + scannedDoc.detectedQuad.points[3].x
                    + " -> x: " + trX + ", y: " + trY);

            path.moveTo(tlX, tlY);
            path.lineTo(trX, trY);
            path.lineTo(brX, brY);
            path.lineTo(blX, blY);
            path.close();

            canvas.drawPath(path, borderPaint);
            canvas.drawPath(path, bodyPaint);

            Log.d("DOC-GRAPHIC", "DONE DRAWING");
        }
    }
}
