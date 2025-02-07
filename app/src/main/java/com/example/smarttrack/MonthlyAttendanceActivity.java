package com.example.smarttrack;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.Calendar;
import java.util.Date;

public class MonthlyAttendanceActivity extends AppCompatActivity {
    private TextView lateLabel, absentLabel, presentLabel;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monthly_attendance);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize views
        lateLabel = findViewById(R.id.lateLabel);
        absentLabel = findViewById(R.id.absentLabel);
        presentLabel = findViewById(R.id.presentLabel);

        // Get user ID from intent
        String uid = getIntent().getStringExtra("uid");

        // Fetch monthly attendance statistics
        fetchMonthlyStatistics(uid);
    }

    private void fetchMonthlyStatistics(String studentUid) {
        // Calculate first and last day of current month
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        Date startOfMonth = calendar.getTime();

        calendar.add(Calendar.MONTH, 1);
        Date startOfNextMonth = calendar.getTime();

        // Query attendance records for the month
        db.collectionGroup("attendance")
                .whereGreaterThanOrEqualTo("timeIn", startOfMonth)
                .whereLessThan("timeIn", startOfNextMonth)
                .orderBy("timeIn", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int totalPresent = queryDocumentSnapshots.size();
                    int totalLate = 0;

                    // Count late arrivals (you'll need to define what constitutes "late")
                    for (var document : queryDocumentSnapshots) {
                        Date timeIn = document.getTimestamp("timeIn").toDate();
                        Calendar timeInCal = Calendar.getInstance();
                        timeInCal.setTime(timeIn);

                        // Example: Consider arrival after 9 AM as late
                        if (timeInCal.get(Calendar.HOUR_OF_DAY) >= 9) {
                            totalLate++;
                        }
                    }

                    // Calculate absences (assuming there should be one attendance per weekday)
                    Calendar cal = Calendar.getInstance();
                    int workDays = getWorkdaysInMonth(cal);
                    int totalAbsent = workDays - totalPresent;

                    // Update UI
                    presentLabel.setText("Presents: " + totalPresent);
                    lateLabel.setText("Lates: " + totalLate);
                    absentLabel.setText("Absents: " + Math.max(0, totalAbsent));
                })
                .addOnFailureListener(e -> {
                    Log.e("MonthlyAttendance", "Error fetching statistics", e);
                    // Show error message to user
                });
    }

    private int getWorkdaysInMonth(Calendar calendar) {
        int workDays = 0;
        int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

        for (int day = 1; day <= daysInMonth; day++) {
            calendar.set(Calendar.DAY_OF_MONTH, day);
            int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
            if (dayOfWeek != Calendar.SATURDAY && dayOfWeek != Calendar.SUNDAY) {
                workDays++;
            }
        }

        return workDays;
    }
}