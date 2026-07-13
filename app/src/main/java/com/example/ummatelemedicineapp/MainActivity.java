package com.example.ummatelemedicineapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;

import com.example.ummatelemedicineapp.utils.LocaleHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BaseActivity {

    private OnboardingAdapter onboardingAdapter;
    private LinearLayout layoutIndicators;
    private MaterialButton btnNext;
    private ViewPager2 viewPagerOnboarding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Firebase Auth and Role Routing
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            android.content.SharedPreferences prefs = getSharedPreferences("UMMA_PREFS", MODE_PRIVATE);
            String lastRole = prefs.getString("last_role", "");

            if ("doctor".equals(lastRole)) {
                startActivity(new Intent(this, DoctorHomeActivity.class));
                finish();
                return;
            } else if ("patient".equals(lastRole)) {
                startActivity(new Intent(this, HomeActivity.class));
                finish();
                return;
            }
            // If role is unknown but user is logged in, we might want to fetch it from DB 
            // but for now, let them see onboarding or login if role is missing.
        }


        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        layoutIndicators = findViewById(R.id.layoutIndicators);
        btnNext = findViewById(R.id.btnNext);
        viewPagerOnboarding = findViewById(R.id.viewPagerOnboarding);

        setupOnboardingItems();
        viewPagerOnboarding.setAdapter(onboardingAdapter);

        setupIndicators();
        setCurrentIndicator(0);

        viewPagerOnboarding.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                setCurrentIndicator(position);
                if (position == onboardingAdapter.getItemCount() - 1) {
                    btnNext.setText(R.string.get_started);
                } else {
                    btnNext.setText(R.string.next);
                }
            }
        });

        btnNext.setOnClickListener(v -> {
            if (viewPagerOnboarding.getCurrentItem() + 1 < onboardingAdapter.getItemCount()) {
                viewPagerOnboarding.setCurrentItem(viewPagerOnboarding.getCurrentItem() + 1);
            } else {
                navigateToLogin();
            }
        });

        findViewById(R.id.btnSkip).setOnClickListener(v -> navigateToLogin());
    }

    private void setupOnboardingItems() {
        List<OnboardingItem> onboardingItems = new ArrayList<>();

        // Screen 1: Book appointments instantly
        onboardingItems.add(new OnboardingItem(
                R.drawable.img_onboarding_booking_new,
                getString(R.string.onboarding_title_1),
                getString(R.string.onboarding_desc_1)
        ));

        // Screen 2: Track queue in real time
        onboardingItems.add(new OnboardingItem(
                R.drawable.img_onboarding_queue_new,
                getString(R.string.onboarding_title_2),
                getString(R.string.onboarding_desc_2)
        ));

        // Screen 3: Consult doctors remotely
        onboardingItems.add(new OnboardingItem(
                R.drawable.img_onboarding_telehealth_new,
                getString(R.string.onboarding_title_3),
                getString(R.string.onboarding_desc_3)
        ));

        onboardingAdapter = new OnboardingAdapter(onboardingItems);
    }

    private void setupIndicators() {
        ImageView[] indicators = new ImageView[onboardingAdapter.getItemCount()];
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        layoutParams.setMargins(8, 0, 8, 0);
        for (int i = 0; i < indicators.length; i++) {
            indicators[i] = new ImageView(getApplicationContext());
            indicators[i].setImageDrawable(ContextCompat.getDrawable(
                    getApplicationContext(),
                    R.drawable.indicator_inactive
            ));
            indicators[i].setLayoutParams(layoutParams);
            layoutIndicators.addView(indicators[i]);
        }
    }

    private void setCurrentIndicator(int index) {
        int childCount = layoutIndicators.getChildCount();
        for (int i = 0; i < childCount; i++) {
            ImageView imageView = (ImageView) layoutIndicators.getChildAt(i);
            if (i == index) {
                imageView.setImageDrawable(ContextCompat.getDrawable(
                        getApplicationContext(),
                        R.drawable.indicator_active
                ));
            } else {
                imageView.setImageDrawable(ContextCompat.getDrawable(
                        getApplicationContext(),
                        R.drawable.indicator_inactive
                ));
            }
        }
    }

    private void navigateToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}