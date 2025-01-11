package com.example.smarttrack;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class Login extends AppCompatActivity {

    private EditText idNumberField;
    private EditText passwordField;
    private Button signInButton;
    private TextView forgotPassword;
    private TextView registerLink;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        setContentView(R.layout.activity_login);

        // Initialize views
        idNumberField = findViewById(R.id.idNumberField);
        passwordField = findViewById(R.id.passwordField);
        signInButton = findViewById(R.id.signInButton);
        forgotPassword = findViewById(R.id.forgotPassword);
        registerLink = findViewById(R.id.registerLink);

        // Set click listener for the sign-in button
        signInButton.setOnClickListener(v -> {
            String idNumber = idNumberField.getText().toString().trim();
            String password = passwordField.getText().toString().trim();

            if (validateInputs(idNumber, password)) {
                signInUser(idNumber, password);
            } else {
                Toast.makeText(Login.this, "Please enter both ID Number and password.", Toast.LENGTH_SHORT).show();
            }
        });

        // Set click listener for the forgot password link
        forgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(Login.this, ForgotPassword.class);
            startActivity(intent);
        });

        // Set click listener for the register link
        registerLink.setOnClickListener(v -> {
            Intent intent = new Intent(Login.this, Register.class);
            startActivity(intent);
        });
    }

    private boolean validateInputs(String idNumber, String password) {
        return !idNumber.isEmpty() && !password.isEmpty();
    }

    private void signInUser(String idNumber, String password) {
        firestore.collection("students")
                .whereEqualTo("idNumber", idNumber)
                .get()
                .addOnCompleteListener(studentTask -> {
                    if (studentTask.isSuccessful() && !studentTask.getResult().isEmpty()) {
                        DocumentSnapshot studentDoc = studentTask.getResult().getDocuments().get(0);
                        verifyLogin(studentDoc, "students", password);
                    } else {
                        firestore.collection("teachers")
                                .whereEqualTo("idNumber", idNumber)
                                .get()
                                .addOnCompleteListener(teacherTask -> {
                                    if (teacherTask.isSuccessful() && !teacherTask.getResult().isEmpty()) {
                                        DocumentSnapshot teacherDoc = teacherTask.getResult().getDocuments().get(0);
                                        verifyLogin(teacherDoc, "teachers", password);
                                    } else {
                                        Toast.makeText(Login.this, "Invalid ID Number or password.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                });
    }

    private void verifyLogin(DocumentSnapshot document, String collection, String password) {
        String email = document.getString("email");
        boolean isConfirmedByAdmin = collection.equals("teachers") && document.getBoolean("isConfirmedByAdmin");

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Reload user data to ensure the email verification status is updated
                        firebaseAuth.getCurrentUser().reload().addOnCompleteListener(reloadTask -> {
                            if (reloadTask.isSuccessful()) {
                                boolean emailVerified = firebaseAuth.getCurrentUser().isEmailVerified();

                                if (!emailVerified) {
                                    Toast.makeText(this, "Please verify your email before logging in.", Toast.LENGTH_SHORT).show();
                                    firebaseAuth.signOut(); // Log out the user
                                    return;
                                }

                                if (collection.equals("teachers") && !isConfirmedByAdmin) {
                                    Toast.makeText(this, "Your account has not been approved by an admin yet.", Toast.LENGTH_SHORT).show();
                                    firebaseAuth.signOut(); // Log out the user
                                    return;
                                }

                                Toast.makeText(this, "Login successful! Welcome " + (collection.equals("students") ? "Student" : "Teacher") + ".", Toast.LENGTH_SHORT).show();
                                navigateToDashboard(document); // Pass the document to navigateToDashboard
                            } else {
                                Toast.makeText(this, "Error verifying email status. Please try again.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        Toast.makeText(this, "Invalid email or password.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void navigateToDashboard(DocumentSnapshot document) {
        String userType = getUserType(document);  // Get the userType from the document
        Log.d("Login", "User Type: " + userType);  // Debug log

        String uid = document.getId();  // Fetch the UID from Firestore document ID

        Intent intent;
        if (userType.equals("admin")) {
            intent = new Intent(this, Admins_Home.class);
        } else if (userType.equals("Teacher")) {
            intent = new Intent(this, Teachers_Home.class);
        } else {
            intent = new Intent(this, Students_Home.class);
        }

        // Pass the UID to the next activity
        intent.putExtra("uid", uid);
        startActivity(intent);
        finish();
    }

    private String getUserType(DocumentSnapshot document) {
        // Check if 'userType' exists in the document
        String userType = document.getString("userType");

        // If 'userType' is not set, determine user type based on collection name
        if (userType == null || userType.isEmpty()) {
            if (document.getReference().getPath().contains("teachers")) {
                userType = "Teacher";
            } else if (document.getReference().getPath().contains("students")) {
                userType = "Student";
            }
        }

        return userType != null ? userType : "Student"; // Default to "Student" if userType is still null
    }
}
