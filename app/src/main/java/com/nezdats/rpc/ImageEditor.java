package com.nezdats.rpc;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * A class of static methods to modify images so that they work with the resource pack.
 */
public class ImageEditor {

    private static int maxRes = 64;

    /**
     * Copies and crops an image to a square where the length == width.
     *
     * @param source the image to be cropped
     * @param dest   the image to be copied to
     * @return true if source was modified and false if it was not.
     * @throws IOException                   if an I/O error occurs.
     * @throws java.io.FileNotFoundException if file exists but is a directory rather than a regular file,
     *                                       does not exist but cannot be created, cannot be opened for any other reason.
     */
    public static boolean cropSquare(File source, File dest) throws IOException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inMutable = true;
        Bitmap sourceBitmap = BitmapFactory.decodeFile(source.getAbsolutePath(), options);
        int x = sourceBitmap.getWidth();
        int y = sourceBitmap.getHeight();
        if ((x > maxRes || y > maxRes) && SettingsFragment.isResizeImage())
            //sourceBitmap = sourceBitmap.copy(Bitmap.Config.ARGB_8888, true);
            sourceBitmap = Bitmap.createScaledBitmap(sourceBitmap, maxRes, maxRes, false);
        else if (x != y) {
            int size = Math.max(x, y);
            sourceBitmap = Bitmap.createScaledBitmap(sourceBitmap, size, size, false);
        }
        convertToFile(sourceBitmap, dest);
        return true;
    }

    /**
     * Copies data from the given Bitmap to the given File.
     *
     * @param bitmap the Bitmap to be copied from.
     * @param file   the File to be copied to.
     * @throws IOException                   if an I/O error occurs.
     * @throws java.io.FileNotFoundException if file exists but is a directory rather than a regular file,
     *                                       does not exist but cannot be created, cannot be opened for any other reason.
     */
    public static void convertToFile(Bitmap bitmap, File file) throws IOException {

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 0, bos);
        byte[] bitmaparr = bos.toByteArray();

        FileOutputStream fos = new FileOutputStream(file);
        fos.write(bitmaparr);
        fos.flush();
        fos.close();
    }
}
