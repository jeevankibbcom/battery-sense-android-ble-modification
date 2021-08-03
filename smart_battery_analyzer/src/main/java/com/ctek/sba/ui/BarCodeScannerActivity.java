package com.ctek.sba.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;
import com.ctek.sba.R;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import java.io.IOException;

public class BarCodeScannerActivity extends AppCompatActivity {
  SurfaceView surfaceView;
  TextView txtBarcodeValue;
  private BarcodeDetector barcodeDetector;
  private CameraSource cameraSource;
  private static final int REQUEST_CAMERA_PERMISSION = 201;
  //Button btnAction;
  String intentData = "";


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_bar_code_scanner);
    initViews();
  }

  public void back(View view) {
    finish();
  }


  private void initViews() {
    txtBarcodeValue = findViewById(R.id.txtBarcodeValue);
    surfaceView = findViewById(R.id.surfaceView);
  }

  private void initialiseDetectorsAndSources() {
    barcodeDetector = new BarcodeDetector.Builder(this)
        .setBarcodeFormats(Barcode.CODE_128)
        .build();

    cameraSource = new CameraSource.Builder(this, barcodeDetector)
        .setRequestedPreviewSize(1920, 1080)
        .setAutoFocusEnabled(true) //you should add this feature
        .build();

    surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
      @Override
      public void surfaceCreated(SurfaceHolder holder) {
        try {
          if (ActivityCompat.checkSelfPermission(BarCodeScannerActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            cameraSource.start(surfaceView.getHolder());
          } else {
            ActivityCompat.requestPermissions(BarCodeScannerActivity.this, new
                String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
      }

      @Override
      public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
      }

      @Override
      public void surfaceDestroyed(SurfaceHolder holder) {
        cameraSource.stop();
      }
    });


    barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
      @Override
      public void release() {

      }

      @Override
      public void receiveDetections(Detector.Detections<Barcode> detections) {
        final SparseArray<Barcode> barcodes = detections.getDetectedItems();
        if (barcodes.size() != 0) {

          txtBarcodeValue.post(new Runnable() {

            @Override
            public void run() {
                intentData = barcodes.valueAt(0).displayValue;
                //txtBarcodeValue.setText(intentData);
                Intent intent = new Intent();
                intent.putExtra("barCode", intentData);
                setResult(RESULT_OK, intent);
                finish();
            }
          });

        }
      }
    });
  }
  @Override
  protected void onPause() {
    super.onPause();
    cameraSource.release();
  }

  @Override
  protected void onResume() {
    super.onResume();

    initialiseDetectorsAndSources();
  }
}

