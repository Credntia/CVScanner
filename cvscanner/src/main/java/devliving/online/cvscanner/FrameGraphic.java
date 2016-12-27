package devliving.online.cvscanner;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.Log;

import devliving.online.cvscanner.camera.GraphicOverlay;

/**
 * Created by Mehedi Hasan Khan <mehedi.mailing@gmail.com> on 12/27/16.
 */

public class FrameGraphic extends GraphicOverlay.Graphic {
    boolean isForPassport = false;
    Paint borderPaint = null;
    int frameWidth = 0;

    public FrameGraphic(GraphicOverlay overlay, boolean isForPassport) {
        super(overlay);
        this.isForPassport = isForPassport;

        borderPaint = new Paint();
        borderPaint.setColor(Color.parseColor("#41fa97"));
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeCap(Paint.Cap.ROUND);
        borderPaint.setStrokeJoin(Paint.Join.ROUND);
        borderPaint.setStrokeWidth(8);
    }

    /**
     * Draw the graphic on the supplied canvas.  Drawing should use the following methods to
     * convert to view coordinates for the graphics that are drawn:
     * <ol>
     * <li>{@link GraphicOverlay.Graphic#scaleX(float)} and {@link GraphicOverlay.Graphic#scaleY(float)} adjust the size of
     * the supplied value from the preview scale to the view scale.</li>
     * <li>{@link GraphicOverlay.Graphic#translateX(float)} and {@link GraphicOverlay.Graphic#translateY(float)} adjust the
     * coordinate from the preview's coordinate system to the view coordinate system.</li>
     * </ol>
     *
     * @param canvas drawing canvas
     */
    @Override
    public void draw(Canvas canvas) {
        float padding = 32;
        float width = canvas.getWidth();
        float height = canvas.getHeight();

        RectF rect = null;

        if(isForPassport){
            float frameHeight;
            float frameWidth;

            if(mOverlay.isPortraitMode()){
                frameWidth = width - (2 * padding);
                frameHeight = frameWidth * CVProcessor.PASSPORT_ASPECT_RATIO;
            }
            else{
                frameHeight = height - (2 * padding);
                frameWidth = height/CVProcessor.PASSPORT_ASPECT_RATIO;
            }

            rect = new RectF(padding, padding, frameWidth, frameHeight);

            float cx = canvas.getWidth()/2.0f;
            float cy = canvas.getHeight()/2.0f;
            float dx = cx - rect.centerX();
            float dy = cy - rect.centerY();
            rect.offset(dx, dy);
        }
        else{
            rect = new RectF(padding, padding, width - padding, height - padding);
        }

        frameWidth = Float.valueOf(rect.width()).intValue();
        Log.d("FRAME-GRAPHIC", "frame width " + frameWidth);
        canvas.drawRoundRect(rect, 8, 8, borderPaint);
    }

    public int getFrameWidth() {
        return frameWidth;
    }
}
