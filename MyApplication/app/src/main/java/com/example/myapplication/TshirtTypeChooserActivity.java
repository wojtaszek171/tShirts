package com.example.myapplication;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.view.View;

public class TshirtTypeChooserActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tshirt_type_chooser);

        CardView tshirt1 = findViewById(R.id.tshirt1);
        tshirt1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), EditImageActivity.class);
                intent.putExtra("type", "1");
                intent.putExtra("color", "white");

                startActivity(intent);
            }
        });
    }
}
