package com.example.ummatelemedicineapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.FirebaseDatabase;
import android.util.Log;
import androidx.annotation.NonNull;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends BaseActivity {

    private TextInputEditText etFullName, etEmail, etPassword;
    private MaterialButton btnRegister;
    private TextView tvLogin;
    private SignInButton btnGoogleSignUp;
    private FirebaseAuth auth;
    private GoogleSignInClient googleSignInClient;
    private static final int RC_SIGN_IN = 9001;
    private static final String TAG = "RegisterActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        auth = FirebaseAuth.getInstance();

        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmailRegister);
        etPassword = findViewById(R.id.etPasswordRegister);
        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLogin);
        btnGoogleSignUp = findViewById(R.id.btnGoogleSignUp);
        MaterialButton btnDoctorRegister = findViewById(R.id.btnDoctorRegister);

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);

        btnGoogleSignUp.setOnClickListener(v -> signInWithGoogle());

        btnDoctorRegister.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, DoctorRegistrationActivity.class));
        });

        btnRegister.setOnClickListener(v -> {
            String name = etFullName.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            } else {
                // Check if email already exists and if it's a doctor email
                FirebaseDatabase.getInstance().getReference("users").orderByChild("email").equalTo(email)
                        .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                                if (snapshot.exists()) {
                                    Toast.makeText(RegisterActivity.this, "This email is already registered. Please login.", Toast.LENGTH_SHORT).show();
                                } else {
                                    registerPatient(name, email, password);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                                registerPatient(name, email, password);
                            }
                        });
            }
        });

        tvLogin.setOnClickListener(v -> finish());
    }

    private void registerPatient(String name, String email, String password) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String userId = auth.getCurrentUser().getUid();
                        Map<String, Object> user = new HashMap<>();
                        user.put("name", name);
                        user.put("email", email);
                        user.put("role", "patient");

                        FirebaseDatabase.getInstance().getReference("users")
                                .child(userId)
                                .setValue(user)
                                .addOnCompleteListener(dbTask -> {
                                    if (dbTask.isSuccessful()) {
                                        getSharedPreferences("UMMA_PREFS", MODE_PRIVATE).edit()
                                                .putString("last_role", "patient")
                                                .apply();
                                        Toast.makeText(RegisterActivity.this, "Patient account created successfully", Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(RegisterActivity.this, HomeActivity.class));
                                        finish();
                                    } else {
                                        Toast.makeText(RegisterActivity.this, "Database Error: " + dbTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        Toast.makeText(RegisterActivity.this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void signInWithGoogle() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            com.google.android.gms.tasks.Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Log.w(TAG, "Google sign in failed", e);
                Toast.makeText(this, "Google sign in failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String userId = auth.getCurrentUser().getUid();
                        String email = auth.getCurrentUser().getEmail();
                        String name = auth.getCurrentUser().getDisplayName();

                        FirebaseDatabase.getInstance().getReference("users").child(userId)
                                .get().addOnCompleteListener(dbTask -> {
                                    if (dbTask.isSuccessful() && dbTask.getResult().exists()) {
                                        // User already exists, just save role and navigate
                                        String role = dbTask.getResult().child("role").getValue(String.class);
                                        saveAndNavigate(role, name, email);
                                    } else {
                                        // New user, create profile as patient
                                        Map<String, Object> user = new HashMap<>();
                                        user.put("name", name);
                                        user.put("email", email);
                                        user.put("role", "patient");

                                        FirebaseDatabase.getInstance().getReference("users")
                                                .child(userId)
                                                .setValue(user)
                                                .addOnCompleteListener(saveTask -> {
                                                    if (saveTask.isSuccessful()) {
                                                        saveAndNavigate("patient", name, email);
                                                    } else {
                                                        Toast.makeText(RegisterActivity.this, "Failed to create profile", Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                    }
                                });
                    } else {
                        Toast.makeText(RegisterActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveAndNavigate(String role, String name, String email) {
        android.content.SharedPreferences prefs = getSharedPreferences("UMMA_PREFS", MODE_PRIVATE);
        android.content.SharedPreferences.Editor editor = prefs.edit();
        editor.putString("last_role", role);
        editor.putString("user_email", email);
        if (name != null) {
            if ("doctor".equals(role)) {
                editor.putString("doctor_name", name);
                editor.putString("doctor_email", email);
            } else {
                editor.putString("user_name", name);
            }
        }
        editor.apply();

        if ("doctor".equals(role)) {
            startActivity(new Intent(RegisterActivity.this, DoctorHomeActivity.class));
        } else {
            startActivity(new Intent(RegisterActivity.this, HomeActivity.class));
        }
        finish();
    }
}
