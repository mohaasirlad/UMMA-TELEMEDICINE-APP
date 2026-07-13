package com.example.ummatelemedicineapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class CreatePasswordActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_password);

        TextInputEditText etPassword = findViewById(R.id.etNewPassword);
        TextInputEditText etConfirm = findViewById(R.id.etConfirmPassword);
        MaterialButton btnActivate = findViewById(R.id.btnFinalizeAccount);

        btnActivate.setOnClickListener(v -> {
            String pass = etPassword.getText().toString();
            String confirm = etConfirm.getText().toString();

            if (pass.isEmpty() || confirm.isEmpty()) {
                Toast.makeText(this, "Fields cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            if (pass.equals(confirm)) {
                // Step 5: Save Doctor Credentials and Activate Account
                String docEmail = getIntent().getStringExtra("doc_email");
                String docName = getIntent().getStringExtra("doc_name"); // Retrieve name passed through flow
                
                getSharedPreferences("UMMA_PREFS", MODE_PRIVATE)
                        .edit()
                        .putString("doctor_email", docEmail)
                        .putString("doctor_name", docName)
                        .putString("doctor_password", pass)
                        .putBoolean("is_doctor_registered", true)
                        .putString("last_role", "doctor") // Lock role to doctor
                        .apply();

                Toast.makeText(this, "Account Activated! Please log in to your dashboard.", Toast.LENGTH_LONG).show();
                
                Intent intent = new Intent(this, DoctorHomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            }
        });
    }
}