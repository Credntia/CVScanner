package devliving.online.cvscanner;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.annotation.NonNull;

import com.google.android.gms.vision.Frame;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

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

    public void saveDocument(Context context, DocumentSaveCallback callback){
        new DocumentSaveTask(context, callback)
                .execute();
    }

    class DocumentSaveTask extends AsyncTask<Void, Void, String>{
        DocumentSaveCallback mCallback;
        Context mContext;

        public DocumentSaveTask(Context context, DocumentSaveCallback callback){
            super();
            this.mCallback = callback;
            this.mContext = context;
        }

        @Override
        protected void onPreExecute() {
            mCallback.onStartTask();
        }

        /**
         * Override this method to perform a computation on a background thread. The
         * specified parameters are the parameters passed to {@link #execute}
         * by the caller of this task.
         * <p/>
         * This method can call {@link #publishProgress} to publish updates
         * on the UI thread.
         *
         * @param params The parameters of the task.
         * @return A result, defined by the subclass of this task.
         * @see #onPreExecute()
         * @see #onPostExecute
         * @see #publishProgress
         */
        @Override
        protected String doInBackground(Void... params) {
            Size imageSize = new Size(getImage().getMetadata().getWidth(), getImage().getMetadata().getHeight());
            Mat image = new Mat();
            Utils.bitmapToMat(getImage().getBitmap(), image);

            Mat croppedImage = CVProcessor.fourPointTransform(image, detectedQuad.points);
            image.release();

            Mat enhancedImage = CVProcessor.adjustBirghtnessAndContrast(croppedImage, 1);
            croppedImage.release();

            String path = saveImageSecurely(mContext, "image_" + getImage().getMetadata().getTimestampMillis() + ".jpg", enhancedImage);
            enhancedImage.release();

            if(path != null){
                try {
                    ExifInterface exif = new ExifInterface(path);
                    exif.setAttribute("UserComment", "Generated using CVScanner");
                    exif.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(getImage().getMetadata().getRotation()));
                    exif.saveAttributes();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return path;
        }

        void saveTestImage(Bitmap image, String name){
            File dir = new File(Environment.getExternalStorageDirectory(), "/CVScanner/");
            dir.mkdirs();

            File file = new File(dir, name);
            FileOutputStream fOut;
            try {
                if (!file.exists()) {
                    file.createNewFile();
                }
                fOut = new FileOutputStream(file);
                image.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
                fOut.flush();
                fOut.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        String saveImageSecurely(Context context, String imageName, @NonNull Mat img){
            if(img != null) {
                File cacheDir = context.getCacheDir();
                if (!cacheDir.exists()) {
                    cacheDir.mkdirs();
                }

                File imageFile = new File(cacheDir, imageName);

                Bitmap bitmap = Bitmap.createBitmap((int) img.size().width, (int) img.size().height, Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(img, bitmap);
                try {
                    FileOutputStream fout = new FileOutputStream(imageFile);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fout);
                    fout.flush();
                    fout.close();
                    return imageFile.getAbsolutePath();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            mCallback.onSaved(s);
        }
    }

    public interface DocumentSaveCallback{
        void onStartTask();
        void onSaved(String path);
    }
}
