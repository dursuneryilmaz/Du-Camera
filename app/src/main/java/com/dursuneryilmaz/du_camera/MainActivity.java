package com.dursuneryilmaz.du_camera;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


    }


    public void takePhotoCamera2Api(View view) {
        Intent intent = new Intent(MainActivity.this, Camera2Api.class);
        startActivity(intent);
    }


    public void takePhotoCamera2ApiGoogle(View view) {
        Intent intent = new Intent(MainActivity.this, Camera2ApiV2.class);
        startActivity(intent);
    }


}
