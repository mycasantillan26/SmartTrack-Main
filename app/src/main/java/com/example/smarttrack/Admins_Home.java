package com.example.smarttrack;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class Admins_Home extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Initialize and set message
        TextView dashboardMessage = findViewById(R.id.dashboardMessage);
        dashboardMessage.setText("Welcome Admin!");
    }
}