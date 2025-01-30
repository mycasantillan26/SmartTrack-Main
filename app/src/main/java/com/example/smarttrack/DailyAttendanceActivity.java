package com.example.smarttrack;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DailyAttendanceActivity extends AppCompatActivity {
    private ListView dailyAttendanceList;
    private FirebaseFirestore db;
    private ArrayList<String> attendanceRecords;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily_attendance);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize views
        dailyAttendanceList = findViewById(R.id.dailyAttendanceList);

        // Initialize attendance records list and adapter
        attendanceRecords = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, attendanceRecords);
        dailyAttendanceList.setAdapter(adapter);

        // Get user ID from intent
        String uid = getIntent().getStringExtra("uid");

        // Fetch today's attendance records
        fetchTodayAttendance(uid);
    }

    private void fetchTodayAttendance(String studentUid) {
        // Calculate today's start and end times
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        Date startOfDay = calendar.getTime();

        calendar.add(Calendar.DAY_OF_MONTH, 1);
        Date startOfNextDay = calendar.getTime();

        // Query attendance records for today
        db.collectionGroup("attendance")
                .whereGreaterThanOrEqualTo("timeIn", startOfDay)
                .whereLessThan("timeIn", startOfNextDay)
                .orderBy("timeIn", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    attendanceRecords.clear();
                    SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Timestamp timeIn = document.getTimestamp("timeIn");
                        String roomId = document.getReference().getPath().split("/")[1];

                        // Format the record string
                        String recordEntry = String.format("Room: %s\nTime In: %s",
                                roomId,
                                timeFormat.format(timeIn.toDate())
                        );

                        attendanceRecords.add(recordEntry);
                    }

                    adapter.notifyDataSetChanged();

                    // Show "No records" message if needed
                    if (attendanceRecords.isEmpty()) {
                        findViewById(R.id.dailyAttendanceList).setVisibility(View.GONE);
                        TextView noRecordsText = new TextView(this);
                        noRecordsText.setText("No attendance records for today");
                        noRecordsText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                        ((ViewGroup) findViewById(R.id.dailyAttendanceList).getParent())
                                .addView(noRecordsText);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("DailyAttendance", "Error fetching attendance", e);
                    // Show error message to user
                });
    }
}