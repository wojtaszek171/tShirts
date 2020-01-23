package com.example.myapplication;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.myapplication.CoreSamples.app.ImageTargets.ImageTargets;

import java.io.File;
import java.util.ArrayList;

public class TshirtsCustomsAdapter extends BaseAdapter {
    private ArrayList<String> paths;
    private Context context;
    private LayoutInflater thisInflater;

    public TshirtsCustomsAdapter(Context con, ArrayList<String> paths) {
        this.context = con;
        this.thisInflater = LayoutInflater.from(con);
        this.paths = paths;
    }
    @Override
    public int getCount() {
        return paths.size();
    }

    @Override
    public String getItem(int i) {
        return paths.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if(view == null){
            view = thisInflater.inflate(R.layout.grid_item, viewGroup, false);
            ImageButton tshirtImage = view.findViewById(R.id.tshirtImage);
            tshirtImage.setImageURI(Uri.parse(getItem(i)));
            tshirtImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view2) {
                    context.startActivity(new Intent(context.getApplicationContext(), ImageTargets.class));
                }
            });
            final View finalView = view;
            final int finali = i;
            tshirtImage.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(final View v) {
                    showDialog(finali);

                    return false;
                }
            });
        }
        return view;
    }

    void showDialog(final int i) {
        AlertDialog.Builder alert = new AlertDialog.Builder(context);

        alert.setTitle("Usuń koszulkę");
        alert.setMessage("Czy chcesz usunąć wybrany wzór?");
        alert.setPositiveButton("TAK", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                File fdelete = new File(getItem(i));
                if (fdelete.exists()) {
                    if (fdelete.delete()) {
                        Toast.makeText(context, "Usunięto", Toast.LENGTH_SHORT).show();
                        paths.remove(i);
                        ((TshirtsGalleryActivity)context).refreshAdapter(paths);
                        notifyDataSetChanged();
                    } else {
                        Toast.makeText(context, "Nie można usunąć", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
        alert.setNegativeButton("NIE", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // close dialog
                dialog.cancel();
            }
        });
        alert.show();
    }
}
