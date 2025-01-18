package com.example.smarttrack;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class ViewStudents extends AppCompatActivity {

    private TextView studentsTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_students);

        studentsTextView = findViewById(R.id.studentsTextView);

        // Get the room identifier passed from the previous activity
        String room = getIntent().getStringExtra("room");
        fetchStudents(room);
    }

    private void fetchStudents(String room) {
        // Placeholder for fetching student data
        studentsTextView.setText("List of students for " + room);
        // Ideally, fetch data from Firestore or your database here
    }
}
