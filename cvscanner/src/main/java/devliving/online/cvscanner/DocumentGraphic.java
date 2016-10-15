package devliving.online.cvscanner;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.shapes.PathShape;

import devliving.online.cvscanner.camera.GraphicOverlay;

/**
 * Created by user on 10/15/16.
 */
public class DocumentGraphic extends GraphicOverlay.Graphic {
    int Id;
    Document scannedDoc;
    Paint borderPaint, bodyPaint;

    public DocumentGraphic(GraphicOverlay overlay, int id, Document doc) {
        super(overlay);
        Id = id;
        scannedDoc = doc;

        borderPaint = new Paint();
        borderPaint.setColor(Color.parseColor("#41fa97"));
        borderPaint.setAlpha(225);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeCap(Paint.Cap.ROUND);
        borderPaint.setStrokeJoin(Paint.Join.ROUND);
        borderPaint.setStrokeWidth(12);

        bodyPaint = new Paint();
        bodyPaint.setColor(Color.parseColor("#69fbad"));
        bodyPaint.setAlpha(200);
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
            path.moveTo(translateX((float) scannedDoc.detectedQuad.points[0].x),
                    translateY((float) scannedDoc.detectedQuad.points[0].y));
            path.lineTo(translateX((float) scannedDoc.detectedQuad.points[1].x),
                    translateY((float) scannedDoc.detectedQuad.points[1].y));
            path.lineTo(translateX((float) scannedDoc.detectedQuad.points[2].x),
                    translateY((float) scannedDoc.detectedQuad.points[2].y));
            path.lineTo(translateX((float) scannedDoc.detectedQuad.points[3].x),
                    translateY((float) scannedDoc.detectedQuad.points[3].y));
            path.close();

            PathShape box = new PathShape(path, scaleX(scannedDoc.image.getMetadata().getWidth()),
                    scaleY(scannedDoc.image.getMetadata().getHeight()));
            box.draw(canvas, borderPaint);
            box.draw(canvas, bodyPaint);
        }
    }
}
