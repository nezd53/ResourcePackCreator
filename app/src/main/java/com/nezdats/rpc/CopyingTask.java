package com.nezdats.rpc;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Looper;
import android.util.JsonWriter;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

public class CopyingTask extends AsyncTask<String, String, Boolean> {
    private String vPack, img, ext, nPack, assetFile, altExt;
    private File[] files;
    static final String TAG = "CopyingTask",
            mineDir = Environment.getExternalStorageDirectory() + "/games/com.mojang/resource_packs";

    private AlertDialog.Builder builder;
    private ProgressDialog progressDialog;
    private WeakReference<Activity> weakReference;

    CopyingTask(Activity activity) {
        weakReference = new WeakReference<>(activity);
    }

    /**
     * @param strings strings[0] = String of template pack to use (null if none),
     *                strings[1] = String of path to file to copy,
     *                strings[2] = String name of new pack,
     * @return true if successful, false if an IOException was thrown
     */
    @Override
    protected Boolean doInBackground(String... strings) {
        vPack = strings[0];
        img = strings[1];
        nPack = strings[2];

        boolean isImage = BitmapFactory.decodeFile(img) != null;

        if (img.contains(".")) {
            ext = isImage ? ".png" : ".ogg";
            altExt = isImage ? ".tga" : ".fsb";

            // if a template pack is being used
            if (vPack != null) {
                files = new File(vPack).listFiles();
            }

            assetFile = isImage ? "images.txt" : "sounds.txt";

        } else assetFile = "images.txt";


        try {
            if (isImage) {
                File temp = File.createTempFile("img", ".png");
                temp.deleteOnExit();
                ImageEditor.cropSquare(new File(img), temp);
                img = temp.getAbsolutePath();
            }


            if (vPack != null)
                copyFromTemplate(files, new File(img));

            else copyFromTxt();

            makeManifest();
            Log.i(TAG, "Done, no exceptions caught");
        } catch (IOException e) {
            e.printStackTrace();
            Looper.prepare(); // i needed this so there's no crash i think
            Toast.makeText(weakReference.get(), e.toString(), Toast.LENGTH_LONG).show();
            Log.e(TAG, e.toString());
            return false;
        }

        return true;

    }

    /**
     * Copies files from a template.
     *
     * @param files the array of files returned by File.listFiles() from the root File of the template
     * @param img   a File representation of the file to be copied
     * @throws IOException if there was an error copying the file
     */
    private void copyFromTemplate(File[] files, File img) throws IOException {
        Log.i(TAG, "Copying files using template...");
        String dir = vPack.substring(vPack.lastIndexOf("/") + 1);

        for (File f : files)
            if (f.isDirectory())
                copyFromTemplate(f.listFiles(), img);
            else {
                String fPath = f.getAbsolutePath();
                String fName = fPath.substring(fPath.lastIndexOf("/") + 1);
                if (fName.contains(".") && (fName.substring(fName.lastIndexOf(".")).equals(ext)
                        || fName.substring(fName.lastIndexOf(".")).equals(altExt))) {
                    /*
                    String newPath = f.getAbsolutePath();
                    newPath = newPath.substring(newPath.indexOf(dir) + dir.length() + 1);
                    newPath = mineDir + "/" + nPack + "/" + newPath; */
                    String newPath = mineDir + "/" + nPack + "/" +
                            f.getAbsolutePath().substring(f.getAbsolutePath().indexOf(dir) + dir.length() + 1)
                                    .replace(altExt, ext);
                    //File dirOfNP = new File(newPath.substring(0, newPath.lastIndexOf("/")));
                    //System.out.println(dirOfNP);
                    //if(!dirOfNP.exists()) dirOfNP.mkdirs();
                    //FileUtils.copyFile(img, new File(newPath));
                    copy(img, new File(newPath));
                    //Log.i(TAG, img.getAbsolutePath() + " -> " + newPath);
                    //textView.setText(img.getAbsolutePath() + " -> " + newPath);
                }
            }
    }

