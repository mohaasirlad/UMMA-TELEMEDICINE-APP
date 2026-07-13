package com.example.ummatelemedicineapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class ChangePasswordActivity extends BaseActivity {

    private TextInputEditText etCurrent, etNew, etConfirm;
    private MaterialButton btnUpdate;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        Toolbar toolbar = findViewById(R.id.toolbarChangePassword);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        etCurrent = findViewById(R.id.etCurrentPassword);
        etNew = findViewById(R.id.etNewPassword);
        etConfirm = findViewById(R.id.etConfirmNewPassword);
        btnUpdate = findViewById(R.id.btnUpdatePassword);
        prefs = getSharedPreferences("UMMA_PREFS", MODE_PRIVATE);

        btnUpdate.setOnClickListener(v -> {
            String current = etCurrent.getText().toString();
            String newPass = etNew.getText().toString();
            String confirm = etConfirm.getText().toString();

            String savedPass = prefs.getString("user_password", "");

            if (current.isEmpty() || newPass.isEmpty() || confirm.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            } else if (!current.equals(savedPass)) {
                Toast.makeText(this, "Current password is incorrect", Toast.LENGTH_SHORT).show();
            } else if (!newPass.equals(confirm)) {
                Toast.makeText(this, "New passwords do not match", Toast.LENGTH_SHORT).show();
            } else {
                prefs.edit().putString("user_password", newPass).apply();
                Toast.makeText(this, "Password updated successfully", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
}