package com.example.ummatelemedicineapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

public class SymptomCheckerActivity extends BaseActivity {

    private String recommendedDoctor = "";
    private String recommendedSpecialty = "";
    private String recommendedDoctorId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_symptom_checker);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        TextInputEditText etSymptoms = findViewById(R.id.etSymptoms);
        Button btnAnalyze = findViewById(R.id.btnAnalyze);
        MaterialCardView cardResult = findViewById(R.id.cardResult);
        
        TextView tvPriority = findViewById(R.id.tvPriority);
        TextView tvAssessment = findViewById(R.id.tvAssessment);
        TextView tvRecommendation = findViewById(R.id.tvRecommendation);
        TextView tvRecommendedDoctor = findViewById(R.id.tvRecommendedDoctor);
        TextView tvRecommendedSpecialty = findViewById(R.id.tvRecommendedSpecialty);
        
        Button btnBookNow = findViewById(R.id.btnBookNow);

        btnAnalyze.setOnClickListener(v -> {
            String symptoms = etSymptoms.getText().toString().trim().toLowerCase();
            
            if (symptoms.isEmpty()) {
                Toast.makeText(this, "Please describe how you feel so I can help you.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Simulated AI processing
            btnAnalyze.setEnabled(false);
            btnAnalyze.setText("Analyzing...");
            cardResult.setVisibility(View.GONE);

            // Mocking a small delay for AI processing
            btnAnalyze.postDelayed(() -> {
                performAiAnalysis(symptoms, tvPriority, tvAssessment, tvRecommendation, tvRecommendedDoctor, tvRecommendedSpecialty);
                cardResult.setVisibility(View.VISIBLE);
                btnAnalyze.setEnabled(true);
                btnAnalyze.setText("Run AI Analysis");
            }, 1500);
        });

        btnBookNow.setOnClickListener(v -> {
            if (recommendedDoctorId.isEmpty()) {
                Toast.makeText(this, "Please wait while we find a doctor for you.", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, DoctorDetailsActivity.class);
            intent.putExtra("doctor_id", recommendedDoctorId);
            intent.putExtra("doctor_name", recommendedDoctor);
            intent.putExtra("doctor_specialty", recommendedSpecialty);
            intent.putExtra("doctor_image", R.drawable.ic_doctor_avatar);
            startActivity(intent);
        });
    }

    private void performAiAnalysis(String symptoms, TextView tvPriority, TextView tvAssessment, TextView tvRecommendation, 
                                 TextView tvRecommendedDoctor, TextView tvRecommendedSpecialty) {
        
        String s = symptoms.toLowerCase();

        // Default to General Physician
        recommendedDoctor = getString(R.string.searching); 
        recommendedSpecialty = getString(R.string.general_physician);
        recommendedDoctorId = "";
        
        tvRecommendedDoctor.setText(recommendedDoctor);
        tvRecommendedSpecialty.setText(recommendedSpecialty);
        tvPriority.setText(R.string.symptom_priority_general);
        tvPriority.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.gray)));
        tvAssessment.setText(R.string.symptom_assess_gen);
        tvRecommendation.setText(R.string.symptom_recom_gen);

        if (s.contains("chest pain") || s.contains("heart") || s.contains("shortness of breath") || s.contains("palpitation") || s.contains("pressure in chest")) {
            recommendedSpecialty = "Cardiologist";
            tvPriority.setText(R.string.symptom_priority_urgent);
            tvPriority.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.emergency_red)));
            tvAssessment.setText(R.string.symptom_assess_cardio);
            tvRecommendation.setText(R.string.symptom_recom_cardio);
        } 
        else if (s.contains("headache") || s.contains("migraine") || s.contains("dizziness") || s.contains("seizure") || s.contains("numbness") || s.contains("confusion")) {
            recommendedSpecialty = "Neurologist";
            tvPriority.setText(R.string.symptom_priority_moderate);
            tvPriority.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.dot_active)));
            tvAssessment.setText(R.string.symptom_assess_neuro);
            tvRecommendation.setText(R.string.symptom_recom_neuro);
        } 
        else if (s.contains("tooth") || s.contains("gum") || s.contains("cavity") || s.contains("bleeding gums") || s.contains("toothache")) {
            recommendedSpecialty = "Dentist";
            tvPriority.setText(R.string.symptom_priority_moderate);
            tvPriority.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.dot_active)));
            tvAssessment.setText(R.string.symptom_assess_dental);
            tvRecommendation.setText(R.string.symptom_recom_dental);
        } 
        else if (s.contains("stomach") || s.contains("nausea") || s.contains("vomiting") || s.contains("diarrhea") || s.contains("abdominal") || s.contains("bloating")) {
            recommendedSpecialty = "General Physician";
            tvPriority.setText(R.string.symptom_priority_routine);
            tvPriority.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.success_green)));
            tvAssessment.setText(R.string.symptom_assess_gastro);
            tvRecommendation.setText(R.string.symptom_recom_gastro);
        }
        else if (s.contains("fever") || s.contains("cough") || s.contains("sore throat") || s.contains("flu") || s.contains("cold") || s.contains("fatigue")) {
            recommendedSpecialty = "General Physician";
            tvPriority.setText(R.string.symptom_priority_routine);
            tvPriority.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.success_green)));
            tvAssessment.setText(R.string.symptom_assess_viral);
            tvRecommendation.setText(R.string.symptom_recom_viral);
        }
        else if (s.contains("skin") || s.contains("rash") || s.contains("itch") || s.contains("acne") || s.contains("spot")) {
            recommendedSpecialty = "General Physician";
            tvPriority.setText(R.string.symptom_priority_routine);
            tvPriority.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.success_green)));
            tvAssessment.setText(R.string.symptom_assess_skin);
            tvRecommendation.setText(R.string.symptom_recom_skin);
        }
        else if (s.contains("joint") || s.contains("bone") || s.contains("back pain") || s.contains("muscle") || s.contains("sprain")) {
            recommendedSpecialty = "General Physician";
            tvPriority.setText(R.string.symptom_priority_moderate);
            tvPriority.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.dot_active)));
            tvAssessment.setText(R.string.symptom_assess_ortho);
            tvRecommendation.setText(R.string.symptom_recom_ortho);
        }

        // Fetch the first doctor with the recommended specialty from Firebase
        com.google.firebase.database.FirebaseDatabase.getInstance().getReference("users")
            .orderByChild("role").equalTo("doctor")
            .get().addOnSuccessListener(snapshot -> {
                boolean found = false;
                for (com.google.firebase.database.DataSnapshot data : snapshot.getChildren()) {
                    String spec = data.child("specialty").getValue(String.class);
                    if (recommendedSpecialty.equalsIgnoreCase(spec)) {
                        recommendedDoctor = data.child("name").getValue(String.class);
                        recommendedDoctorId = data.getKey();
                        tvRecommendedDoctor.setText(recommendedDoctor);
                        tvRecommendedSpecialty.setText(recommendedSpecialty);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                     // If no doctor with specific specialty, find any available doctor
                     for (com.google.firebase.database.DataSnapshot data : snapshot.getChildren()) {
                         recommendedDoctor = data.child("name").getValue(String.class);
                         recommendedDoctorId = data.getKey();
                         tvRecommendedDoctor.setText(recommendedDoctor);
                         tvRecommendedSpecialty.setText(R.string.general_physician);
                         found = true;
                         break;
                     }
                }
                
                if (!found) {
                    tvRecommendedDoctor.setText(R.string.no_doctor_available);
                    tvRecommendedSpecialty.setText("");
                }
            });
    }
}