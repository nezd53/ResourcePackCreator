package com.nezdats.rpc;

import android.Manifest;
import android.animation.Animator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.Rect;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.FileChannel;

public class MainActivity extends AppCompatActivity {

    // codes
    public static final int CHOOSE_FILE_CODE = 0, CHOOSE_VANILLA_CODE = 1, COPYING_CODE = 2, DO_NOTHING_CODE = -1;
    public static final String requestCode = "requestcode", hasTemplateCode = "hasTemplate",
            vanillaPackPathCode = "vpcode", nameCode = "namecode", filePathCode = "filepath";
    // the switch for using vanilla resource pack as template

    Uri fileUri;

    final static String[] supportedExtensions = {".png", ".jpg", ".jpeg", ".ogg"};

    final String imagesUrl = "https://github.com/funnyvinescompilation/resourcepackcreator/blob/master/images.txt";
    final String soundsUrl = "https://github.com/funnyvinescompilation/resourcepackcreator/blob/master/sounds.txt";

    MediaPlayer mediaPlayer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // check for permissions at app start
        if (!isStoragePermission()) requestStorage();
    }

    boolean isStoragePermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;

    }

    void requestStorage() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
    }

    @Override
    public void onResume() {
        super.onResume();
        new Thread(new Runnable() {
            @Override
            public void run() {

                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                SettingsFragment.setCustomTemplate(sharedPreferences.getBoolean(SettingsFragment.KEY_CUSTOM_TEMPLATE, false));
                SettingsFragment.setResizeImage(sharedPreferences.getBoolean(SettingsFragment.KEY_RESIZE_IMAGE, true));
                SettingsFragment.setShowUnsupported(sharedPreferences.getBoolean(SettingsFragment.KEY_SHOW_UNSUPPORTED, false));


                // toggle vanilla pack selection textview and button visibility with switch
                final TextView textView = findViewById(R.id.vanillapath);
                final Button button = findViewById(R.id.vanillabrowsebutton);
                textView.post(new Runnable() {
                    @Override
                    public void run() {
                        textView.setVisibility(SettingsFragment.isCustomTemplate() ? View.VISIBLE : View.GONE);
                    }
                });
                button.post(new Runnable() {
                    @Override
                    public void run() {
                        button.setVisibility(textView.getVisibility());
                    }
                });
            }
        }).start();

    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();


        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        if(sharedPreferences.getBoolean("firstRun", true)) {
            new AlertDialog.Builder(this)
                    .setTitle("How to use this app")
                    .setMessage("This app will make a Minecraft resource pack that makes every texture and/or sound the same.\n" +
                            "1. Select an image (png, jpeg, etc.) or audio file (only ogg).\n" +
                            "2. Enter a name for the new resource pack.\n" +
                            "3. Press start.\n" +
                            "4. After the files are finished copying, open Minecraft and test the resource pack on a NEW world first to make sure that it doesn't crash. (Don't set is as a global resource pack unless you're absolutely sure.)\n\n" +
                            "For more info, tap the help button in the top-right corner of the app.\n" +
                            "(Use this app at your own risk!)")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }).show();
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("firstRun", false);
            editor.apply();
        }

    }

    public void setFile(View view) {
        if (!isStoragePermission()) {
            requestStorage();
            return;
        }
        /*
        Intent intent = new Intent(this, RecyclerViewFileChooser.class);
        intent.putExtra(requestCode, CHOOSE_FILE_CODE);
        startActivityForResult(intent, CHOOSE_FILE_CODE);
        */

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("*/*");

        startActivityForResult(intent, CHOOSE_FILE_CODE);
    }

    public void setVanilla(View view) {
        if (!isStoragePermission()) {
            requestStorage();
            return;
        }
        Intent intent = new Intent(this, RecyclerViewFileChooser.class);
        intent.putExtra(requestCode, CHOOSE_VANILLA_CODE);
        Toast.makeText(this, "Long-press on a directory to select it", Toast.LENGTH_LONG).show();
        startActivityForResult(intent, CHOOSE_VANILLA_CODE);
    }

    public void startButtonPressed(View view) {
        if (!isStoragePermission()) {
            requestStorage();
            return;
        }
        //TextView filepath = findViewById(R.id.filePath);
        final TextView packname = findViewById(R.id.packName);
        //final String filepathstr = filepath.getText().toString();
        final String packnamestr = packname.getText().toString();


        if (/*filepathstr.length() == 0*/ fileUri == null) {
            Toast.makeText(this, "Specify the file to copy", Toast.LENGTH_SHORT).show();
            return;
        }
        if (packnamestr.length() == 0) {
            Toast.makeText(this, "Specify a name", Toast.LENGTH_SHORT).show();
            return;
        }
        /*if (!new File(filepathstr).exists()) {
            Toast.makeText(this, filepathstr + " doesn't exist", Toast.LENGTH_SHORT).show();
            return;
        }*/

        File copyFileTemp;

        try {
            copyFileTemp = File.createTempFile("file", null);
            copyFileTemp.deleteOnExit();

            ParcelFileDescriptor parcelFileDescriptor =
                    getContentResolver().openFileDescriptor(fileUri, "r");
            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();


            FileChannel fromChannel = new FileInputStream(fileDescriptor).getChannel();

            FileChannel toChannel = new FileOutputStream(copyFileTemp).getChannel();

            toChannel.transferFrom(fromChannel, 0, fromChannel.size());

            fromChannel.close();
            toChannel.close();

        } catch (IOException e) {
            Log.e("Error: ", "oopsie", e);
            Toast.makeText(this, "There was an error...", Toast.LENGTH_SHORT).show();
            return;
        }

        final String filepathstr = copyFileTemp.getAbsolutePath();


        TextView vanillapath = findViewById(R.id.vanillapath);
        String vanillapathstr = vanillapath.getText().toString();
        if (SettingsFragment.isCustomTemplate() && vanillapathstr.length() == 0) {
            Toast.makeText(this, "Specify the resource pack to use as a template", Toast.LENGTH_SHORT).show();
            return;
        } else if (!SettingsFragment.isCustomTemplate())
            vanillapathstr = null;

        final String finalVanillapathstr = vanillapathstr;

        if (!isSupported(fileUri)) {

            final Activity activity = this;
            new AlertDialog.Builder(this)
                    .setMessage("The filetype of this file is not supported. Continue anyway?")
                    .setNegativeButton("YES (NOT RECOMMENDED)", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            CopyingTask copyingTask = new CopyingTask(activity);
                            copyingTask.execute(finalVanillapathstr, filepathstr, packnamestr);
                        }
                    })
                    .setPositiveButton("NO", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    })
                    .setCancelable(true)
                    .show();
        } else {
            CopyingTask copyingTask = new CopyingTask(this);
            copyingTask.execute(finalVanillapathstr, filepathstr, packnamestr);
        }


    }

    static boolean isSupported(String path) {
        if (!path.contains(".")) return false;
        String ext = path.substring(path.lastIndexOf("."));


        for (String str : supportedExtensions)
            if (str.equalsIgnoreCase(ext))
                return true;

        return false;
    }

    boolean isSupported(Uri uri) {
        if (uri == null) return false;
        String mime = getContentResolver().getType(uri);

        Log.i("iSSupported(Uri)", "Mimetype is: " + mime);

        return mime.contains("image/") || mime.contains("/ogg");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case CHOOSE_FILE_CODE: {/*
                    TextView textView = findViewById(R.id.filePath);
                    final String path = intent.getStringExtra("filepath");
                    textView.setText(path);

                    final ImageView imageView = findViewById(R.id.previewImg);

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            final Bitmap bitmap = BitmapFactory.decodeFile(path);

                            imageView.post(new Runnable() {
                                @Override
                                public void run() {
                                    imageView.setImageBitmap(bitmap);

                                    // get the center for the clipping circle
                                    int cx = imageView.getWidth() / 2;
                                    int cy = imageView.getHeight() / 2;

                                    Log.d("HEIGHTS", "x = " + cx + "\ty = " + cy);

                                    // get the final radius for the clipping circle
                                    float finalRadius = (float) Math.hypot(cx, cy);

                                    // create the animator for this view (the start radius is zero)
                                    Animator anim = ViewAnimationUtils.createCircularReveal(imageView, cx, cy, 0f, finalRadius);

                                    // make the view visible and start the animation
                                    imageView.setVisibility(View.VISIBLE);
                                    anim.start();
                                }
                            });
                        }
                    }).start();
*/
                    fileUri = intent.getData();

                    if (mediaPlayer != null) {
                        if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                        mediaPlayer.release();
                        mediaPlayer = null;
                    }

                    final ImageView imageView = findViewById(R.id.previewImg);
                    imageView.setImageBitmap(null);

                    final ProgressBar progressBar = findViewById(R.id.progressBar);
                    progressBar.setVisibility(View.VISIBLE);

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {


                                ParcelFileDescriptor parcelFileDescriptor =
                                        getContentResolver().openFileDescriptor(fileUri, "r");
                                FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                                final Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
                                parcelFileDescriptor.close();


                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (image != null) {
                                            imageView.setOnClickListener(null);
                                            Log.i("MainActiity", "setting image bitmap");
                                            Point point = new Point();
                                            getWindowManager().getDefaultDisplay().getSize(point);
                                            if (image.getWidth() > point.x || image.getHeight() > point.y)
                                                imageView.setImageBitmap(Bitmap.createScaledBitmap(image, point.x, image.getHeight() / (image.getWidth() / point.x), false));
                                            else imageView.setImageBitmap(image);


                                        } else {


                                            mediaPlayer = MediaPlayer.create(getApplicationContext(), fileUri);
                                            if (mediaPlayer != null) {
                                                imageView.setImageResource(R.drawable.play_arrow);
                                                imageView.setOnClickListener(new View.OnClickListener() {
                                                    @Override
                                                    public void onClick(View v) {
                                                        mediaPlayer.start();
                                                    }
                                                });

                                            }
                                        }
                                    }

                                });


                            } catch (IOException e) {
                                Log.e("Error: ", "oopsie", e);
                            } finally {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        progressBar.setVisibility(View.GONE);
                                    }
                                });
                            }

                        }
                    }).start();

                    break;
                }

                case CHOOSE_VANILLA_CODE: {
                    TextView textView = findViewById(R.id.vanillapath);
                    String path = intent.getStringExtra("filepath");
                    textView.setText(path);
                    break;
                }

                case COPYING_CODE: {
                    AlertDialog.Builder alert = new AlertDialog.Builder(this);
                    alert.setTitle("Finished")
                            .setTitle("The files have been copied")
                            .setMessage("Your new resource pack is located in " + Environment.getExternalStorageDirectory().getAbsolutePath() + "/games/com.mojang/resource_packs/")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();
                                }
                            })
                            .show();
                    break;
                }
            }
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main_toolbar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.action_settings: {
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            }

            case R.id.action_help: {
                Intent intent = new Intent(this, HelpActivity.class);
                startActivity(intent);
                return true;
            }

            default:
                return super.onOptionsItemSelected(menuItem);
        }
    }
}


