package com.example.smarttrack;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.annotation.NonNull;
import android.os.Build;
import android.graphics.Color;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;

import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.CalendarDay;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.HashSet;
import java.util.Calendar;
import java.util.Date;

public class Teachers_Calendar extends AppCompatActivity implements EventsAdapter.OnEventActionListener {

    private ImageView roomIcon;
    private ImageView homeIcon;
    private ImageView reportIcon;
    private String uid;
    private MaterialCalendarView calendarView;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private TextView navUsername, navIdNumber;
    private RecyclerView eventRecyclerView;
    private EventsAdapter eventsAdapter;
    private List<Event> eventList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        uid = getIntent().getStringExtra("uid");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        TextView toolbarTitle = findViewById(R.id.toolbarTitle);
        toolbarTitle.setText("Calendar");

        roomIcon = findViewById(R.id.roomIcon);
        homeIcon = findViewById(R.id.homeIcon);
        reportIcon = findViewById(R.id.reportIcon);
        calendarView = findViewById(R.id.calendarView);
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

        // Fetch teacher details
        fetchTeacherDetails(uid);

        // Logout button in navigation drawer
        Button logoutButton = findViewById(R.id.logoutButton);
        logoutButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(Teachers_Calendar.this, Login.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // Navigation buttons
        roomIcon.setOnClickListener(v -> navigateToActivity(Teachers_Room.class));
        homeIcon.setOnClickListener(v -> navigateToActivity(Teachers_Home.class));
        reportIcon.setOnClickListener(v -> navigateToActivity(Teachers_Report.class));

        // Floating action button for adding events
        FloatingActionButton addEventButton = findViewById(R.id.addEventButton);
        addEventButton.setOnClickListener(v -> {
            Intent intent = new Intent(Teachers_Calendar.this, Teachers_CreateEvent.class);
            intent.putExtra("uid", uid);
            startActivity(intent);
        });

        // Initialize RecyclerView
        eventList = new ArrayList<>();
        eventsAdapter = new EventsAdapter(this, eventList, this); // Pass context and listener
        eventRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        eventRecyclerView.setAdapter(eventsAdapter);

        Calendar currentCalendar = Calendar.getInstance();
        int currentYear = currentCalendar.get(Calendar.YEAR);
        int currentMonth = currentCalendar.get(Calendar.MONTH); // 0-based index

        fetchEvents(currentYear, currentMonth); // Load initial events
        calendarView.setOnMonthChangedListener((widget, date) -> {
            int selectedYear = date.getYear();
            int selectedMonth = date.getMonth(); // 0-based index

            System.out.println("Month Changed: " + selectedYear + "-" + (selectedMonth + 1));

            fetchEvents(selectedYear, selectedMonth); // Fetch events for the new month
        });

        // Ensure Notification Channel is created
        createNotificationChannel();

        // Request Notification Permission if required
        requestNotificationPermission();
    }


    private void fetchTeacherDetails(String uid) {
        FirebaseFirestore.getInstance().collection("teachers")
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

    private void fetchEvents(int selectedYear, int selectedMonth) {
        FirebaseFirestore.getInstance().collection("events")
                .whereEqualTo("teacherId", uid)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    eventList.clear();
                    HashSet<CalendarDay> eventDays = new HashSet<>();

                    System.out.println("🔥 Fetching events for Year: " + selectedYear + ", Month: " + (selectedMonth + 1));

                    for (QueryDocumentSnapshot document : querySnapshot) {
                        Event event = document.toObject(Event.class);
                        event.setId(document.getId());

                        // Debug: Print all retrieved events
                        System.out.println("📅 Event Found: " + event.getTitle() + " | Date: " + event.getEventDate());

                        // Ensure eventDate is in "YYYY-MM-DD" format
                        String eventDate = event.getEventDate();
                        if (eventDate != null && eventDate.matches("\\d{4}-\\d{2}-\\d{2}")) {
                            try {
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                                Date date = sdf.parse(eventDate);
                                Calendar calendar = Calendar.getInstance();
                                calendar.setTime(date);

                                int year = calendar.get(Calendar.YEAR);
                                int month = calendar.get(Calendar.MONTH); // Calendar uses 0-based month

                                // **FILTER EVENTS FOR SELECTED MONTH & YEAR**
                                if (year == selectedYear && month == selectedMonth) {
                                    fetchRoomsForEvent(event.getId(), event); // Fetch rooms from sub-collection
                                    eventList.add(event);

                                    int day = calendar.get(Calendar.DAY_OF_MONTH);
                                    CalendarDay calendarDay = CalendarDay.from(year, month, day);
                                    eventDays.add(calendarDay);

                                    // Debugging logs
                                    System.out.println("✅ Matched Event: " + event.getTitle() + " on " + eventDate);
                                } else {
                                    System.out.println("❌ Skipped Event: " + event.getTitle() + " (Not in selected month)");
                                }

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            System.out.println("⚠️ Invalid Date Format for Event: " + event.getTitle() + " | Date: " + eventDate);
                        }
                    }

                    System.out.println("📌 Total Events After Filtering: " + eventList.size());

                    // **Update UI**
                    eventsAdapter.notifyDataSetChanged(); // Refresh event list
                    addEventIndicatorsToCalendar(eventDays); // Update calendar markers
                    // **Schedule Reminders**
                    scheduleRemindersForEvents();
                })
                .addOnFailureListener(e -> {
                    e.printStackTrace(); // Log errors
                });
    }

