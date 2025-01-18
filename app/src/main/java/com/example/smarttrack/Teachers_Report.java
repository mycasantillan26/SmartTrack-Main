package com.example.smarttrack;

import android.content.Intent;
import android.os.Bundle;
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


        uid = getIntent().getStringExtra("uid");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        TextView toolbarTitle = findViewById(R.id.toolbarTitle);
        toolbarTitle.setText("Report");


        roomIcon = findViewById(R.id.roomIcon);
        homeIcon = findViewById(R.id.homeIcon);
        scheduleIcon = findViewById(R.id.scheduleIcon);
        dailyAttendanceButton = findViewById(R.id.dailyAttendanceButton);
        monthlyAttendanceButton = findViewById(R.id.monthlyAttendanceButton);

        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        navUsername = navigationView.findViewById(R.id.navUsername);
        navIdNumber = navigationView.findViewById(R.id.navIdNumber);

        roomIcon.setClickable(true);
        homeIcon.setClickable(true);
        scheduleIcon.setClickable(true);
        dailyAttendanceButton.setClickable(true);
        monthlyAttendanceButton.setClickable(true);

        ImageView menuIcon = findViewById(R.id.menuIcon);
        menuIcon.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));


        String uid = getIntent().getStringExtra("uid");
        fetchStudentDetailed(uid);


        Button logoutButton = findViewById(R.id.logoutButton);
        logoutButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(Teachers_Report.this, Login.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });



        roomIcon.setOnClickListener(v -> {
            getSupportActionBar().setTitle("Room");
            Intent intent = new Intent(Teachers_Report.this, Teachers_Room.class);
            intent.putExtra("uid", uid);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });


        homeIcon.setOnClickListener(v -> {
            getSupportActionBar().setTitle("Home");
            Intent intent = new Intent(Teachers_Report.this, Teachers_Home.class);
            intent.putExtra("uid", uid);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });


        scheduleIcon.setOnClickListener(v -> {
            getSupportActionBar().setTitle("Calendar");
            Intent intent = new Intent(Teachers_Report.this, Teachers_Calendar.class);
            intent.putExtra("uid", uid);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });
    }

    private void fetchStudentDetailed(String uid) {
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


