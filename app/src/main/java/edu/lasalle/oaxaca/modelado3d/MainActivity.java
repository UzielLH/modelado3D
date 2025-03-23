package edu.lasalle.oaxaca.modelado3d;

import android.app.Activity;
import android.os.Bundle;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";
    private GLSurfaceView glSurfaceView;
    private MyRenderer renderer;

    // Gesture detectors
    private ScaleGestureDetector scaleDetector;
    private GestureDetector gestureDetector;

    // Touch position tracking
    private float previousX;
    private float previousY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            // Use standard Android GLSurfaceView
            glSurfaceView = new GLSurfaceView(this);

            // Set GLES version explicitly
            glSurfaceView.setEGLContextClientVersion(2);

            // Use our custom config chooser
            glSurfaceView.setEGLConfigChooser(new SimpleEGLConfigChooser());

            // Create renderer
            renderer = new MyRenderer(this);

            // Set the renderer
            glSurfaceView.setRenderer(renderer);
            glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

            // Initialize gesture detectors
            scaleDetector = new ScaleGestureDetector(this, new ScaleListener());
            gestureDetector = new GestureDetector(this, new GestureListener());

            // Set the content view to our surface
            setContentView(glSurfaceView);

            Log.d(TAG, "Surface setup complete");
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate", e);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Let the scale detector process the event first
        scaleDetector.onTouchEvent(event);

        // Let the gesture detector handle tap events
        gestureDetector.onTouchEvent(event);

        // Handle rotation gestures
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                previousX = x;
                previousY = y;
                break;

            case MotionEvent.ACTION_MOVE:
                if (!scaleDetector.isInProgress()) {
                    float dx = x - previousX;
                    float dy = y - previousY;

                    // Update rotation
                    renderer.handleRotation(dx, dy);
                }
                previousX = x;
                previousY = y;
                break;
        }

        return true;
    }

    // Scale listener for pinch-zoom
    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scaleFactor = detector.getScaleFactor();
            renderer.handleScale(scaleFactor);
            return true;
        }
    }

    // Gesture listener for taps
    private class GestureListener extends SimpleOnGestureListener {
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            // Reset model position and scale on double tap
            renderer.resetTransformation();
            return true;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            if (glSurfaceView != null) {
                glSurfaceView.onResume();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onResume", e);
        }
    }

    @Override
    protected void onPause() {
        try {
            if (glSurfaceView != null) {
                glSurfaceView.onPause();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onPause", e);
        }
        super.onPause();
    }
}