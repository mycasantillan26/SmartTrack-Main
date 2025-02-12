package com.example.smarttrack;

import android.app.DatePickerDialog;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

public class Teachers_EditEvent extends AppCompatActivity {
    private String eventId;
    private FirebaseFirestore db;

    private EditText eventTitleField, eventDescField, locationField;
    private TextView selectedDateTextView, selectedRoomsTextView;
    private Button selectDateButton, startTimePicker, endTimePicker, editEventButton, eventBackButton, selectRoomsButton;
    private CheckBox notifyCheckBox, wholeDayCheckBox;

    private List<String> selectedRoomIds = new ArrayList<>();
    private List<String> subjectNames = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_event);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize UI elements
        eventTitleField = findViewById(R.id.eventTitleField);
        eventDescField = findViewById(R.id.eventDescField);
        locationField = findViewById(R.id.locationField);
        selectedRoomsTextView = findViewById(R.id.selectedRoomsTextView);
        selectRoomsButton = findViewById(R.id.selectRoomsButton);
        selectedDateTextView = findViewById(R.id.selectedDateTextView);
        selectDateButton = findViewById(R.id.selectDateButton);
        startTimePicker = findViewById(R.id.eventStartTimeField);
        endTimePicker = findViewById(R.id.eventEndTimeField);
        notifyCheckBox = findViewById(R.id.notifycheckBox);
        wholeDayCheckBox = findViewById(R.id.wholeDaycheckBox);
        editEventButton = findViewById(R.id.editEventButton);
        eventBackButton = findViewById(R.id.eventBackButton);

        // Get eventId from Intent
        eventId = getIntent().getStringExtra("eventId");
        selectedRoomIds = getIntent().getStringArrayListExtra("eventRooms");

        // Fetch event details from Firestore
        fetchEventDetails();

        // Handle editing the event
        editEventButton.setOnClickListener(v -> updateEventInFirestore());

        // Set listeners for date and time pickers
        selectDateButton.setOnClickListener(v -> showDatePickerDialog());
        startTimePicker.setOnClickListener(v -> showTimePickerDialog(startTimePicker));
        endTimePicker.setOnClickListener(v -> showTimePickerDialog(endTimePicker));

        // Handle room selection
        selectRoomsButton.setOnClickListener(v -> showRoomSelectionDialog());

        // Handle back button click
        eventBackButton.setOnClickListener(v -> finish());
    }

    private void fetchEventDetails() {
        db.collection("events").document(eventId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Populate fields with fetched data
                        eventTitleField.setText(documentSnapshot.getString("title"));
                        eventDescField.setText(documentSnapshot.getString("description"));
                        locationField.setText(documentSnapshot.getString("location"));

                        String eventDate = documentSnapshot.getString("eventDate");
                        selectedDateTextView.setText("Selected Date: " + eventDate);

                        startTimePicker.setText(documentSnapshot.getString("startTime"));
                        endTimePicker.setText(documentSnapshot.getString("endTime"));
                        notifyCheckBox.setChecked(documentSnapshot.getBoolean("notify"));
                        wholeDayCheckBox.setChecked(documentSnapshot.getBoolean("wholeDay"));

                        // **Clear old room IDs before adding new ones**
                        selectedRoomIds.clear();

                        // Load selected room IDs
                        List<String> roomIds = (List<String>) documentSnapshot.get("rooms");
                        if (roomIds != null) {
                            selectedRoomIds.addAll(roomIds);
                        }

                        // Fetch available rooms
                        fetchRooms();
                    } else {
                        Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to fetch event details: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void fetchRooms() {
        String teacherId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        db.collection("rooms")
                .whereEqualTo("teacherId", teacherId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    subjectNames.clear();

                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        String subjectName = doc.getString("subjectName");
                        String roomCode = doc.getString("roomCode");
                        String roomId = doc.getId();

                        if (subjectName != null && roomCode != null) {
                            subjectNames.add(subjectName + " - " + roomCode);
                        }
                    }

                    updateSelectedRoomsUI();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load rooms", Toast.LENGTH_SHORT).show());
    }

    private void showRoomSelectionDialog() {
        boolean[] checkedRooms = new boolean[subjectNames.size()];
        Arrays.fill(checkedRooms, false);

        List<String> selectedSubjectNames = new ArrayList<>();
        List<String> selectedRoomIdsList = new ArrayList<>();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Rooms");

        builder.setMultiChoiceItems(subjectNames.toArray(new String[0]), checkedRooms, (dialog, which, isChecked) -> {
            if (isChecked) {
                selectedSubjectNames.add(subjectNames.get(which));
                selectedRoomIdsList.add(selectedRoomIds.get(which));
            } else {
                selectedSubjectNames.remove(subjectNames.get(which));
                selectedRoomIdsList.remove(selectedRoomIds.get(which));
            }
        });

        builder.setPositiveButton("OK", (dialog, which) -> {
            selectedRoomsTextView.setText(selectedSubjectNames.isEmpty() ? "No rooms selected" : String.join(", ", selectedSubjectNames));
            selectedRoomIds = new ArrayList<>(selectedRoomIdsList);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void updateEventInFirestore() {
        String updatedTitle = eventTitleField.getText().toString().trim();
        String updatedDescription = eventDescField.getText().toString().trim();
        String updatedLocation = locationField.getText().toString().trim();
        String updatedDate = selectedDateTextView.getText().toString().replace("Selected Date: ", "").trim();
        String updatedStartTime = startTimePicker.getText().toString().trim();
        String updatedEndTime = endTimePicker.getText().toString().trim();
        boolean updatedNotify = notifyCheckBox.isChecked();
        boolean updatedWholeDay = wholeDayCheckBox.isChecked();

        if (updatedTitle.isEmpty() || updatedDescription.isEmpty() || updatedLocation.isEmpty() || updatedDate.isEmpty()) {
            Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> eventData = new HashMap<>();
        eventData.put("title", updatedTitle);
        eventData.put("description", updatedDescription);
        eventData.put("location", updatedLocation);
        eventData.put("eventDate", updatedDate);
        eventData.put("startTime", updatedStartTime);
        eventData.put("endTime", updatedEndTime);
        eventData.put("notify", updatedNotify);
        eventData.put("wholeDay", updatedWholeDay);
        eventData.put("rooms", selectedRoomIds);

        db.collection("events").document(eventId)
                .update(eventData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Event updated successfully", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update event: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String formattedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay);
                    selectedDateTextView.setText("Selected Date: " + formattedDate);
                },
                year, month, day
        );
        datePickerDialog.show();
    }

    private void showTimePickerDialog(Button timeButton) {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, selectedHour, selectedMinute) -> {
                    String formattedTime = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute);
                    timeButton.setText(formattedTime);
                },
                hour, minute, false
        );
        timePickerDialog.show();
    }

    private void updateSelectedRoomsUI() {
        List<String> selectedSubjectNames = new ArrayList<>();

        for (String roomId : selectedRoomIds) {
            int index = selectedRoomIds.indexOf(roomId);
            if (index != -1 && index < subjectNames.size()) {
                selectedSubjectNames.add(subjectNames.get(index));
            }
        }

        selectedRoomsTextView.setText(selectedSubjectNames.isEmpty() ? "No rooms selected" : String.join(", ", selectedSubjectNames));
    }
}
