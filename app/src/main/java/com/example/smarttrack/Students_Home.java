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
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import android.view.View;
import android.widget.Toast;

import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;


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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

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

        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        navUsername = navigationView.findViewById(R.id.navUsername);
        navIdNumber = navigationView.findViewById(R.id.navIdNumber);



        roomIcon.setClickable(true);
        scheduleIcon.setClickable(true);
        reportIcon.setClickable(true);

        // Fetch UID from Intent
        ImageView menuIcon = findViewById(R.id.menuIcon);
        menuIcon.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));


        uid = getIntent().getStringExtra("uid");
        fetchStudentDetailed(uid);

        fetchRooms();

        Button logoutButton = findViewById(R.id.logoutButton);
        logoutButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(Students_Home.this, Login.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });


        roomIcon.setOnClickListener(v -> {
            getSupportActionBar().setTitle("Room");
            Intent intent = new Intent(Students_Home.this, Students_Room.class);
            intent.putExtra("uid", uid);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        scheduleIcon.setOnClickListener(v -> {
            getSupportActionBar().setTitle("Calendar");
            Intent intent = new Intent(Students_Home.this, Students_Calendar.class);
            intent.putExtra("uid", uid);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        reportIcon.setOnClickListener(v -> {
            getSupportActionBar().setTitle("Home");
            Intent intent = new Intent(Students_Home.this, Students_Report.class);
            intent.putExtra("uid", uid);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        fetchStudentDetails(uid);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        requestLocationPermission();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if the user is logged in
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            // Redirect to login page if no user is logged in
            Intent intent = new Intent(Students_Home.this, Login.class);
            startActivity(intent);
            finish();
        }
    }

    private void fetchStudentDetails(String uid) {
        FirebaseFirestore.getInstance().collection("students")
                .document(uid)
                .get()
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
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();
                        getAddressFromLocation(latitude, longitude);
                        locationDisplayed = true;
                    }
                }

                @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
                @Override public void onProviderEnabled(String provider) {}
                @Override public void onProviderDisabled(String provider) {}
            });
        }
    }

    private void getAddressFromLocation(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (!addresses.isEmpty()) {
                Address address = addresses.get(0);
                String fullAddress = address.getAddressLine(0);
                locationTextView.setText("Location: " + fullAddress);
            } else {
                locationTextView.setText("Unable to fetch address.");
            }
        } catch (IOException e) {
            e.printStackTrace();
            locationTextView.setText("Error fetching address.");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation();
        } else {
            locationTextView.setText("Permission denied. Cannot fetch location.");
        }
    }

    private void fetchRooms() {
        Log.d(TAG, "Fetching rooms...");
        FirebaseFirestore.getInstance().collection("rooms")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        Log.d(TAG, "Rooms fetched successfully. Count: " + queryDocumentSnapshots.size());
                        noRoomsTextView.setVisibility(View.GONE);
                        roomsLayout.removeAllViews();

                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            String roomId = document.getId();
                            String subjectCode = document.getString("subjectCode");
                            String section = document.getString("section");

                            if (subjectCode == null || section == null) {
                                Log.w(TAG, "Room ID: " + roomId + " has missing 'subjectCode' or 'section'. Skipping.");
                                continue; // Skip this room if data is missing
                            }

                            Log.d(TAG, "Room ID found: " + roomId + ", Subject Code: " + subjectCode + ", Section: " + section);
                            checkStudentInRoom(roomId, subjectCode, section);
                        }
                    } else {
                        noRoomsTextView.setVisibility(View.VISIBLE);
                        Log.d(TAG, "No rooms available.");
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error fetching rooms: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error fetching rooms: " + e.getMessage());
                });
    }

    private void checkStudentInRoom(String roomId, String subjectCode, String section) {
        Log.d(TAG, "Checking if student is part of room: " + roomId);

        FirebaseFirestore.getInstance()
                .collection("rooms")
                .document(roomId)
                .collection("students")
                .document(uid)
                .get()
                .addOnSuccessListener(studentDoc -> {
                    if (studentDoc.exists()) {
                        Log.d(TAG, "Student is part of room: " + roomId);
                        createRoomButton(subjectCode, section, roomId);
                    } else {
                        Log.d(TAG, "Student not part of room: " + roomId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking student in room: " + roomId, e);
                });
    }

    private void createRoomButton(String subjectCode, String section, String roomId) {
        Log.d(TAG, "Creating button for room: " + subjectCode + " - " + section);

        Button roomButton = new Button(this);
        roomButton.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        roomButton.setText(subjectCode + " - " + section);
        roomButton.setTextSize(25);
        roomButton.setPadding(20, 20, 20, 20);
        roomButton.setTextColor(getResources().getColor(R.color.maroon, null));
        roomButton.setBackgroundResource(R.drawable.button_border);

        roomButton.setOnClickListener(v -> {
            Toast.makeText(this, "Room clicked: " + subjectCode + " - " + section, Toast.LENGTH_SHORT).show();
            showFloatingWindow(subjectCode, section, roomId);
        });

        runOnUiThread(() -> {
            // Make the layout visible and add the button
            if (roomsLayout.getVisibility() == View.GONE) {
                roomsLayout.setVisibility(View.VISIBLE);
            }
            roomsLayout.addView(roomButton);
            Log.d(TAG, "Button added to UI. Total children: " + roomsLayout.getChildCount());
        });
    }

    private void showFloatingWindow(String roomCode, String section, String roomId) {
        View blurBackground = findViewById(R.id.blurBackground);
        floatingWindow.setVisibility(View.VISIBLE);  // Show the floating window
        blurBackground.setVisibility(View.VISIBLE);  // Show the blur background
        roomsLayout.setVisibility(View.GONE);

        // Set room-related text
        Button timeInButton = findViewById(R.id.timeInButton);
        Button timeOutButton = findViewById(R.id.timeOutButton);
        timeInButton.setText("Time - In");
        timeOutButton.setText("Time - Out");

        timeInButton.setOnClickListener(v -> {
            Intent intent = new Intent(Students_Home.this, ScanQRTimeIn.class);
            startActivityForResult(intent, QR_CODE_REQUEST_CODE);
        });

        timeOutButton.setOnClickListener(v -> {
            Intent intent = new Intent(Students_Home.this, ScanQRTimeOut.class);
            startActivityForResult(intent, QR_CODE_REQUEST_CODE);
        });


        // Close button for the floating window
        ImageView closeFloatingWindow = findViewById(R.id.closeFloatingWindow);
        closeFloatingWindow.setOnClickListener(v -> {
            floatingWindow.setVisibility(View.GONE);  // Hide the floating window
            blurBackground.setVisibility(View.GONE);  // Hide the blur background
            roomsLayout.setVisibility(View.VISIBLE);  // Show the rooms layout again
        });
    }
}