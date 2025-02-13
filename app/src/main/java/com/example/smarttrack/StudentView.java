package com.example.smarttrack;

import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class StudentView extends AppCompatActivity {

    private static final String TAG = "StudentView";
    private FirebaseFirestore db;
    private TableLayout tableLayout;
    private LinearLayout containerLayout;
    private String roomId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_students);

        db = FirebaseFirestore.getInstance();
        containerLayout = findViewById(R.id.containerLayout);
        tableLayout = findViewById(R.id.studentsTable);

        roomId = getIntent().getStringExtra("roomId");

        if (roomId == null || roomId.isEmpty()) {
            Log.e(TAG, "StudentView: Received NULL roomId");
            Toast.makeText(this, "Error: Room ID is missing!", Toast.LENGTH_LONG).show();
            finish(); // 🚨 Close the activity to prevent further errors
            return;
        }

        Log.d(TAG, "StudentView: Successfully received roomId = " + roomId);

        fetchTeacherDetails(roomId);
        fetchStudentsFromRoom(roomId);
    }

    private void fetchTeacherDetails(String roomId) {
        Log.d(TAG, "Fetching teacher details for room ID: " + roomId);

        db.collection("rooms")
                .document(roomId)
                .get()
                .addOnSuccessListener(roomDoc -> {
                    if (roomDoc.exists()) {
                        String teacherId = roomDoc.getString("teacherId");
                        if (teacherId != null) {
                            fetchTeacherName(teacherId);
                        } else {
                            Log.e(TAG, "Teacher ID not found for room ID: " + roomId);
                            addTeacherNameToUI("Unknown Teacher");
                        }
                    } else {
                        Log.e(TAG, "Room document not found for ID: " + roomId);
                        addTeacherNameToUI("Unknown Teacher");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching room details for room ID: " + roomId, e);
                    addTeacherNameToUI("Error fetching teacher details");
                });
    }

    private void fetchTeacherName(String teacherId) {
        Log.d(TAG, "Fetching teacher details for Teacher ID: " + teacherId);

        db.collection("teachers")
                .document(teacherId)
                .get()
                .addOnSuccessListener(teacherDoc -> {
                    if (teacherDoc.exists()) {
                        String firstName = teacherDoc.getString("firstName");
                        String middleName = teacherDoc.getString("middleName");
                        String lastName = teacherDoc.getString("lastName");

                        String fullName = firstName + " " +
                                (middleName != null ? middleName + " " : "") + lastName;

                        Log.d(TAG, "Teacher Name: " + fullName);
                        addTeacherNameToUI(fullName);
                    } else {
                        Log.e(TAG, "Teacher document not found for ID: " + teacherId);
                        addTeacherNameToUI("Unknown Teacher");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching teacher details for ID: " + teacherId, e);
                    addTeacherNameToUI("Error fetching teacher details");
                });
    }

    private void addTeacherNameToUI(String teacherName) {
        TextView teacherNameTextView = new TextView(this);
        teacherNameTextView.setText("Teacher: " + teacherName);
        teacherNameTextView.setPadding(8, 50, 8, 50);
        teacherNameTextView.setGravity(Gravity.CENTER);
        teacherNameTextView.setTextSize(18);
        teacherNameTextView.setTypeface(null, android.graphics.Typeface.BOLD);

        containerLayout.addView(teacherNameTextView, 0); // Add teacher name at the top
    }

    private void fetchStudentsFromRoom(String roomId) {
        String roomPath = "rooms/" + roomId + "/students";
        Log.d(TAG, "Fetching students from path: " + roomPath);

        db.collection(roomPath)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot studentDoc : queryDocumentSnapshots) {
                            String studentId = studentDoc.getId();

                            if (studentId != null) {
                                fetchStudentDetails(studentId);
                            } else {
                                Log.e(TAG, "Missing student ID for a document in room: " + roomId);
                            }
                        }
                    } else {
                        Toast.makeText(this, "No students found in this room.", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "No students found for room ID: " + roomId);
                        addEmptyRowToTable("No students found");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching students from room: " + roomId, e);
                    Toast.makeText(this, "Error fetching students: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void fetchStudentDetails(String studentId) {
        Log.d(TAG, "Fetching details for Student ID: " + studentId);

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

                        addStudentRowToTable(idNumber, fullName);
                    } else {
                        Log.e(TAG, "Student document not found for ID: " + studentId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching student details for ID: " + studentId, e);
                    Toast.makeText(this, "Error fetching student details: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void addStudentRowToTable(String idNumber, String fullName) {
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

        tableLayout.addView(row);
    }

    private void addEmptyRowToTable(String message) {
        TableRow row = new TableRow(this);
        row.setLayoutParams(new TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT
        ));

        TextView messageTextView = new TextView(this);
        messageTextView.setText(message);
        messageTextView.setPadding(8, 8, 8, 8);
        messageTextView.setGravity(Gravity.CENTER);
        row.addView(messageTextView);

        tableLayout.addView(row);
    }
}
