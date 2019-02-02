package com.example.myapplication;

import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;

import java.io.File;
import java.util.ArrayList;

public class TshirtsGalleryActivity extends AppCompatActivity {
    GridView grid;
    TshirtsCustomsAdapter myAdapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tshirts_gallery);

        grid = (GridView) findViewById(R.id.shirtsGrid);

        String path = Environment.getExternalStorageDirectory() + File.separator + "fittingroom";

        File directory = new File(path);
        File[] filesNames = directory.listFiles();
        ArrayList<String> files = new ArrayList<>();
        for (int i = 0; i < filesNames.length; i++)
        {
            files.add(path + "/" + filesNames[i].getName());
        }

        myAdapter = new TshirtsCustomsAdapter(this, files);

        grid.setAdapter(myAdapter);

    }

    public void refreshAdapter(ArrayList<String> newFiles){
        myAdapter = new TshirtsCustomsAdapter(this, newFiles);
        grid.setAdapter(myAdapter);
    }
}
