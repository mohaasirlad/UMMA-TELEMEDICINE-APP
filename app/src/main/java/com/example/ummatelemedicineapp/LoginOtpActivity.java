package com.example.ummatelemedicineapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class LoginOtpActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp_verification); // Reuse the same layout

        String correctOtp = getIntent().getStringExtra("otp");
        
        TextView tvTitle = findViewById(R.id.tvOtpDescription);
        tvTitle.setText("Enter the 2-step verification code to access your doctor dashboard.");
        
        TextInputEditText etOtp = findViewById(R.id.etOtpCode);
        MaterialButton btnVerify = findViewById(R.id.btnVerifyOtp);

        btnVerify.setOnClickListener(v -> {
            if (etOtp.getText().toString().equals(correctOtp)) {
                Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, DoctorHomeActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Invalid Security Code", Toast.LENGTH_SHORT).show();
            }
        });
    }
}