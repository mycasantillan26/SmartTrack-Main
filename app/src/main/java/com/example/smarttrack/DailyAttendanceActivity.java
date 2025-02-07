package com.example.smarttrack;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DailyAttendanceActivity extends AppCompatActivity {

    private static final String TAG = "DailyAttendance";

    private ListView dailyAttendanceList;
    private TextView emptyMessage;

    private FirebaseFirestore db;
    private ArrayList<String> attendanceRecords;
    private ArrayList<String> roomIds;
    private ArrayList<String> roomNames;
    private ArrayAdapter<String> listViewAdapter;

    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_daily_attendance);

        // Retrieve UID from Intent first
        uid = getIntent().getStringExtra("uid");

        // Fallback to FirebaseAuth UID if not provided in the intent
        if (uid == null) {
            uid = FirebaseAuth.getInstance().getUid();
        }

        // If still null, log error and exit
        if (uid == null) {
            Log.e(TAG, "No UID available. Cannot proceed.");
            Toast.makeText(this, "Error: No user ID found. Please log in again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        } else {
            Log.d(TAG, "UID successfully retrieved: " + uid);
        }

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize views
        dailyAttendanceList = findViewById(R.id.dailyAttendanceList);
        emptyMessage = findViewById(R.id.emptyMessage);

        // Initialize lists
        attendanceRecords = new ArrayList<>();
        roomIds = new ArrayList<>();
        roomNames = new ArrayList<>();

        // Set up the ListView adapter
        setupListViewAdapter();

        // Load rooms for the student
        loadStudentRooms();
    }

    private void setupListViewAdapter() {
        listViewAdapter = new ArrayAdapter<String>(
                this,
                R.layout.list_item_attendance,
                attendanceRecords
        ) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = getLayoutInflater().inflate(
                            R.layout.list_item_attendance,
                            parent,
                            false
                    );
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

    private void loadStudentRooms() {
        // Query all rooms to find the ones where the student is enrolled
        db.collection("rooms")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    roomIds.clear();
                    roomNames.clear();

                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.d(TAG, "No rooms found in the database.");
                        showNoRecordsMessage("No rooms available.");
                        return;
                    }

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String roomId = document.getId(); // Room ID
                        checkStudentEnrollment(roomId, document); // Check if the student is enrolled
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching rooms.", e);
                    showNoRecordsMessage("Error loading rooms.");
                });
    }


    private void checkStudentEnrollment(String roomId, QueryDocumentSnapshot roomDocument) {
        // Check if the student is enrolled in this room
        db.collection("rooms")
                .document(roomId)
                .collection("students")
                .document(uid)
                .get()
                .addOnSuccessListener(studentDoc -> {
                    if (studentDoc.exists()) {
                        Log.d(TAG, "Student is enrolled in room: " + roomId);
                        loadRoomDetails(roomId);
                        // Get room details
                        String subjectName = roomDocument.getString("subjectName");
                        String section = roomDocument.getString("section");

                        if (subjectName == null) subjectName = "Unknown Subject";
                        if (section == null) section = "Unknown Section";

                        String roomName = subjectName + " (" + section + ")";
                        roomIds.add(roomId);
                        roomNames.add(roomName);

                        // Fetch attendance for this room (only fetch for the first room as an example)
                        if (roomIds.size() == 1) {
                            fetchTodayAttendance(roomId, uid);
                        }

                        listViewAdapter.notifyDataSetChanged(); // Update UI
                    } else {
                        Log.d(TAG, "Student is not enrolled in room: " + roomId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking student enrollment in room: " + roomId, e);
                });
    }



    private void loadRoomDetails(String roomId) {
        db.collection("rooms")
                .document(roomId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String subjectName = documentSnapshot.getString("subjectName");
                        String section = documentSnapshot.getString("section");
                        if (subjectName == null) subjectName = "Unknown Subject";
                        if (section == null) section = "Unknown Section";

                        String roomName = subjectName + " (" + section + ")";
                        roomIds.add(roomId);
                        roomNames.add(roomName);

                        if (roomIds.size() == 1) {
                            fetchTodayAttendance(roomId, uid);
                        }

                    } else {
                        Log.d(TAG, "Room document " + roomId + " does not exist.");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading room details", e);
                });
    }

    private void fetchTodayAttendance(String roomId, String studentUid) {
        Log.d(TAG, "Fetching attendance for roomId=" + roomId + ", uid=" + studentUid);

        // Get today's date in YYYYMMDD format
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        String todayDate = dateFormat.format(new Date());

        db.collection("rooms")
                .document(roomId)
                .collection("students")
                .document(studentUid)
                .collection("attendance")
                .get() // ✅ Get all records and filter manually
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    attendanceRecords.clear();
                    SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Timestamp date = document.getTimestamp("date"); // ✅ Ensure `date` field exists
                        Timestamp timeIn = document.getTimestamp("timeIn");
                        Timestamp timeOut = document.getTimestamp("timeOut");

                        // Convert Firestore timestamp to YYYYMMDD format for comparison
                        if (date != null && dateFormat.format(date.toDate()).equals(todayDate)) {
                            Log.d(TAG, "✅ Found attendance for today in room: " + roomId);

                            String timeDetails = "Time In: " + (timeIn != null ? timeFormat.format(timeIn.toDate()) : "N/A");
                            timeDetails += ", Time Out: " + (timeOut != null ? timeFormat.format(timeOut.toDate()) : "N/A");

                            String record = "Room Name: " + roomNames.get(roomIds.indexOf(roomId)) + " | " + timeDetails;
                            attendanceRecords.add(record);
                            Log.d(TAG, "Attendance record added: " + record);
                        }
                    }

                    listViewAdapter.notifyDataSetChanged();

                    if (attendanceRecords.isEmpty()) {
                        showNoRecordsMessage("No attendance records for today.");
                    } else {
                        showListView();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error fetching attendance", e);
                    showNoRecordsMessage("Error fetching attendance.");
                });
    }


    private void showNoRecordsMessage(String message) {
        dailyAttendanceList.setVisibility(View.GONE);
        emptyMessage.setVisibility(View.VISIBLE);
        emptyMessage.setText(message);
    }

    private void showListView() {
        emptyMessage.setVisibility(View.GONE);
        dailyAttendanceList.setVisibility(View.VISIBLE);
    }
}