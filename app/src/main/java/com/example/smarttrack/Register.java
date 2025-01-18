package com.example.smarttrack;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class Register extends AppCompatActivity {

    private EditText emailField, firstNameField, middleNameField, lastNameField, idNumberField, passwordField, confirmPasswordField;
    private EditText courseYearField, dobField, homeAddressField, cityAddressField, contactNumberField;
    private Spinner userTypeSpinner, genderSpinner;
    private Button signUpButton;
    private TextView welcomeText, subtitleText;
    private View logoImageView;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private TextView alreadyHaveAccount;

    private boolean isNextClicked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseApp.initializeApp(this);
        setContentView(R.layout.activity_register);

        // Initialize Firebase services
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Initialize views
        initializeViews();

        alreadyHaveAccount.setOnClickListener(v -> navigateToLogin());

        // Set initial visibility for student fields
        toggleStudentFields(View.GONE);

        // Set up DatePicker for DOB
        dobField.setOnClickListener(v -> showDatePicker());

        // Automatically add "+63" to contact number field
        contactNumberField.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && TextUtils.isEmpty(contactNumberField.getText().toString())) {
                contactNumberField.setText("+63");
                contactNumberField.setSelection(contactNumberField.getText().length());
            }
        });

        // Handle user type selection
        userTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedUserType = userTypeSpinner.getSelectedItem().toString();
                if (selectedUserType.equals("Student")) {
                    signUpButton.setText("NEXT");
                } else {
                    toggleStudentFields(View.GONE);
                    signUpButton.setText("SIGN UP");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Handle sign-up button click
        signUpButton.setOnClickListener(v -> {
            String selectedUserType = userTypeSpinner.getSelectedItem().toString();

            if (!isNextClicked && selectedUserType.equals("Student")) {
                if (validateInitialInputs()) {
                    toggleInitialFields(View.GONE);
                    toggleStudentFields(View.VISIBLE);
                    welcomeText.setText("Student Information");
                    subtitleText.setVisibility(View.GONE);
                    logoImageView.setVisibility(View.GONE);
                    isNextClicked = true;
                }
            } else if (selectedUserType.equals("Student") && isNextClicked) {
                if (validateStudentInputs()) {
                    registerUser();
                }
            } else if (selectedUserType.equals("Teacher")) {
                if (validateInitialInputs()) {
                    registerTeacher();
                }
            }
        });
    }

    private void initializeViews() {
        emailField = findViewById(R.id.emailField);
        firstNameField = findViewById(R.id.firstNameField);
        middleNameField = findViewById(R.id.middleNameField);
        lastNameField = findViewById(R.id.lastNameField);
        idNumberField = findViewById(R.id.idNumberField);
        passwordField = findViewById(R.id.passwordField);
        confirmPasswordField = findViewById(R.id.confirmPasswordField);
        userTypeSpinner = findViewById(R.id.userTypeSpinner);
        courseYearField = findViewById(R.id.courseYearField);
        dobField = findViewById(R.id.dobPicker);
        homeAddressField = findViewById(R.id.homeAddressField);
        cityAddressField = findViewById(R.id.cityAddressField);
        contactNumberField = findViewById(R.id.contactNumberField);
        genderSpinner = findViewById(R.id.genderSpinner);
        signUpButton = findViewById(R.id.signUpButton);
        welcomeText = findViewById(R.id.welcomeText);
        subtitleText = findViewById(R.id.subtitleText);
        logoImageView = findViewById(R.id.logoImageView);
        alreadyHaveAccount = findViewById(R.id.alreadyHaveAccount);
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year1, month1, dayOfMonth) -> {
            String date = dayOfMonth + "/" + (month1 + 1) + "/" + year1;
            dobField.setText(date);
        }, year, month, day);

        datePickerDialog.show();
    }

    private void toggleInitialFields(int visibility) {
        emailField.setVisibility(visibility);
        firstNameField.setVisibility(visibility);
        middleNameField.setVisibility(visibility);
        lastNameField.setVisibility(visibility);
        idNumberField.setVisibility(visibility);
        passwordField.setVisibility(visibility);
        confirmPasswordField.setVisibility(visibility);
        userTypeSpinner.setVisibility(visibility);
    }

    private void toggleStudentFields(int visibility) {
        courseYearField.setVisibility(visibility);
        genderSpinner.setVisibility(visibility);
        dobField.setVisibility(visibility);
        homeAddressField.setVisibility(visibility);
        cityAddressField.setVisibility(visibility);
        contactNumberField.setVisibility(visibility);
    }

    private boolean validateInitialInputs() {
        String email = emailField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();
        String confirmPassword = confirmPasswordField.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
            Toast.makeText(this, "All fields are required!", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!email.endsWith("@cit.edu")) {
            Toast.makeText(this, "Email must be @cit.edu domain!", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match!", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private boolean validateStudentInputs() {
        if (TextUtils.isEmpty(courseYearField.getText().toString()) ||
                TextUtils.isEmpty(homeAddressField.getText().toString()) ||
                TextUtils.isEmpty(cityAddressField.getText().toString()) ||
                TextUtils.isEmpty(contactNumberField.getText().toString())) {
            Toast.makeText(this, "All student fields are required!", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void registerTeacher() {
        String email = emailField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // User registration successful, now send email verification
                        sendEmailVerification(() -> saveTeacherToFirestore());
                    } else {
                        Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private void saveTeacherToFirestore() {
        String uid = firebaseAuth.getCurrentUser().getUid();  // Get the Firebase Auth UID

        Map<String, Object> teacherData = new HashMap<>();
        teacherData.put("email", emailField.getText().toString());
        teacherData.put("firstName", firstNameField.getText().toString());
        teacherData.put("middleName", middleNameField.getText().toString());
        teacherData.put("lastName", lastNameField.getText().toString());
        teacherData.put("idNumber", idNumberField.getText().toString());
        teacherData.put("userType", "Teacher");
        teacherData.put("isConfirmedByAdmin", false); // Admin approval required

        // Save teacher data with UID as document ID
        firestore.collection("teachers")
                .document(uid)  // Use the Firebase UID as the document ID
                .set(teacherData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Confirmation email sent. Please verify your email and wait for admin approval to log in.", Toast.LENGTH_SHORT).show();
                    navigateToLogin();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void registerUser() {
        String email = emailField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // User registration successful, now send email verification
                        sendEmailVerification(() -> saveStudentToFirestore());
                    } else {
                        Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveStudentToFirestore() {
        String uid = firebaseAuth.getCurrentUser().getUid();  // Get the Firebase Auth UID

        Map<String, Object> studentData = new HashMap<>();
        studentData.put("email", emailField.getText().toString());
        studentData.put("firstName", firstNameField.getText().toString());
        studentData.put("middleName", middleNameField.getText().toString());
        studentData.put("lastName", lastNameField.getText().toString());
        studentData.put("idNumber", idNumberField.getText().toString());
        studentData.put("userType", "Student");
        studentData.put("courseYear", courseYearField.getText().toString());
        studentData.put("gender", genderSpinner.getSelectedItem().toString());
        studentData.put("dateOfBirth", dobField.getText().toString().trim());
        studentData.put("homeAddress", homeAddressField.getText().toString());
        studentData.put("cityAddress", cityAddressField.getText().toString());
        studentData.put("contactNumber", contactNumberField.getText().toString());

        // Save student data with UID as document ID
        firestore.collection("students")
                .document(uid)  // Use the Firebase UID as the document ID
                .set(studentData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Confirmation email sent. Please verify your email to log in.", Toast.LENGTH_SHORT).show();
                    navigateToFaceRecognition(uid); // Pass the UID for further use
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
    private void sendEmailVerification(Runnable onSuccess) {
        firebaseAuth.getCurrentUser().sendEmailVerification()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        onSuccess.run();
                    } else {
                        Toast.makeText(this, "Failed to send verification email.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void navigateToFaceRecognition(String documentId) {
        Intent intent = new Intent(this, FaceRecognition.class);
        intent.putExtra("documentId", documentId); // Pass the document ID
        startActivity(intent);
        finish();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, Login.class);
        startActivity(intent);
        finish();
    }
}