    /**
     * Copies the files using the assets file.
     *
     * @throws IOException if there was an error copying the files
     */
    private void copyFromTxt() throws IOException {
        Log.i(TAG, "Copying files from assets file...");
        BufferedReader br = new BufferedReader(new InputStreamReader(
                weakReference.get().getAssets().open(assetFile)));
        File image = new File(img);

        String line = br.readLine();
        while (line != null) {
            File fromLine = new File(mineDir + "/" + nPack + "/" + line);
            copy(image, fromLine);
            //textView.setText(img + " -> " + fromLine);
            Log.i(TAG, img + " -> " + fromLine);
            line = br.readLine();
        }
        br.close();
    }

    /**
     * Makes a manifest.json file for the resource pack.
     *
     * @throws IOException if the JSON document is incomplete or an IO error occurred
     */
    private void makeManifest() throws IOException {
        File f = new File(mineDir + "/" + nPack + "/manifest.json");
        if (f.exists()) {
            Log.i(TAG, "Skipping manifest.json, for it already exists");
            return;
        }

        Log.i(TAG, "Creating manifest.json");

        JsonWriter jw = new JsonWriter(new FileWriter(f));

        jw.setIndent("  ");
        jw.beginObject();
        jw.name("format_version").value(1);
        jw.name("header").beginObject();
        jw.name("description").value(nPack);
        jw.name("name").value(nPack);
        jw.name("uuid").value(UUID.randomUUID().toString());
        jw.name("version").beginArray();
        jw.value(0);
        jw.value(0);
        jw.value(1);
        jw.endArray();
        jw.endObject();
        jw.name("modules").beginArray();
        jw.beginObject();
        jw.name("description").value(nPack);
        jw.name("type").value("resources");
        jw.name("uuid").value(UUID.randomUUID().toString());
        jw.name("version").beginArray();
        jw.value(0);
        jw.value(0);
        jw.value(1);
        jw.endArray();
        jw.endObject();
        jw.endArray();
        jw.endObject();

        jw.close();
    }


    /**
     * Copies a file to a given destination. If the device's SDK version is greater than 26,
     * Files.copy(Path, Path, CopyOption) is used. Otherwise, a FileChannel is used. If File.exists()
     * returns true, it is replaced.
     *
     * @param from the file to be copied
     * @param to   the file to be copied to
     * @throws IOException if an IO error occurred
     */
    void copy(File from, File to) throws IOException {

        publishProgress(to.getName());

        File dirOfTo = to.getParentFile();
        if (!dirOfTo.exists()) dirOfTo.mkdirs();

        if (Build.VERSION.SDK_INT >= 26)
            Files.copy(from.toPath(), to.toPath(), StandardCopyOption.REPLACE_EXISTING);

        else {
            if (to.exists()) to.delete();
            FileChannel fromChannel = new FileInputStream(from).getChannel();

            FileChannel toChannel = new FileOutputStream(to).getChannel();

            toChannel.transferFrom(fromChannel, 0, fromChannel.size());

            fromChannel.close();
            toChannel.close();
        }
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        /*builder = new AlertDialog.Builder(weakReference.get())
                .setMessage("Copying files...")
                .setCancelable(false)
                .setView(R.layout.alert_dialog_progress);

        builder.show();*/

        progressDialog = ProgressDialog.show(weakReference.get(), "Copying files...", "Getting ready...");
        progressDialog.setIndeterminate(false);

    }

    //strings[0] is file name
    @Override
    protected void onProgressUpdate(String... strings) {
        progressDialog.setMessage(strings[0]);
    }

    @Override
    protected void onPostExecute(Boolean success) {
        progressDialog.dismiss();

        if (success)
            new AlertDialog.Builder(weakReference.get())
                    .setTitle("Finished")
                    .setTitle("The files have been copied")
                    .setMessage("Your new resource pack is located in " + Environment.getExternalStorageDirectory().getAbsolutePath() + "/games/com.mojang/resource_packs/")
                    .setPositiveButton("Open Minecraft", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Intent launchIntent = weakReference.get().getApplicationContext().getPackageManager().getLaunchIntentForPackage("com.mojang.minecraftpe");
                            if (launchIntent != null) {
                                weakReference.get().getApplicationContext().startActivity(launchIntent);
                            }
                            else Toast.makeText(weakReference.get().getApplicationContext(), "You don't have it installed...", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Close", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .show();
    }
}