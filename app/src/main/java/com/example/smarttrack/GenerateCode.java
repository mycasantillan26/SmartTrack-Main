package com.example.smarttrack;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;

public class GenerateCode extends AppCompatActivity {

    private TextView titleTextView;
    private TextView roomCodeTextView;
    private ImageView qrCodeImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generate_code);

        titleTextView = findViewById(R.id.titleTextView);
        roomCodeTextView = findViewById(R.id.roomCodeTextView);
        qrCodeImageView = findViewById(R.id.qrCodeImageView);

        // Get extras passed from previous activity
        String roomCode = getIntent().getStringExtra("roomCode");
        String subjectSection = getIntent().getStringExtra("subjectSection");

        if (subjectSection != null) {
            titleTextView.setText(subjectSection);
        } else {
            titleTextView.setText("No Subject - Section Provided");
        }

        if (roomCode != null) {
            roomCodeTextView.setText("Room Code: " + roomCode);
            generateQRCode(roomCode);
        } else {
            roomCodeTextView.setText("No Room Code Provided");
        }
    }

    private void generateQRCode(String data) {
        MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
        try {
            BitMatrix bitMatrix = multiFormatWriter.encode(data, BarcodeFormat.QR_CODE, 500, 500);
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.createBitmap(bitMatrix);
            qrCodeImageView.setImageBitmap(bitmap);
        } catch (WriterException e) {
            e.printStackTrace();
        }


}
}