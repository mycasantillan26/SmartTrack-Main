package com.example.smarttrack;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class Students_Room extends AppCompatActivity {

    private static final String TAG = "Students_Room";

    private ImageView reportIcon;
    private ImageView homeIcon;
    private ImageView scheduleIcon;
    private String uid;
    private Button scanQRButton, inputCodeButton, createRoomButton;
    private TextView noRoomsTextView;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private TextView navUsername, navIdNumber;
    private LinearLayout roomsLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rooms);

        // Setup Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        TextView toolbarTitle = findViewById(R.id.toolbarTitle);
        toolbarTitle.setText("Rooms");

        // Retrieve data from the Intent
        String roomId = getIntent().getStringExtra("roomId");
        String teacherId = getIntent().getStringExtra("teacherId");
        String section = getIntent().getStringExtra("section");
        String subjectCode = getIntent().getStringExtra("subjectCode");
        String studentId = getIntent().getStringExtra("studentId");

        // Initialize uid
        if (studentId != null) {
            uid = studentId;
        } else {
            uid = FirebaseAuth.getInstance().getUid(); // Fallback to logged-in user
        }

        // Validate data
        if (uid == null) {
            Toast.makeText(this, "Student ID is missing. Please log in again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Log the received data
        Log.d(TAG, "Room ID: " + roomId);
        Log.d(TAG, "Teacher ID: " + teacherId);
        Log.d(TAG, "Section: " + section);
        Log.d(TAG, "Subject Code: " + subjectCode);
        Log.d(TAG, "Student ID: " + uid);

        // Allow user to open the room even if no data is fetched
        if (roomId != null) {
            fetchStudentSections(roomId, uid);
        } else {
            Toast.makeText(this, "Room opened without fetched data.", Toast.LENGTH_SHORT).show();
        }

        noRoomsTextView = findViewById(R.id.noRoomsTextView);
        roomsLayout = findViewById(R.id.roomsLayout);

        // Setup Views
        setupUI();
        fetchStudentDetailed(uid);
        fetchRooms();
    }

    private void setupUI() {
        reportIcon = findViewById(R.id.reportIcon);
        homeIcon = findViewById(R.id.homeIcon);
        scheduleIcon = findViewById(R.id.scheduleIcon);
        scanQRButton = findViewById(R.id.scanQRButtons);
        inputCodeButton = findViewById(R.id.inputCodeButton);

        // Drawer setup
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        navUsername = navigationView.findViewById(R.id.navUsername);
        navIdNumber = navigationView.findViewById(R.id.navIdNumber);
        createRoomButton = findViewById(R.id.createRoomButton);


        createRoomButton.setVisibility(View.GONE);


        inputCodeButton.setOnClickListener(v -> {
            Intent intent = new Intent(Students_Room.this, InputCodeActivity.class);
            startActivity(intent);
        });

// Set click listener for scanQRButton
        scanQRButton.setOnClickListener(v -> {
            Intent intent = new Intent(Students_Room.this, ScanQRActivity.class);
            startActivity(intent);
        });



        reportIcon.setOnClickListener(v -> {
            getSupportActionBar().setTitle("Room");
            Intent intent = new Intent(Students_Room.this, Students_Report.class);
            intent.putExtra("uid", uid);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        // Set click listener for homeIcon
        homeIcon.setOnClickListener(v -> {
            getSupportActionBar().setTitle("Home");
            Intent intent = new Intent(Students_Room.this, Students_Home.class);
            intent.putExtra("uid", uid);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        // Set click listener for scheduleIcon
        scheduleIcon.setOnClickListener(v -> {
            getSupportActionBar().setTitle("Calendar");
            Intent intent = new Intent(Students_Room.this, Students_Calendar.class);
            intent.putExtra("uid", uid);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        Button logoutButton = findViewById(R.id.logoutButton);
        logoutButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(Students_Room.this, Login.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        ImageView menuIcon = findViewById(R.id.menuIcon);
        menuIcon.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
    }

    private void fetchStudentDetailed(String uid) {
        Log.d(TAG, "Fetching student details for UID: " + uid);
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
                        Log.d(TAG, "Student details fetched: " + firstName + " " + lastName);
                    } else {
                        Log.d(TAG, "Student document does not exist.");
                        navUsername.setText("Unknown User");
                        navIdNumber.setText("N/A");
                    }
                })
                .addOnFailureListener(e -> {
                    navUsername.setText("Error fetching details");
                    navIdNumber.setText("N/A");
                    Log.e(TAG, "Error fetching student details: " + e.getMessage());
                });
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

        // ✅ Add space between buttons using margins
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        layoutParams.setMargins(0, 20, 0, 20); // Adds 20dp space above and below
        roomButton.setLayoutParams(layoutParams);

        // Set button text and style
        roomButton.setText(subjectCode + " - " + section);
        roomButton.setTextSize(25);
        roomButton.setTextColor(getResources().getColor(R.color.maroon, null));
        roomButton.setBackgroundResource(R.drawable.button_border);

        // ✅ Correcting the Intent to Start StudentView with roomId
        roomButton.setOnClickListener(v -> {
            if (roomId == null || roomId.isEmpty()) {
                Log.e(TAG, "Room button click failed: roomId is NULL or EMPTY");
                Toast.makeText(this, "Error: Room ID missing!", Toast.LENGTH_SHORT).show();
                return;
            }

            Log.d(TAG, "Room button clicked: Redirecting to StudentView with roomId: " + roomId);

            Intent intent = new Intent(Students_Room.this, StudentView.class);
            intent.putExtra("roomId", roomId); // ✅ Pass the roomId properly
            startActivity(intent);
        });

        // ✅ Run UI update on the main thread
        runOnUiThread(() -> {
            if (roomsLayout.getVisibility() == View.GONE) {
                roomsLayout.setVisibility(View.VISIBLE);
            }
            roomsLayout.addView(roomButton);
            Log.d(TAG, "Button added to UI. Total children: " + roomsLayout.getChildCount());

            // ✅ Scroll to the bottom to show the latest added button
            ScrollView scrollView = findViewById(R.id.scrollView);
            scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
        });
    }
    private void fetchStudentSections(String roomId, String studentId) {
        Log.d(TAG, "Fetching student sections for Room ID: " + roomId + ", Student ID: " + studentId);

        FirebaseFirestore.getInstance()
                .collection("rooms")
                .document(roomId)
                .collection("students")
                .document(studentId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String section = document.getString("section");
                        Log.d(TAG, "Student section found: " + section);
                        // You can perform any UI update or logic based on this section data
                    } else {
                        Log.d(TAG, "No section data found for the student in this room.");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching student sections: " + e.getMessage(), e);
                    Toast.makeText(this, "Failed to fetch section data.", Toast.LENGTH_SHORT).show();
                });
    }



}
