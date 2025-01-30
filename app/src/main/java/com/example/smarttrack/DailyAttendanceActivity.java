package com.example.smarttrack;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Spinner;
import android.widget.AdapterView;
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
import java.util.TimeZone;

public class DailyAttendanceActivity extends AppCompatActivity {
    private ListView dailyAttendanceList;
    private Spinner roomSpinner;
    private FirebaseFirestore db;
    private ArrayList<String> attendanceRecords;
    private ArrayList<String> roomIds;
    private ArrayList<String> roomNames;
    private ArrayAdapter<String> listViewAdapter;  // Renamed from attendanceAdapter
    private ArrayAdapter<String> spinnerAdapter;   // Renamed from roomAdapter
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily_attendance);

        // Get UID from intent
        uid = getIntent().getStringExtra("uid");
        if (uid == null) {
            Log.e("DailyAttendance", "No UID provided");
            finish();
            return;
        }

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize views
        dailyAttendanceList = findViewById(R.id.dailyAttendanceList);
        roomSpinner = findViewById(R.id.roomSpinner);

        // Initialize lists
        attendanceRecords = new ArrayList<>();
        roomIds = new ArrayList<>();
        roomNames = new ArrayList<>();

        // Setup adapters
        setupListViewAdapter();
        setupSpinnerAdapter();

        // Load rooms for the student
        loadStudentRooms();

        // Setup room selection listener
        roomSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedRoomId = roomIds.get(position);
                fetchTodayAttendance(selectedRoomId, uid);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    private void setupListViewAdapter() {
        listViewAdapter = new ArrayAdapter<String>(this, R.layout.list_item_attendance, attendanceRecords) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = getLayoutInflater().inflate(R.layout.list_item_attendance, parent, false);
                }

                String record = getItem(position);
                TextView roomNameText = convertView.findViewById(R.id.roomNameText);
                TextView timeDetailsText = convertView.findViewById(R.id.timeDetailsText);

                if (record != null) {
                    String[] parts = record.split(" \\| ");
                    if (parts.length >= 2) {
                        roomNameText.setText(parts[0].replace("Room Name: ", "").trim());
                        timeDetailsText.setText(parts[1].trim());
                    }
                }

                return convertView;
            }
        };
        dailyAttendanceList.setAdapter(listViewAdapter);
    }

    private void setupSpinnerAdapter() {
        spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, roomNames);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        roomSpinner.setAdapter(spinnerAdapter);
    }

    private void loadStudentRooms() {
        db.collection("students")
                .document(uid)
                .collection("enrolledRooms")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    roomIds.clear();
                    roomNames.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String roomId = document.getString("roomId");
                        if (roomId != null) {
                            loadRoomDetails(roomId);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("DailyAttendance", "Error loading student rooms", e));
    }

    private void loadRoomDetails(String roomId) {
        db.collection("rooms")
                .document(roomId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String subjectName = documentSnapshot.getString("subjectName");
                        String section = documentSnapshot.getString("section");
                        String roomName = subjectName + " (" + section + ")";

                        roomIds.add(roomId);
                        roomNames.add(roomName);
                        spinnerAdapter.notifyDataSetChanged();

                        // Load attendance for first room if this is the first room loaded
                        if (roomIds.size() == 1) {
                            fetchTodayAttendance(roomId, uid);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("DailyAttendance", "Error loading room details", e));
    }

    private void fetchTodayAttendance(String roomId, String studentUid) {
        db.collection("rooms").document(roomId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String subjectName = documentSnapshot.contains("subjectName") ?
                                documentSnapshot.getString("subjectName") : "Unknown Subject";
                        String section = documentSnapshot.contains("section") ?
                                documentSnapshot.getString("section") : "Unknown Section";
                        String roomName = subjectName + " (" + section + ")";

                        ArrayList<String> schedule = (ArrayList<String>) documentSnapshot.get("schedule");
                        SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
                        String today = dayFormat.format(new Date());

                        if (schedule != null && schedule.contains(today)) {
                            fetchAttendanceRecords(roomId, studentUid, roomName);
                        } else {
                            attendanceRecords.clear();
                            listViewAdapter.notifyDataSetChanged();
                            showNoClassMessage();
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("DailyAttendance", "Error fetching room details", e));
    }

    private void fetchAttendanceRecords(String roomId, String studentUid, String roomName) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        Date startOfDay = calendar.getTime();

        calendar.add(Calendar.DAY_OF_MONTH, 1);
        Date startOfNextDay = calendar.getTime();

        db.collection("rooms")
                .document(roomId)
                .collection("students")
                .document(studentUid)
                .collection("attendance")
                .whereGreaterThanOrEqualTo("timeIn", new Timestamp(startOfDay))
                .whereLessThan("timeIn", new Timestamp(startOfNextDay))
                .orderBy("timeIn", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    attendanceRecords.clear();
                    SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Timestamp timeIn = document.getTimestamp("timeIn");
                        Timestamp timeOut = document.getTimestamp("timeOut");

                        if (timeIn != null) {
                            String timeDetails = "Time In: " + timeFormat.format(timeIn.toDate());
                            if (timeOut != null) {
                                timeDetails += ", Time Out: " + timeFormat.format(timeOut.toDate());
                            } else {
                                timeDetails += ", Time Out: N/A";
                            }

                            String record = "Room Name: " + roomName + " | " + timeDetails;
                            attendanceRecords.add(record);
                        }
                    }

                    listViewAdapter.notifyDataSetChanged();

                    if (attendanceRecords.isEmpty()) {
                        showNoRecordsMessage();
                    }
                })
                .addOnFailureListener(e -> Log.e("DailyAttendance", "Error fetching attendance", e));
    }

    private void showNoClassMessage() {
        findViewById(R.id.dailyAttendanceList).setVisibility(View.GONE);
        TextView noClassText = new TextView(this);
        noClassText.setText("No class scheduled for today");
        noClassText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        ((ViewGroup) findViewById(R.id.dailyAttendanceList).getParent()).addView(noClassText);
    }

    private void showNoRecordsMessage() {
        findViewById(R.id.dailyAttendanceList).setVisibility(View.GONE);
        TextView noRecordsText = new TextView(this);
        noRecordsText.setText("No attendance records for today");
        noRecordsText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        ((ViewGroup) findViewById(R.id.dailyAttendanceList).getParent()).addView(noRecordsText);
    }
}