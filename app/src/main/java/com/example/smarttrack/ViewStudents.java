package com.example.smarttrack;

import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class ViewStudents extends AppCompatActivity {

    private static final String TAG = "ViewStudents";
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_students);

        db = FirebaseFirestore.getInstance();

        String roomCode = getIntent().getStringExtra("roomCode");

        if (roomCode == null || roomCode.isEmpty()) {
            Toast.makeText(this, "Room code not provided. Please try again.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Room code is null or empty.");
            finish(); // Exit the activity if no roomCode is provided
            return;
        }

        Log.d(TAG, "Room code provided: " + roomCode);
        fetchRoomIdFromRoomCode(roomCode);
    }

    private void fetchRoomIdFromRoomCode(String roomCode) {
        Log.d(TAG, "Fetching room ID for room code: " + roomCode);

        db.collection("rooms")
                .whereEqualTo("roomCode", roomCode)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot roomDoc = queryDocumentSnapshots.getDocuments().get(0);
                        String roomId = roomDoc.getId();
                        Log.d(TAG, "Room ID found: " + roomId);
                        fetchStudentsFromRoom(roomId);
                    } else {
                        Toast.makeText(this, "No room found for this code.", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "No room found for room code: " + roomCode);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching room ID for room code: " + roomCode, e);
                    Toast.makeText(this, "Error fetching room ID: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void fetchStudentsFromRoom(String roomId) {
        TableLayout tableLayout = findViewById(R.id.studentsTable);

        String roomPath = "rooms/" + roomId + "/students";
        Log.d(TAG, "Fetching students from path: " + roomPath);

        CollectionReference studentsCollection = db.collection(roomPath);

        studentsCollection.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot studentDoc : queryDocumentSnapshots) {
                            String studentId = studentDoc.getId();
                            String subjectCode = studentDoc.getString("subjectCode");
                            String section = studentDoc.getString("section");

                            if (studentId != null && subjectCode != null && section != null) {
                                Log.d(TAG, "Room ID: " + roomId + ", Student ID: " + studentId +
                                        ", Subject Code: " + subjectCode + ", Section: " + section);
                                fetchStudentDetails(studentId, subjectCode, section, tableLayout);
                            } else {
                                Log.e(TAG, "Missing data for student in room: " + roomId);
                            }
                        }
                    } else {
                        Toast.makeText(this, "No students found in this room.", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "No students found for room ID: " + roomId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching students from room: " + roomId, e);
                    Toast.makeText(this, "Error fetching students: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void fetchStudentDetails(String studentId, String subjectCode, String section, TableLayout tableLayout) {
        Log.d(TAG, "Fetching details for Student ID: " + studentId + " from 'students' collection.");

        db.collection("students")
                .document(studentId)
                .get()
                .addOnSuccessListener(studentSnapshot -> {
                    if (studentSnapshot.exists()) {
                        String idNumber = studentSnapshot.getString("idNumber");
                        String firstName = studentSnapshot.getString("firstName");
                        String middleName = studentSnapshot.getString("middleName");
                        String lastName = studentSnapshot.getString("lastName");

                        String fullName = firstName + " " +
                                (middleName != null ? middleName + " " : "") + lastName;

                        Log.d(TAG, "Student Details: " + fullName + ", ID: " + idNumber);

                        addStudentRowToTable(idNumber, fullName, subjectCode, section, tableLayout);
                    } else {
                        Log.e(TAG, "Student document not found for ID: " + studentId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching student details for ID: " + studentId, e);
                    Toast.makeText(this, "Error fetching student details: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void addStudentRowToTable(String idNumber, String fullName, String subjectCode, String section, TableLayout tableLayout) {
        TableRow row = new TableRow(this);
        row.setLayoutParams(new TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT
        ));

        // Add ID Number column
        TextView idNumberTextView = new TextView(this);
        idNumberTextView.setText(idNumber);
        idNumberTextView.setPadding(8, 8, 8, 8);
        idNumberTextView.setGravity(Gravity.CENTER);
        row.addView(idNumberTextView);

        // Add Full Name column
        TextView nameTextView = new TextView(this);
        nameTextView.setText(fullName);
        nameTextView.setPadding(8, 8, 8, 8);
        nameTextView.setGravity(Gravity.CENTER);
        row.addView(nameTextView);

        // Add Subject Code - Section column
        TextView subjectSectionTextView = new TextView(this);
        subjectSectionTextView.setText(subjectCode + " - " + section);
        subjectSectionTextView.setPadding(8, 8, 8, 8);
        subjectSectionTextView.setGravity(Gravity.CENTER);
        row.addView(subjectSectionTextView);

        // Add the row to the table layout
        tableLayout.addView(row);
    }
}
