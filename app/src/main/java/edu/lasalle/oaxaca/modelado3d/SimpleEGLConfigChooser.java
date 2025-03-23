package edu.lasalle.oaxaca.modelado3d;

import android.opengl.GLSurfaceView;
import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLDisplay;
import android.util.Log;

public class SimpleEGLConfigChooser implements GLSurfaceView.EGLConfigChooser {
    private static final String TAG = "EGLConfigChooser";

    @Override
    public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {
        int[] configAttributes = {
                EGL10.EGL_RENDERABLE_TYPE, 4, // EGL_OPENGL_ES2_BIT
                EGL10.EGL_RED_SIZE, 8,
                EGL10.EGL_GREEN_SIZE, 8,
                EGL10.EGL_BLUE_SIZE, 8,
                EGL10.EGL_ALPHA_SIZE, 8,
                EGL10.EGL_DEPTH_SIZE, 16,
                EGL10.EGL_STENCIL_SIZE, 0,
                EGL10.EGL_NONE
        };

        int[] numConfigs = new int[1];
        if (!egl.eglChooseConfig(display, configAttributes, null, 0, numConfigs)) {
            Log.e(TAG, "Unable to get count of EGL configs");
            return null;
        }

        int configCount = numConfigs[0];
        if (configCount <= 0) {
            Log.e(TAG, "No configs match configSpec");
            return null;
        }

        EGLConfig[] configs = new EGLConfig[configCount];
        if (!egl.eglChooseConfig(display, configAttributes, configs, configCount, numConfigs)) {
            Log.e(TAG, "Unable to retrieve EGL configs");
            return null;
        }

        return configs[0]; // Return the first config that matches our criteria
    }
}