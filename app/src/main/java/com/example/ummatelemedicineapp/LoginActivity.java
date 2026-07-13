package com.example.ummatelemedicineapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ummatelemedicineapp.utils.LocaleHelper;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.ActionCodeSettings;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

import androidx.annotation.NonNull;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends BaseActivity {

    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin;
    private TextView tvRegister, tvSwitchRole, tvWelcome, tvForgotPassword;
    private SignInButton btnGoogleSignIn;
    private boolean isDoctorLogin = false;
    private FirebaseAuth auth;
    private GoogleSignInClient googleSignInClient;
    private static final int RC_SIGN_IN = 9001;
    private static final String TAG = "LoginActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);
        tvSwitchRole = findViewById(R.id.tvSwitchRole);
        tvWelcome = findViewById(R.id.tvWelcome);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);

        auth = FirebaseAuth.getInstance();

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);

        btnGoogleSignIn.setOnClickListener(v -> signInWithGoogle());

        // Handle incoming sign-in link
        handleSignInLink(getIntent());

        tvForgotPassword.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your email first", Toast.LENGTH_SHORT).show();
                return;
            }
            sendSignInLink(email);
        });

        tvSwitchRole.setOnClickListener(v -> {
            isDoctorLogin = !isDoctorLogin;
            if (isDoctorLogin) {
                tvWelcome.setText("Doctor Portal");
                tvSwitchRole.setText("Login as Patient");
                btnLogin.setText("Doctor Login");
            } else {
                tvWelcome.setText("Welcome Back");
                tvSwitchRole.setText("Login as Doctor");
                btnLogin.setText("Login");
            }
        });

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            } else {
                auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this, task -> {
                            if (task.isSuccessful()) {
                                String userId = auth.getCurrentUser().getUid();
                                FirebaseDatabase.getInstance().getReference("users").child(userId)
                                        .addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                String role = snapshot.child("role").getValue(String.class);
                                                String name = snapshot.child("name").getValue(String.class);
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

                                                if (isDoctorLogin) {
                                                    if ("doctor".equals(role)) {
                                                        Toast.makeText(LoginActivity.this, "Doctor Login Successful", Toast.LENGTH_SHORT).show();
                                                        startActivity(new Intent(LoginActivity.this, DoctorHomeActivity.class));
                                                        finish();
                                                    } else {
                                                        Toast.makeText(LoginActivity.this, "This account is not a Doctor account. Please use the Patient login.", Toast.LENGTH_SHORT).show();
                                                        auth.signOut();
                                                    }
                                                } else {
                                                    if ("patient".equals(role)) {
                                                        Toast.makeText(LoginActivity.this, "Patient Login Successful", Toast.LENGTH_SHORT).show();
                                                        startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                                                        finish();
                                                    } else {
                                                        Toast.makeText(LoginActivity.this, "This account is a Doctor account. Please use the Doctor Portal.", Toast.LENGTH_SHORT).show();
                                                        auth.signOut();
                                                    }
                                                }

                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError error) {
                                                Toast.makeText(LoginActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            } else {
                                Toast.makeText(LoginActivity.this, "Login failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });

        tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
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
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
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
                        
                        FirebaseDatabase.getInstance().getReference("users").child(userId)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        if (snapshot.exists()) {
                                            String role = snapshot.child("role").getValue(String.class);
                                            String name = snapshot.child("name").getValue(String.class);
                                            saveAndNavigate(role, name, email);
                                        } else {
                                            // New user from Google, default to patient or redirect to role selection
                                            // For simplicity, let's create a patient profile
                                            String name = auth.getCurrentUser().getDisplayName();
                                            java.util.HashMap<String, Object> userMap = new java.util.HashMap<>();
                                            userMap.put("name", name);
                                            userMap.put("email", email);
                                            userMap.put("role", "patient");
                                            
                                            FirebaseDatabase.getInstance().getReference("users").child(userId)
                                                    .setValue(userMap)
                                                    .addOnCompleteListener(dbTask -> {
                                                        if (dbTask.isSuccessful()) {
                                                            saveAndNavigate("patient", name, email);
                                                        } else {
                                                            Toast.makeText(LoginActivity.this, "Failed to create profile", Toast.LENGTH_SHORT).show();
                                                        }
                                                    });
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        Toast.makeText(LoginActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        Toast.makeText(LoginActivity.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
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
            startActivity(new Intent(LoginActivity.this, DoctorHomeActivity.class));
        } else {
            startActivity(new Intent(LoginActivity.this, HomeActivity.class));
        }
        finish();
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleSignInLink(intent);
    }

    private void handleSignInLink(Intent intent) {
        if (intent == null || intent.getData() == null) return;

        String emailLink = intent.getData().toString();
        if (auth.isSignInWithEmailLink(emailLink)) {
            android.content.SharedPreferences prefs = getSharedPreferences("UMMA_PREFS", MODE_PRIVATE);
            String email = prefs.getString("pending_email", "");

            if (email.isEmpty()) {
                // If email is missing, we might need to ask the user to re-enter it
                // For now, let's try to get it from the UI if present
                email = etEmail.getText().toString().trim();
            }

            if (email.isEmpty()) {
                Toast.makeText(this, "Please verify your email to complete sign-in", Toast.LENGTH_SHORT).show();
                return;
            }

            AuthCredential credential = EmailAuthProvider.getCredentialWithLink(email, emailLink);

            if (auth.getCurrentUser() != null) {
                // Re-authenticate the user with this credential.
                auth.getCurrentUser().reauthenticateAndRetrieveData(credential)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "Successfully reauthenticated with email link!");
                                navigateToNextScreen(prefs);
                            } else {
                                Log.e(TAG, "Error reauthenticating", task.getException());
                                Toast.makeText(this, "Re-authentication failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            } else {
                auth.signInWithEmailLink(email, emailLink)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "Successfully signed in with email link!");
                                navigateToNextScreen(prefs);
                            } else {
                                Log.e(TAG, "Error signing in with email link", task.getException());
                                Toast.makeText(this, "Sign-in failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        }
    }

    private void navigateToNextScreen(android.content.SharedPreferences prefs) {
        String role = prefs.getString("last_role", "patient");
        if ("doctor".equals(role)) {
            startActivity(new Intent(this, DoctorHomeActivity.class));
        } else {
            startActivity(new Intent(this, HomeActivity.class));
        }
        finish();
    }

    private void sendSignInLink(String email) {
        // Save email locally to complete sign-in on return
        getSharedPreferences("UMMA_PREFS", MODE_PRIVATE)
                .edit()
                .putString("pending_email", email)
                .apply();

        ActionCodeSettings actionCodeSettings =
                ActionCodeSettings.newBuilder()
                        .setUrl("https://ummatelemedicine.page.link/login")
                        .setHandleCodeInApp(true)
                        .setAndroidPackageName(
                                "com.example.ummatelemedicineapp",
                                true, /* installIfNotAvailable */
                                "1"    /* minimumVersion */)
                        .build();

        auth.sendSignInLinkToEmail(email, actionCodeSettings)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Email sent.");
                            Toast.makeText(LoginActivity.this, "Sign-in link sent to " + email, Toast.LENGTH_LONG).show();
                        } else {
                            Log.e(TAG, "Error sending email", task.getException());
                            Toast.makeText(LoginActivity.this, "Failed to send link: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}