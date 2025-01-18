package com.example.smarttrack;

import android.content.Intent;
import android.os.Bundle;
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

        reportIcon.setClickable(true);
        homeIcon.setClickable(true);
        scheduleIcon.setClickable(true);
        scanQRButton.setClickable(true);
        inputCodeButton.setClickable(true);

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
                    }
                })
                .addOnFailureListener(e -> {
                    navUsername.setText("Error fetching details");
                    navIdNumber.setText("");
                });
    }

    private void fetchSections() {
        FirebaseFirestore.getInstance().collection("sections")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        noRoomsTextView.setVisibility(View.GONE);
                        roomsLayout.removeAllViews();
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            String sectionName = document.getId();
                            createSectionButton(sectionName, document);
                        }
                    } else {
                        noRoomsTextView.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error fetching sections: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }


    private void createSectionButton(String sectionName, QueryDocumentSnapshot document) {
        Button sectionButton = new Button(this);
        sectionButton.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                100
        ));
        sectionButton.setText(sectionName);
        sectionButton.setTextSize(18);
        sectionButton.setTextColor(getResources().getColor(R.color.maroon, null));
        sectionButton.setBackgroundResource(R.drawable.button_border);
        sectionButton.setPadding(20, 20, 20, 20);

        // Add a layout for displaying students
        LinearLayout studentLayout = new LinearLayout(this);
        studentLayout.setOrientation(LinearLayout.VERTICAL);
        studentLayout.setVisibility(View.GONE);

        sectionButton.setOnClickListener(v -> {
            if (studentLayout.getVisibility() == View.GONE) {
                studentLayout.setVisibility(View.VISIBLE);
                fetchStudents(sectionName, studentLayout);
            } else {
                studentLayout.setVisibility(View.GONE);
            }
        });

        roomsLayout.addView(sectionButton);
        roomsLayout.addView(studentLayout);
    }

    private void fetchStudents(String sectionName, LinearLayout studentLayout) {
        FirebaseFirestore.getInstance().collection("sections")
                .document(sectionName)
                .collection("students")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    studentLayout.removeAllViews();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String studentId = document.getId();
                        String roomId = document.getString("roomId");

                        TextView studentTextView = new TextView(this);
                        studentTextView.setText("Student ID: " + studentId + " | Room ID: " + roomId);
                        studentTextView.setTextSize(16);
                        studentTextView.setPadding(20, 10, 20, 10);
                        studentLayout.addView(studentTextView);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error fetching students: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