    private void fetchRoomsForEvent(String eventId, Event event) {
        FirebaseFirestore.getInstance().collection("events").document(eventId).collection("rooms")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> roomIds = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        roomIds.add(doc.getString("roomId"));
                    }
                    event.setRooms(roomIds);
                    eventsAdapter.notifyDataSetChanged();
                });
    }

    private void addEventIndicatorsToCalendar(HashSet<CalendarDay> eventDays) {
            calendarView.addDecorator(new EventDecorator(eventDays, Color.RED)); // Red dots for events
        }



    private void scheduleRemindersForEvents() {
        for (Event event : eventList) {
            long eventTimeMillis = event.getEventTimestamp();
            if (eventTimeMillis == 0) continue; // Skip invalid events

            long twoHoursBefore = eventTimeMillis - (2 * 60 * 60 * 1000); // 2 hours before
            long oneHourBefore = eventTimeMillis - (1 * 60 * 60 * 1000); // 1 hour before

            scheduleTeacherEventReminder(event, twoHoursBefore, "Reminder: " + event.getTitle() + " starts in 2 hours!");
            scheduleTeacherEventReminder(event, oneHourBefore, "Reminder: " + event.getTitle() + " starts in 1 hour!");
        }
    }


    private void scheduleTeacherEventReminder(Event event, long reminderTimeMillis, String message) {
        if (reminderTimeMillis < System.currentTimeMillis()) return; // Skip past events

        Intent intent = new Intent(this, EventReminderReceiver.class);
        intent.putExtra("eventTitle", "Upcoming Event: " + event.getTitle());
        intent.putExtra("eventMessage", message);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, (event.getId() + message).hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTimeMillis, pendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminderTimeMillis, pendingIntent);
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

    private static final int NOTIFICATION_PERMISSION_CODE = 102;

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                System.out.println("Notification permission granted.");
            } else {
                System.out.println("Notification permission denied.");
            }
        }
    }


    private void navigateToActivity(Class<?> activityClass) {
        Intent intent = new Intent(Teachers_Calendar.this, activityClass);
        intent.putExtra("uid", uid);
        startActivity(intent);
        overridePendingTransition(0, 0);
    }
    @Override
    public void onEditEvent(Event event) {
        Intent intent = new Intent(this, Teachers_EditEvent.class);
        intent.putExtra("eventId", event.getId());
        intent.putExtra("eventTitle", event.getTitle());
        intent.putExtra("eventDescription", event.getDescription());
        intent.putExtra("eventLocation", event.getLocation());
        intent.putExtra("eventDate", event.getEventDate());
        intent.putExtra("eventStartTime", event.getStartTime());
        intent.putExtra("eventEndTime", event.getEndTime());
        intent.putExtra("notify", event.isNotify());
        intent.putExtra("wholeDay", event.isWholeDay());

        // Pass the list of selected rooms instead of students
        ArrayList<String> eventRooms = (ArrayList<String>) event.getRooms();
        intent.putStringArrayListExtra("eventRooms", eventRooms);

        startActivity(intent);
    }


    @Override
    public void onDeleteEvent(Event event) {
        FirebaseFirestore.getInstance().collection("events")
                .document(event.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    eventList.remove(event);
                    eventsAdapter.notifyDataSetChanged();
                    Toast.makeText(this, "Event deleted successfully.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to delete event.", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Refresh the event list
        Calendar currentCalendar = Calendar.getInstance();
        int currentYear = currentCalendar.get(Calendar.YEAR);
        int currentMonth = currentCalendar.get(Calendar.MONTH); // 0-based index

        fetchEvents(currentYear, currentMonth); // Refresh the events
    }
}