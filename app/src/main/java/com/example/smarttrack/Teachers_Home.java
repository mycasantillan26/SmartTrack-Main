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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class Teachers_Home extends AppCompatActivity {

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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        TextView toolbarTitle = findViewById(R.id.toolbarTitle);
        toolbarTitle.setText("Home");

        dashboardMessage = findViewById(R.id.dashboardMessage);
        locationTextView = findViewById(R.id.locationTextView);


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


        ImageView menuIcon = findViewById(R.id.menuIcon);
        menuIcon.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));


        String uid = getIntent().getStringExtra("uid");
        fetchStudentDetailed(uid);


        Button logoutButton = findViewById(R.id.logoutButton);
        logoutButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(Teachers_Home.this, Login.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });





        roomIcon.setOnClickListener(v -> {
            getSupportActionBar().setTitle("Room");
            Intent intent = new Intent(Teachers_Home.this, Teachers_Room.class);
            intent.putExtra("uid", uid);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        scheduleIcon.setOnClickListener(v -> {
            getSupportActionBar().setTitle("Calendar");
            Intent intent = new Intent(Teachers_Home.this, Teachers_Calendar.class);
            intent.putExtra("uid", uid);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        reportIcon.setOnClickListener(v -> {
            getSupportActionBar().setTitle("Home");
            Intent intent = new Intent(Teachers_Home.this, Teachers_Report.class);
            intent.putExtra("uid", uid);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        fetchStudentDetails(uid);

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        requestLocationPermission();
    }


    private void fetchStudentDetails(String uid) {
        FirebaseFirestore.getInstance().collection("teachers")
                .document(uid)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String firstName = document.getString("firstName");
                        String lastName = document.getString("lastName");
                        dashboardMessage.setText("Hi, Teacher " + firstName + " " + lastName + "!");
                    } else {
                        dashboardMessage.setText("Teacher details not found.");
                    }
                })
                .addOnFailureListener(e -> dashboardMessage.setText("Error fetching teacher details."));
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
}
