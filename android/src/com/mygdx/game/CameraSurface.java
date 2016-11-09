package com.mygdx.game;

import android.content.Context;
import android.hardware.Camera;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Created by luokangyu on 2016/11/9.
 */

public class CameraSurface extends SurfaceView implements SurfaceHolder.Callback {

    private Camera camera;

    public CameraSurface(Context context) {
        super(context);

        getHolder().addCallback(this);

        getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {

        camera = Camera.open();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {

        Camera.Parameters p = camera.getParameters();
        p.setPreviewSize(width, height);
        camera.setParameters(p);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

        camera.startPreview();
        camera.release();
        camera = null;
    }

    public Camera getCamera(){
        return camera;
    }
}
