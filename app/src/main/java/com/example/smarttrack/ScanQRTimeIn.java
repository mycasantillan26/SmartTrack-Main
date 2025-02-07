package com.example.smarttrack;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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
                            startFaceRecognition(); // ✅ Proceed to Face Recognition
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




    private void startFaceRecognition() {
        Intent intent = new Intent(this, FaceRecognition.class);
        intent.putExtra("roomId", scannedRoomId);
        intent.putExtra("uid", FirebaseAuth.getInstance().getCurrentUser().getUid());
        startActivity(intent);

        // ✅ Close this activity after starting Face Recognition
        finish();
    }
}
