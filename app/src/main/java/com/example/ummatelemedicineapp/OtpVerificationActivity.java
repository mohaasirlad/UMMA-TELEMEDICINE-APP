package com.example.ummatelemedicineapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class OtpVerificationActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp_verification);

        String name = getIntent().getStringExtra("doc_name");
        String email = getIntent().getStringExtra("doc_email");
        String correctOtp = getIntent().getStringExtra("generated_otp");

        TextView tvDesc = findViewById(R.id.tvOtpDescription);
        TextInputEditText etOtp = findViewById(R.id.etOtpCode);
        MaterialButton btnVerify = findViewById(R.id.btnVerifyOtp);

        tvDesc.setText("Enter the 6-digit code sent to " + email);

        btnVerify.setOnClickListener(v -> {
            String enteredOtp = etOtp.getText().toString();
            if (enteredOtp.equals(correctOtp)) {
                Toast.makeText(this, "OTP Verified Successfully", Toast.LENGTH_SHORT).show();
                
                // Step 5: Create Password
                Intent intent = new Intent(this, CreatePasswordActivity.class);
                intent.putExtra("doc_name", name);
                intent.putExtra("doc_email", email);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "Invalid OTP. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.tvResendOtp).setOnClickListener(v -> {
            Toast.makeText(this, "A new OTP has been sent to your email", Toast.LENGTH_SHORT).show();
        });
    }
}