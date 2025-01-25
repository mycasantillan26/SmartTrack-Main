package com.example.smarttrack;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class ScanQRTimeOut extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_qr);

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
        // Assume scannedData contains the room code, not the room ID
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Query the rooms collection to find the roomId for the given room code
        db.collection("rooms")
                .whereEqualTo("roomCode", scannedData) // Replace "roomCode" with the actual field name in Firestore
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Room found, get the roomId
                        String roomId = queryDocumentSnapshots.getDocuments().get(0).getId();

                        // Record time-out for the student in Firestore
                        recordTimeOut(roomId);
                    } else {
                        // No room found for the scanned code
                        Toast.makeText(this, "Room not found for the scanned code.", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_CANCELED);
                        finish(); // Close the activity
                    }
                })
                .addOnFailureListener(e -> {
                    // Error occurred while querying Firestore
                    Toast.makeText(this, "Error fetching room details: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    setResult(RESULT_CANCELED);
                    finish(); // Close the activity
                });
    }

    private void recordTimeOut(String roomId) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid(); // Get current user ID
        String attendancePath = "rooms/" + roomId + "/students/" + uid + "/attendance";

        // Fetch the latest attendance record
        FirebaseFirestore.getInstance().collection(attendancePath)
                .orderBy("timeIn", Query.Direction.DESCENDING) // Sort by timeIn in descending order
                .limit(1) // Fetch the latest record
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Get the latest attendance record
                        queryDocumentSnapshots.getDocuments().get(0).getReference()
                                .update("timeOut", FieldValue.serverTimestamp()) // Update timeOut
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Time Out recorded successfully.", Toast.LENGTH_SHORT).show();

                                    // Send the roomId back to the calling activity
                                    Intent resultIntent = new Intent();
                                    resultIntent.putExtra("roomId", roomId);
                                    setResult(RESULT_OK, resultIntent);
                                    finish(); // Close the activity
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Failed to record Time Out: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    setResult(RESULT_CANCELED);
                                    finish(); // Close the activity
                                });
                    } else {
                        Toast.makeText(this, "No attendance record found to update Time Out.", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_CANCELED);
                        finish(); // Close the activity
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error fetching attendance records: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    setResult(RESULT_CANCELED);
                    finish(); // Close the activity
                });
    }
}
