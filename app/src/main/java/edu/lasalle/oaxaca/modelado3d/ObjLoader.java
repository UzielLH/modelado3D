package edu.lasalle.oaxaca.modelado3d;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;

public class ObjLoader {
    private static final String TAG = "ObjLoader";

    // Model data
    private FloatBuffer vertexBuffer;
    private FloatBuffer normalBuffer;
    private FloatBuffer texCoordBuffer;
    private ShortBuffer indexBuffer;
    private int numFaces = 0;

    // Constructor to load an OBJ file from resources
    public ObjLoader(Context context, int resourceId) {
        try {
            loadObj(context, resourceId);
        } catch (IOException e) {
            Log.e(TAG, "Error loading OBJ file", e);
        }
    }

    private void loadObj(Context context, int resourceId) throws IOException {
        // Initialize lists to store data
        List<Float> vertices = new ArrayList<>();
        List<Float> normals = new ArrayList<>();
        List<Float> texCoords = new ArrayList<>();
        List<Short> indices = new ArrayList<>();

        // Lists to store model data (v, vn, vt)
        List<Float> tempVertices = new ArrayList<>();
        List<Float> tempNormals = new ArrayList<>();
        List<Float> tempTexCoords = new ArrayList<>();

        // Open the resource file
        InputStream inputStream = context.getResources().openRawResource(resourceId);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        String line;
        short index = 0;

        // Read file line by line
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("v ")) {
                // Vertex
                String[] parts = line.split("\\s+");
                if (parts.length >= 4) {
                    tempVertices.add(Float.parseFloat(parts[1]));
                    tempVertices.add(Float.parseFloat(parts[2]));
                    tempVertices.add(Float.parseFloat(parts[3]));
                }
            } else if (line.startsWith("vn ")) {
                // Normal
                String[] parts = line.split("\\s+");
                if (parts.length >= 4) {
                    tempNormals.add(Float.parseFloat(parts[1]));
                    tempNormals.add(Float.parseFloat(parts[2]));
                    tempNormals.add(Float.parseFloat(parts[3]));
                }
            } else if (line.startsWith("vt ")) {
                // Texture coordinate
                String[] parts = line.split("\\s+");
                if (parts.length >= 3) {
                    tempTexCoords.add(Float.parseFloat(parts[1]));
                    tempTexCoords.add(Float.parseFloat(parts[2]));
                }
            } else if (line.startsWith("f ")) {
                // Face - parse indices
                String[] parts = line.split("\\s+");

                // Process triangles (3 vertices per face)
                for (int i = 1; i <= 3; i++) {
                    String[] faceParts = parts[i].split("/");

                    // Get vertex index (OBJ is 1-based, so subtract 1)
                    int vertexIdx = Integer.parseInt(faceParts[0]) - 1;

                    // Add vertex coordinates
                    vertices.add(tempVertices.get(vertexIdx * 3));
                    vertices.add(tempVertices.get(vertexIdx * 3 + 1));
                    vertices.add(tempVertices.get(vertexIdx * 3 + 2));

                    // Add texture coordinates if available
                    if (faceParts.length > 1 && !faceParts[1].isEmpty()) {
                        int texIdx = Integer.parseInt(faceParts[1]) - 1;
                        texCoords.add(tempTexCoords.get(texIdx * 2));
                        texCoords.add(tempTexCoords.get(texIdx * 2 + 1));
                    } else {
                        // Default texture coordinates
                        texCoords.add(0.0f);
                        texCoords.add(0.0f);
                    }

                    // Add normal coordinates if available
                    if (faceParts.length > 2) {
                        int normalIdx = Integer.parseInt(faceParts[2]) - 1;
                        normals.add(tempNormals.get(normalIdx * 3));
                        normals.add(tempNormals.get(normalIdx * 3 + 1));
                        normals.add(tempNormals.get(normalIdx * 3 + 2));
                    } else {
                        // Default normal
                        normals.add(0.0f);
                        normals.add(1.0f);
                        normals.add(0.0f);
                    }

                    // Add to index buffer
                    indices.add(index++);
                }
                numFaces++;
            }
        }

        reader.close();

        // Create GPU buffers
        vertexBuffer = createFloatBuffer(vertices);
        normalBuffer = createFloatBuffer(normals);
        texCoordBuffer = createFloatBuffer(texCoords);
        indexBuffer = createShortBuffer(indices);

        Log.d(TAG, "Loaded OBJ with " + numFaces + " faces");
    }

    // Create a float buffer from a list of floats
    private FloatBuffer createFloatBuffer(List<Float> data) {
        if (data.isEmpty()) return null;

        FloatBuffer buffer;
        ByteBuffer bb = ByteBuffer.allocateDirect(data.size() * 4);
        bb.order(ByteOrder.nativeOrder());
        buffer = bb.asFloatBuffer();

        for (float f : data) {
            buffer.put(f);
        }
        buffer.position(0);
        return buffer;
    }

    // Create a short buffer from a list of shorts
    private ShortBuffer createShortBuffer(List<Short> data) {
        if (data.isEmpty()) return null;

        ShortBuffer buffer;
        ByteBuffer bb = ByteBuffer.allocateDirect(data.size() * 2);
        bb.order(ByteOrder.nativeOrder());
        buffer = bb.asShortBuffer();

        for (short s : data) {
            buffer.put(s);
        }
        buffer.position(0);
        return buffer;
    }

    // Getters for buffers
    public FloatBuffer getVertexBuffer() {
        return vertexBuffer;
    }

    public FloatBuffer getNormalBuffer() {
        return normalBuffer;
    }

    public FloatBuffer getTexCoordBuffer() {
        return texCoordBuffer;
    }

    public ShortBuffer getIndexBuffer() {
        return indexBuffer;
    }

    public int getNumFaces() {
        return numFaces;
    }

    public int getVertexCount() {
        return numFaces * 3;
    }
}