package devliving.online.cvscannersample;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by user on 9/23/16.
 */
public class Utility {

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight, boolean keepAspectRatio)
    {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        final float aspectRatio = (float)height/width;
        int inSampleSize = 1;

        if(keepAspectRatio)
        {
            reqHeight = Math.round(reqWidth * aspectRatio);
        }

        if (reqHeight > 0 && reqWidth > 0) {

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

    public static Bitmap getBitmapFromPath(String path, int width, int height)
    {
        if(path != null) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, options);

            int sampleSize = calculateInSampleSize(options, width, height, true);
            options.inJustDecodeBounds = false;
            options.inSampleSize = sampleSize;

            return BitmapFactory.decodeFile(path, options);
        }

        return null;
    }

    public static String saveBitmapJPG(Bitmap img, String imageName) {
        File dir = new File(Environment.getExternalStorageDirectory(), "/" + "CVScannerSample" + "/");
        dir.mkdirs();

        File file = new File(dir, imageName);
        FileOutputStream fOut;
        try {
            if(!file.exists())
            {
                file.createNewFile();
            }
            fOut = new FileOutputStream(file);
            img.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
            fOut.flush();
            fOut.close();
            return file.getAbsolutePath();
        } catch (FileNotFoundException e) {
           e.printStackTrace();
        } catch (IOException e) {
           e.printStackTrace();
        }

        return null;
    }

    public static boolean deleteFilePermanently(String filePath){
        if(filePath != null) {
            File file = new File(filePath);

            if (file.exists()) {
                if (file.delete()) {
                    return true;
                } else {
                    return file.getAbsoluteFile().delete();
                }
            }
        }

        return false;
    }
}
