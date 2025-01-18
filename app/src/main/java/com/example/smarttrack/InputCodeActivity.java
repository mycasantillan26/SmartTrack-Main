package com.example.smarttrack;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class InputCodeActivity extends AppCompatActivity {

    private EditText editTextRoomCode;
    private Button buttonSubmitCode;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_input_code);

        editTextRoomCode = findViewById(R.id.editTextRoomCode);
        buttonSubmitCode = findViewById(R.id.buttonSubmitCode);
        firestore = FirebaseFirestore.getInstance();

        buttonSubmitCode.setOnClickListener(v -> {
            String roomCode = editTextRoomCode.getText().toString().trim();
            if (!roomCode.isEmpty()) {
                verifyRoomCode(roomCode);
            } else {
                Toast.makeText(InputCodeActivity.this, "Please enter a room code.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void verifyRoomCode(String roomCode) {
        firestore.collection("rooms")
                .whereEqualTo("roomCode", roomCode)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        // Extract room details from the first matching document
                        DocumentSnapshot document = task.getResult().getDocuments().get(0);
                        String subjectCode = document.getString("subjectCode");
                        String section = document.getString("section");
                        String roomId = document.getId(); // Use document ID as roomId
                        String teacherId = document.getString("teacherId");

                        saveToSection(subjectCode, section, roomId, teacherId);
                    } else {
                        Toast.makeText(InputCodeActivity.this, "Invalid room code. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(InputCodeActivity.this, "Error fetching room details: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveToSection(String subjectCode, String section, String roomId, String teacherId) {
        String studentId = FirebaseAuth.getInstance().getUid();
        if (studentId != null) {
            String sectionName = subjectCode + "-" + section;

            // Add the student data into the 'students' subcollection
            firestore.collection("sections").document(sectionName)
                    .collection("students").document(studentId)
                    .set(new StudentModel(studentId, roomId, subjectCode, section, teacherId))
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(InputCodeActivity.this, "Successfully joined the section.", Toast.LENGTH_SHORT).show();
                        navigateToStudentsRoom(sectionName, teacherId);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(InputCodeActivity.this, "Error saving student data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            Toast.makeText(this, "You are not logged in.", Toast.LENGTH_SHORT).show();
        }
    }

    private void navigateToStudentsRoom(String sectionName, String teacherId) {
        Intent intent = new Intent(InputCodeActivity.this, Students_Room.class);
        intent.putExtra("sectionName", sectionName);
        intent.putExtra("teacherId", teacherId);
        startActivity(intent);
        finish();
    }
}
