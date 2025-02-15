package com.example.smarttrack;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import com.google.firebase.Timestamp;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class Teachers_DailyAttendanceActivity extends AppCompatActivity {

    private static final String TAG = "TeachersDailyAttendance";

    private LinearLayout roomContainer, eventContainer;
    private ProgressBar loadingIndicator;
    private TextView emptyRoomsMessage, emptyEventsMessage, roomsLabel, eventsLabel;

    private FirebaseFirestore db;
    private String teacherUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teachers_daily_attendance);

        // Initialize UI Elements
        roomContainer = findViewById(R.id.roomContainer);
        eventContainer = findViewById(R.id.eventContainer);
        loadingIndicator = findViewById(R.id.loadingIndicator);
        emptyRoomsMessage = findViewById(R.id.emptyRoomsMessage);
        emptyEventsMessage = findViewById(R.id.emptyEventsMessage);
        roomsLabel = findViewById(R.id.roomsLabel);
        eventsLabel = findViewById(R.id.eventsLabel);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Get Teacher UID
        teacherUid = FirebaseAuth.getInstance().getUid();
        if (teacherUid == null) {
            Log.e(TAG, "❌ teacherUid is NULL. Redirecting to login.");
            Toast.makeText(this, "Error: No teacher ID found. Please log in again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Log.d(TAG, "✅ teacherUid retrieved: " + teacherUid);

        // Load rooms and events created by the teacher
        loadTeacherRooms();
        loadTeacherEvents();
    }

    private void loadTeacherRooms() {
        loadingIndicator.setVisibility(View.VISIBLE);
        roomContainer.removeAllViews();

        // Get today's day name (e.g., "Monday", "Tuesday")
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE", Locale.getDefault());
        String today = sdf.format(Calendar.getInstance().getTime());

        Log.d(TAG, "📅 Today is: " + today);

        db.collection("rooms")
                .whereEqualTo("teacherId", teacherUid)
                .get()
                .addOnCompleteListener(task -> {
                    loadingIndicator.setVisibility(View.GONE);

                    if (!task.isSuccessful()) {
                        Log.e(TAG, "❌ Error fetching rooms", task.getException());
                        Toast.makeText(this, "Error loading rooms", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    boolean hasScheduledRooms = false;

                    for (QueryDocumentSnapshot document : task.getResult()) {
                        String roomId = document.getId();
                        String subjectName = document.getString("subjectName");
                        String section = document.getString("section");

                        // Check if "schedule" exists in the room document
                        if (document.contains("schedule")) {
                            List<String> schedule = (List<String>) document.get("schedule");

                            // If today's day is in the schedule, show the room
                            if (schedule != null && schedule.contains(today)) {
                                hasScheduledRooms = true;
                                createRoomButton(roomId, subjectName, section);
                            }
                        }
                    }

                    if (!hasScheduledRooms) {
                        emptyRoomsMessage.setVisibility(View.VISIBLE);
                        roomsLabel.setVisibility(View.GONE);
                        Log.d(TAG, "⚠️ No rooms scheduled for today.");
                    } else {
                        emptyRoomsMessage.setVisibility(View.GONE);
                        roomsLabel.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void createRoomButton(String roomId, String subjectName, String section) {
        if (subjectName == null) subjectName = "Unknown Subject";
        if (section == null) section = "Unknown Section";

        String buttonText = subjectName + " (" + section + ")";
        Log.d(TAG, "✅ Scheduled Room Found: " + buttonText);

        Button roomButton = new Button(this);
        roomButton.setText(buttonText);
        roomButton.setPadding(16, 16, 16, 16);
        roomButton.setBackgroundResource(R.drawable.button_gold_border);
        roomButton.setTextColor(Color.WHITE);

        roomButton.setOnClickListener(v -> showAttendancePopup(this, roomId, buttonText));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 8, 0, 8);
        roomButton.setLayoutParams(params);

        roomContainer.addView(roomButton);
    }

    private void fetchAttendanceForRoom(String roomId, LinearLayout studentListContainer, ProgressBar popupLoadingIndicator) {
        popupLoadingIndicator.setVisibility(View.VISIBLE);
        studentListContainer.removeAllViews();

        db.collection("rooms")
                .document(roomId)
                .collection("students")
                .get()
                .addOnSuccessListener(studentSnapshots -> {
                    popupLoadingIndicator.setVisibility(View.GONE);

                    if (studentSnapshots.isEmpty()) {
                        Log.d(TAG, "⚠️ No students found in this room.");
                        TextView noDataText = new TextView(this);
                        noDataText.setText("No students attended today.");
                        studentListContainer.addView(noDataText);
                        return;
                    }

                    for (QueryDocumentSnapshot studentDoc : studentSnapshots) {
                        String studentId = studentDoc.getId();
                        fetchStudentAttendance(roomId, studentId, studentListContainer);
                    }
                })
                .addOnFailureListener(e -> {
                    popupLoadingIndicator.setVisibility(View.GONE);
                    Log.e(TAG, "❌ Error fetching students", e);
                });
    }

    private void fetchStudentAttendance(String roomId, String studentId, LinearLayout studentListContainer) {
        Timestamp todayTimestamp = getTodayTimestamp(); // Get today's Timestamp

        db.collection("rooms")
                .document(roomId)
                .collection("students")
                .document(studentId)
                .collection("attendance")
                .whereGreaterThanOrEqualTo("date", todayTimestamp) // Fetch attendance only for today
                .get()
                .addOnSuccessListener(attendanceSnapshots -> {
                    for (QueryDocumentSnapshot document : attendanceSnapshots) {
                        Timestamp dateTimestamp = document.getTimestamp("date");
                        String status = document.getString("status");

                        if (dateTimestamp != null && isSameDay(dateTimestamp, todayTimestamp)) { // Match today's date
                            if (status == null) status = "Unknown";
                            fetchStudentName(studentId, status, studentListContainer);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "❌ Error fetching attendance for student " + studentId, e));
    }

    private void fetchStudentName(String studentId, String status, LinearLayout studentListContainer) {
        db.collection("students")  // Fetch name from /students/{studentId}
                .document(studentId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String firstName = documentSnapshot.getString("firstName");
                    String lastName = documentSnapshot.getString("lastName");
                    if (firstName == null) firstName = "Unknown";
                    if (lastName == null) lastName = "Student";

                    String studentName = firstName + " " + lastName;

                    // Display only students who attended today
                    TextView studentAttendanceText = new TextView(this);
                    studentAttendanceText.setText(studentName + " - " + status);
                    studentListContainer.addView(studentAttendanceText);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Error fetching student name for " + studentId, e);
                });
    }

    // ✅ Converts the current date into Firestore Timestamp format
    private Timestamp getTodayTimestamp() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return new Timestamp(calendar.getTime());
    }

    // ✅ Checks if two timestamps are from the same day
    private boolean isSameDay(Timestamp ts1, Timestamp ts2) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        return dateFormat.format(ts1.toDate()).equals(dateFormat.format(ts2.toDate()));
    }

    private void loadTeacherEvents() {
        loadingIndicator.setVisibility(View.VISIBLE);
        eventContainer.removeAllViews();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayDate = sdf.format(Calendar.getInstance().getTime());

        Log.d(TAG, "📅 Today is: " + todayDate);

        db.collection("events")
                .whereEqualTo("teacherId", teacherUid)
                .get()
                .addOnCompleteListener(task -> {
                    loadingIndicator.setVisibility(View.GONE);

                    if (!task.isSuccessful()) {
                        Log.e(TAG, "❌ Error fetching events", task.getException());
                        Toast.makeText(this, "Error loading events", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    boolean hasScheduledEvents = false;

                    for (QueryDocumentSnapshot document : task.getResult()) {
                        final String eventId = document.getId(); // Make final
                        final String eventTitle = document.getString("title");
                        final String eventDate = document.getString("eventDate");
                        final String startTime = document.getString("startTime");
                        final String endTime = document.getString("endTime");
                        final String location = document.getString("location");

                        if (eventDate != null && eventDate.equals(todayDate)) {
                            hasScheduledEvents = true;
                            createEventButton(eventId, eventTitle, eventDate, startTime, endTime, location);
                        }
                    }

                    if (!hasScheduledEvents) {
                        emptyEventsMessage.setVisibility(View.VISIBLE);
                        eventsLabel.setVisibility(View.GONE);
                        Log.d(TAG, "⚠️ No events scheduled for today.");
                    } else {
                        emptyEventsMessage.setVisibility(View.GONE);
                        eventsLabel.setVisibility(View.VISIBLE);
                    }
                });
    }
    private void createEventButton(final String eventId, final String eventTitle, final String eventDate,
                                   final String startTime, final String endTime, final String location) {
        String title = (eventTitle != null) ? eventTitle : "Unknown Event";
        String date = (eventDate != null) ? eventDate : "Unknown Date";
        String start = (startTime != null) ? startTime : "Unknown Time";
        String end = (endTime != null) ? endTime : "Unknown Time";
        String eventLocation = (location != null) ? location : "Unknown Location";

        String buttonText = title + " (" + start + " - " + end + ")";
        Log.d(TAG, "✅ Scheduled Event Found: " + buttonText);

        Button eventButton = new Button(this);
        eventButton.setText(buttonText);
        eventButton.setPadding(16, 16, 16, 16);
        eventButton.setBackgroundResource(R.drawable.button_gold_border);
        eventButton.setTextColor(Color.WHITE); // Set text color to white

        eventButton.setOnClickListener(v -> {
            Intent intent = new Intent(Teachers_DailyAttendanceActivity.this, EventRoomsActivity.class);
            intent.putExtra("eventId", eventId);
            intent.putExtra("eventTitle", title);
            intent.putExtra("eventDate", date);
            intent.putExtra("startTime", start);
            intent.putExtra("endTime", end);
            intent.putExtra("location", eventLocation);
            startActivity(intent);
        });


        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 8, 0, 8);
        eventButton.setLayoutParams(params);

        eventContainer.addView(eventButton);
    }




    private void showEventPopup(Context context, String eventId, String title, String date, String startTime, String endTime, String location) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.activity_event_rooms, null);
        builder.setView(dialogView);

        TextView eventTitle = dialogView.findViewById(R.id.eventTitle);
        TextView eventDate = dialogView.findViewById(R.id.eventDate);
        TextView eventTime = dialogView.findViewById(R.id.eventTime);
        TextView eventLocation = dialogView.findViewById(R.id.eventLocation);
        LinearLayout roomListContainer = dialogView.findViewById(R.id.studentListContainer); // Using same container for rooms
        ProgressBar popupLoadingIndicator = dialogView.findViewById(R.id.popupLoadingIndicator);
        Button closeButton = dialogView.findViewById(R.id.closePopupButton);

        eventTitle.setText(title);
        eventDate.setText("Date: " + date);
        eventTime.setText("Time: " + startTime + " - " + endTime);
        eventLocation.setText("Location: " + location);

        AlertDialog dialog = builder.create();
        dialog.show();

        closeButton.setOnClickListener(v -> dialog.dismiss());

        // Fetch and display rooms for this event
        fetchEventRooms(eventId, roomListContainer, popupLoadingIndicator, context);
    }

    private void fetchEventRooms(String eventId, LinearLayout roomListContainer, ProgressBar popupLoadingIndicator, Context context) {
        popupLoadingIndicator.setVisibility(View.VISIBLE);
        roomListContainer.removeAllViews();

        db.collection("events")
                .document(eventId)
                .collection("rooms") // Fetch subcollection rooms (mapping)
                .get()
                .addOnSuccessListener(roomSnapshots -> {
                    popupLoadingIndicator.setVisibility(View.GONE);

                    if (roomSnapshots.isEmpty()) {
                        Log.d(TAG, "⚠️ No rooms linked to this event.");
                        TextView noRoomsText = new TextView(context);
                        noRoomsText.setText("No rooms linked to this event.");
                        roomListContainer.addView(noRoomsText);
                        return;
                    }

                    for (QueryDocumentSnapshot roomDoc : roomSnapshots) {
                        String roomId = roomDoc.getId(); // Get room ID
                        fetchRoomDetails(roomId, roomListContainer, context); // Fetch room details from main collection
                    }
                })
                .addOnFailureListener(e -> {
                    popupLoadingIndicator.setVisibility(View.GONE);
                    Log.e(TAG, "❌ Error fetching rooms for event", e);
                });
    }

    private void fetchRoomDetails(String roomId, LinearLayout roomListContainer, Context context) {
        db.collection("rooms")
                .document(roomId)
                .get()
                .addOnSuccessListener(roomDoc -> {
                    if (!roomDoc.exists()) {
                        Log.d(TAG, "⚠️ Room " + roomId + " does not exist in main collection.");
                        return;
                    }

                    String subjectName = roomDoc.getString("subjectName");
                    String section = roomDoc.getString("section");

                    if (subjectName == null) subjectName = "Unknown Subject";
                    if (section == null) section = "Unknown Section";

                    String buttonText = subjectName + " (" + section + ")";

                    Button roomButton = new Button(context);
                    roomButton.setText(buttonText);
                    roomButton.setPadding(16, 16, 16, 16);
                    roomButton.setBackgroundResource(R.drawable.button_gold_border);
                    roomButton.setTextColor(Color.WHITE);

                    roomButton.setOnClickListener(v -> showAttendancePopup(context, roomId, buttonText));

                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    params.setMargins(0, 8, 0, 8);
                    roomButton.setLayoutParams(params);

                    roomListContainer.addView(roomButton);
                })
                .addOnFailureListener(e -> Log.e(TAG, "❌ Error fetching room details", e));
    }
    private void showAttendancePopup(Context context, String roomId, String roomName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_attendance_popup, null);
        builder.setView(dialogView);

        TextView popupTitle = dialogView.findViewById(R.id.popupTitle);
        LinearLayout studentListContainer = dialogView.findViewById(R.id.studentListContainer);
        ProgressBar popupLoadingIndicator = dialogView.findViewById(R.id.popupLoadingIndicator);
        Button closeButton = dialogView.findViewById(R.id.closePopupButton);

        popupTitle.setText("Attendance - " + roomName);
        AlertDialog dialog = builder.create();
        dialog.show();

        closeButton.setOnClickListener(v -> dialog.dismiss());

        fetchAttendanceForRoom(roomId, studentListContainer, popupLoadingIndicator);
    }

}
