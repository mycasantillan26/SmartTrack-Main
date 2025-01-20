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
    private Button scanQRButton, inputCodeButton;
    private TextView noRoomsTextView;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private TextView navUsername, navIdNumber;
    private Button createRoomButton;
    private LinearLayout roomsLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rooms);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        TextView toolbarTitle = findViewById(R.id.toolbarTitle);
        toolbarTitle.setText("Rooms");

        uid = getIntent().getStringExtra("uid");

        noRoomsTextView = findViewById(R.id.noRoomsTextView);
        roomsLayout = findViewById(R.id.roomsLayout);

        Button logoutButton = findViewById(R.id.logoutButton);
        logoutButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(Students_Room.this, Login.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        reportIcon = findViewById(R.id.reportIcon);
        homeIcon = findViewById(R.id.homeIcon);
        scheduleIcon = findViewById(R.id.scheduleIcon);
        scanQRButton = findViewById(R.id.scanQRButtons);
        inputCodeButton = findViewById(R.id.inputCodeButton);

        createRoomButton = findViewById(R.id.createRoomButton);
        createRoomButton.setVisibility(View.GONE);

        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        navUsername = navigationView.findViewById(R.id.navUsername);
        navIdNumber = navigationView.findViewById(R.id.navIdNumber);

        ImageView menuIcon = findViewById(R.id.menuIcon);
        menuIcon.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        fetchStudentDetailed(uid);
        fetchSections();
        setupListeners();

        reportIcon.setOnClickListener(v -> {
            getSupportActionBar().setTitle("Report");
            Intent intent = new Intent(Students_Room.this, Students_Report.class);
            intent.putExtra("uid", uid);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        homeIcon.setOnClickListener(v -> {
            getSupportActionBar().setTitle("Home");
            Intent intent = new Intent(Students_Room.this, Students_Home.class);
            intent.putExtra("uid", uid);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        scheduleIcon.setOnClickListener(v -> {
            getSupportActionBar().setTitle("Calendar");
            Intent intent = new Intent(Students_Room.this, Students_Calendar.class);
            intent.putExtra("uid", uid);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });
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
                        Log.d(TAG, "Student details fetched: " + firstName + " " + lastName);
                    } else {
                        Log.d(TAG, "Student document does not exist.");
                    }
                })
                .addOnFailureListener(e -> {
                    navUsername.setText("Error fetching details");
                    navIdNumber.setText("");
                    Log.e(TAG, "Error fetching student details: " + e.getMessage());
                });
    }

    private void fetchSections() {
        Log.d(TAG, "Fetching sections...");

        FirebaseFirestore.getInstance().collection("sections")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        Log.d(TAG, "Sections fetched successfully. Count: " + queryDocumentSnapshots.size());
                        noRoomsTextView.setVisibility(View.GONE);
                        roomsLayout.removeAllViews();

                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            String sectionId = document.getId(); // Example: "11111-G01"
                            Log.d(TAG, "Section ID found: " + sectionId);

                            // Check if the user is enrolled in this section
                            checkStudentEnrollment(sectionId);
                        }
                    } else {
                        noRoomsTextView.setVisibility(View.VISIBLE);
                        Log.d(TAG, "No sections available.");
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error fetching sections: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error fetching sections: " + e.getMessage());
                });
    }


    private void checkStudentEnrollment(String sectionId) {
        Log.d(TAG, "Checking student enrollment in section: " + sectionId);

        FirebaseFirestore.getInstance()
                .collection("sections")
                .document(sectionId)
                .collection("students")
                .document(uid) // Match the current user ID
                .get()
                .addOnSuccessListener(studentDoc -> {
                    if (studentDoc.exists()) {
                        Log.d(TAG, "User is enrolled in section: " + sectionId);

                        String[] parts = sectionId.split("-");
                        if (parts.length == 2) {
                            String subjectCode = parts[0];
                            String sectionName = parts[1];
                            createSectionButton(subjectCode, sectionName, sectionId);
                        } else {
                            Log.e(TAG, "Invalid section ID format: " + sectionId);
                        }
                    } else {
                        Log.d(TAG, "User not enrolled in section: " + sectionId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking enrollment in section: " + sectionId, e);
                });
    }




    private void createSectionButton(String subjectCode, String sectionName, String sectionId) {
        Log.d(TAG, "Creating button for: " + subjectCode + " - " + sectionName);

        Button sectionButton = new Button(this);
        sectionButton.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                100
        ));
        String buttonLabel = subjectCode + " - " + sectionName;
        sectionButton.setText(buttonLabel);
        sectionButton.setTextSize(18);
        sectionButton.setTextColor(getResources().getColor(R.color.maroon, null));
        sectionButton.setBackgroundResource(R.drawable.button_border);
        sectionButton.setPadding(20, 20, 20, 20);

        sectionButton.setOnClickListener(v -> {
            Toast.makeText(this, "Section clicked: " + buttonLabel, Toast.LENGTH_SHORT).show();
        });

        roomsLayout.addView(sectionButton);
    }




    private void fetchStudents(String sectionPath, LinearLayout studentLayout) {
        Log.d(TAG, "Fetching students for: " + sectionPath);

        FirebaseFirestore.getInstance().collection(sectionPath + "/students")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    studentLayout.removeAllViews();
                    if (!queryDocumentSnapshots.isEmpty()) {
                        Log.d(TAG, "Students fetched for section: " + sectionPath + ". Count: " + queryDocumentSnapshots.size());
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            String studentId = document.getId();
                            Log.d(TAG, "Student ID: " + studentId);

                            TextView studentTextView = new TextView(this);
                            studentTextView.setText("Student ID: " + studentId);
                            studentTextView.setTextSize(16);
                            studentTextView.setPadding(20, 10, 20, 10);

                            studentLayout.addView(studentTextView);
                        }
                    } else {
                        Toast.makeText(this, "No students found in this section.", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "No students found for section: " + sectionPath);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error fetching students: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "Error fetching students for section: " + sectionPath, e);
                });
    }

    private void setupListeners() {
        scanQRButton.setOnClickListener(v -> {
            Intent intent = new Intent(Students_Room.this, ScanQRActivity.class);
            startActivity(intent);
        });

        inputCodeButton.setOnClickListener(v -> {
            Intent intent = new Intent(Students_Room.this, InputCodeActivity.class);
            startActivity(intent);
        });
    }
}
