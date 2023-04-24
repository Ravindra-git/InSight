package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.widget.ImageView;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

public class MyActivity extends AppCompatActivity {
    ImageView IV ;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);
        if (! Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }
        Intent intent = getIntent();
      String name = intent.getStringExtra("name");
        File sdCard = Environment.getExternalStorageDirectory();

        File directory = new File (sdCard.getAbsolutePath() + "/Pictures");

        File file = new File(directory, name+".jpg"); //or any other format supported

        FileInputStream streamIn = null;
        try {
            streamIn = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        Bitmap bitmap = BitmapFactory.decodeStream(streamIn);

        try {
            streamIn.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        int x = bitmap.getWidth();
        int y = bitmap.getHeight();
        Matrix matrix = new Matrix();
        matrix.preRotate(90);
        Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap , 0, 0, x, y, matrix, true);
        String bm = getStringImage(rotatedBitmap);
        Python py = Python.getInstance();
        PyObject pyObject = py.getModule("myscript");
        PyObject object2 =  pyObject.callAttr("main",bm);
        System.out.println(object2);
//
//        IV = findViewById(R.id.imageView);
//        IV.setImageBitmap(rotatedBitmap);


    }

    private String getStringImage(Bitmap rotatedBitmap) {
        ByteArrayOutputStream bs = new ByteArrayOutputStream();
        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG,100,bs);
        byte[] imageBytes = bs.toByteArray();
        String encodedImage = android.util.Base64.encodeToString(imageBytes, Base64.DEFAULT);
        return  encodedImage;
    }
}