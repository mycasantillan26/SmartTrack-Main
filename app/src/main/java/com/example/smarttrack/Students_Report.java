package com.example.smarttrack;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class Students_Report extends AppCompatActivity {

    // UI Elements
    private ImageView roomIcon;
    private ImageView homeIcon;
    private ImageView scheduleIcon;
    private String uid;
    private Button dailyAttendanceButton;
    private Button monthlyAttendanceButton;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private TextView navUsername, navIdNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

        // Retrieve the uid from the Intent
        uid = getIntent().getStringExtra("uid");

        // Set up toolbar
        setupToolbar();

        // Initialize views and navigation
        initializeViews();
        setupNavigationDrawer();
        setupClickListeners();

        // Fetch student details for the navigation drawer
        fetchStudentDetails(uid);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        TextView toolbarTitle = findViewById(R.id.toolbarTitle);
        toolbarTitle.setText("Report");
    }

    private void initializeViews() {
        // Initialize navigation icons
        roomIcon = findViewById(R.id.roomIcon);
        homeIcon = findViewById(R.id.homeIcon);
        scheduleIcon = findViewById(R.id.scheduleIcon);

        // Initialize buttons with clear purpose
        dailyAttendanceButton = findViewById(R.id.dailyAttendanceButton);
        monthlyAttendanceButton = findViewById(R.id.monthlyAttendanceButton);

        // Initialize drawer components
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        navUsername = navigationView.findViewById(R.id.navUsername);
        navIdNumber = navigationView.findViewById(R.id.navIdNumber);
    }

    private void setupClickListeners() {
        // Daily Attendance button launches detailed daily view
        dailyAttendanceButton.setOnClickListener(v -> {
            if (uid == null) {
                Log.e("Students_Report", "❌ Error: UID is NULL. Cannot open DailyAttendanceActivity.");
                Toast.makeText(Students_Report.this, "Error: Unable to open attendance. No user ID.", Toast.LENGTH_SHORT).show();
                return;
            }

            Log.d("Students_Report", "✅ Opening DailyAttendanceActivity with UID: " + uid);

            Intent intent = new Intent(Students_Report.this, DailyAttendanceActivity.class);
            intent.putExtra("uid", uid);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        // Monthly Attendance button launches monthly summary view
        monthlyAttendanceButton.setOnClickListener(v -> {
            Intent intent = new Intent(Students_Report.this, MonthlyAttendanceActivity.class);
            intent.putExtra("uid", uid);  // Pass the user ID for attendance lookup
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        // Navigation icons setup
        roomIcon.setOnClickListener(v -> navigateToRoom());
        homeIcon.setOnClickListener(v -> navigateToHome());
        scheduleIcon.setOnClickListener(v -> navigateToCalendar());
    }

    private void setupNavigationDrawer() {
        ImageView menuIcon = findViewById(R.id.menuIcon);
        menuIcon.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        Button logoutButton = findViewById(R.id.logoutButton);
        logoutButton.setOnClickListener(v -> handleLogout());
    }

    private void handleLogout() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(Students_Report.this, Login.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToRoom() {
        Intent intent = new Intent(Students_Report.this, Students_Room.class);
        intent.putExtra("uid", uid);
        startActivity(intent);
        overridePendingTransition(0, 0);
    }

    private void navigateToHome() {
        Intent intent = new Intent(Students_Report.this, Students_Home.class);
        intent.putExtra("uid", uid);
        startActivity(intent);
        overridePendingTransition(0, 0);
    }

    private void navigateToCalendar() {
        Intent intent = new Intent(Students_Report.this, Students_Calendar.class);
        intent.putExtra("uid", uid);
        startActivity(intent);
        overridePendingTransition(0, 0);
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
                    Log.e("Students_Report", "Error fetching student details", e);
                });
    }
}