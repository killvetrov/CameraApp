package com.killvetrov.cameraapp;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import java.io.IOException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Killvetrov on 02-Nov-15.
 */
public class CameraPreview extends ViewGroup implements SurfaceHolder.Callback {

    private SurfaceView mSurface;
    private SurfaceHolder mHolder;
    private Context mContext;
    private Camera mCamera;

    private Camera.Size previewSize;
    private List<Camera.Size> supportPreviewSizes;
    private String mFlashMode;

    public CameraPreview(Context context, SurfaceView surface) {
        super(context);
        this.mContext = context;
        this.mSurface = surface;
        this.mHolder = surface.getHolder();
        this.mHolder.addCallback(this);
        mFlashMode = Camera.Parameters.FLASH_MODE_AUTO;
    }

    public void setmCamera(Camera mCamera) {
        this.mCamera = mCamera;
        if (this.mCamera != null) {
            this.supportPreviewSizes = this.mCamera.getParameters().getSupportedPreviewSizes();
            int i = 0;
            for (Camera.Size s : this.supportPreviewSizes) {
                i++;
                Log.d("camera", "" + i + " " + s.width + " x " + s.height);
            }
            previewSize = this.supportPreviewSizes.get(0);
            try {
                mCamera.setPreviewDisplay(mHolder);
            } catch (IOException e) {
                e.printStackTrace();
            }
            requestLayout();
        }
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int width = resolveSize(getSuggestedMinimumWidth(),
                widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(),
                heightMeasureSpec);
        setMeasuredDimension(width, height);

        if (supportPreviewSizes != null) {
            previewSize = getOptimalPreviewSize(supportPreviewSizes, width,
                    height);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (changed && getChildCount() > 0) {
            final View child = getChildAt(0);

            final int width = r - l;
            final int height = b - t;

            int previewWidth = width;
            int previewHeight = height;
            if (previewSize != null) {
                previewWidth = previewSize.width;
                previewHeight = previewSize.height;
            }

            // Center the child SurfaceView within the parent.
            if (width * previewHeight > height * previewWidth) {
                final int scaledChildWidth = previewWidth * height
                        / previewHeight;
                child.layout((width - scaledChildWidth) / 2, 0,
                        (width + scaledChildWidth) / 2, height);
            } else {
                final int scaledChildHeight = previewHeight * width
                        / previewWidth;
                child.layout(0, (height - scaledChildHeight) / 2, width,
                        (height + scaledChildHeight) / 2);
            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        try {
            if (this.mCamera != null)
                mCamera.setPreviewDisplay(mHolder);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2){
        if (surfaceHolder == null) return;
        if (mCamera != null) {
            Camera.Parameters camParams = mCamera.getParameters();
            camParams.setPreviewSize(this.previewSize.width, this.previewSize.height);
            camParams.setJpegQuality(80);
            if (camParams.getSupportedFocusModes().contains(
                    Camera.Parameters.FOCUS_MODE_AUTO)) {
                camParams.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            }
            List<Camera.Size> sizes = camParams.getSupportedPictureSizes();
            int cntr = 0;
            for (Camera.Size s : sizes) {
                cntr++;
                Log.d("camera_picturesize", "" + cntr + " " + s.width + " x " + s.height);
            }
            camParams.setPictureSize(sizes.get(0).width,
                    sizes.get(0).height);
            camParams.setFlashMode(mFlashMode);
            requestLayout();
            mCamera.setParameters(camParams);
            mCamera.startPreview();
        }
    }

    public void setFlashMode(String flashModeValue) {
        Camera.Parameters camParams = mCamera.getParameters();
        camParams.setFlashMode(flashModeValue);
        mCamera.setParameters(camParams);
        mFlashMode = flashModeValue;
        Log.d("camera_setflash", "set to " + flashModeValue);
    }

    public String getFlashMode() {
        return mCamera.getParameters().getFlashMode();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        if (mCamera != null)
            mCamera.stopPreview();
    }

    public void setCameraOrientation(int rotation) {

        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        Camera.CameraInfo info = new Camera.CameraInfo();

        Camera.getCameraInfo(0, info);
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360; // compensate the mirror
        } else { // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        mCamera.setDisplayOrientation(result);

        Camera.Parameters params = mCamera.getParameters();
        params.setRotation(result);
        mCamera.setParameters(params);
    }

    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) h / w;

        if (sizes == null)
            return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
                continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

}
