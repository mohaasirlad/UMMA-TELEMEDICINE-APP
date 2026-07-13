package com.example.ummatelemedicineapp;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.example.ummatelemedicineapp.fragments.AppointmentsFragment;
import com.example.ummatelemedicineapp.fragments.HomeFragment;
import com.example.ummatelemedicineapp.fragments.MessagesFragment;
import com.example.ummatelemedicineapp.fragments.NotificationsFragment;
import com.example.ummatelemedicineapp.fragments.ProfileFragment;
import com.example.ummatelemedicineapp.fragments.QueueFragment;
import com.example.ummatelemedicineapp.utils.LocaleHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class HomeActivity extends BaseActivity {

    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        bottomNav = findViewById(R.id.bottom_navigation);
        
        setupNotificationBadge();

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.nav_appointments) {
                selectedFragment = new AppointmentsFragment();
            } else if (itemId == R.id.nav_notifications) {
                selectedFragment = new NotificationsFragment();
            } else if (itemId == R.id.nav_messages) {
                selectedFragment = new MessagesFragment();
            } else if (itemId == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }
            return true;
        });

        // Set default fragment
        if (savedInstanceState == null) {
            String navigateTo = getIntent().getStringExtra("navigate_to");
            if ("appointments".equals(navigateTo)) {
                bottomNav.setSelectedItemId(R.id.nav_appointments);
            } else {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new HomeFragment())
                        .commit();
            }
        }
    }

    public void navigateToTab(int itemId) {
        bottomNav.setSelectedItemId(itemId);
    }

    private void setupNotificationBadge() {
        com.example.ummatelemedicineapp.database.AppDatabase.getInstance(this).notificationDao().getUnreadCountLive()
                .observe(this, count -> {
                    com.google.android.material.badge.BadgeDrawable badge = bottomNav.getOrCreateBadge(R.id.nav_notifications);
                    if (count != null && count > 0) {
                        badge.setVisible(true);
                        badge.setNumber(count);
                    } else {
                        badge.setVisible(false);
                    }
                });
    }

    @Override
    public void onBackPressed() {
        if (bottomNav.getSelectedItemId() != R.id.nav_home) {
            bottomNav.setSelectedItemId(R.id.nav_home);
        } else {
            super.onBackPressed();
        }
    }
}