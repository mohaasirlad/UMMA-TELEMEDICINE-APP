package com.example.ummatelemedicineapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.util.Random;

public class AdminVerificationActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_verification);

        String docName = getIntent().getStringExtra("doc_name");
        String docEmail = getIntent().getStringExtra("doc_email");

        TextView tvName = findViewById(R.id.tvReviewDocName);
        TextView tvEmail = findViewById(R.id.tvReviewDocEmail);
        MaterialButton btnApprove = findViewById(R.id.btnApprove);
        MaterialButton btnReject = findViewById(R.id.btnReject);

        tvName.setText("Name: " + docName);
        tvEmail.setText("Email: " + docEmail);

        btnApprove.setOnClickListener(v -> {
            Toast.makeText(this, "Application Approved!", Toast.LENGTH_LONG).show();

            Intent intent = new Intent(this, CreatePasswordActivity.class);
            intent.putExtra("doc_name", docName);
            intent.putExtra("doc_email", docEmail);
            startActivity(intent);
            finish();
        });

        btnReject.setOnClickListener(v -> {
            Toast.makeText(this, "Application Rejected", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}