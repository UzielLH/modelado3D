package edu.lasalle.oaxaca.modelado3d;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MyRenderer implements GLSurfaceView.Renderer {
    private static final String TAG = "MyRenderer";
    private Context context;

    // Model view projection matrices
    private final float[] mMVPMatrix = new float[16];
    private final float[] mProjectionMatrix = new float[16];
    private final float[] mViewMatrix = new float[16];
    private final float[] mModelMatrix = new float[16];

    // Transformation parameters
    private float rotationX = 0;
    private float rotationY = 0;
    private float scale = 1.0f;
    private float positionZ = 5.0f; // Initial camera distance

    // Touch sensitivity
    private final float ROTATION_FACTOR = 0.5f;
    private final float SCALE_MIN = 0.5f;
    private final float SCALE_MAX = 3.0f;

    // Shader program
    private int mProgram;

    // Handles for shader uniforms/attributes
    private int mPositionHandle;
    private int mNormalHandle;
    private int mTexCoordHandle;
    private int mMVPMatrixHandle;
    private int mTextureHandle;
    private int mLightDirHandle;

    // Texture related
    private int[] textureId = new int[1];

    // OBJ model
    private ObjLoader objModel;

    // Shader source code
    private final String vertexShaderCode =
            "uniform mat4 uMVPMatrix;" +
                    "attribute vec4 aPosition;" +
                    "attribute vec3 aNormal;" +
                    "attribute vec2 aTexCoord;" +
                    "varying vec2 vTexCoord;" +
                    "varying vec3 vNormal;" +
                    "void main() {" +
                    "  gl_Position = uMVPMatrix * aPosition;" +
                    "  vTexCoord = aTexCoord;" +
                    "  vNormal = aNormal;" +
                    "}";

    private final String fragmentShaderCode =
            "precision highp float;" + // Cambia a highp para mejor precisión
                    "varying vec2 vTexCoord;" +
                    "varying vec3 vNormal;" +
                    "uniform sampler2D uTexture;" +
                    "uniform vec3 uLightDir;" +
                    "void main() {" +
                    "  vec3 normal = normalize(vNormal);" +
                    "  float ambient = 0.4;" + // Aumenta la luz ambiente
                    "  float diffuse = max(dot(normal, normalize(uLightDir)), 0.0);" +
                    "  float lighting = ambient + diffuse * 0.6;" +
                    "  vec4 texColor = texture2D(uTexture, vTexCoord);" +
                    "  gl_FragColor = vec4(texColor.rgb * lighting, texColor.a);" +
                    "}";

    public MyRenderer(Context context) {
        this.context = context;
        Log.d(TAG, "Renderer constructor called");
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.d(TAG, "onSurfaceCreated started");
        try {
            // Set background color to black
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

            // Enable depth testing
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);
            GLES20.glEnable(GLES20.GL_CULL_FACE); // Añadir esta línea
            // Load and compile shaders
            int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
            int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

            // Create shader program
            mProgram = GLES20.glCreateProgram();
            GLES20.glAttachShader(mProgram, vertexShader);
            GLES20.glAttachShader(mProgram, fragmentShader);
            GLES20.glLinkProgram(mProgram);

            // Get attribute and uniform handles
            mPositionHandle = GLES20.glGetAttribLocation(mProgram, "aPosition");
            mNormalHandle = GLES20.glGetAttribLocation(mProgram, "aNormal");
            mTexCoordHandle = GLES20.glGetAttribLocation(mProgram, "aTexCoord");
            mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
            mTextureHandle = GLES20.glGetUniformLocation(mProgram, "uTexture");
            mLightDirHandle = GLES20.glGetUniformLocation(mProgram, "uLightDir");

            // Load texture
            loadTexture();

            // Load OBJ model
            objModel = new ObjLoader(context, R.raw.old);

            Log.d(TAG, "onSurfaceCreated completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error in onSurfaceCreated", e);
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        try {
            // Adjust the viewport based on geometry changes
            GLES20.glViewport(0, 0, width, height);

            // Calculate the projection matrix
            float ratio = (float) width / height;
            Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1, 1, 1, 100);

            Log.d(TAG, "onSurfaceChanged: viewport set to " + width + "x" + height);
        } catch (Exception e) {
            Log.e(TAG, "Error in onSurfaceChanged", e);
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        try {
            // Clear the screen
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

            // Use the shader program
            GLES20.glUseProgram(mProgram);

            // Set the camera position with zoom factor
            Matrix.setLookAtM(mViewMatrix, 0,
                    0, 0, positionZ, // Camera position with zoom
                    0, 0, 0,         // Point to look at
                    0, 1, 0);        // Up vector

            // Calculate the model matrix for user-controlled rotation
            Matrix.setIdentityM(mModelMatrix, 0);
            Matrix.scaleM(mModelMatrix, 0, scale, scale, scale);
            Matrix.rotateM(mModelMatrix, 0, rotationX, 1, 0, 0);
            Matrix.rotateM(mModelMatrix, 0, rotationY, 0, 1, 0);

            // Combine the matrices
            Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);
            Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);

            // Set matrix uniform
            GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);

            // Set light direction
            GLES20.glUniform3f(mLightDirHandle, 0.5f, 0.5f, 1.0f);
            // Set active texture
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId[0]);
            GLES20.glUniform1i(mTextureHandle, 0);

            // Draw model
            drawModel();
        } catch (Exception e) {
            Log.e(TAG, "Error in onDrawFrame", e);
        }
    }

    // Handle rotation from touch events
    public void handleRotation(float dx, float dy) {
        rotationY += dx * ROTATION_FACTOR;
        rotationX += dy * ROTATION_FACTOR;
    }

    // Handle scaling from pinch gestures
    public void handleScale(float scaleFactor) {
        // Update scale, keeping it within bounds
        scale *= scaleFactor;
        scale = Math.max(SCALE_MIN, Math.min(scale, SCALE_MAX));
    }

    // Reset transformations on double tap
    public void resetTransformation() {
        rotationX = 0;
        rotationY = 0;
        scale = 1.0f;
        positionZ = 5.0f;
    }

    // Loads a texture from resources
    private void loadTexture() {
        try {
            // Generate texture ID
            GLES20.glGenTextures(1, textureId, 0);

            // Bind to the texture
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId[0]);

            // Set filtering parameters
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

            // Load the bitmap
            Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.raw.fondo);

            // Load the bitmap into the texture
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

            // Recycle the bitmap
            bitmap.recycle();

            Log.d(TAG, "Texture loaded successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error loading texture", e);
        }
    }

    // Draw the OBJ model
    private void drawModel() {
        if (objModel == null || objModel.getVertexBuffer() == null) {
            Log.e(TAG, "Model not loaded properly");
            return;
        }

        // Enable vertex attributes
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glEnableVertexAttribArray(mNormalHandle);
        GLES20.glEnableVertexAttribArray(mTexCoordHandle);

        // Set vertex data
        GLES20.glVertexAttribPointer(mPositionHandle, 3, GLES20.GL_FLOAT, false, 0, objModel.getVertexBuffer());
        GLES20.glVertexAttribPointer(mNormalHandle, 3, GLES20.GL_FLOAT, false, 0, objModel.getNormalBuffer());
        GLES20.glVertexAttribPointer(mTexCoordHandle, 2, GLES20.GL_FLOAT, false, 0, objModel.getTexCoordBuffer());

        // Draw the model triangles
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, objModel.getVertexCount(), GLES20.GL_UNSIGNED_SHORT, objModel.getIndexBuffer());

        // Disable vertex attributes
        GLES20.glDisableVertexAttribArray(mPositionHandle);
        GLES20.glDisableVertexAttribArray(mNormalHandle);
        GLES20.glDisableVertexAttribArray(mTexCoordHandle);
    }

    // Helper method to load shaders
    private int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        // Check for compilation errors
        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            Log.e(TAG, "Shader compilation failed: " + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }
}