package com.example.smarttrack;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Students_Home extends AppCompatActivity {
    private static final String TAG = "Students_Home";
    private static final int QR_CODE_REQUEST_CODE = 1001;

    private ImageView roomIcon;
    private ImageView reportIcon;
    private ImageView scheduleIcon;
    private TextView dashboardMessage;
    private TextView locationTextView;
    private LocationManager locationManager;

    private boolean locationDisplayed = false;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private TextView navUsername, navIdNumber;
    private TextView noRoomsTextView;
    private LinearLayout roomsLayout;
    private LinearLayout floatingWindow;
    private String uid;
    private Button faceRegisterButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Setup toolbar and layout elements
        setupToolbarAndUI();

        // Get user ID from intent
        uid = getIntent().getStringExtra("uid");
        fetchStudentDetails(uid);
        fetchStudentDetailed(uid);

        // Fetch rooms and initialize location tracking
        fetchRoomsForToday();
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        requestLocationPermission();
    }

    private void setupToolbarAndUI() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        TextView toolbarTitle = findViewById(R.id.toolbarTitle);
        toolbarTitle.setText("Home");
        floatingWindow = findViewById(R.id.floatingWindow);
        dashboardMessage = findViewById(R.id.dashboardMessage);
        locationTextView = findViewById(R.id.locationTextView);
        roomsLayout = findViewById(R.id.roomsLayout);
        noRoomsTextView = findViewById(R.id.noRoomsTextView);
        roomIcon = findViewById(R.id.roomIcon);
        scheduleIcon = findViewById(R.id.scheduleIcon);
        reportIcon = findViewById(R.id.reportIcon);
        faceRegisterButton = findViewById(R.id.faceRegisterButton);

        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        navUsername = navigationView.findViewById(R.id.navUsername);
        navIdNumber = navigationView.findViewById(R.id.navIdNumber);

        ImageView menuIcon = findViewById(R.id.menuIcon);
        menuIcon.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        Button logoutButton = findViewById(R.id.logoutButton);
        logoutButton.setOnClickListener(v -> logout());

        roomIcon.setOnClickListener(v -> startActivity(new Intent(this, Students_Room.class).putExtra("uid", uid)));
        scheduleIcon.setOnClickListener(v -> startActivity(new Intent(this, Students_Calendar.class).putExtra("uid", uid)));
        reportIcon.setOnClickListener(v -> startActivity(new Intent(this, Students_Report.class).putExtra("uid", uid)));

        faceRegisterButton.setOnClickListener(v -> {
            Intent intent = new Intent(Students_Home.this, FaceRegister.class);
            intent.putExtra("uid", uid);
            startActivity(intent);
        });
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(Students_Home.this, Login.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        } else {
            getCurrentLocation();
        }
    }

    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationTextView.setText("Scanning your location...");
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, new LocationListener() {
                @Override
                public void onLocationChanged(@NonNull Location location) {
                    if (!locationDisplayed) {
                        getAddressFromLocation(location.getLatitude(), location.getLongitude());
                        locationDisplayed = true;
                    }
                }
                public void onStatusChanged(String provider, int status, Bundle extras) {}
                public void onProviderEnabled(String provider) {}
                public void onProviderDisabled(String provider) {}
            });
        }
    }

    private void getAddressFromLocation(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            locationTextView.setText(addresses.isEmpty() ? "Unable to fetch address." : "Location: " + addresses.get(0).getAddressLine(0));
        } catch (IOException e) {
            locationTextView.setText("Error fetching address.");
        }
    }

    private void fetchStudentDetails(String uid) {
        FirebaseFirestore.getInstance().collection("students").document(uid).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String firstName = document.getString("firstName");
                        String lastName = document.getString("lastName");
                        dashboardMessage.setText("Hi, Student " + firstName + " " + lastName + "!");
                    } else {
                        dashboardMessage.setText("Student details not found.");
                    }
                })
                .addOnFailureListener(e -> dashboardMessage.setText("Error fetching student details."));
    }

    private void fetchStudentDetailed(String uid) {
        FirebaseFirestore.getInstance().collection("students").document(uid).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        navUsername.setText(document.getString("firstName") + " " + document.getString("lastName"));
                        navIdNumber.setText(document.getString("idNumber"));
                    }
                })
                .addOnFailureListener(e -> {
                    navUsername.setText("Error fetching details");
                    navIdNumber.setText("");
                });
    }

    private void fetchRoomsForToday() {
        String today = getTodayDayName(); // Get today's name (e.g., "Tuesday")

        FirebaseFirestore.getInstance().collection("rooms").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        noRoomsTextView.setVisibility(View.GONE);
                        roomsLayout.removeAllViews();

                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            String roomId = document.getId();
                            String subjectCode = document.getString("subjectCode");
                            String section = document.getString("section");
                            List<String> schedule = (List<String>) document.get("schedule"); // Get schedule array

                            // Only display rooms that are scheduled for today
                            if (schedule != null && schedule.contains(today) && subjectCode != null && section != null) {
                                checkStudentInRoom(roomId, subjectCode, section);
                            }
                        }
                    } else {
                        noRoomsTextView.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error fetching rooms: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    // Get today's day name (e.g., "Monday", "Tuesday", etc.)
    private String getTodayDayName() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
        return dateFormat.format(calendar.getTime());
    }

    private void checkStudentInRoom(String roomId, String subjectCode, String section) {
        FirebaseFirestore.getInstance().collection("rooms").document(roomId)
                .collection("students").document(uid).get()
                .addOnSuccessListener(studentDoc -> {
                    if (studentDoc.exists()) {
                        createRoomButton(subjectCode, section, roomId);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error checking student in room: " + roomId, e));
    }

    private void createRoomButton(String subjectCode, String section, String roomId) {
        Button roomButton = new Button(this);
        roomButton.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        roomButton.setText(subjectCode + " - " + section);
        roomButton.setTextSize(18);
        roomButton.setTextColor(getResources().getColor(R.color.maroon, null));
        roomButton.setBackgroundResource(R.drawable.button_border);

        roomButton.setOnClickListener(v -> showFloatingWindow(roomId));
        runOnUiThread(() -> roomsLayout.addView(roomButton));
    }

    private void showFloatingWindow(String roomId) {
        View blurBackground = findViewById(R.id.blurBackground);
        floatingWindow.setVisibility(View.VISIBLE);
        blurBackground.setVisibility(View.VISIBLE);
        roomsLayout.setVisibility(View.GONE);

        Button timeInButton = findViewById(R.id.timeInButton);
        Button timeOutButton = findViewById(R.id.timeOutButton);

        timeInButton.setVisibility(View.GONE);
        timeOutButton.setVisibility(View.GONE);

        checkAttendanceStatus(roomId, timeInButton, timeOutButton);

        timeInButton.setOnClickListener(v -> {
            Intent intent = new Intent(Students_Home.this, ScanQRTimeIn.class);
            intent.putExtra("roomId", roomId);
            startActivity(intent);
            floatingWindow.setVisibility(View.GONE);
            blurBackground.setVisibility(View.GONE);
        });

        timeOutButton.setOnClickListener(v -> {
            Intent intent = new Intent(Students_Home.this, ScanQRTimeOut.class);
            showFeedbackDialog(roomId);
            intent.putExtra("roomId", roomId);
            startActivity(intent);
            floatingWindow.setVisibility(View.GONE);
            blurBackground.setVisibility(View.GONE);
        });

        findViewById(R.id.closeFloatingWindow).setOnClickListener(v -> {
            floatingWindow.setVisibility(View.GONE);
            blurBackground.setVisibility(View.GONE);
            roomsLayout.setVisibility(View.VISIBLE);
        });
    }

    private void checkAttendanceStatus(String roomId, Button timeInButton, Button timeOutButton) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("rooms").document(roomId)
                .collection("students").document(uid)
                .collection("attendance")
                .orderBy("timeIn", Query.Direction.DESCENDING)  // ✅ Get latest time-in first
                .limit(1)  // ✅ Only check the latest record
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // ✅ Fetch latest attendance document
                        DocumentSnapshot documentSnapshot = queryDocumentSnapshots.getDocuments().get(0);

                        boolean hasTimeIn = documentSnapshot.contains("timeIn");
                        boolean hasTimeOut = documentSnapshot.contains("timeOut");

                        if (!hasTimeIn) {
                            timeInButton.setVisibility(View.VISIBLE);  // ✅ Show Time In
                            timeOutButton.setVisibility(View.GONE);    // ❌ Hide Time Out
                        } else if (!hasTimeOut) {
                            timeInButton.setVisibility(View.GONE);    // ❌ Hide Time In
                            timeOutButton.setVisibility(View.VISIBLE); // ✅ Show Time Out
                        } else {
                            timeInButton.setVisibility(View.GONE);  // ❌ Hide Time In
                            timeOutButton.setVisibility(View.GONE); // ❌ Hide Time Out
                        }
                    } else {
                        timeInButton.setVisibility(View.VISIBLE);  // ✅ No attendance yet, show Time In
                        timeOutButton.setVisibility(View.GONE);    // ❌ Hide Time Out
                    }
                })
                .addOnFailureListener(e -> Log.e("AttendanceStatus", "❌ Error checking attendance: ", e));
    }

    private void showFeedbackDialog(String roomId) {
        // Inflate the custom layout for the dialog
        View dialogView = getLayoutInflater().inflate(R.layout.activity_feedback_dialog, null);
        EditText feedbackEditText = dialogView.findViewById(R.id.feedbackEditText);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);
        Button submitButton = dialogView.findViewById(R.id.submitButton);

        // Create and show the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialog.show();

        // Handle cancel button click
        cancelButton.setOnClickListener(v -> dialog.dismiss());

        // Handle submit button click
        submitButton.setOnClickListener(v -> {
            String feedback = feedbackEditText.getText().toString().trim();
            if (!feedback.isEmpty()) {
                // Save feedback to Firestore
                saveFeedback(roomId, feedback);
                dialog.dismiss();
                proceedWithTimeOut(roomId); // Proceed with the time-out process
            } else {
                Toast.makeText(this, "Feedback cannot be empty!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveFeedback(String roomId, String feedback) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> feedbackData = new HashMap<>();
        feedbackData.put("uid", uid);
        feedbackData.put("roomId", roomId);
        feedbackData.put("feedback", feedback);
        feedbackData.put("timestamp", new Date());

        db.collection("feedback")
                .add(feedbackData)
                .addOnSuccessListener(documentReference -> Log.d(TAG, "Feedback saved successfully"))
                .addOnFailureListener(e -> Log.e(TAG, "Error saving feedback", e));
    }

    private void proceedWithTimeOut(String roomId) {
        Intent intent = new Intent(Students_Home.this, ScanQRTimeOut.class);
        intent.putExtra("roomId", roomId);
        startActivity(intent);
    }
}