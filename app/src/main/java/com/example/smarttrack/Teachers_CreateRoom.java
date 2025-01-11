package com.example.smarttrack;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Teachers_CreateRoom extends AppCompatActivity {

    private EditText subjectNameField, subjectCodeField, sectionField, addStudentField;
    private EditText startDateField, endDateField, startTimeField, endTimeField, numberOfStudentsField;
    private Button createRoomButton, backButton;
    private RecyclerView studentRecyclerView;
    private StudentAdapter studentAdapter;
    private FirebaseFirestore firestore;

    private List<Student> studentList = new ArrayList<>();
    private List<Student> allStudents = new ArrayList<>();
    private List<String> selectedEmails = new ArrayList<>();
    private boolean isUpdating = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_room);

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance();

        // Initialize views
        subjectNameField = findViewById(R.id.subjectNameField);
        subjectCodeField = findViewById(R.id.subjectCodeField);
        sectionField = findViewById(R.id.sectionField);
        addStudentField = findViewById(R.id.addStudentField);
        startDateField = findViewById(R.id.startDateField);
        endDateField = findViewById(R.id.endDateField);
        startTimeField = findViewById(R.id.startTimeField);
        endTimeField = findViewById(R.id.endTimeField);
        numberOfStudentsField = findViewById(R.id.numberOfStudentsField);
        createRoomButton = findViewById(R.id.createRoomButton);
        backButton = findViewById(R.id.backButton);
        studentRecyclerView = findViewById(R.id.studentRecyclerView);

        // Set RecyclerView visibility to GONE initially
        studentRecyclerView.setVisibility(View.GONE);

        // Setup RecyclerView
        studentAdapter = new StudentAdapter(studentList, student -> {
            if (numberOfStudentsField.getText().toString().isEmpty() || selectedEmails.size() < Integer.parseInt(numberOfStudentsField.getText().toString())) {
                if (!selectedEmails.contains(student.getEmail())) { // Avoid duplicate emails
                    selectedEmails.add(student.getEmail());
                    updateAddStudentField();
                } else {
                    Toast.makeText(this, "Student already added", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Student limit reached", Toast.LENGTH_SHORT).show();
            }
        });

        studentRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        studentRecyclerView.setAdapter(studentAdapter);

        // Back Button
        backButton.setOnClickListener(v -> {
            Intent intent = new Intent(Teachers_CreateRoom.this, Teachers_Room.class);
            intent.putExtra("uid", FirebaseAuth.getInstance().getCurrentUser().getUid());
            startActivity(intent);
            finish();
        });

        // Date Pickers
        startDateField.setOnClickListener(v -> showDatePicker(startDateField));
        endDateField.setOnClickListener(v -> showDatePicker(endDateField));

        // Time Pickers
        startTimeField.setOnClickListener(v -> showTimePicker(startTimeField));
        endTimeField.setOnClickListener(v -> showTimePicker(endTimeField));

        // Create Room Button
        createRoomButton.setOnClickListener(v -> createRoom());

        // Fetch Students
        fetchAllStudents();

        // Add Student Field Listener
        addStudentField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isUpdating) return; // Skip if updating programmatically

                if (s.length() > 0) {
                    filterStudents(s.toString());
                } else {
                    studentList.clear();
                    studentAdapter.notifyDataSetChanged();
                    studentRecyclerView.setVisibility(View.GONE); // Hide RecyclerView if no input
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (isUpdating) return;

                String text = s.toString();
                List<String> emails = new ArrayList<>(List.of(text.split(", ")));

                // Find and remove emails that are no longer in the field
                selectedEmails.removeIf(email -> !emails.contains(email));

                // Update the field to reflect changes
                isUpdating = true;
                updateAddStudentField();
                isUpdating = false;
            }
        });
    }

    private void fetchAllStudents() {
        firestore.collection("students")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        allStudents.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String firstName = document.getString("firstName");
                            String middleName = document.getString("middleName");
                            String lastName = document.getString("lastName");
                            String email = document.getString("email");
                            allStudents.add(new Student(firstName, middleName, lastName, email));
                        }
                    }
                });
    }

    private void filterStudents(String query) {
        studentList.clear();

        for (Student student : allStudents) {
            String fullName = (student.getFirstName() + " " + student.getMiddleName() + " " + student.getLastName()).toLowerCase();
            if (fullName.contains(query.toLowerCase())) {
                studentList.add(student);
            }
        }

        if (studentList.isEmpty()) {
            studentList.add(new Student("No students found", "", "", ""));
        }

        studentAdapter.notifyDataSetChanged();
        studentRecyclerView.setVisibility(View.VISIBLE); // Show RecyclerView
    }

    private void updateAddStudentField() {
        String emails = String.join(", ", selectedEmails);
        addStudentField.setText(emails);
        addStudentField.setSelection(emails.length()); // Move cursor to end
    }

    private void showDatePicker(EditText field) {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> field.setText(dayOfMonth + "/" + (month + 1) + "/" + year),
                calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000); // Disable past dates
        datePickerDialog.show();
    }

    private void showTimePicker(EditText field) {
        Calendar calendar = Calendar.getInstance();
        new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            String time = String.format("%02d:%02d", hourOfDay, minute);
            field.setText(time);
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false).show();
    }

    private void createRoom() {
        String subjectName = subjectNameField.getText().toString();
        String subjectCode = subjectCodeField.getText().toString();
        String section = sectionField.getText().toString();
        String startDate = startDateField.getText().toString();
        String endDate = endDateField.getText().toString();
        String startTime = startTimeField.getText().toString();
        String endTime = endTimeField.getText().toString();
        String numberOfStudents = numberOfStudentsField.getText().toString();

        if (subjectName.isEmpty() || subjectCode.isEmpty() || section.isEmpty() || startDate.isEmpty() ||
                endDate.isEmpty() || startTime.isEmpty() || endTime.isEmpty() || numberOfStudents.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields except Add Students", Toast.LENGTH_SHORT).show();
            return;
        }

        // Generate Room Code and Save
        String roomCode = "ROOM" + (int) (Math.random() * 10000);
        Map<String, Object> roomData = new HashMap<>();
        roomData.put("subjectName", subjectName);
        roomData.put("subjectCode", subjectCode);
        roomData.put("section", section);
        roomData.put("startDate", startDate);
        roomData.put("endDate", endDate);
        roomData.put("startTime", startTime);
        roomData.put("endTime", endTime);
        roomData.put("numberOfStudents", numberOfStudents);
        roomData.put("roomCode", roomCode);
        roomData.put("teacherId", FirebaseAuth.getInstance().getCurrentUser().getUid());

        firestore.collection("rooms")
                .add(roomData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Room Created Successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error creating room: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
