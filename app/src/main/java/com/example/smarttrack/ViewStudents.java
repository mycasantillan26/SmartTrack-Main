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

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_students);

        db = FirebaseFirestore.getInstance();

        String section = getIntent().getStringExtra("section");
        String subjectCode = getIntent().getStringExtra("subjectCode");

        if (section == null || subjectCode == null) {
            Toast.makeText(this, "Section or subject code not provided.", Toast.LENGTH_LONG).show();
            return;
        }

        fetchStudentIdsFromSection(subjectCode, section);
    }

    private void fetchStudentIdsFromSection(String subjectCode, String section) {
        TableLayout tableLayout = findViewById(R.id.studentsTable);

        String sectionPath = "sections/" + subjectCode + "-" + section + "/students";

        CollectionReference studentsCollection = db.collection(sectionPath);

        studentsCollection.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot document : queryDocumentSnapshots) {
                            String studentId = document.getId();
                            fetchStudentDetails(studentId, tableLayout);
                        }
                    } else {
                        Toast.makeText(this, "No students found in this section.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ViewStudents", "Error fetching student IDs", e);
                    Toast.makeText(this, "Error fetching student IDs: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void fetchStudentDetails(String studentId, TableLayout tableLayout) {
        db.collection("students")
                .document(studentId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String idNumber = document.getString("idNumber");
                        String firstName = document.getString("firstName");
                        String middleName = document.getString("middleName");
                        String lastName = document.getString("lastName");

                        String fullName = firstName + " " +
                                (middleName != null ? middleName + " " : "") + lastName;

                        TableRow row = new TableRow(this);
                        row.setLayoutParams(new TableRow.LayoutParams(
                                TableRow.LayoutParams.MATCH_PARENT,
                                TableRow.LayoutParams.WRAP_CONTENT
                        ));

                        TextView idNumberTextView = new TextView(this);
                        idNumberTextView.setText(idNumber);
                        idNumberTextView.setPadding(8, 8, 8, 8);
                        idNumberTextView.setGravity(Gravity.CENTER);
                        row.addView(idNumberTextView);

                        TextView nameTextView = new TextView(this);
                        nameTextView.setText(fullName);
                        nameTextView.setPadding(8, 8, 8, 8);
                        nameTextView.setGravity(Gravity.CENTER);
                        row.addView(nameTextView);

                        tableLayout.addView(row);
                    } else {
                        Log.d("ViewStudents", "Student document not found: " + studentId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ViewStudents", "Error fetching student details for ID: " + studentId, e);
                    Toast.makeText(this, "Error fetching student details: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
