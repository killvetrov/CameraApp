package com.killvetrov.cameraapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by Killvetrov on 02-Nov-15.
 */
public class SaveImageTask extends AsyncTask<byte[], Void, String> {

    public final static File FILE_SD_CARD = Environment
            .getExternalStorageDirectory();
    public final static String DIR_PROJECT = "MyApp";

    private Context context;
    private OnImageSavedListener imgListener;

    public SaveImageTask(Context context, OnImageSavedListener imgListener) {
        this.context = context;
        this.imgListener = imgListener;
    }

    @SuppressLint("DefaultLocale")
    @Override
    protected String doInBackground(byte[]... data) {
        FileOutputStream outStream = null;

        try {
            File dir = new File(FILE_SD_CARD.getAbsolutePath() + "/"
                    + DIR_PROJECT);

            if (!dir.exists())
                dir.mkdirs();

            String fileName = String.format("%d.jpg",
                    System.currentTimeMillis() / 1000);
            File outFile = new File(dir, fileName);

            outStream = new FileOutputStream(outFile);
            outStream.write(data[0]);
            outStream.flush();
            outStream.close();

            refreshGallery(context, outFile);

            return outFile.getAbsolutePath();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
        }
        return null;
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        if (imgListener != null)
            imgListener.onImageSaved(result);
    }

    public static void refreshGallery(Context context, File file) {
        try {
            Intent mediaScanIntent = new Intent(
                    Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            mediaScanIntent.setData(Uri.fromFile(file));
            context.sendBroadcast(mediaScanIntent);
        } catch (Exception e) {
        }
    }

}