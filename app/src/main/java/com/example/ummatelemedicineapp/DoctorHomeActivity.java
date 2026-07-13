package com.example.ummatelemedicineapp;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.example.ummatelemedicineapp.fragments.DoctorDashboardFragment;
import com.example.ummatelemedicineapp.fragments.DoctorMessagesFragment;
import com.example.ummatelemedicineapp.fragments.DoctorProfileFragment;
import com.example.ummatelemedicineapp.fragments.DoctorScheduleFragment;
import com.example.ummatelemedicineapp.utils.LocaleHelper;
import com.example.ummatelemedicineapp.models.Notification;
import com.example.ummatelemedicineapp.database.AppDatabase;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class DoctorHomeActivity extends BaseActivity {

    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_home);

        bottomNav = findViewById(R.id.doctor_bottom_navigation);
        
        setupUrgentNotificationObserver();
        setupNotificationBadge();

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_doctor_home) {
                selectedFragment = new DoctorDashboardFragment();
            } else if (itemId == R.id.nav_doctor_appointments) {
                selectedFragment = new DoctorScheduleFragment();
            } else if (itemId == R.id.nav_doctor_messages) {
                selectedFragment = new DoctorMessagesFragment();
            } else if (itemId == R.id.nav_doctor_profile) {
                selectedFragment = new DoctorProfileFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.doctor_fragment_container, selectedFragment)
                        .commit();
            }
            return true;
        });

        // Set default fragment
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.doctor_fragment_container, new DoctorDashboardFragment())
                    .commit();
        }
    }

    public void navigateToTab(int itemId) {
        bottomNav.setSelectedItemId(itemId);
    }

    private void setupUrgentNotificationObserver() {
        AppDatabase.getInstance(this).notificationDao().getUrgentNotificationsLive()
                .observe(this, notifications -> {
                    if (notifications != null && !notifications.isEmpty()) {
                        showUrgentAlert(notifications.get(0));
                    }
                });
    }

    private void setupNotificationBadge() {
        AppDatabase.getInstance(this).notificationDao().getUnreadCountLive()
                .observe(this, count -> {
                    com.google.android.material.badge.BadgeDrawable badge = bottomNav.getOrCreateBadge(R.id.nav_doctor_home);
                    if (count != null && count > 0) {
                        badge.setVisible(true);
                        badge.setNumber(count);
                    } else {
                        badge.setVisible(false);
                    }
                });
    }

    private void showUrgentAlert(Notification notification) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("URGENT: " + notification.getTitle())
                .setMessage(notification.getMessage())
                .setPositiveButton("View", (dialog, which) -> {
                    // Mark as read and navigate to notifications or specific action
                    new Thread(() -> {
                        AppDatabase.getInstance(this).notificationDao().markAsRead(notification.getId());
                    }).start();
                    // Example navigation
                    bottomNav.setSelectedItemId(R.id.nav_doctor_home); 
                })
                .setNegativeButton("Dismiss", (dialog, which) -> {
                    new Thread(() -> {
                        AppDatabase.getInstance(this).notificationDao().markAsRead(notification.getId());
                    }).start();
                })
                .setCancelable(false)
                .show();
    }

    @Override
    public void onBackPressed() {
        if (bottomNav.getSelectedItemId() != R.id.nav_doctor_home) {
            bottomNav.setSelectedItemId(R.id.nav_doctor_home);
        } else {
            super.onBackPressed();
        }
    }
}