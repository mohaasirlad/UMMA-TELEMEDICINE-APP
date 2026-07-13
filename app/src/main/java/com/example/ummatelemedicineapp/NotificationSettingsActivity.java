package com.example.ummatelemedicineapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;

public class NotificationSettingsActivity extends BaseActivity {

    private MaterialSwitch switchAppt, switchQueue, switchHealth, switchEmail, switchSms;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_settings);

        Toolbar toolbar = findViewById(R.id.toolbarNotifications);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        prefs = getSharedPreferences("UMMA_PREFS", MODE_PRIVATE);

        switchAppt = findViewById(R.id.switchApptReminders);
        switchQueue = findViewById(R.id.switchQueueUpdates);
        switchHealth = findViewById(R.id.switchHealthTips);
        switchEmail = findViewById(R.id.switchEmail);
        switchSms = findViewById(R.id.switchSms);

        // Load saved states
        switchAppt.setChecked(prefs.getBoolean("notif_appt", true));
        switchQueue.setChecked(prefs.getBoolean("notif_queue", true));
        switchHealth.setChecked(prefs.getBoolean("notif_health", false));
        switchEmail.setChecked(prefs.getBoolean("notif_email", true));
        switchSms.setChecked(prefs.getBoolean("notif_sms", false));

        MaterialButton btnSave = findViewById(R.id.btnSaveSettings);
        btnSave.setOnClickListener(v -> {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("notif_appt", switchAppt.isChecked());
            editor.putBoolean("notif_queue", switchQueue.isChecked());
            editor.putBoolean("notif_health", switchHealth.isChecked());
            editor.putBoolean("notif_email", switchEmail.isChecked());
            editor.putBoolean("notif_sms", switchSms.isChecked());
            editor.apply();

            Toast.makeText(this, "Notification preferences saved", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}