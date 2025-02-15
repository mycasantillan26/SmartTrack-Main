package com.example.smarttrack;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ScanQRTimeOut extends AppCompatActivity {
    String feedback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_qr);

        feedback = getIntent().getStringExtra("feedback");

        // Lock orientation to portrait
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Configure and start the QR scanner
        new IntentIntegrator(this)
                .setCaptureActivity(CustomCaptureActivity.class) // Use the custom activity
                .setOrientationLocked(true) // Lock orientation
                .setDesiredBarcodeFormats(IntentIntegrator.QR_CODE) // Restrict to QR codes
                .setBeepEnabled(true) // Enable beep sound
                .setPrompt("Scan a QR code") // Display prompt
                .initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(this, "Scan cancelled", Toast.LENGTH_SHORT).show();
            } else {
                // Process scanned data
                processScannedData(result.getContents());
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void processScannedData(String scannedData) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Log.d("ScanQRTimeOut", "🔍 Scanned QR Data: " + scannedData);

        // Get the expected roomId from Intent
        String expectedRoomId = getIntent().getStringExtra("roomId");
        Log.d("ScanQRTimeOut", "🏫 Expected Room ID (From Intent): " + expectedRoomId);

        // Extract the roomId and date from scanned QR
        String[] parts = scannedData.split("_");
        if (parts.length != 2) {
            Toast.makeText(this, "❌ Invalid QR format!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String scannedRoomId = parts[0];  // Extract roomId from QR
        String scannedDate = parts[1]; // Extract scanned date (YYYYMMDD)

        // 🔥 STRICT VALIDATION: Ensure the scanned QR belongs to the correct room
        if (!scannedRoomId.equals(expectedRoomId)) {
            Log.e("ScanQRTimeOut", "❌ QR belongs to another room! Expected: " + expectedRoomId + " | Scanned: " + scannedRoomId);
            Toast.makeText(this, "❌ This QR code is NOT for this room!", Toast.LENGTH_LONG).show();
            finish();
            return; // 🔥 Stop further execution
        }

        // 🔥 Proceed to check if the QR code is valid for today
        verifyDailyCodeForTimeOut(scannedRoomId, scannedDate, scannedData);
    }


    private void verifyDailyCodeForTimeOut(String roomId, String scannedDate, String scannedData) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Generate today's date in YYYYMMDD format
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        String currentDate = sdf.format(new Date());

        // Step 3: Check if the scanned QR matches the stored dailyCode for today
        db.collection("rooms").document(roomId)
                .collection("dailyCodes")
                .document(currentDate) // Get today's dailyCode document
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String validDailyCode = documentSnapshot.getString("attendanceCode");

                        if (validDailyCode != null && validDailyCode.equals(scannedData) && scannedDate.equals(currentDate)) {
                            // ✅ QR Code is valid for today, proceed to Time-Out recording
                            recordTimeOut(roomId);
                        } else {
                            // ❌ Scanned code is either expired or invalid
                            Toast.makeText(this, "❌ Invalid or expired QR Code!", Toast.LENGTH_LONG).show();
                            finish();
                        }
                    } else {
                        // ❌ No dailyCode found for today
                        Toast.makeText(this, "❌ No active attendance code for this room today.", Toast.LENGTH_LONG).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "❌ Error verifying attendance code: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }




    private void recordTimeOut(String roomId) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid(); // Get current user ID
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Reference to the student's attendance subcollection inside the room
        db.collection("rooms").document(roomId)
                .collection("students").document(uid)
                .collection("attendance") // ✅ Query the attendance subcollection
                .orderBy("timeIn", Query.Direction.DESCENDING) // ✅ Get the latest attendance record
                .limit(1) // ✅ Only fetch the latest one
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot documentSnapshot = queryDocumentSnapshots.getDocuments().get(0); // ✅ Get the latest attendance document
                        Timestamp timeIn = documentSnapshot.getTimestamp("timeIn");
                        Timestamp timeOut = documentSnapshot.getTimestamp("timeOut");

                        if (timeIn != null && timeOut == null) {
                            Map<String, Object> updateData = new HashMap<>();
                            updateData.put("timeOut", FieldValue.serverTimestamp());
                            updateData.put("feedback", feedback);

                            documentSnapshot.getReference()
                                    .update(updateData)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(this, "✅ Time Out recorded successfully.", Toast.LENGTH_SHORT).show();
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this, "❌ Failed to record Time Out: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        finish();
                                    });
                        } else if (timeOut != null) {
                            Toast.makeText(this, "❌ You have already timed out!", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(this, "❌ No Time-In record found!", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    } else {
                        Toast.makeText(this, "❌ No attendance record found!", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "❌ Error fetching attendance record: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

}
