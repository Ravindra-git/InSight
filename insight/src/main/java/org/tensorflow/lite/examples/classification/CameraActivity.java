/*
 * Copyright 2019 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tensorflow.lite.examples.classification;

import android.Manifest;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.Image.Plane;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Trace;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.UiThread;


import com.google.android.material.bottomsheet.BottomSheetBehavior;
import androidx.appcompat.app.AppCompatActivity;
//import androidx.appcompat.widget.Toolbar;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Size;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.List;
import android.speech.tts.TextToSpeech;
import java.util.Locale;
import java.util.Set;

import org.tensorflow.lite.examples.classification.env.ImageUtils;
import org.tensorflow.lite.examples.classification.env.Logger;
import org.tensorflow.lite.examples.classification.tflite.Classifier.Device;
import org.tensorflow.lite.examples.classification.tflite.Classifier.Model;
import org.tensorflow.lite.examples.classification.tflite.Classifier.Recognition;

@RequiresApi(api = Build.VERSION_CODES.KITKAT)
public abstract class CameraActivity extends AppCompatActivity
    implements OnImageAvailableListener,
        Camera.PreviewCallback,
        View.OnClickListener,
        AdapterView.OnItemSelectedListener {
  private float threshold = 0.9f;
  private TextWatcher textWatcher;

  private static final Logger LOGGER = new Logger();

  private static final int PERMISSIONS_REQUEST = 1;

  private static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;
  protected int previewWidth = 0;
  protected int previewHeight = 0;
  private Handler handler;
  private HandlerThread handlerThread;
  private boolean useCamera2API;
  private boolean isProcessingFrame = false;
  private byte[][] yuvBytes = new byte[3][];
  private int[] rgbBytes = null;
  private int yRowStride;
  private Runnable postInferenceCallback;
  private Runnable imageConverter;
  private LinearLayout bottomSheetLayout;
  private LinearLayout gestureLayout;
  private BottomSheetBehavior sheetBehavior;
  protected TextView recognitionTextView;
//      recognition1TextView,
//      recognition2TextView,
//      recognition1ValueTextView,
//      recognition2ValueTextView,
//      recognitionValueTextView;

  protected EditText thresholdValueTextView;

  TextToSpeech t1;
  private Model model = Model.FLOAT;
  private Device device = Device.CPU;
  private int numThreads = 1;

  private TextToSpeech textToSpeech;
  private Button bCapture,bCapture1,bCapture2;
  private static final int SWIPE_DISTANCE_THRESHOLD = 100;
  private static final int SWIPE_VELOCITY_THRESHOLD = 100;

  public FrameLayout container;
  @Override
  protected void onCreate(final Bundle savedInstanceState) {
    LOGGER.d("onCreate " + this);
    super.onCreate(null);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    setContentView(R.layout.activity_camera);

    if (hasPermission()) {
      setFragment();
    } else {
      requestPermission();
    }
    bCapture = findViewById(R.id.bCapture);
    bCapture1 = findViewById(R.id.bCapture1);
    bCapture2 = findViewById(R.id.bCapture2);
    container = findViewById(R.id.container);
    t1 = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
      @Override
      public void onInit(int i) {
        if(i!=TextToSpeech.ERROR){
          t1.setLanguage(Locale.ENGLISH);
          t1.speak("Currency",TextToSpeech.QUEUE_ADD,null);

        }
      }
    });
    bCapture1.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        Toast.makeText(CameraActivity.this, "Bills", Toast.LENGTH_SHORT).show();
//                Thread objThread = new Thread(objRun);
//                objThread.start();
        Thread objThread1 = new Thread(objRun1);
        objThread1.start();
      }
    });
    bCapture2.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        Toast.makeText(CameraActivity.this, "Meds", Toast.LENGTH_SHORT).show();
        Thread objThread = new Thread(objRun);
        objThread.start();
        try {
          objThread.sleep(1000);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      }
    });
    //initialising tts object
    textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
      @Override
      public void onInit(int status) {
        if(status == TextToSpeech.SUCCESS){
            int ttsLang = textToSpeech.setLanguage(Locale.ENGLISH);


            if(ttsLang == TextToSpeech.LANG_MISSING_DATA || ttsLang == TextToSpeech.LANG_NOT_SUPPORTED){
              Log.e("TTS", "The Language is not supported!");
            }else{
              Log.i("TTS", "Language Supported.");
            }
          Toast.makeText(getApplicationContext(), "TTS Initialization succeed!", Toast.LENGTH_SHORT).show();
          Log.i("TTS", "Initialization success.");
        }else{
          Toast.makeText(getApplicationContext(), "TTS Initialization failed!", Toast.LENGTH_SHORT).show();
        }
      }
    });


    bottomSheetLayout = findViewById(R.id.bottom_sheet_layout);
    gestureLayout = findViewById(R.id.gesture_layout);
    sheetBehavior = BottomSheetBehavior.from(bottomSheetLayout);
    thresholdValueTextView = findViewById(R.id.threshold_value);

    ViewTreeObserver vto = gestureLayout.getViewTreeObserver();
    vto.addOnGlobalLayoutListener(
        new ViewTreeObserver.OnGlobalLayoutListener() {
          @Override
          public void onGlobalLayout() {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
              gestureLayout.getViewTreeObserver().removeGlobalOnLayoutListener(this);
            } else {
              gestureLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
            int height = gestureLayout.getMeasuredHeight();

            sheetBehavior.setPeekHeight(height);
          }
        });
    sheetBehavior.setHideable(false);



    recognitionTextView = findViewById(R.id.detected_item);
    threshold = Float.valueOf(thresholdValueTextView.getText().toString());

    textWatcher = new TextWatcher(){

      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        //do nothing
      }

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
        //do nothing
      }

      @Override
      public void afterTextChanged(Editable s) {
        if(s.length() != 0){
          threshold = Float.valueOf(s.toString());
        }
      }
    };

    thresholdValueTextView.addTextChangedListener(textWatcher);

    container.setOnTouchListener(new View.OnTouchListener() {
      private GestureDetector gestureDetector = new GestureDetector(CameraActivity.this, new GestureDetector.SimpleOnGestureListener() {

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
            if (distanceX < 0)
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

  }

  private void onSwipeLeft() {
    Toast.makeText(CameraActivity.this, "Bills", Toast.LENGTH_SHORT).show();
//                Thread objThread = new Thread(objRun);
//                objThread.start();
    Thread objThread1 = new Thread(objRun1);
    objThread1.start();
  }

  Runnable objRun = new Runnable() {
    @Override
    public void run() {
      Intent intent = new Intent();
      intent.setComponent(new ComponentName("com.example.myapplication", "com.example.myapplication.MainActivity4"));
      startActivity(intent);

    }
  };
  Runnable objRun1 = new Runnable() {
    @Override
    public void run() {
      Intent intent = new Intent();
      intent.setComponent(new ComponentName("com.example.myapplication", "com.example.myapplication.MainActivity"));
      startActivity(intent);

    }
  };
  ////////////////////////////////////onCreate() Ends Here///////////////////////////////////////////////////

  protected int[] getRgbBytes() {
    imageConverter.run();
    return rgbBytes;
  }

  protected int getLuminanceStride() {
    return yRowStride;
  }

  protected byte[] getLuminance() {
    return yuvBytes[0];
  }

  /** Callback for android.hardware.Camera API */
  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  @Override
  public void onPreviewFrame(final byte[] bytes, final Camera camera) {
    if (isProcessingFrame) {
      LOGGER.w("Dropping frame!");
      return;
    }

    try {
      // Initialize the storage bitmaps once when the resolution is known.
      if (rgbBytes == null) {
        Camera.Size previewSize = camera.getParameters().getPreviewSize();
        previewHeight = previewSize.height;
        previewWidth = previewSize.width;
        rgbBytes = new int[previewWidth * previewHeight];
        onPreviewSizeChosen(new Size(previewSize.width, previewSize.height), 90);
      }
    } catch (final Exception e) {
      LOGGER.e(e, "Exception!");
      return;
    }

    isProcessingFrame = true;
    yuvBytes[0] = bytes;
    yRowStride = previewWidth;

    imageConverter =
        new Runnable() {
          @Override
          public void run() {
            ImageUtils.convertYUV420SPToARGB8888(bytes, previewWidth, previewHeight, rgbBytes);
          }
        };

    postInferenceCallback =
        new Runnable() {
          @Override
          public void run() {
            camera.addCallbackBuffer(bytes);
            isProcessingFrame = false;
          }
        };
    processImage();
  }

  /** Callback for Camera2 API */
  @Override
  public void onImageAvailable(final ImageReader reader) {
    // We need wait until we have some size from onPreviewSizeChosen
    if (previewWidth == 0 || previewHeight == 0) {
      return;
    }
    if (rgbBytes == null) {
      rgbBytes = new int[previewWidth * previewHeight];
    }
    try {
      final Image image = reader.acquireLatestImage();

      if (image == null) {
        return;
      }

      if (isProcessingFrame) {
        image.close();
        return;
      }
      isProcessingFrame = true;
      Trace.beginSection("imageAvailable");
      final Plane[] planes = image.getPlanes();
      fillBytes(planes, yuvBytes);
      yRowStride = planes[0].getRowStride();
      final int uvRowStride = planes[1].getRowStride();
      final int uvPixelStride = planes[1].getPixelStride();

      imageConverter =
          new Runnable() {
            @Override
            public void run() {
              ImageUtils.convertYUV420ToARGB8888(
                  yuvBytes[0],
                  yuvBytes[1],
                  yuvBytes[2],
                  previewWidth,
                  previewHeight,
                  yRowStride,
                  uvRowStride,
                  uvPixelStride,
                  rgbBytes);
            }
          };

      postInferenceCallback =
          new Runnable() {
            @Override
            public void run() {
              image.close();
              isProcessingFrame = false;
            }
          };

      processImage();
    } catch (final Exception e) {
      LOGGER.e(e, "Exception!");
      Trace.endSection();
      return;
    }
    Trace.endSection();
  }

  @Override
  public synchronized void onStart() {
    LOGGER.d("onStart " + this);
    super.onStart();
  }

  @Override
  public synchronized void onResume() {
    LOGGER.d("onResume " + this);
    super.onResume();

    handlerThread = new HandlerThread("inference");
    handlerThread.start();
    handler = new Handler(handlerThread.getLooper());
  }

  @Override
  public synchronized void onPause() {
    LOGGER.d("onPause " + this);

    handlerThread.quitSafely();
    try {
      handlerThread.join();
      handlerThread = null;
      handler = null;
    } catch (final InterruptedException e) {
      LOGGER.e(e, "Exception!");
    }

    super.onPause();
  }

  @Override
  public synchronized void onStop() {
    LOGGER.d("onStop " + this);
    super.onStop();
  }

  @Override
  public synchronized void onDestroy() {
    LOGGER.d("onDestroy " + this);
    super.onDestroy();

    if (textToSpeech != null) {
      textToSpeech.stop();
//      textToSpeech.shutdown();
    }

  }

  protected synchronized void runInBackground(final Runnable r) {
    if (handler != null) {
      handler.post(r);
    }
  }

  @Override
  public void onRequestPermissionsResult(
      final int requestCode, final String[] permissions, final int[] grantResults) {
    if (requestCode == PERMISSIONS_REQUEST) {
      if (grantResults.length > 0
          && grantResults[0] == PackageManager.PERMISSION_GRANTED
          && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
        setFragment();
      } else {
        requestPermission();
      }
    }
  }

  private boolean hasPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      return checkSelfPermission(PERMISSION_CAMERA) == PackageManager.PERMISSION_GRANTED;
    } else {
      return true;
    }
  }

  private void requestPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      if (shouldShowRequestPermissionRationale(PERMISSION_CAMERA)) {
        Toast.makeText(
                CameraActivity.this,
                "Camera permission is required for this demo",
                Toast.LENGTH_LONG)
            .show();
      }
      requestPermissions(new String[] {PERMISSION_CAMERA}, PERMISSIONS_REQUEST);
    }
  }

  // Returns true if the device supports the required hardware level, or better.
  private boolean isHardwareLevelSupported(
      CameraCharacteristics characteristics, int requiredLevel) {
    int deviceLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
    if (deviceLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
      return requiredLevel == deviceLevel;
    }
    // deviceLevel is not LEGACY, can use numerical sort
    return requiredLevel <= deviceLevel;
  }

  private String chooseCamera() {
    final CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
    try {
      for (final String cameraId : manager.getCameraIdList()) {
        final CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

        // We don't use a front facing camera in this sample.
        final Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
        if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
          continue;
        }

        final StreamConfigurationMap map =
            characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

        if (map == null) {
          continue;
        }

        // Fallback to camera1 API for internal cameras that don't have full support.
        // This should help with legacy situations where using the camera2 API causes
        // distorted or otherwise broken previews.
        useCamera2API =
            (facing == CameraCharacteristics.LENS_FACING_EXTERNAL)
                || isHardwareLevelSupported(
                    characteristics, CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL);
        LOGGER.i("Camera API lv2?: %s", useCamera2API);
        return cameraId;
      }
    } catch (CameraAccessException e) {
      LOGGER.e(e, "Not allowed to access camera");
    }

    return null;
  }

  protected void setFragment() {
    String cameraId = chooseCamera();

    Fragment fragment;
    if (useCamera2API) {
      CameraConnectionFragment camera2Fragment =
          CameraConnectionFragment.newInstance(
              new CameraConnectionFragment.ConnectionCallback() {
                @Override
                public void onPreviewSizeChosen(final Size size, final int rotation) {
                  previewHeight = size.getHeight();
                  previewWidth = size.getWidth();
                  CameraActivity.this.onPreviewSizeChosen(size, rotation);
                }
              },
              this,
              getLayoutId(),
              getDesiredPreviewFrameSize());

      camera2Fragment.setCamera(cameraId);
      fragment = camera2Fragment;
    } else {
      fragment =
          new LegacyCameraConnectionFragment(this, getLayoutId(), getDesiredPreviewFrameSize());
    }

    getFragmentManager().beginTransaction().replace(R.id.container, fragment).commit();
  }

  protected void fillBytes(final Plane[] planes, final byte[][] yuvBytes) {
    // Because of the variable row stride it's not possible to know in
    // advance the actual necessary dimensions of the yuv planes.
    for (int i = 0; i < planes.length; ++i) {
      final ByteBuffer buffer = planes[i].getBuffer();
      if (yuvBytes[i] == null) {
        LOGGER.d("Initializing buffer %d at size %d", i, buffer.capacity());
        yuvBytes[i] = new byte[buffer.capacity()];
      }
      buffer.get(yuvBytes[i]);
    }
  }

  protected void readyForNextImage() {
    if (postInferenceCallback != null) {
      postInferenceCallback.run();
    }
  }

  protected int getScreenOrientation() {
    switch (getWindowManager().getDefaultDisplay().getRotation()) {
      case Surface.ROTATION_270:
        return 270;
      case Surface.ROTATION_180:
        return 180;
      case Surface.ROTATION_90:
        return 90;
      default:
        return 0;
    }
  }

  @UiThread
  protected void showResultsInBottomSheet(List<Recognition> results) {
    if (results != null && results.size() >= 3) {
      Recognition recognition = results.get(0);
      if (recognition != null) {
        if (recognition.getTitle() != null){
            recognitionTextView.setText(recognition.getTitle());
            if(!recognition.getTitle().equals("none")){
              Set<String> fin = new HashSet<>();
              fin.add(recognition.getTitle());
              for(String val: fin) {
                if (recognition.getConfidence() != null && recognition.getConfidence() * 100 > threshold) {
                  if (!textToSpeech.isSpeaking() && !val.isEmpty()) {
                    textToSpeech.speak(val+"Indian rupees", TextToSpeech.QUEUE_FLUSH, null);
                  }
                }
              }
            }
        }
      }

    }
  }

  protected void showFrameInfo(String frameInfo) {
//    frameValueTextView.setText(frameInfo);
  }

  protected void showCropInfo(String cropInfo) {
//    cropValueTextView.setText(cropInfo);
  }

  protected void showCameraResolution(String cameraInfo) {
//    cameraResolutionTextView.setText(previewWidth + "x" + previewHeight);
  }

  protected void showRotationInfo(String rotation) {
//    rotationTextView.setText(rotation);
  }

  protected void showInference(String inferenceTime) {
//    inferenceTimeTextView.setText(inferenceTime);
  }

  protected Model getModel() {
    return model;
  }


  protected Device getDevice() {
    return device;
  }


  protected int getNumThreads() {
    return numThreads;
  }

  private void setNumThreads(int numThreads) {
    if (this.numThreads != numThreads) {
      LOGGER.d("Updating  numThreads: " + numThreads);
      this.numThreads = numThreads;
      onInferenceConfigurationChanged();
    }
  }

  protected abstract void processImage();

  protected abstract void onPreviewSizeChosen(final Size size, final int rotation);

  protected abstract int getLayoutId();

  protected abstract Size getDesiredPreviewFrameSize();

  protected abstract void onInferenceConfigurationChanged();

  @Override
  public void onClick(View v) {
  }

  @Override
  public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
  }

  @Override
  public void onNothingSelected(AdapterView<?> parent) {
//     Do nothing.
  }
}
