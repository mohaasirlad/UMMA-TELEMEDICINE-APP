package com.example.ummatelemedicineapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class TelehealthLobbyActivity extends BaseActivity {

    private String appointmentId;
    private String name;
    private String role;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_telehealth_lobby);

        appointmentId = getIntent().getStringExtra("appointment_id");
        name = getIntent().getStringExtra("display_name");
        role = getIntent().getStringExtra("display_role");

        TextView tvName = findViewById(R.id.tvLobbyName);
        TextView tvRole = findViewById(R.id.tvLobbyRole);
        ImageView ivAvatar = findViewById(R.id.ivLobbyAvatar);
        MaterialButton btnJoin = findViewById(R.id.btnJoinCall);
        MaterialButton btnCancel = findViewById(R.id.btnCancelCall);
        MaterialButton btnBack = findViewById(R.id.btnBack);

        if (name != null) tvName.setText(name);
        if (role != null) tvRole.setText(role);

        btnJoin.setOnClickListener(v -> {
            Intent intent = new Intent(this, VideoConsultationActivity.class);
            intent.putExtra("appointment_id", appointmentId);
            intent.putExtra("user_identity", (role != null && role.contains("Patient")) ? "Patient_" + System.currentTimeMillis() : "Doctor_" + System.currentTimeMillis());
            startActivity(intent);
            finish();
        });

        btnCancel.setOnClickListener(v -> finish());
        btnBack.setOnClickListener(v -> finish());
    }
}
