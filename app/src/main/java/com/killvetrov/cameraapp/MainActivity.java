package com.killvetrov.cameraapp;

import android.hardware.Camera;
import android.os.Parcelable;
import android.os.PersistableBundle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import java.util.EnumMap;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, OnImageSavedListener {

    SurfaceView surfaceMain;
    ImageButton ibtnOne;
    ViewGroup surfaceContainer;
    GridLayout grid;
    ImageButton ibtnGrid;
    ImageButton ibtnFlash;

    private boolean mGridVisible;

    CameraPreview camPrev;
    Camera camera;
    CameraFlashMode mFlashMode;

    private static class CameraFlashState {
        private String value;
        private int iconResId;

        public CameraFlashState(String value, int iconResId) {
            this.value = value;
            this.iconResId = iconResId;
        }

        public String getValue() {
            return value;
        }

        public int getIconResId() {
            return iconResId;
        }

    }

    private enum CameraFlashMode {
        AUTO,
        ON,
        OFF;

        private static final EnumMap<CameraFlashMode, CameraFlashState> cameraFlashMap;
        static {
            cameraFlashMap = new EnumMap<>(CameraFlashMode.class);
            cameraFlashMap.put(AUTO, new CameraFlashState(
                    Camera.Parameters.FLASH_MODE_AUTO,
                    R.drawable.ic_flash_auto_24dp));
            cameraFlashMap.put(ON, new CameraFlashState(
                    Camera.Parameters.FLASH_MODE_ON,
                    R.drawable.ic_flash_on_24dp));
            cameraFlashMap.put(OFF, new CameraFlashState(
                    Camera.Parameters.FLASH_MODE_OFF,
                    R.drawable.ic_flash_off_24dp));
        }

        private static final HashMap<String, CameraFlashMode> lookupMap = new HashMap<>();
        static {
            for (CameraFlashMode mode : cameraFlashMap.keySet()) {
                lookupMap.put(cameraFlashMap.get(mode).getValue(), mode);
            }
        }

        public CameraFlashMode getNext() {
            return values()[(ordinal() + 1) % values().length];
        }

        public String getCameraParamValue() {
            return cameraFlashMap.get(this).getValue();
        }

        public int getFlashIconResId() {
            return cameraFlashMap.get(this).getIconResId();
        }

        static CameraFlashMode fromCameraParamValue(String paramValue) {
            return lookupMap.get(paramValue);
        }
    }

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

        grid = (GridLayout) findViewById(R.id.grid);
        ibtnGrid = (ImageButton) findViewById(R.id.ibtn_grid);
        ibtnFlash = (ImageButton) findViewById(R.id.ibtn_flash);

        ibtnOne.setOnClickListener(this);
        ibtnGrid.setOnClickListener(this);
        ibtnFlash.setOnClickListener(this);
        camPrev = new CameraPreview(this, surfaceMain);
        surfaceContainer.addView(camPrev);
        camPrev.setKeepScreenOn(true);

        mGridVisible = true;
        if (savedInstanceState != null) {
            Log.d("camera_restore_instance", "savedInstanceState.getString(\"flash\") = " + savedInstanceState.getString("flash"));
            mFlashMode = CameraFlashMode.fromCameraParamValue(savedInstanceState.getString("flash"));
            ibtnFlash.setImageResource(mFlashMode.getFlashIconResId());
        } else
            mFlashMode = CameraFlashMode.AUTO;
    }

    @Override
    protected void onResume() {
        super.onResume();
        prepareCamera();
        camPrev.setFlashMode(mFlashMode.getCameraParamValue());
        Log.d("camera_flash_misleading", "camPrev.getFlashMode = " + camPrev.getFlashMode());
        Log.d("camera_flash_misleading", "mFlashMode is set to " + mFlashMode.getCameraParamValue());
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseCamera();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("flash", mFlashMode.getCameraParamValue());
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

    private void toggleGrid() {
        if ((grid == null) | (ibtnGrid == null)) return;

        mGridVisible = !mGridVisible;
        grid.setVisibility(mGridVisible ? View.VISIBLE : View.INVISIBLE);
        grid.bringToFront();
        ibtnGrid.setImageResource(mGridVisible ? R.drawable.ic_grid_on_24dp : R.drawable.ic_grid_off_24dp);
    }

    private void cycleThroughFlashModes() {
        mFlashMode = mFlashMode.getNext();
        camPrev.setFlashMode(mFlashMode.getCameraParamValue());
        ibtnFlash.setImageResource(mFlashMode.getFlashIconResId());
    }

    @Override
    public void onClick(View view) {
        if (!mFlashMode.getCameraParamValue().equals(camPrev.getFlashMode()))
            Log.e("camera_flash_misleading", "inconsistent flash state");
        switch (view.getId()) {
            case R.id.ibtn_one:
                camera.takePicture(shutterCallback, rawCallback, jpegCallback);
                ibtnOne.setOnClickListener(null);
                break;

            case R.id.ibtn_grid:
                toggleGrid();
                break;

            case R.id.ibtn_flash:
                cycleThroughFlashModes();
                break;
        }
    }

    @Override
    public void onImageSaved(String filePath) {
        if(filePath != null)
            Toast.makeText(MainActivity.this, "File saved!", Toast.LENGTH_SHORT).show();
        onPause();
        onResume();
    }
}
