package com.example.myapplication;


import androidx.annotation.NonNull;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;

import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;

import androidx.camera.core.Preview;

import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;

import androidx.core.content.ContextCompat;

import androidx.lifecycle.LifecycleOwner;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;

import android.os.Environment;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.util.Base64;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.google.common.util.concurrent.ListenableFuture;

import org.tensorflow.lite.examples.classification.CameraActivity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

public class MainActivity extends AppCompatActivity{
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    PreviewView previewView;
    private ImageCapture imageCapture;

    private static final int SWIPE_DISTANCE_THRESHOLD = 100;
    private static final int SWIPE_VELOCITY_THRESHOLD = 100;
    TextToSpeech t1;
    private Button bCapture,bCapture1,bCapture2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (! Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }
        previewView = findViewById(R.id.previewView);
        bCapture = findViewById(R.id.bCapture);
        bCapture1 = findViewById(R.id.bCapture1);
        bCapture2 = findViewById(R.id.bCapture2);
        t1 = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                if(i!=TextToSpeech.ERROR){
                    t1.setLanguage(Locale.ENGLISH);
                    t1.speak("Bills",TextToSpeech.QUEUE_ADD,null,null);

                }
            }
        });

        bCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(MainActivity.this, "Currency", Toast.LENGTH_SHORT).show();
//                Intent intent = new Intent(MainActivity.this,MainActivity3.class);
//
//                startActivity(intent);
                Thread objThread1 = new Thread(objRun1);
                objThread1.start();
            }
        });

        bCapture2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(MainActivity.this, "Medicines", Toast.LENGTH_SHORT).show();
               Thread objThread = new Thread(objRun);
               objThread.start();
                try {
                    objThread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        previewView.setOnTouchListener(new View.OnTouchListener() {
            private GestureDetector gestureDetector = new GestureDetector(MainActivity.this, new GestureDetector.SimpleOnGestureListener() {

                @Override
                public boolean onSingleTapConfirmed(@NonNull MotionEvent motionEvent) {
                    return false;
                }

                @Override
                public boolean onDoubleTap(@NonNull MotionEvent motionEvent) {
                    Toast.makeText(MainActivity.this, "double tap.", Toast.LENGTH_SHORT).show();
                    capturePhoto();

                    return super.onDoubleTap(motionEvent);
                }

                @Override
                public boolean onDoubleTapEvent(@NonNull MotionEvent motionEvent) {
                    return false;
                }

                @Override
                public boolean onDown(@NonNull MotionEvent motionEvent) {
                    return false;
                }

                @Override
                public void onShowPress(@NonNull MotionEvent motionEvent) {

                }

                @Override
                public boolean onSingleTapUp(@NonNull MotionEvent motionEvent) {
                    return false;
                }

                @Override
                public boolean onScroll(@NonNull MotionEvent motionEvent, @NonNull MotionEvent motionEvent1, float v, float v1) {
                    return false;
                }

                @Override
                public void onLongPress(@NonNull MotionEvent motionEvent) {

                }

                @Override
                public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                    float distanceX = e2.getX() - e1.getX();
                    float distanceY = e2.getY() - e1.getY();
                    if (Math.abs(distanceX) > Math.abs(distanceY) && Math.abs(distanceX) > SWIPE_DISTANCE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (distanceX > 0)
                            onSwipeRight();
                        else
                            onSwipeLeft();
                        return true;
                    }
                    return false;
                }
            });
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                Log.d("TEST", "Raw event: " + motionEvent.getAction() + ", (" + motionEvent.getRawX() + ", " + motionEvent.getRawY() + ")");
                gestureDetector.onTouchEvent(motionEvent);
                return true;            }
        });


            cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                startCameraX(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, getExecutor());

    }
    Runnable objRun = new Runnable() {
        @Override
        public void run() {

            Intent intent = new Intent(MainActivity.this,MainActivity4.class);

            startActivity(intent);

        }
    };
    Runnable objRun1 = new Runnable() {
        @Override
        public void run() {
            Intent intent = new Intent(MainActivity.this, org.tensorflow.lite.examples.classification.ClassifierActivity.class);
            startActivity(intent);

        }
    };
    private void onSwipeLeft() {
        Toast.makeText(MainActivity.this, "Medicines", Toast.LENGTH_SHORT).show();
        Thread objThread = new Thread(objRun);
        objThread.start();
        try {
            objThread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    private void onSwipeRight() {
        Toast.makeText(MainActivity.this, "Currency", Toast.LENGTH_SHORT).show();
//                Intent intent = new Intent(MainActivity.this,MainActivity3.class);
//
//                startActivity(intent);
        Thread objThread1 = new Thread(objRun1);
        objThread1.start();


    }

    Executor getExecutor() {
        return ContextCompat.getMainExecutor(this);
    }

    @SuppressLint("RestrictedApi")
    private void startCameraX(ProcessCameraProvider cameraProvider) {
        cameraProvider.unbindAll();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();
        Preview preview = new Preview.Builder()
                .build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        // Image capture use case
        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build();

        //bind to lifecycle:
        cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, preview, imageCapture);
    }



    private void capturePhoto() {
        long timestamp = System.currentTimeMillis();

        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, timestamp);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");



        imageCapture.takePicture(
                new ImageCapture.OutputFileOptions.Builder(
                        getContentResolver(),
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        contentValues
                ).build(),
                getExecutor(),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                       Toast.makeText(MainActivity.this, "Photo has been saved successfully.", Toast.LENGTH_SHORT).show();
                        //Intent intent = new Intent(MainActivity.this,MyActivity.class);
                        //intent.putExtra("name",String.valueOf(timestamp));

                       // System.out.println(timestamp);
                       // startActivity(intent);

                                File sdCard = Environment.getExternalStorageDirectory();

                                File directory = new File (sdCard.getAbsolutePath() + "/Pictures");

                                File file = new File(directory, timestamp+".jpg"); //or any other format supported

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
                        Set<String> fin = new HashSet<>();
                        String str = object2.toString();
                        fin.add(str);
                        for (String val : fin) {
                            if (!t1.isSpeaking() && !val.isEmpty()) {
                                t1.speak(val+" rupees ", TextToSpeech.QUEUE_FLUSH, null);


                            }
                        }

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



                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Toast.makeText(MainActivity.this, "Error saving photo: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
        );

    }


}