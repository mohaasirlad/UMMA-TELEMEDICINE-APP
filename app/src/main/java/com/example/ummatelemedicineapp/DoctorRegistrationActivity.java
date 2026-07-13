package com.example.ummatelemedicineapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;
import java.util.Map;

public class DoctorRegistrationActivity extends BaseActivity {

    private TextInputEditText etName, etEmail, etPassword, etSpecialty;
    private MaterialButton btnSubmit;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_registration);

        auth = FirebaseAuth.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        etName = findViewById(R.id.etDocName);
        etEmail = findViewById(R.id.etDocEmail);
        etSpecialty = findViewById(R.id.etDocSpecialty);
        etPassword = findViewById(R.id.etDocPassword);
        btnSubmit = findViewById(R.id.btnSubmitRegistration);

        btnSubmit.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String specialty = etSpecialty.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || specialty.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            } else {
                FirebaseDatabase.getInstance().getReference("users").orderByChild("email").equalTo(email)
                        .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                            @Override
                            public void onDataChange(@androidx.annotation.NonNull com.google.firebase.database.DataSnapshot snapshot) {
                                if (snapshot.exists()) {
                                    Toast.makeText(DoctorRegistrationActivity.this, "This email is already registered. Please login.", Toast.LENGTH_SHORT).show();
                                } else {
                                    registerDoctor(name, email, specialty, password);
                                }
                            }

                            @Override
                            public void onCancelled(@androidx.annotation.NonNull com.google.firebase.database.DatabaseError error) {
                                registerDoctor(name, email, specialty, password);
                            }
                        });
            }
        });
    }

    private void registerDoctor(String name, String email, String specialty, String password) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String userId = auth.getCurrentUser().getUid();
                        Map<String, Object> doctor = new HashMap<>();
                        doctor.put("name", name);
                        doctor.put("email", email);
                        doctor.put("specialty", specialty);
                        doctor.put("role", "doctor");
                        doctor.put("availability", "Available");
                        doctor.put("rating", 5.0);

                        FirebaseDatabase.getInstance().getReference("users")
                                .child(userId)
                                .setValue(doctor)
                                .addOnCompleteListener(dbTask -> {
                                    if (dbTask.isSuccessful()) {
                                        getSharedPreferences("UMMA_PREFS", MODE_PRIVATE).edit()
                                                .putString("last_role", "doctor")
                                                .apply();
                                        Toast.makeText(DoctorRegistrationActivity.this, "Doctor Account Created Successfully!", Toast.LENGTH_LONG).show();
                                        Intent intent = new Intent(this, DoctorHomeActivity.class);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                        startActivity(intent);
                                        finish();
                                    } else {
                                        Toast.makeText(DoctorRegistrationActivity.this, "Database Error: " + dbTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        Toast.makeText(DoctorRegistrationActivity.this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}

