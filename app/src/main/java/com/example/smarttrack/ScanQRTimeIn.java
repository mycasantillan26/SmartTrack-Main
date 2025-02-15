package com.example.smarttrack;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ScanQRTimeIn extends AppCompatActivity {

    private String scannedRoomId; // Store the scanned roomId

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_qr);

        // Lock orientation to portrait
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Start QR Scanner
        new IntentIntegrator(this)
                .setCaptureActivity(CustomCaptureActivity.class)
                .setOrientationLocked(true)
                .setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
                .setBeepEnabled(true)
                .setPrompt("Scan a QR code")
                .initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        handleQRScanResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data); // Pass unhandled cases to the superclass
    }

    private void handleQRScanResult(int requestCode, int resultCode, Intent data) {
        IntentResult qrResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (qrResult != null) {
            if (qrResult.getContents() == null) {
                Toast.makeText(this, "QR Scan canceled.", Toast.LENGTH_SHORT).show();
                finish(); // Close activity
            } else {
                processScannedData(qrResult.getContents());
            }
        }
    }

    private void processScannedData(String scannedData) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Log.d("ScanQRTimeIn", "🔍 Scanned QR Data: " + scannedData);

        // Get the expected roomId from Intent
        String expectedRoomId = getIntent().getStringExtra("roomId");
        Log.d("ScanQRTimeIn", "🏫 Expected Room ID (From Intent): " + expectedRoomId);

        // Extract the roomId and date from scanned QR
        String[] parts = scannedData.split("_");
        if (parts.length != 2) {
            Toast.makeText(this, "❌ Invalid QR format!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        scannedRoomId = parts[0];  // Extract roomId from QR
        String scannedDate = parts[1]; // Extract scanned date (YYYYMMDD)

        // 🔥 STRICT VALIDATION: Ensure the scanned QR belongs to the correct room
        if (!scannedRoomId.equals(expectedRoomId)) {
            Log.e("ScanQRTimeIn", "❌ QR belongs to another room! Expected: " + expectedRoomId + " | Scanned: " + scannedRoomId);
            Toast.makeText(this, "❌ This QR code is NOT for this room!", Toast.LENGTH_LONG).show();
            finish();
            return; // 🔥 Stop further execution
        }

        // 🔥 Proceed to check if the QR code is valid for today
        verifyDailyCode(scannedRoomId, scannedDate, scannedData);
    }


    private void verifyDailyCode(String scannedRoomId, String scannedDate, String scannedData) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Generate today's date in YYYYMMDD format
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        String currentDate = sdf.format(new Date());

        // Step 3: Strictly validate the scanned QR with today's dailyCode
        db.collection("rooms").document(scannedRoomId)
                .collection("dailyCodes")
                .document(currentDate) // Get today's dailyCode document
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String validDailyCode = documentSnapshot.getString("attendanceCode");

                        Log.d("ScanQRTimeIn", "✅ Firestore Attendance Code: " + validDailyCode);
                        Log.d("ScanQRTimeIn", "🔍 Scanned QR Code: " + scannedData);
                        Log.d("ScanQRTimeIn", "🔍 Expected Room ID: " + scannedRoomId);

                        // 🔥 STRICT VALIDATION: The attendanceCode in Firestore MUST start with the scannedRoomId
                        if (validDailyCode != null && validDailyCode.equals(scannedData)
                                && scannedDate.equals(currentDate)
                                && validDailyCode.startsWith(scannedRoomId + "_")) {

                            // ✅ QR Code is valid for today and belongs to this specific room
                            Log.d("ScanQRTimeIn", "✅ Attendance Code Validated Successfully!");
                            recordTimeIn(scannedRoomId, FirebaseAuth.getInstance().getCurrentUser().getUid());
                            //startFaceRecognition(); // ✅ Proceed to Face Recognition
                        } else {
                            // ❌ QR code is either expired or belongs to another room
                            Log.e("ScanQRTimeIn", "❌ Invalid QR Code! This QR is NOT for this room.");
                            Toast.makeText(this, "❌ This QR code is NOT for this room!", Toast.LENGTH_LONG).show();
                            finish(); // 🔥 Close activity, do NOT proceed
                        }
                    } else {
                        // ❌ No dailyCode found for today in this room
                        Log.e("ScanQRTimeIn", "❌ No active attendance code for this room today.");
                        Toast.makeText(this, "❌ No active attendance code for this room today.", Toast.LENGTH_LONG).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ScanQRTimeIn", "❌ Error verifying attendance code: " + e.getMessage());
                    Toast.makeText(this, "❌ Error verifying attendance code: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
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

    private void startFaceRecognition() {
        Intent intent = new Intent(this, FaceRecognition.class);
        intent.putExtra("roomId", scannedRoomId);
        intent.putExtra("uid", FirebaseAuth.getInstance().getCurrentUser().getUid());
        startActivity(intent);
    }

    private void debugMessage(String message) {
        Log.d("DEBUG_LOG", message); // ✅ Logs message to Logcat
        showToast(message);
    }


    private void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(ScanQRTimeIn.this, message, Toast.LENGTH_SHORT).show());
    }

    private void goToHome() {
        startActivity(new Intent(ScanQRTimeIn.this, Students_Home.class));
        finish();
    }

}

