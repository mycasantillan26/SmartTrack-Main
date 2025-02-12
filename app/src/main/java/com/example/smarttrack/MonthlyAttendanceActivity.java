package com.example.smarttrack;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class MonthlyAttendanceActivity extends AppCompatActivity {
    private static final String TAG = "MonthlyAttendance";

    private TextView lateLabel, absentLabel, presentLabel;
    private FirebaseFirestore db;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monthly_attendance);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize UI components
        lateLabel = findViewById(R.id.lateLabel);
        absentLabel = findViewById(R.id.absentLabel);
        presentLabel = findViewById(R.id.presentLabel);

        // Retrieve UID from Intent or FirebaseAuth
        uid = getIntent().getStringExtra("uid");
        if (uid == null) {
            uid = FirebaseAuth.getInstance().getUid();
        }

        if (uid == null) {
            Log.e(TAG, "No UID available.");
            Toast.makeText(this, "Error: No user ID found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Fetch monthly attendance statistics
        fetchMonthlyStatistics(uid);
    }

    private void fetchMonthlyStatistics(String studentUid) {
        // Get first and last day of current month
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        Date startOfMonth = calendar.getTime();

        calendar.add(Calendar.MONTH, 1);
        Date startOfNextMonth = calendar.getTime();

        // Expected workdays in the month
        Set<String> expectedWorkdays = getWorkdaysInMonth();

        // Query attendance for the student
        db.collectionGroup("attendance")
                .whereGreaterThanOrEqualTo("date", new Timestamp(startOfMonth))
                .whereLessThan("date", new Timestamp(startOfNextMonth))
                .whereEqualTo("studentId", studentUid)
                .orderBy("date", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int totalPresent = 0;
                    int totalLate = 0;
                    Set<String> attendedDays = new HashSet<>();

                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Timestamp dateTimestamp = document.getTimestamp("date");
                        Timestamp timeInTimestamp = document.getTimestamp("timeIn");

                        if (dateTimestamp != null) {
                            String formattedDate = dateFormat.format(dateTimestamp.toDate());
                            attendedDays.add(formattedDate);
                            totalPresent++;
                        }

                        if (timeInTimestamp != null) {
                            Calendar timeInCal = Calendar.getInstance();
                            timeInCal.setTime(timeInTimestamp.toDate());

                            // Consider arrival after 9 AM as late
                            if (timeInCal.get(Calendar.HOUR_OF_DAY) >= 9) {
                                totalLate++;
                            }
                        }
                    }

                    // Absences = Expected workdays - Attended days
                    expectedWorkdays.removeAll(attendedDays);
                    int totalAbsent = expectedWorkdays.size();

                    // Update UI
                    presentLabel.setText("Presents: " + totalPresent);
                    lateLabel.setText("Lates: " + totalLate);
                    absentLabel.setText("Absents: " + Math.max(0, totalAbsent));

                    Log.d(TAG, "✅ Monthly Stats -> Present: " + totalPresent + ", Late: " + totalLate + ", Absent: " + totalAbsent);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error fetching monthly statistics", e);
                    Toast.makeText(this, "Failed to fetch attendance data.", Toast.LENGTH_SHORT).show();
                });
    }

    private Set<String> getWorkdaysInMonth() {
        Set<String> workdays = new HashSet<>();
        Calendar calendar = Calendar.getInstance();
        int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());

        for (int day = 1; day <= daysInMonth; day++) {
            calendar.set(Calendar.DAY_OF_MONTH, day);
            int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
            if (dayOfWeek != Calendar.SATURDAY && dayOfWeek != Calendar.SUNDAY) {
                workdays.add(dateFormat.format(calendar.getTime()));
            }
        }

        return workdays;
    }
}
