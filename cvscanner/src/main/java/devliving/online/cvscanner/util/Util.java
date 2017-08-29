package devliving.online.cvscanner.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Rect;
import android.net.Uri;
import android.opengl.GLES10;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.media.ExifInterface;
import android.widget.Toast;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by Mehedi Hasan Khan <mehedi.mailing@gmail.com> on 8/20/17.
 */

public final class Util {
    private static final int SIZE_DEFAULT = 2048;
    private static final int SIZE_LIMIT = 4096;

    public static void closeSilently(Closeable c) {
        if (c == null) return;
        try {
            c.close();
        } catch (Throwable t) {
            // Do nothing
        }
    }

    static void showToast(Context context, final String text){
        final Context mcontext = context;
        Handler handler = new Handler(context.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mcontext, text, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static Uri saveImage(Context context, String imageName, @NonNull Mat img){
        Uri imageUri = null;

        File cacheDir = context.getCacheDir();
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }

        File imageFile = new File(cacheDir, imageName);

        Bitmap bitmap = Bitmap.createBitmap((int) img.size().width, (int) img.size().height, Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(img, bitmap);

        FileOutputStream fout = null;
        try {
            fout = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fout);
            fout.flush();

            imageUri = Uri.fromFile(imageFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            closeSilently(fout);
        }

        return imageUri;
    }

    public static int calculateBitmapSampleSize(Context context, Uri bitmapUri) throws IOException {
        InputStream is = null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        try {
            is = context.getContentResolver().openInputStream(bitmapUri);
            BitmapFactory.decodeStream(is, null, options); // Just get image size
        } finally {
            Util.closeSilently(is);
        }

        int maxSize = getMaxImageSize();
        int sampleSize = 1;
        while (options.outHeight / sampleSize > maxSize || options.outWidth / sampleSize > maxSize) {
            sampleSize = sampleSize << 1;
        }

        return sampleSize;
    }

    public static Bitmap loadBitmapFromUri(Context context, int sampleSize, Uri uri) throws FileNotFoundException {
        InputStream is = null;
        Bitmap out = null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = false;
        options.inSampleSize = sampleSize;

        try {
            is = context.getContentResolver().openInputStream(uri);
            out = BitmapFactory.decodeStream(is, null, options);
        } finally {
            Util.closeSilently(is);
        }

        return out;
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth,
                                            int reqHeight, boolean keepAspectRatio)
    {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        final float aspectRatio = (float)height/width;
        int inSampleSize = 1;

        if (reqHeight > 0 && reqWidth > 0) {
            if(keepAspectRatio)
            {
                reqHeight = Math.round(reqWidth * aspectRatio);
            }

            // Calculate ratios of height and width to requested height and width
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);

            // Choose the smallest ratio as inSampleSize value, this will guarantee
            // a final image with both dimensions larger than or equal to the
            // requested height and width.
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }

        return inSampleSize;
    }

    /**
     * Tries to preserve aspect ratio
     * @param context
     * @param imageUri
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    public static Bitmap loadBitmapFromUri(Context context, Uri imageUri, int reqWidth, int reqHeight) {

        InputStream imageStream = null;
        Bitmap image = null;
        try {
            imageStream = context.getContentResolver().openInputStream(imageUri);
            // First decode with inJustDecodeBounds=true to check dimensions
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(imageStream, null, options);

            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight, true);

            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false;
            image = BitmapFactory.decodeStream(imageStream, null, options);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }finally {
            closeSilently(imageStream);
        }

        return image;
    }

    public static int getExifRotation(Context context, Uri imageUri) throws IOException {
        if (imageUri == null) return 0;
        InputStream inputStream = null;
        try {
            inputStream = context.getContentResolver().openInputStream(imageUri);
            ExifInterface exifInterface = new ExifInterface(inputStream);
            // We only recognize a subset of orientation tag values
            switch (exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return 90;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return 180;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return 270;
                default:
                    return ExifInterface.ORIENTATION_UNDEFINED;
            }
        }finally {
            closeSilently(inputStream);
        }
    }

    public static boolean copyExifRotation(Context context, Uri sourceUri, Uri destinationUri) throws IOException {
        if (sourceUri == null || destinationUri == null) return false;
        InputStream srcStream = null;
        InputStream destStream = null;
        try{
            srcStream = context.getContentResolver().openInputStream(sourceUri);
            destStream = context.getContentResolver().openInputStream(destinationUri);
            ExifInterface exifSource = new ExifInterface(srcStream);
            ExifInterface exifDest = new ExifInterface(destStream);
            exifDest.setAttribute(ExifInterface.TAG_ORIENTATION, exifSource.getAttribute(ExifInterface.TAG_ORIENTATION));
            exifDest.saveAttributes();
        }finally {
            closeSilently(srcStream);
            closeSilently(destStream);
        }
        return true;
    }

    public static Bitmap decodeRegionCrop(Context context, Uri sourceUri, Rect rect) {
        Bitmap croppedImage = null;
        InputStream is = null;
        try {
            is = context.getContentResolver().openInputStream(sourceUri);
            BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(is, false);

            try {
                croppedImage = decoder.decodeRegion(rect, new BitmapFactory.Options());

            } catch (IllegalArgumentException e) {
                // Rethrow with some extra information
                throw new IllegalArgumentException("Rectangle " + rect + " is outside of the image", e);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
        } finally {
            closeSilently(is);
        }

        return croppedImage;
    }

    public static Uri saveBitmap(Context context, Bitmap image){
        String filename = "cvscanner_" + System.currentTimeMillis() + "_image.jpg";
        File outputFile = new File(context.getExternalCacheDir(), filename);
        OutputStream outputStream = null;
        boolean success = false;

        try {
            outputStream = new FileOutputStream(outputFile);
            image.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            outputStream.flush();
            success = true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeSilently(outputStream);
        }

        if(success){
            return Uri.fromFile(outputFile);
        }

        return null;
    }

    private static int getMaxImageSize() {
        int textureLimit = getMaxTextureSize();
        if (textureLimit == 0) {
            return SIZE_DEFAULT;
        } else {
            return Math.min(textureLimit, SIZE_LIMIT);
        }
    }

    private static int getMaxTextureSize() {
        // The OpenGL texture size is the maximum size that can be drawn in an ImageView
        int[] maxSize = new int[1];
        GLES10.glGetIntegerv(GLES10.GL_MAX_TEXTURE_SIZE, maxSize, 0);
        return maxSize[0];
    }
}
