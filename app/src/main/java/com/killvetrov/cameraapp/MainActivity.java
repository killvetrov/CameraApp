package com.killvetrov.cameraapp;

import android.hardware.Camera;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, OnImageSavedListener {

    SurfaceView surfaceMain;
    ImageButton ibtnOne;
    ViewGroup surfaceContainer;

    CameraPreview camPrev;
    Camera camera;

    Camera.ShutterCallback shutterCallback = new Camera.ShutterCallback() {
        public void onShutter() {
        }
    };

    Camera.PictureCallback rawCallback = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
        }
    };

    Camera.PictureCallback jpegCallback = new Camera.PictureCallback() {
        public void onPictureTaken(byte[] data, Camera camera) {
            ibtnOne.setOnClickListener(MainActivity.this);
            new SaveImageTask(MainActivity.this, MainActivity.this)
                    .execute(data);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        surfaceMain = (SurfaceView) findViewById(R.id.surface_main);
        ibtnOne = (ImageButton) findViewById(R.id.ibtn_one);
        surfaceContainer = (ViewGroup) findViewById(R.id.frame_container);

        ibtnOne.setOnClickListener(this);
        camPrev = new CameraPreview(this, surfaceMain);
        surfaceContainer.addView(camPrev);
        camPrev.setKeepScreenOn(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        prepareCamera();
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseCamera();
    }

    public void prepareCamera() {
        int camCount = Camera.getNumberOfCameras();
        if (camCount > 0) {
            try {
                camera = Camera.open(0);
                camPrev.setmCamera(camera);
                camPrev.setCameraOrientation(getWindowManager()
                        .getDefaultDisplay().getRotation());
                camera.startPreview();
            } catch (Exception e) {
                Toast.makeText(MainActivity.this, "Camera cannot be accessed", Toast.LENGTH_SHORT).show();
            }
        }
    }

     public void releaseCamera() {
        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
            camPrev.setmCamera(null);
        }
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.ibtn_one:
                camera.takePicture(shutterCallback, rawCallback, jpegCallback);
                ibtnOne.setOnClickListener(null);
                break;
        }
    }

    @Override
    public void onImageSaved(String filePath) {
        if(filePath != null)
            Toast.makeText(MainActivity.this, "File saved!", Toast.LENGTH_SHORT).show();
    }
}
