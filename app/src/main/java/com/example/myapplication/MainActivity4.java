package com.example.myapplication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.util.SparseArray;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;


public class MainActivity4 extends AppCompatActivity {
    TextToSpeech t1;
    SurfaceView previewView;
    private Button bCapture,bCapture1,bCapture2;
    private static final int SWIPE_DISTANCE_THRESHOLD = 100;
    private static final int SWIPE_VELOCITY_THRESHOLD = 100;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main4);
        // Open the desired website
        t1 = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int i) {
                if(i!=TextToSpeech.ERROR){
                    t1.setLanguage(Locale.ENGLISH);
                    t1.speak("Medicines",TextToSpeech.QUEUE_ADD,null,null);
                }
            }
        });
      //  t1.speak("Medicines",TextToSpeech.QUEUE_FLUSH,null,null);

        if (! Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }
        Python py = Python.getInstance();
        PyObject pyObject = py.getModule("script");
        // PyObject object = pyObject.callAttr("main",stringBuilder.toString());
        PyObject object2 =  pyObject.callAttr("Commands");
        object2.callAttr("insert_data");
       // System.out.println(object2.callAttr("get_med").toString());
        bCapture = findViewById(R.id.bCapture);
        bCapture1 = findViewById(R.id.bCapture1);
        bCapture2 = findViewById(R.id.bCapture2);
        previewView = findViewById(R.id.surface_camera_preview);
        bCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(MainActivity4.this, "Currency", Toast.LENGTH_SHORT).show();
               Thread objThread1 = new Thread(objRun1);
               objThread1.start();
            }
        });

        bCapture1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(MainActivity4.this, "Bills", Toast.LENGTH_SHORT).show();
//                Thread objThread = new Thread(objRun);
//                objThread.start();
                Thread objThread = new Thread(objRun);
                objThread.start();
            }
        });



        TextRecognizer textRecognizer = new TextRecognizer.Builder(this).build();
        if (!textRecognizer.isOperational()) {
            Toast.makeText(this, "Dependencies are not loaded yet...please try after few moments!!", Toast.LENGTH_SHORT).show();
            Log.d("er","Dependencies are downloading....try after few moment");
            return;
        }

//  Init camera source to use high resolution and auto focus
        CameraSource mCameraSource = new CameraSource.Builder(getApplicationContext(), textRecognizer)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedPreviewSize(1280, 1024)
                .setAutoFocusEnabled(true)
                .setRequestedFps(2.0f)
                .build();
        SurfaceView surfaceCameraPreview = findViewById(R.id.surface_camera_preview);
        surfaceCameraPreview.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceChanged(SurfaceHolder p0, int p1, int p2, int p3) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder p0) {
                mCameraSource.stop();
            }

            @SuppressLint("MissingPermission")
            @Override
            public void surfaceCreated(SurfaceHolder p0) {
                try {
                    if (isCameraPermissionGranted()) {
                        mCameraSource.start(surfaceCameraPreview.getHolder());
                    } else {
                        requestForPermission();
                    }
                } catch (Exception e) {
                    Toast.makeText(MainActivity4.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

            private void requestForPermission() {
                 if(ContextCompat.checkSelfPermission(MainActivity4.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED);
                {
                 if(ActivityCompat.shouldShowRequestPermissionRationale(MainActivity4.this, Manifest.permission.CAMERA)){

                 }else{
                     int MY_PERMISSIONS_REQUEST_CAMERA = 0;
                     ActivityCompat.requestPermissions(MainActivity4.this,
                             new String[]{Manifest.permission.CAMERA},
                             MY_PERMISSIONS_REQUEST_CAMERA);
                 }
                }
            }

            private boolean isCameraPermissionGranted() {
                return (ContextCompat.checkSelfPermission(MainActivity4.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED);
                   }
        });
        textRecognizer.setProcessor(new Detector.Processor<TextBlock>() {
            @Override
            public void release() {
            }

            @Override
            public void receiveDetections(Detector.Detections<TextBlock> detections) {
                SparseArray<TextBlock> items = detections.getDetectedItems();

                if (items.size() <= 0) {
                    return;
                }

                TextView tvResult = findViewById(R.id.tv_result);
                tvResult.post(new Runnable() {
                    @Override
                    public void run() {
                        StringBuilder stringBuilder = new StringBuilder();
                        for (int i = 0; i < items.size(); i++) {
                            TextBlock item = items.valueAt(i);
                            stringBuilder.append(item.getValue());
                            stringBuilder.append("\n");
                        }
                        Python py = Python.getInstance();
                        PyObject pyObject = py.getModule("script");
                        PyObject object = pyObject.callAttr("main", stringBuilder.toString());
                        // PyObject object = pyObject.callAttr("main","Dolo");
                        if(object!=null){
                        System.out.println(object);
                        //System.out.println(object.asList().get(0).toString());
                        Set<String> fin = new HashSet<>();
                        String str = object.asList().get(0).toString() + " " + object.asList().get(1).toString();
                        fin.add(str);

                        tvResult.setText(str);
                        for (String val : fin) {
                            if (!t1.isSpeaking() && !val.isEmpty()) {
                                t1.speak(val, TextToSpeech.QUEUE_FLUSH, null);

                            }
                        }
                    }

                    }
                });
            }
        });


        previewView.setOnTouchListener(new View.OnTouchListener() {
            private GestureDetector gestureDetector = new GestureDetector(MainActivity4.this, new GestureDetector.SimpleOnGestureListener() {

                @Override
                public boolean onSingleTapConfirmed(@NonNull MotionEvent motionEvent) {
                    return false;
                }

                @Override
                public boolean onDoubleTap(@NonNull MotionEvent motionEvent) {


                    return false;
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

    }


    private void onSwipeRight() {
        Toast.makeText(MainActivity4.this, "Bills", Toast.LENGTH_SHORT).show();
//                Thread objThread = new Thread(objRun);
//                objThread.start();
        Thread objThread = new Thread(objRun);
        objThread.start();
    }

    Runnable objRun = new Runnable() {
        @Override
        public void run() {

            Intent intent = new Intent(MainActivity4.this,MainActivity.class);

            startActivity(intent);

        }
    };
    Runnable objRun1 = new Runnable() {
        @Override
        public void run() {
            Intent intent = new Intent(MainActivity4.this, org.tensorflow.lite.examples.classification.ClassifierActivity.class);
            startActivity(intent);

        }
    };

}