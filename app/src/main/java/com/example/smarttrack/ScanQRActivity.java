package com.example.smarttrack;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class ScanQRActivity extends AppCompatActivity {

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
        String roomId = scannedData; // Assume QR code contains the room ID

        // Fetch room details from Firestore
        FirebaseFirestore.getInstance().collection("rooms").document(roomId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String subjectCode = documentSnapshot.getString("subjectCode");
                        String section = documentSnapshot.getString("section");
                        String teacherId = documentSnapshot.getString("teacherId");

                        // Save to the 'section' collection
                        FirebaseFirestore.getInstance().collection("section")
                                .add(new SectionModel(subjectCode, section, teacherId, FirebaseAuth.getInstance().getCurrentUser().getUid()))
                                .addOnSuccessListener(documentReference ->
                                        Toast.makeText(this, "Successfully joined the room!", Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, "Error saving data: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    } else {
                        Toast.makeText(this, "Room not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to fetch room details: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
