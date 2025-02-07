package com.example.smarttrack;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.Timestamp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import org.tensorflow.lite.Interpreter;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@OptIn(markerClass = androidx.camera.core.ExperimentalGetImage.class)
public class FaceRecognition extends AppCompatActivity {

    private static final float MATCH_THRESHOLD = 0.5f;
    private PreviewView previewView;
    private TextView instructionText;
    private Interpreter tflite;
    private Map<String, float[]> databaseEmbeddings;
    private ExecutorService cameraExecutor;
    private boolean isFaceRecognized = false;
    private String roomId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_recognition);
        requestLocationPermission();

        previewView = findViewById(R.id.previewView);
        instructionText = findViewById(R.id.instructionText);
        cameraExecutor = Executors.newSingleThreadExecutor();
        databaseEmbeddings = new HashMap<>();

        try {
            tflite = new Interpreter(loadModelFile());
            debugMessage("✅ TensorFlow Lite Model Loaded Successfully!");
        } catch (IOException e) {
            debugMessage("❌ ERROR: Failed to load TensorFlow Lite model");
            tflite = null;
        }

        roomId = getIntent().getStringExtra("roomId");
        debugMessage("🔥 Room ID: " + roomId);

        loadDatabaseEmbeddings();
        startCamera();
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                        .build();

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
                imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeImage);
                cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis);
                debugMessage("✅ Camera Started Successfully!");
            } catch (Exception e) {
                debugMessage("❌ Error: Failed to start camera.");
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void analyzeImage(@NonNull ImageProxy imageProxy) {
        if (isFaceRecognized) {
            imageProxy.close();
            return;
        }

        InputImage image;
        try {
            image = InputImage.fromMediaImage(imageProxy.getImage(), imageProxy.getImageInfo().getRotationDegrees());
        } catch (Exception e) {
            debugMessage("❌ Error processing image.");
            imageProxy.close();
            return;
        }

        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .build();
        FaceDetector detector = FaceDetection.getClient(options);

        detector.process(image)
                .addOnSuccessListener(faces -> {
                    if (!faces.isEmpty()) {
                        debugMessage("✅ Face detected!");
                        cameraExecutor.execute(() -> processFaceRecognition(imageProxy));
                    } else {
                        debugMessage("❌ No face detected.");
                        imageProxy.close();
                    }
                })
                .addOnFailureListener(e -> {
                    debugMessage("❌ Face detection failed.");
                    imageProxy.close();
                });
    }

    private void processFaceRecognition(ImageProxy imageProxy) {
        Bitmap bitmap = imageProxyToBitmap(imageProxy);
        imageProxy.close();
        if (bitmap == null) {
            debugMessage("❌ Error: Failed to process image.");
            return;
        }

        float[] detectedEmbedding = extractEmbedding(bitmap);
        if (detectedEmbedding == null) {
            debugMessage("❌ Error: Failed to extract face features.");
            return;
        }

        String recognizedUid = matchFace(detectedEmbedding);
        if (recognizedUid != null) {
            if (!isFaceRecognized) {
                isFaceRecognized = true;
                debugMessage("✅ Face recognized! Recording time-in...");
                recordTimeIn(roomId, recognizedUid);
            }
        } else {
            debugMessage("❌ Face not recognized. Try again.");
        }
    }

    private void recordTimeIn(String roomId, String recognizedUid) {
        if (roomId == null || recognizedUid == null) {
            debugMessage("❌ Error: Room ID or UID missing.");
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("rooms").document(roomId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Timestamp startTime = documentSnapshot.getTimestamp("startTime");
                        if (startTime != null) {
                            compareTimeAndRecord(roomId, recognizedUid, startTime);
                        } else {
                            debugMessage("⚠️ No startTime found for this room. Recording attendance without status.");
                            compareTimeAndRecord(roomId, recognizedUid, null);
                        }
                    }
                })
                .addOnFailureListener(e -> debugMessage("❌ Error fetching room startTime."));
    }

    private void compareTimeAndRecord(String roomId, String recognizedUid, Timestamp startTime) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            debugMessage("❌ Location permission not granted!");
            return;
        }

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        double latitude = lastKnownLocation != null ? lastKnownLocation.getLatitude() : 0.0;
        double longitude = lastKnownLocation != null ? lastKnownLocation.getLongitude() : 0.0;

        Timestamp timeIn = Timestamp.now();
        String status = determineStatus(timeIn, startTime);

        saveAttendanceData(roomId, recognizedUid, timeIn, latitude, longitude, status);
    }

    private void saveAttendanceData(String roomId, String recognizedUid, Timestamp timeIn, Double latitude, Double longitude, String status) {
        Map<String, Object> attendanceData = new HashMap<>();
        attendanceData.put("timeIn", timeIn);
        attendanceData.put("date", FieldValue.serverTimestamp()); // Date for filtering
        attendanceData.put("status", status);

        if (latitude != 0.0 && longitude != 0.0) {
            Map<String, Object> locationData = new HashMap<>();
            locationData.put("latitude", latitude);
            locationData.put("longitude", longitude);
            attendanceData.put("location", locationData);
        }

        FirebaseFirestore.getInstance()
                .collection("rooms").document(roomId)
                .collection("students").document(recognizedUid)
                .collection("attendance")
                .add(attendanceData)
                .addOnSuccessListener(documentReference -> {
                    debugMessage("✅ Time In recorded with status: " + status);
                    goToHome();
                })
                .addOnFailureListener(e -> debugMessage("❌ Error: Time In failed."));
    }


    private String determineStatus(Timestamp timeIn, Timestamp startTime) {
        if (startTime == null) return "Unknown"; // If no startTime is set in Firestore

        Date startDate = startTime.toDate();
        Date timeInDate = timeIn.toDate();

        long difference = timeInDate.getTime() - startDate.getTime(); // Time difference in milliseconds

        if (difference <= 0) {
            return "On Time"; // Student clocked in at or before class start time
        } else {
            return "Late"; // Student clocked in after the class start time
        }
    }


    private void requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
    }



    private String matchFace(float[] detectedEmbedding) {
        float minDistance = Float.MAX_VALUE;
        String bestMatch = null;

        if (databaseEmbeddings.isEmpty()) {
            debugMessage("❌ No stored face embeddings found in the database.");
            return null;
        }

        for (Map.Entry<String, float[]> entry : databaseEmbeddings.entrySet()) {
            float distance = calculateDistance(detectedEmbedding, entry.getValue());

            debugMessage("🔍 Checking " + entry.getKey() + " | Distance: " + distance);

            if (distance < minDistance) {
                minDistance = distance;
                bestMatch = entry.getKey();
            }
        }

        debugMessage("🔥 Best Match: " + bestMatch + " | Distance: " + minDistance);

        return (minDistance < MATCH_THRESHOLD) ? bestMatch : null;
    }

    private float calculateDistance(float[] embedding1, float[] embedding2) {
        if (embedding1 == null || embedding2 == null) {
            debugMessage("❌ Error: One or both embeddings are NULL!");
            return Float.MAX_VALUE;
        }

        if (embedding1.length != embedding2.length) {
            debugMessage("❌ Error: Embeddings have different dimensions! " +
                    "Expected: 128, Found: " + embedding1.length + " and " + embedding2.length);
            return Float.MAX_VALUE;
        }

        float sum = 0;
        for (int i = 0; i < embedding1.length; i++) {
            float diff = embedding1[i] - embedding2[i];
            sum += diff * diff;
        }

        float distance = (float) Math.sqrt(sum);
        debugMessage("📏 Calculated Distance: " + distance);
        return distance;
    }


    private float[] extractEmbedding(Bitmap bitmap) {
        if (tflite == null) {
            debugMessage("❌ TensorFlow Lite model is NULL! Cannot extract features.");
            return null;
        }

        if (bitmap == null) {
            debugMessage("❌ Error: Bitmap is NULL! Cannot extract features.");
            return null;
        }

        try {
            debugMessage("ℹ️ Resizing image to 80x80 for FaceNet model...");
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, 80, 80, true);
            if (resizedBitmap == null) {
                debugMessage("❌ Error: Failed to resize bitmap.");
                return null;
            }

            debugMessage("ℹ️ Preparing TensorFlow input...");
            ByteBuffer inputBuffer = ByteBuffer.allocateDirect(80 * 80 * 3 * 4); // ✅ UINT8 model (1 byte per channel)
            inputBuffer.order(ByteOrder.nativeOrder());

            int[] intValues = new int[80 * 80];
            resizedBitmap.getPixels(intValues, 0, 80, 0, 0, 80, 80);

            for (int pixel : intValues) {
                inputBuffer.put((byte) (pixel & 0xFF));         // Blue
                inputBuffer.put((byte) ((pixel >> 8) & 0xFF));  // Green
                inputBuffer.put((byte) ((pixel >> 16) & 0xFF)); // Red
            }

            debugMessage("ℹ️ Running TensorFlow inference...");
            byte[][] output = new byte[1][512]; // ✅ Ensure output matches expected data type
            tflite.run(inputBuffer, output);

            float[] floatOutput = new float[512];
            for (int i = 0; i < 128; i++) {
                floatOutput[i] = (output[0][i] & 0xFF) / 255.0f; // Convert UINT8 to FLOAT32
            }

            debugMessage("✅ Face feature extraction successful!");
            return floatOutput;

        } catch (Exception e) {
            debugMessage("❌ Error: Exception in feature extraction: " + e.getMessage());
            return null;
        }
    }




    private Bitmap imageProxyToBitmap(ImageProxy imageProxy) {
        Image image = imageProxy.getImage();
        if (image == null) {
            debugMessage("❌ Error: ImageProxy is NULL");
            return null;
        }

        try {
            ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
            ByteBuffer uBuffer = image.getPlanes()[1].getBuffer();
            ByteBuffer vBuffer = image.getPlanes()[2].getBuffer();

            int ySize = yBuffer.remaining();
            int uSize = uBuffer.remaining();
            int vSize = vBuffer.remaining();

            byte[] nv21 = new byte[ySize + uSize + vSize];

            yBuffer.get(nv21, 0, ySize);
            vBuffer.get(nv21, ySize, vSize);
            uBuffer.get(nv21, ySize + vSize, uSize);

            YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            yuvImage.compressToJpeg(new Rect(0, 0, image.getWidth(), image.getHeight()), 100, out);

            byte[] imageBytes = out.toByteArray();
            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

            if (bitmap == null) {
                debugMessage("❌ Error: Failed to convert ImageProxy to Bitmap");
                return null;
            }

            debugMessage("✅ ImageProxy converted to Bitmap successfully!");
            return bitmap;
        } catch (Exception e) {
            debugMessage("❌ Exception in Bitmap conversion: " + e.getMessage());
            return null;
        }
    }

    private void loadDatabaseEmbeddings() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("face_detections").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        debugMessage("❌ No face embeddings found in the database.");
                        return;
                    }

                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        String userId = document.getId();
                        Object embeddingObject = document.get("embedding");

                        if (embeddingObject instanceof List) {
                            List<Double> embeddingList = (List<Double>) embeddingObject;
                            float[] embeddingArray = new float[embeddingList.size()];

                            for (int i = 0; i < embeddingList.size(); i++) {
                                embeddingArray[i] = embeddingList.get(i).floatValue();
                            }

                            databaseEmbeddings.put(userId, embeddingArray);
                            debugMessage("✅ Loaded embedding for user: " + userId);
                        } else {
                            debugMessage("⚠️ No valid embedding found for user: " + userId);
                        }
                    }
                })
                .addOnFailureListener(e -> debugMessage("❌ Error loading embeddings: " + e.getMessage()));
    }



    private MappedByteBuffer loadModelFile() throws IOException {
        try {
            AssetFileDescriptor fileDescriptor = getAssets().openFd("facenet.tflite");
            FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
            FileChannel fileChannel = inputStream.getChannel();
            long startOffset = fileDescriptor.getStartOffset();
            long declaredLength = fileDescriptor.getDeclaredLength();

            debugMessage("✅ TensorFlow Lite Model Loaded Successfully!");
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
        } catch (Exception e) {
            debugMessage("❌ ERROR: Could not load TensorFlow model. " + e.getMessage());
            return null;
        }
    }



    private void goToHome() {
        startActivity(new Intent(FaceRecognition.this, Students_Home.class));
        finish();
    }

    private void debugMessage(String message) {
        Log.d("DEBUG_LOG", message); // ✅ Logs message to Logcat
        showToast(message);
        updateInstruction(message);
    }


    private void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(FaceRecognition.this, message, Toast.LENGTH_SHORT).show());
    }

    private void updateInstruction(String instruction) {
        runOnUiThread(() -> instructionText.setText(instruction));
    }
}
