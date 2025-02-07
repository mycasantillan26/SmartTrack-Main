package com.example.smarttrack;

import android.app.DatePickerDialog;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

public class Teachers_CreateEvent extends AppCompatActivity {

    private EditText eventTitleField, eventDescField, locationField, studentsField;
    private Button startTimePicker, endTimePicker, createEventButton, selectDateButton, eventBackButton;
    private CheckBox notifyCheckBox, wholeDayCheckBox;
    private TextView selectedDateTextView;
    private Button selectRoomsButton;
    private TextView selectedRoomsTextView;
    private List<String> selectedRoomIds = new ArrayList<>();
    private List<String> subjectNames = new ArrayList<>();

    private int selectedYear, selectedMonth, selectedDay;

    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event);

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance();

        // Initialize UI elements
        eventTitleField = findViewById(R.id.eventTitleField);
        eventDescField = findViewById(R.id.eventDescField);
        locationField = findViewById(R.id.locationField);
        selectRoomsButton = findViewById(R.id.selectRoomsButton);
        selectedRoomsTextView = findViewById(R.id.selectedRoomsTextView);
        startTimePicker = findViewById(R.id.eventStartTimeField);
        endTimePicker = findViewById(R.id.eventEndTimeField);
        createEventButton = findViewById(R.id.createEventButton);
        selectDateButton = findViewById(R.id.selectDateButton);
        eventBackButton = findViewById(R.id.eventBackButton);
        notifyCheckBox = findViewById(R.id.notifycheckBox);
        wholeDayCheckBox = findViewById(R.id.wholeDaycheckBox);
        selectedDateTextView = findViewById(R.id.selectedDateTextView);


        // Initialize the current date
        Calendar calendar = Calendar.getInstance();
        selectedYear = calendar.get(Calendar.YEAR);
        selectedMonth = calendar.get(Calendar.MONTH);
        selectedDay = calendar.get(Calendar.DAY_OF_MONTH);

        // Set the default selected date
        selectedDateTextView.setText(String.format(Locale.getDefault(), "Selected Date: %04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay));

        // Set up button listeners
        selectDateButton.setOnClickListener(v -> showDatePickerDialog());
        startTimePicker.setOnClickListener(v -> showTimePickerDialog(startTimePicker));
        endTimePicker.setOnClickListener(v -> showTimePickerDialog(endTimePicker));
        createEventButton.setOnClickListener(v -> saveEventToFirestore());
        eventBackButton.setOnClickListener(v -> {
            Intent intent = new Intent(Teachers_CreateEvent.this, Teachers_Calendar.class);
            startActivity(intent);
            finish();
        });

        selectRoomsButton.setOnClickListener(v -> showRoomSelectionDialog());
        fetchRooms();
    }

    private void showDatePickerDialog() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedYear = year;
                    selectedMonth = month;
                    selectedDay = dayOfMonth;
                    selectedDateTextView.setText(String.format(Locale.getDefault(), "Selected Date: %04d-%02d-%02d", year, month + 1, dayOfMonth));
                },
                selectedYear, selectedMonth, selectedDay
        );
        datePickerDialog.show();
    }

    private void showTimePickerDialog(Button timeButton) {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minuteOfHour) -> timeButton.setText(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minuteOfHour)),
                hour,
                minute,
                false
        );
        timePickerDialog.show();
    }

    private void fetchRooms() {
        String teacherId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        firestore.collection("rooms")
                .whereEqualTo("teacherId", teacherId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    subjectNames.clear();
                    selectedRoomIds.clear();

                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        String subjectName = doc.getString("subjectName");
                        String roomCode = doc.getString("roomCode");
                        String roomId = doc.getId(); // Fetch document ID

                        if (subjectName != null && roomCode != null) {
                            subjectNames.add(subjectName + " - " + roomCode);
                            selectedRoomIds.add(roomId); // Store Room Document ID
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load rooms", Toast.LENGTH_SHORT).show());
    }

    private void showRoomSelectionDialog() {
        boolean[] checkedRooms = new boolean[subjectNames.size()];
        List<String> selectedSubjectNames = new ArrayList<>();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Rooms");
        builder.setMultiChoiceItems(subjectNames.toArray(new String[0]), checkedRooms, (dialog, which, isChecked) -> {
            if (isChecked) {
                selectedSubjectNames.add(subjectNames.get(which));
            } else {
                selectedSubjectNames.remove(subjectNames.get(which));
            }
        });

        builder.setPositiveButton("OK", (dialog, which) -> {
            selectedRoomsTextView.setText(selectedSubjectNames.isEmpty() ? "No rooms selected" : String.join(", ", selectedSubjectNames));
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        builder.show();
    }

    private void saveEventToFirestore() {
        // Retrieve data from fields
        String eventTitle = eventTitleField.getText().toString().trim();
        String eventDesc = eventDescField.getText().toString().trim();
        String location = locationField.getText().toString().trim();

        if (selectedRoomIds.isEmpty()) {
            Toast.makeText(this, "Please select at least one room", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean notify = notifyCheckBox.isChecked();
        boolean allDay = wholeDayCheckBox.isChecked();
        String eventDate = selectedDateTextView.getText().toString().replace("Selected Date: ", "").trim();
        String startTime = startTimePicker.getText().toString().trim();
        String endTime = endTimePicker.getText().toString().trim();

        // Validate input fields
        if (eventTitle.isEmpty() || eventDesc.isEmpty() || location.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!allDay && (startTime.isEmpty() || endTime.isEmpty())) {
            Toast.makeText(this, "Please select start and end times", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get the UID of the currently signed-in teacher
        String teacherId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (teacherId == null || teacherId.isEmpty()) {
            Toast.makeText(this, "Error: No teacher ID found. Please log in again.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Prepare data to save in Firestore
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("title", eventTitle);
        eventData.put("description", eventDesc);
        eventData.put("location", location);
        eventData.put("rooms", selectedRoomIds); // Save Room Document IDs
        eventData.put("notify", notify);
        eventData.put("wholeDay", allDay);
        eventData.put("eventDate", eventDate);
        eventData.put("teacherId", teacherId); // Save teacher's UID

        if (allDay) {
            eventData.put("startTime", "08:00");
            eventData.put("endTime", "17:00");
        } else {
            eventData.put("startTime", startTime);
            eventData.put("endTime", endTime);
        }

        // Save the event to Firestore
        firestore.collection("events")
                .add(eventData) // Room Document IDs will be stored here
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(Teachers_CreateEvent.this, "Event created successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(Teachers_CreateEvent.this, "Failed to create event: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
