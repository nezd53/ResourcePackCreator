package com.nezdats.rpc;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.TextView;

import java.io.File;
import java.io.FileFilter;

public class RecyclerViewFileChooser extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FileAdapter adapter;
    private RecyclerView.LayoutManager layoutManager;

    private FileFilter fileFilter;
    private boolean dirOnly;

    private File directory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recycler_view_file_chooser);
        recyclerView = findViewById(R.id.recycler_view);

        Intent intent = getIntent();

        directory = Environment.getExternalStorageDirectory();

        switch (intent.getIntExtra(MainActivity.requestCode, -1)) {
            case MainActivity.CHOOSE_FILE_CODE:
                dirOnly = false;
                fileFilter = new FileFilter() {
                    @Override
                    public boolean accept(File file) {
                        return SettingsFragment.isShowUnsupported() ||
                                file.isDirectory() || MainActivity.isSupported(file.getAbsolutePath());
                    }
                };
                break;

            case MainActivity.CHOOSE_VANILLA_CODE:
                dirOnly = true;
                fileFilter = new FileFilter() {
                    @Override
                    public boolean accept(File file) {
                        return file.isDirectory();
                    }
                };

                File packDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + CopyingTask.mineDir);
                if(packDir.exists()) directory = packDir;

                break;

            default:
                dirOnly = false;
                fileFilter = new FileFilter() {
                    @Override
                    public boolean accept(File file) {
                        return true;
                    }
                };
        }


        setDirText(directory);

        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        adapter = new FileAdapter(directory, this);
        recyclerView.setAdapter(adapter);

    }

    public FileFilter getFileFilter() {
        return fileFilter;
    }

    public void returnFile(File f) {
        Intent intent = new Intent();
        intent.putExtra("filepath", f.getAbsolutePath());
        setResult(Activity.RESULT_OK, intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (adapter.getFile().equals(Environment.getExternalStorageDirectory())) {
            Intent intent = new Intent();
            setResult(Activity.RESULT_CANCELED, intent);
            super.onBackPressed();
        } else adapter.toParentFile();
    }

    public void setDirText(File f) {
        TextView t = findViewById(R.id.textView);
        t.setText(f.toString());
    }

    public boolean isDirOnly() {
        return dirOnly;
    }
}
