package com.example.srct.aiscreen;

import android.Manifest;
import android.content.Intent;
//import android.support.v7.app.AppCompatActivity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


public class MainActivity extends AppCompatActivity {
    private final String TAG = "AppCompatActivity";
    private final float PRESS_SIZE = 0.35f;
    private final int REQUEST_CODE = 100;

    Rect r;
    Button text = null;
    ConstraintLayout mBg = null;
    static boolean isBoomed = false;
    private static final int REQUEST_PERMISSION = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mBg = (ConstraintLayout) findViewById(R.id.bg);

        text = (Button)findViewById(R.id.text);
        text.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, REQUEST_CODE);

            }
        });
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // If you do not have permission, request it
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_PHONE_STATE, Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSION);
            Log.d(TAG, "request permission");

        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

       // text.setText("getSize  " + event.getSize() + "getPressure"+ event.getPressure());
        if(!isBoomed && event.getSize() > PRESS_SIZE) {
            Intent intent = new Intent(MainActivity.this, BoomAnimActivity.class);
            intent.putExtra("boom_startx", (int)event.getX());
            intent.putExtra("boom_starty", (int)event.getY());
            startActivity(intent);
            isBoomed = true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onResume() {
        super.onResume();
        isBoomed = false;
        text.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        text.setVisibility(View.GONE);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == RESULT_OK) {
            try {
                Uri uri = data.getData();
                Bitmap bit = BitmapFactory.decodeStream(getContentResolver().openInputStream(uri));
                Drawable drawable = new BitmapDrawable(getResources(),bit);
                mBg.setBackground(drawable);
                text.setVisibility(View.GONE);
            } catch (Exception e) {
                e.printStackTrace();
                Log.d("tag",e.getMessage());
                Toast.makeText(this,"Can't select image",Toast.LENGTH_SHORT).show();
            }


        }
    }
}
