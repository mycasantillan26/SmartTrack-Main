package com.example.smarttrack;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class Teachers_Room extends AppCompatActivity {

    private String uid;
    private LinearLayout roomsLayout;
    private ImageView reportIcon;
    private ImageView homeIcon;
    private ImageView scheduleIcon;
    private TextView navUsername, navIdNumber;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private LinearLayout floatingWindow;
    private Button createRoomButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rooms);

        uid = getIntent().getStringExtra("uid");

        roomsLayout = findViewById(R.id.roomsLayout);
        floatingWindow = findViewById(R.id.floatingWindow);
        reportIcon = findViewById(R.id.reportIcon);
        homeIcon = findViewById(R.id.homeIcon);
        scheduleIcon = findViewById(R.id.scheduleIcon);
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        navUsername = navigationView.findViewById(R.id.navUsername);
        navIdNumber = navigationView.findViewById(R.id.navIdNumber);
        createRoomButton = findViewById(R.id.createRoomButton);
        Button inputCodeButton = findViewById(R.id.inputCodeButton);
        TextView ORtextView = findViewById(R.id.ORtextView);

        inputCodeButton.setVisibility(View.GONE);
        ORtextView.setVisibility(View.GONE);


        ImageView menuIcon = findViewById(R.id.menuIcon);
        menuIcon.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        fetchStudentDetails(uid);

        // Fetch rooms created by the teacher
        fetchRoomsByTeacher(uid);

        // Set up actions for buttons (report, home, etc.)
        setupButtons();

        // Set OnClickListener for createRoomButton to redirect to Teachers_CreateRoom activity
        createRoomButton.setOnClickListener(v -> {
            Intent intent = new Intent(Teachers_Room.this, Teachers_CreateRoom.class);
            intent.putExtra("uid", uid);  // Pass the UID to Teachers_CreateRoom
            startActivity(intent);
        });
    }

    // Method to fetch rooms created by the teacher
    private void fetchRoomsByTeacher(String uid) {
        FirebaseFirestore.getInstance().collection("rooms")
                .whereEqualTo("teacherId", uid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Map<String, String> roomDetails = new HashMap<>();
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            String subjectCode = document.getString("subjectCode");
                            String section = document.getString("section");
                            String roomCode = document.getString("roomCode");

                            String displayText = subjectCode + " - " + section;
                            roomDetails.put(displayText, roomCode);
                        }
                        displayRooms(roomDetails);
                    } else {
                        Toast.makeText(Teachers_Room.this, "No rooms found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(Teachers_Room.this, "Error fetching rooms: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }





    // Method to display rooms as buttons
    private void displayRooms(Map<String, String> roomDetails) {
        roomsLayout.removeAllViews();  // Clear any existing views in the layout

        if (!roomDetails.isEmpty()) {
            roomsLayout.setVisibility(View.VISIBLE);
            TextView noRoomsTextView = findViewById(R.id.noRoomsTextView);
            noRoomsTextView.setVisibility(View.GONE);

            for (Map.Entry<String, String> entry : roomDetails.entrySet()) {
                String displayText = entry.getKey();
                String roomCode = entry.getValue();
                // Assuming the display text includes both subjectCode and section in the format "subjectCode - section"
                String[] parts = displayText.split(" - ");
                String subjectCode = parts[0];
                String section = parts.length > 1 ? parts[1] : "";  // Check if there is a section available

                Button roomButton = new Button(this);
                roomButton.setText(displayText);
                roomButton.setTextSize(25);
                roomButton.setPadding(20, 20, 20, 20);
                roomButton.setTextColor(getResources().getColor(android.R.color.black));
                roomButton.setBackground(getResources().getDrawable(R.drawable.button_border));

                float density = getResources().getDisplayMetrics().density;
                int marginBottom = (int) (10 * density);

                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                layoutParams.setMargins(0, 0, 0, marginBottom);
                roomButton.setLayoutParams(layoutParams);

                roomButton.setOnClickListener(v -> {
                    // Show the floating window with the room code, subject code, and section
                    showFloatingWindow(roomCode, subjectCode, section);
                });

                roomsLayout.addView(roomButton);
            }
        } else {
            TextView noRoomsTextView = findViewById(R.id.noRoomsTextView);
            noRoomsTextView.setVisibility(View.VISIBLE);  // Show "No Rooms Available" message if list is empty
        }
    }



    // Method to show the floating window with buttons
    private void showFloatingWindow(String roomCode, String subjectCode, String section) {
        View blurBackground = findViewById(R.id.blurBackground);
        floatingWindow.setVisibility(View.VISIBLE);  // Show the floating window
        blurBackground.setVisibility(View.VISIBLE);  // Show the blur background
        roomsLayout.setVisibility(View.GONE);

        // Set room-related text
        Button generateCodeButton = findViewById(R.id.generateCodeButton);
        Button viewStudentsButton = findViewById(R.id.viewStudentsButton);
        generateCodeButton.setText("Generate Code");
        viewStudentsButton.setText("View Students");



        // Set onClickListeners for the buttons inside the floating window
        generateCodeButton.setOnClickListener(v -> {
            Intent intent = new Intent(Teachers_Room.this, GenerateCode.class);
            intent.putExtra("roomCode", roomCode); // Pass the roomCode to GenerateCode
            intent.putExtra("subjectSection", subjectCode + " - " + section); // Include subject and section
            startActivity(intent);
        });

        viewStudentsButton.setOnClickListener(v -> {
            if (roomCode == null || roomCode.isEmpty()) {
                Toast.makeText(this, "Room Code is missing. Cannot view students.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Fetch the roomId based on the roomCode before transitioning
            FirebaseFirestore.getInstance().collection("rooms")
                    .whereEqualTo("roomCode", roomCode)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            String roomId = queryDocumentSnapshots.getDocuments().get(0).getId();
                            Log.d("showFloatingWindow", "Room ID resolved: " + roomId);

                            Intent intent = new Intent(Teachers_Room.this, ViewStudents.class);
                            intent.putExtra("roomId", roomId); // Pass the resolved roomId to ViewStudents
                            intent.putExtra("section", section); // Pass the section to ViewStudents
                            intent.putExtra("subjectCode", subjectCode); // Pass the subjectCode to ViewStudents
                            startActivity(intent);
                        } else {
                            Toast.makeText(this, "No room found for the provided code.", Toast.LENGTH_SHORT).show();
                            Log.e("showFloatingWindow", "No room found for roomCode: " + roomCode);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error fetching room ID: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e("showFloatingWindow", "Error fetching room ID for roomCode: " + roomCode, e);
                    });
        });

        // Close button for the floating window
        ImageView closeFloatingWindow = findViewById(R.id.closeFloatingWindow);
        closeFloatingWindow.setOnClickListener(v -> {
            floatingWindow.setVisibility(View.GONE);  // Hide the floating window
            blurBackground.setVisibility(View.GONE);  // Hide the blur background
            roomsLayout.setVisibility(View.VISIBLE);  // Show the rooms layout again
        });
    }



    // Fetch teacher details (name, idNumber, etc.)
    private void fetchStudentDetails(String uid) {
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

    private void setupButtons() {
        reportIcon.setOnClickListener(v -> {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Report");
            }
            Intent intent = new Intent(Teachers_Room.this, Teachers_Report.class);
            intent.putExtra("uid", uid);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        homeIcon.setOnClickListener(v -> {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Home");
            }
            Intent intent = new Intent(Teachers_Room.this, Teachers_Home.class);
            intent.putExtra("uid", uid);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        scheduleIcon.setOnClickListener(v -> {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Calendar");
            }
            Intent intent = new Intent(Teachers_Room.this, Teachers_Calendar.class);
            intent.putExtra("uid", uid);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });
    }
}
