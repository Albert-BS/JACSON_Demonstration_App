package com.example.uploadretrieveimage;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

public class FullscreenImageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen_image);

        Button closeButton = findViewById(R.id.closeButton);
        ImageView fullScreenImage = findViewById(R.id.fullScreenImage);
        String imageURL = getIntent().getStringExtra("imageURL");

        Glide.with(this).load(imageURL).into(fullScreenImage);

        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

    }
}