package com.mygdx.game;

import android.graphics.Bitmap;
import android.hardware.Camera;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewGroup.LayoutParams;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import static android.R.id.list;

/**
 * Created by luokangyu on 2016/11/9.
 */

public class AndroidDeviceCameraController implements DeviceCameraControl, Camera.PictureCallback, Camera.AutoFocusCallback {

    private static final int ONE_SECOND_IIN_MILI = 1000;
    private final AndroidLauncher activity;
    private CameraSurface cameraSurface;
    private byte[] pictureData;

    public AndroidDeviceCameraController(AndroidLauncher activity) {
        this.activity = activity;
    }

    @Override
    public synchronized void prepareCamera() {
        activity.setFixedSize(960, 640);
        if (cameraSurface == null) {
            cameraSurface = new CameraSurface(activity);
        }
        activity.addContentView(cameraSurface, new ViewGroup.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
    }

    @Override
    public synchronized void startPreview() {
        // ...and start previewing. From now on, the camera keeps pushing preview
        // images to the surface.
        if (cameraSurface != null && cameraSurface.getCamera() != null) {
            cameraSurface.getCamera().startPreview();
        }
    }

    @Override
    public synchronized void stopPreview() {
        // stop previewing
        if (cameraSurface != null) {
            ViewParent parentView = cameraSurface.getParent();
            if (parentView instanceof ViewGroup) {
                ViewGroup viewGroup = (ViewGroup) parentView;
                viewGroup.removeView(cameraSurface);
            }
            if (cameraSurface.getCamera() != null) {
                cameraSurface.getCamera().stopPreview();
            }
        }
        activity.restoreFixedSize();
    }


    public void setCameraParametersForPicture(Camera camera) {
        // Before we take the picture - we make sure all camera parameters are as we like them
        // Use max resolution and auto focus

        Camera.Parameters p = camera.getParameters();
        List<Camera.Size> supportedSizes = p.getSupportedPictureSizes();

        int maxSupportedWidth = -1;
        int maxSupporedtHeight = -1;

        for (Camera.Size size : supportedSizes) {
            if (size.width > maxSupportedWidth) {
                maxSupportedWidth = size.width;
                maxSupporedtHeight = size.height;
            }
        }

        p.setPreviewSize(maxSupportedWidth, maxSupporedtHeight);
        p.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        camera.setParameters(p);

    }

    @Override
    public synchronized void takePicture() {
        // the user request to take a picture - start the process by requesting focus
        setCameraParametersForPicture(cameraSurface.getCamera());
        cameraSurface.getCamera().autoFocus(this);
    }


    @Override
    public synchronized void onAutoFocus(boolean success, Camera camera) {
        // Focus process finished, we now have focus (or not)
        if (success) {
            if (camera != null) {
                camera.startPreview();
                // We now have focus take the actual picture
                camera.takePicture(null, null, null, this);
            }
        }
    }

    @Override
    public synchronized void onPictureTaken(byte[] pictureData, Camera camera) {
        // We got the picture data - keep it
        this.pictureData = pictureData;
    }


    @Override
    public synchronized byte[] getPictureData() {
        return pictureData;
    }

    @Override
    public void prepareCameraAsync() {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                prepareCamera();
            }
        };
        activity.post(r);
    }

    @Override
    public synchronized void startPreviewAsync() {
        Runnable r = new Runnable() {
            public void run() {
                startPreview();
            }
        };
        activity.post(r);
    }

    @Override
    public synchronized void stopPreviewAsync() {
        Runnable r = new Runnable() {
            public void run() {
                stopPreview();
            }
        };
        activity.post(r);
    }

    @Override
    public synchronized byte[] takePictureAsync(long timeout) {
        timeout *= ONE_SECOND_IIN_MILI;
        pictureData = null;
        Runnable r = new Runnable() {
            @Override
            public void run() {
                takePicture();
            }
        };
        activity.post(r);
        while (pictureData == null && timeout > 0) {
            try {
                Thread.sleep(ONE_SECOND_IIN_MILI);
                timeout -= ONE_SECOND_IIN_MILI;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


        return new byte[0];
    }

    @Override
    public void saveAsJpeg(FileHandle jpgfile, Pixmap pixmap) {
        FileOutputStream fos;
        int x = 0, y = 0;
        int xl = 0, yl = 0;
        try {
            Bitmap bmp = Bitmap.createBitmap(pixmap.getWidth(), pixmap.getHeight(), Bitmap.Config.ARGB_8888);
            // We need to switch between LibGDX RGBA format to Android ARGB format

            for (x = 0, xl = pixmap.getHeight(); x < xl; x++) {
                for (y = 0, yl = pixmap.getWidth(); y < yl; y++) {
                    int color = pixmap.getPixel(x, y);
                    // RGBA => ARGB

                    int RGB = color >> 8;
                    int A = (color & 0x000000ff) << 24;
                    int ARGB = A | RGB;
                    bmp.setPixel(x, y, ARGB);
                }
            }
            fos = new FileOutputStream(jpgfile.file());
            bmp.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }

    }

    @Override
    public boolean isReady() {
        if (cameraSurface != null && cameraSurface.getCamera() != null) {
            return true;
        }

        return false;
    }


}
