package devliving.online.cvscanner;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.shapes.PathShape;
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
            Path path = new Path();

            float tlX = translateY((float) scannedDoc.detectedQuad.points[0].y);
            float tlY = translateX((float) scannedDoc.detectedQuad.points[0].x);

            Log.d("DOC-GRAPHIC", "Top left: x: " + scannedDoc.detectedQuad.points[0].x + ", y: " + scannedDoc.detectedQuad.points[0].y
            + " -> x: " + tlX + ", y: " + tlY);

            float trX = translateY((float) scannedDoc.detectedQuad.points[1].y);
            float trY = translateX((float) scannedDoc.detectedQuad.points[1].x);

            Log.d("DOC-GRAPHIC", "Top right: x: " + scannedDoc.detectedQuad.points[1].x + ", y: " + scannedDoc.detectedQuad.points[1].y
                    + " -> x: " + trX + ", y: " + trY);

            float brX = translateY((float) scannedDoc.detectedQuad.points[2].y);
            float brY = translateX((float) scannedDoc.detectedQuad.points[2].x);

            Log.d("DOC-GRAPHIC", "Bottom right: x: " + scannedDoc.detectedQuad.points[2].x + ", y: " + scannedDoc.detectedQuad.points[2].y
                    + " -> x: " + brX + ", y: " + brY);

            float blX = translateY((float) scannedDoc.detectedQuad.points[3].y);
            float blY = translateX((float) scannedDoc.detectedQuad.points[3].x);

            Log.d("DOC-GRAPHIC", "Bottom left: x: " + scannedDoc.detectedQuad.points[3].x + ", y: " + scannedDoc.detectedQuad.points[3].y
                    + " -> x: " + blX + ", y: " + blY);

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
