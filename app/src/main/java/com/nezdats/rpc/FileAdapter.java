package com.nezdats.rpc;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.ViewHolder> {

    private File directory;
    private File[] dataset;
    private String[] dataText;

    private RecyclerViewFileChooser obj;

    String TAG = "FileAdapter";

    static class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout linearLayout;
        ImageView imageView;
        TextView textView;

        ViewHolder(LinearLayout v) {
            super(v);
            linearLayout = v;
            textView = v.findViewById(R.id.textView);
            imageView = v.findViewById(R.id.image_view);
        }
    }

    FileAdapter(File f, RecyclerViewFileChooser o) {
        obj = o;

        directory = f;

        dataset = f.listFiles(obj.getFileFilter());
        fileSort(dataset);

        dataText = getNames(dataset);
    }

    private String[] getNames(File[] ar) {
        String[] result = new String[ar.length];
        for (int i = 0; i < ar.length; i++) {
            String str = ar[i].toString();
            result[i] = str.substring(str.lastIndexOf('/') + 1);
        }

        return result;
    }

    @NonNull
    @Override
    public FileAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LinearLayout v = (LinearLayout) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recyclerview_item, parent, false);

        //ViewHolder viewHolder = new ViewHolder(v);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int i) {
        holder.imageView.setImageResource(dataset[i].isDirectory() ? R.drawable.folder : R.drawable.file);

        holder.textView.setText(dataText[i]);

        //holder.imageView.setAnimation(AnimationUtils.loadAnimation(obj, android.R.anim.slide_in_left));
        //holder.textView.setAnimation(AnimationUtils.loadAnimation(obj, android.R.anim.slide_in_left));

        final int index = i;
        holder.linearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeDir(dataset[index]);
            }
        });

        if (obj.isDirOnly()) {
            holder.linearLayout.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    obj.returnFile(dataset[index]);
                    return true;
                }
            });
        }
    }

    private void changeDir(File f) {
        if (f.isDirectory()) {
            directory = f;
            dataset = f.listFiles(obj.getFileFilter());
            fileSort(dataset);
            dataText = getNames(dataset);

            notifyDataSetChanged();

            obj.setDirText(f);
        } else
            obj.returnFile(f);
    }

    @Override
    public int getItemCount() {
        return dataset.length;
    }

    private void fileSort(File[] ar) {
        Arrays.sort(ar, new Comparator<File>() {
            @Override
            public int compare(File file, File t1) {
                return file.toString().compareToIgnoreCase(t1.toString());
            }
        });
    }

    void toParentFile() {
        changeDir(directory.getParentFile());
    }

    public File getFile() {
        return directory;
    }
}

