package com.example.smarttrack;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.CalendarDay;

import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Calendar;

public class Students_Calendar extends AppCompatActivity {

    private ImageView roomIcon, homeIcon, reportIcon;
    private String uid;
    private MaterialCalendarView studentCalendarView;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private TextView navUsername, navIdNumber;
    private RecyclerView eventRecyclerView;
    private Students_EventsAdapter eventsAdapter;
    private List<Event> eventList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar_student);

        uid = getIntent().getStringExtra("uid");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        TextView toolbarTitle = findViewById(R.id.toolbarTitle);
        toolbarTitle.setText("Calendar");

        roomIcon = findViewById(R.id.roomIcon);
        homeIcon = findViewById(R.id.homeIcon);
        reportIcon = findViewById(R.id.reportIcon);
        studentCalendarView = findViewById(R.id.studentCalendarView);
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        navUsername = navigationView.findViewById(R.id.navUsername);
        navIdNumber = navigationView.findViewById(R.id.navIdNumber);
        eventRecyclerView = findViewById(R.id.eventRecyclerView);

        roomIcon.setClickable(true);
        reportIcon.setClickable(true);
        homeIcon.setClickable(true);

        ImageView menuIcon = findViewById(R.id.menuIcon);
        menuIcon.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // Logout button in navigation drawer
        Button logoutButton = findViewById(R.id.logoutButton);
        logoutButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(Students_Calendar.this, Login.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // Navigation buttons
        roomIcon.setOnClickListener(v -> navigateToActivity(Students_Room.class));
        homeIcon.setOnClickListener(v -> navigateToActivity(Students_Home.class));
        reportIcon.setOnClickListener(v -> navigateToActivity(Students_Report.class));

        // Fetch student details
        fetchStudentDetails(uid);


        // Initialize RecyclerView
        eventList = new ArrayList<>();
        eventsAdapter = new Students_EventsAdapter(this, eventList, event -> {
            // Navigate to Students_EventDetails
            Intent intent = new Intent(Students_Calendar.this, Students_EventDetails.class);
            intent.putExtra("eventId", event.getId()); // Pass event ID
            startActivity(intent);
        });
        eventRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        eventRecyclerView.setAdapter(eventsAdapter);


        // Fetch events from Firestore
        // Initialize with the current month
        Calendar currentCalendar = Calendar.getInstance();
        int currentYear = currentCalendar.get(Calendar.YEAR);
        int currentMonth = currentCalendar.get(Calendar.MONTH); // 0-based index

        fetchEvents(currentYear, currentMonth); // Load initial events

        // Listen for month changes
        studentCalendarView.setOnMonthChangedListener((widget, date) -> {
            int selectedYear = date.getYear();
            int selectedMonth = date.getMonth(); // 0-based index

            System.out.println("📆 Month Changed: " + selectedYear + "-" + (selectedMonth + 1));
            fetchEvents(selectedYear, selectedMonth); // Fetch events for the new month
        });

        // Ensure Notification Channel is created
        createNotificationChannel();

        requestNotificationPermission();

    }

    private void fetchStudentDetails(String uid) {
        FirebaseFirestore.getInstance().collection("students")
                .document(uid)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String firstName = document.getString("firstName");
                        String lastName = document.getString("lastName");
                        String idNumber = document.getString("idNumber");
                        navUsername.setText(firstName + " " + lastName);
                        navIdNumber.setText(idNumber);
                    }
                })
                .addOnFailureListener(e -> {
                    navUsername.setText("Error fetching details");
                    navIdNumber.setText("");
                });
    }

    private void navigateToActivity(Class<?> activityClass) {
        Intent intent = new Intent(Students_Calendar.this, activityClass);
        intent.putExtra("uid", uid);
        startActivity(intent);
        overridePendingTransition(0, 0);
    }

    private void fetchEvents(int selectedYear, int selectedMonth) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("rooms").get().addOnSuccessListener(roomsSnapshot -> {
            List<String> studentRooms = new ArrayList<>();
            for (QueryDocumentSnapshot roomDoc : roomsSnapshot) {
                String roomId = roomDoc.getId();
                db.collection("rooms").document(roomId).collection("students").document(uid).get()
                        .addOnSuccessListener(studentDoc -> {
                            if (studentDoc.exists()) {
                                studentRooms.add(roomId);
                                fetchFilteredEventsForRooms(studentRooms, selectedYear, selectedMonth);
                            }
                        });
            }
        });
    }


    private void fetchFilteredEventsForRooms(List<String> studentRooms, int selectedYear, int selectedMonth) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("events").get().addOnSuccessListener(eventsSnapshot -> {
            List<Event> newOrEditedEvents = new ArrayList<>();
            eventList.clear();
            HashSet<CalendarDay> eventDays = new HashSet<>();

            for (QueryDocumentSnapshot eventDoc : eventsSnapshot) {
                Event event = eventDoc.toObject(Event.class);
                event.setId(eventDoc.getId());
                String eventDate = eventDoc.getString("eventDate");
                boolean notify = Boolean.TRUE.equals(eventDoc.getBoolean("notify")); // Get the notify field

                List<String> eventRooms = (List<String>) eventDoc.get("rooms");

                if (eventRooms != null && eventDate != null) {
                    try {
                        // Convert eventDate (YYYY-MM-DD) to year and month
                        String[] dateParts = eventDate.split("-");
                        int eventYear = Integer.parseInt(dateParts[0]);
                        int eventMonth = Integer.parseInt(dateParts[1]) - 1; // CalendarDay uses 0-based months
                        int eventDay = Integer.parseInt(dateParts[2]);

                        // **Filter by selected year & month**
                        if (eventYear == selectedYear && eventMonth == selectedMonth) {
                            for (String studentRoom : studentRooms) {
                                if (eventRooms.contains(studentRoom)) {
                                    if (!eventList.contains(event)) {
                                        eventList.add(event);
                                        eventDays.add(CalendarDay.from(eventYear, eventMonth, eventDay));

                                        // Only add events to newOrEditedEvents if notify is true
                                        if (notify) {
                                            newOrEditedEvents.add(event);
                                        }
                                    }
                                    break;
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace(); // Log errors
                    }
                }
            }

            // Schedule reminders only for events with notify = true
            scheduleRemindersForEvents(newOrEditedEvents);

            System.out.println("📌 Filtered Events for " + selectedYear + "-" + (selectedMonth + 1) + ": " + eventList.size());
            eventsAdapter.notifyDataSetChanged();
            addEventIndicatorsToCalendar(eventDays);
        });
    }




    private void addEventIndicatorsToCalendar(HashSet<CalendarDay> eventDays) {
        studentCalendarView.addDecorator(new EventDecorator(eventDays, Color.RED)); // Red dots for events
    }


    private void scheduleEventReminder(Event event, long reminderTimeMillis, String message) {
        if (reminderTimeMillis < System.currentTimeMillis()) return; // Skip past events

        Intent intent = new Intent(this, EventReminderReceiver.class);
        intent.putExtra("eventTitle", "Upcoming Event: " + event.getTitle());
        intent.putExtra("eventMessage", message);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, event.getId().hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminderTimeMillis, pendingIntent);
    }


    private void scheduleRemindersForEvents(List<Event> events) {
        for (Event event : events) {
            long eventTimeMillis = event.getEventTimestamp();
            if (eventTimeMillis == 0) continue; // Skip invalid events

            long twoHoursBefore = eventTimeMillis - (2 * 60 * 60 * 1000); // 2 hours before
            long oneHourBefore = eventTimeMillis - (1 * 60 * 60 * 1000); // 1 hour before

            scheduleEventReminder(event, twoHoursBefore, "Reminder: " + event.getTitle() + " starts in 2 hours!");
            scheduleEventReminder(event, oneHourBefore, "Reminder: " + event.getTitle() + " starts in 1 hour!");
        }
    }


    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "event_reminder",
                    "Event Reminders",
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private static final int NOTIFICATION_PERMISSION_CODE = 101;

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_CODE);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Refresh the event list
        Calendar currentCalendar = Calendar.getInstance();
        int currentYear = currentCalendar.get(Calendar.YEAR);
        int currentMonth = currentCalendar.get(Calendar.MONTH); // 0-based index

        fetchEvents(currentYear, currentMonth); // Reload events
    }
}