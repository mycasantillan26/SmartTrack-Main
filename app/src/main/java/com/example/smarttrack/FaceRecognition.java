package com.example.smarttrack;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.camera.view.PreviewView;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@OptIn(markerClass = androidx.camera.core.ExperimentalGetImage.class)
public class FaceRecognition extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final String TAG = "FaceRecognition";

    private PreviewView previewView;
    private TextView instructionText;
    private ExecutorService cameraExecutor;

    private boolean frontDetected = false;
    private boolean leftDetected = false;
    private boolean rightDetected = false;
    private boolean upDetected = false;
    private boolean downDetected = false;

    private int currentStep = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_recognition);

        previewView = findViewById(R.id.previewView);
        instructionText = findViewById(R.id.instructionText);
        cameraExecutor = Executors.newSingleThreadExecutor();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        }

        updateInstruction(getNextInstruction());
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                        .build();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeImage);
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            } catch (Exception e) {
                Log.e(TAG, "Error starting camera: ", e);
                Toast.makeText(this, "Failed to start camera.", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void analyzeImage(@NonNull ImageProxy imageProxy) {
        Image mediaImage = imageProxy.getImage();
        if (mediaImage != null) {
            InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());

            FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                    .build();

            FaceDetector detector = FaceDetection.getClient(options);

            detector.process(image)
                    .addOnSuccessListener(faces -> {
                        if (!faces.isEmpty()) {
                            handleDetectedFace(faces.get(0));
                        } else {
                            updateInstruction("No face detected. Please align your face.");
                        }
                        imageProxy.close();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Face detection failed: ", e);
                        imageProxy.close();
                    });
        } else {
            imageProxy.close();
        }
    }

    private void handleDetectedFace(@NonNull Face face) {
        float headEulerY = face.getHeadEulerAngleY(); // Left (-), Right (+)
        float headEulerX = face.getHeadEulerAngleX(); // Up (-), Down (+)

        switch (currentStep) {
            case 0:
                if (Math.abs(headEulerY) < 10 && Math.abs(headEulerX) < 10) {
                    frontDetected = true;
                    updateInstruction("Front face detected!");
                    currentStep++;
                    updateInstruction(getNextInstruction());
                }
                break;
            case 1:
                if (headEulerY < -30) {
                    leftDetected = true;
                    updateInstruction("Left face detected!");
                    currentStep++;
                    updateInstruction(getNextInstruction());
                }
                break;
            case 2:
                if (headEulerY > 30) {
                    rightDetected = true;
                    updateInstruction("Right face detected!");
                    currentStep++;
                    updateInstruction(getNextInstruction());
                }
                break;
            case 3:
                if (headEulerX < -30) {
                    upDetected = true;
                    updateInstruction("Looking up detected!");
                    currentStep++;
                    updateInstruction(getNextInstruction());
                }
                break;
            case 4:
                if (headEulerX > 30) {
                    downDetected = true;
                    updateInstruction("Looking down detected!");
                    currentStep++;
                    updateInstruction("All orientations detected! Proceeding...");
                    navigateToLogin();
                }
                break;
        }
    }

    private String getNextInstruction() {
        switch (currentStep) {
            case 0:
                return "Step 1: Align your face to the front.";
            case 1:
                return "Step 2: Turn your head to the left.";
            case 2:
                return "Step 3: Turn your head to the right.";
            case 3:
                return "Step 4: Look up.";
            case 4:
                return "Step 5: Look down.";
            default:
                return "Face recognition complete.";
        }
    }

    private void updateInstruction(String instruction) {
        runOnUiThread(() -> instructionText.setText(instruction));
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, Login.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}
