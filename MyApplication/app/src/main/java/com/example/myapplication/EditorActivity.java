package com.example.myapplication;

import android.graphics.Typeface;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import ja.burhanrashid52.photoeditor.PhotoEditor;
import ja.burhanrashid52.photoeditor.PhotoEditorView;

public class EditorActivity extends AppCompatActivity {

    PhotoEditor mPhotoEditor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        PhotoEditorView mPhotoEditorView = findViewById(R.id.photoEditorView);

        String type = getIntent().getStringExtra("type");
        String color = getIntent().getStringExtra("color");


        if(type.equals("1")){
            switch (color){
                case "white":
                    mPhotoEditorView.getSource().setImageResource(R.drawable.tshirt1_white);
                    break;
            }
        }else
        if(type.equals("2")){
            switch (color){
                case "white":
                    mPhotoEditorView.getSource().setImageResource(R.drawable.tshirt2_white);
                    break;
            }
        }else
        if(type.equals("3")){
            switch (color){
                case "white":
                    mPhotoEditorView.getSource().setImageResource(R.drawable.tshirt3_white);
                    break;
            }
        }else
        if(type.equals("4")){
            switch (color){
                case "white":
                    mPhotoEditorView.getSource().setImageResource(R.drawable.tshirt4_white);
                    break;
            }
        }


        //Use custom font using latest support library
        //Typeface mTextRobotoTf = ResourcesCompat.getFont(this, R.font.roboto_medium);

        //loading font from assest
        //Typeface mEmojiTypeFace = Typeface.createFromAsset(getAssets(), "emojione-android.ttf");

        mPhotoEditor = new PhotoEditor.Builder(this, mPhotoEditorView)
                .setPinchTextScalable(true)
//                .setDefaultTextTypeface(mTextRobotoTf)
//                .setDefaultEmojiTypeface(mEmojiTypeFace)
                .build();

        mPhotoEditor.setBrushDrawingMode(true);
    }
}
