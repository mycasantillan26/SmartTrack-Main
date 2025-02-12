package com.example.smarttrack;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class Teachers_Report extends AppCompatActivity {

    private ImageView roomIcon, homeIcon, scheduleIcon;
    private Button roomsButton, eventsButton;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private TextView navUsername, navIdNumber;
    private View blurBackground, floatingWindow;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_report);

        uid = getIntent().getStringExtra("uid");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        TextView toolbarTitle = findViewById(R.id.toolbarTitle);
        toolbarTitle.setText("Reports");

        roomIcon = findViewById(R.id.roomIcon);
        homeIcon = findViewById(R.id.homeIcon);
        scheduleIcon = findViewById(R.id.scheduleIcon);
        roomsButton = findViewById(R.id.roomsButton);
        eventsButton = findViewById(R.id.eventsButton);
        blurBackground = findViewById(R.id.blurBackground);
        floatingWindow = findViewById(R.id.floatingWindow);

        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        navUsername = navigationView.findViewById(R.id.navUsername);
        navIdNumber = navigationView.findViewById(R.id.navIdNumber);

        ImageView menuIcon = findViewById(R.id.menuIcon);
        menuIcon.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        fetchTeacherDetails(uid);

        Button logoutButton = findViewById(R.id.logoutButton);
        logoutButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(Teachers_Report.this, Login.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        roomsButton.setOnClickListener(v -> showFloatingWindow("Rooms"));

        eventsButton.setOnClickListener(v -> showFloatingWindow("Events"));

        homeIcon.setOnClickListener(v -> {
            Intent intent = new Intent(Teachers_Report.this, Teachers_Home.class);
            intent.putExtra("uid", uid);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        roomIcon.setOnClickListener(v -> {
            Intent intent = new Intent(Teachers_Report.this, Teachers_Room.class);
            intent.putExtra("uid", uid);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        scheduleIcon.setOnClickListener(v -> {
            Intent intent = new Intent(Teachers_Report.this, Teachers_Calendar.class);
            intent.putExtra("uid", uid);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });
    }

    private void showFloatingWindow(String type) {
        blurBackground.setVisibility(View.VISIBLE);
        floatingWindow.setVisibility(View.VISIBLE);

        Button dailyAttendanceButton = floatingWindow.findViewById(R.id.dailyAttendanceButton);
        Button monthlyAttendanceButton = floatingWindow.findViewById(R.id.monthlyAttendanceButton);
        Button closeFloatingWindow = floatingWindow.findViewById(R.id.closeFloatingWindow);

        if (type.equals("Rooms")) {
            dailyAttendanceButton.setText("Daily Attendance");
            monthlyAttendanceButton.setText("Monthly Attendance");
        } else if (type.equals("Events")) {
            dailyAttendanceButton.setText("Daily Events");
            monthlyAttendanceButton.setText("Monthly Events");
        }

        dailyAttendanceButton.setOnClickListener(v -> {
            Intent intent = new Intent(Teachers_Report.this, DailyAttendanceActivity.class);
            intent.putExtra("uid", uid);
            startActivity(intent);
            blurBackground.setVisibility(View.GONE);
            floatingWindow.setVisibility(View.GONE);
        });

        monthlyAttendanceButton.setOnClickListener(v -> {
            Intent intent = new Intent(Teachers_Report.this, MonthlyAttendanceActivity.class);
            intent.putExtra("uid", uid);
            startActivity(intent);
            blurBackground.setVisibility(View.GONE);
            floatingWindow.setVisibility(View.GONE);
        });

        closeFloatingWindow.setOnClickListener(v -> {
            blurBackground.setVisibility(View.GONE);
            floatingWindow.setVisibility(View.GONE);
        });
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
}