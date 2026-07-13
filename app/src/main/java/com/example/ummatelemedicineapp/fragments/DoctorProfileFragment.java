package com.example.ummatelemedicineapp.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.ummatelemedicineapp.LoginActivity;
import com.example.ummatelemedicineapp.R;
import com.example.ummatelemedicineapp.utils.LocaleHelper;

import android.widget.EditText;
import androidx.lifecycle.ViewModelProvider;
import com.example.ummatelemedicineapp.database.AppDatabase;
import com.example.ummatelemedicineapp.viewmodels.ProfileViewModel;
import com.google.firebase.auth.FirebaseAuth;

import java.util.concurrent.Executors;

public class DoctorProfileFragment extends Fragment {

    private ImageView ivProfile;
    private EditText etName, etSpecialty;
    private TextView tvCurrentAvailability, tvCurrentFees, tvCurrentShift;
    private SharedPreferences prefs;
    private ProfileViewModel profileViewModel;

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        try {
                            // Take persistable permission to keep access after reboot
                            int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
                            requireContext().getContentResolver().takePersistableUriPermission(imageUri, takeFlags);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        
                        try {
                            ivProfile.setImageURI(imageUri);
                            prefs.edit().putString("doctor_profile_uri", imageUri.toString()).apply();
                            profileViewModel.setProfileImageUri(imageUri.toString());
                            Toast.makeText(getContext(), "Profile photo updated", Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            Toast.makeText(getContext(), "Error loading image", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_doctor_profile, container, false);

        profileViewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);
        prefs = requireActivity().getSharedPreferences("UMMA_PREFS", Context.MODE_PRIVATE);
        
        String name = prefs.getString("doctor_name", "Sarah Wilson");
        String specialty = prefs.getString("doctor_specialty", "Cardiologist | MD");
        String profileUri = prefs.getString("doctor_profile_uri", null);
        
        ivProfile = view.findViewById(R.id.ivDoctorProfile);
        etName = view.findViewById(R.id.etProfileDocName);
        etSpecialty = view.findViewById(R.id.etProfileDocSpecialty);
        tvCurrentAvailability = view.findViewById(R.id.tvCurrentAvailability);
        tvCurrentFees = view.findViewById(R.id.tvCurrentFees);
        tvCurrentShift = view.findViewById(R.id.tvCurrentShift);

        // Verification Badge logic
        TextView tvVerified = view.findViewById(R.id.tvVerifiedBadge);
        if (tvVerified != null) {
            boolean isVerified = FirebaseAuth.getInstance().getCurrentUser() != null && 
                                FirebaseAuth.getInstance().getCurrentUser().isEmailVerified();
            tvVerified.setVisibility(isVerified ? View.VISIBLE : View.GONE);
        }

        etName.setText(name);
        etSpecialty.setText(specialty);

        if (profileUri != null && ivProfile != null) {
            try {
                ivProfile.setImageURI(Uri.parse(profileUri));
            } catch (Exception e) {
                ivProfile.setImageResource(R.drawable.ic_doctor_avatar);
            }
        }

        ivProfile.setOnClickListener(v -> selectImage());

        view.findViewById(R.id.btnSaveProfile).setOnClickListener(v -> saveProfile());

        view.findViewById(R.id.tvDoctorLogout).setOnClickListener(v -> {
            // Sign out from Firebase
            FirebaseAuth.getInstance().signOut();

            // Clear local database
            Executors.newSingleThreadExecutor().execute(() -> {
                AppDatabase.getInstance(getContext()).chatMessageDao().clearAll();
            });

            // Clear doctor registration and session data
            prefs.edit()
                .remove("is_doctor_registered")
                .remove("last_role")
                .remove("doctor_email")
                .remove("doctor_password")
                .apply();

            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        view.findViewById(R.id.tvAvailabilitySettings).setOnClickListener(v -> showAvailabilityDialog());
        view.findViewById(R.id.tvConsultationFees).setOnClickListener(v -> showFeesDialog());
        view.findViewById(R.id.tvShiftSettings).setOnClickListener(v -> showShiftDialog());
        view.findViewById(R.id.tvNotificationSettings).setOnClickListener(v -> showNotificationSettings());
        view.findViewById(R.id.tvPrivacyPolicy).setOnClickListener(v -> showPrivacyPolicy());
        view.findViewById(R.id.tvLanguageSettings).setOnClickListener(v -> showLanguageDialog());

        String availability = prefs.getString("doctor_availability", "Mon - Fri");
        String fees = prefs.getString("doctor_fees", "$50");
        String shift = prefs.getString("doctor_shift", "08:00 AM - 04:00 PM");
        
        if (tvCurrentAvailability != null) tvCurrentAvailability.setText(availability);
        if (tvCurrentFees != null) tvCurrentFees.setText(fees);
        if (tvCurrentShift != null) tvCurrentShift.setText(shift);

        return view;
    }

    private void showShiftDialog() {
        String[] shifts = {"08:00 AM - 04:00 PM", "04:00 PM - 12:00 AM", "12:00 AM - 08:00 AM"};
        new AlertDialog.Builder(requireContext())
                .setTitle("Select Work Shift")
                .setItems(shifts, (dialog, which) -> {
                    String selectedShift = shifts[which];
                    prefs.edit().putString("doctor_shift", selectedShift).apply();
                    if (tvCurrentShift != null) tvCurrentShift.setText(selectedShift);
                    profileViewModel.setDoctorShift(selectedShift);
                    Toast.makeText(getContext(), "Shift updated", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveProfile() {
        String newName = etName.getText().toString().trim();
        String newSpecialty = etSpecialty.getText().toString().trim();

        if (newName.isEmpty()) {
            etName.setError("Name cannot be empty");
            return;
        }

        prefs.edit()
                .putString("doctor_name", newName)
                .putString("doctor_specialty", newSpecialty)
                .apply();

        profileViewModel.setDoctorName(newName);
        
        Toast.makeText(getContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show();
    }

    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        pickImageLauncher.launch(intent);
    }

    private void showAvailabilityDialog() {
        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
        boolean[] checkedItems = {true, true, true, true, true, false, false};

        // Try to load current selection if possible (simplified for now)
        new AlertDialog.Builder(requireContext())
                .setTitle("Select Available Days")
                .setMultiChoiceItems(days, checkedItems, (dialog, which, isChecked) -> {
                    checkedItems[which] = isChecked;
                })
                .setPositiveButton("Save", (dialog, which) -> {
                    StringBuilder selectedDays = new StringBuilder();
                    for (int i = 0; i < days.length; i++) {
                        if (checkedItems[i]) {
                            if (selectedDays.length() > 0) selectedDays.append(", ");
                            selectedDays.append(days[i].substring(0, 3));
                        }
                    }
                    String result = selectedDays.toString();
                    if (result.isEmpty()) result = "None";
                    
                    prefs.edit().putString("doctor_availability", result).apply();
                    if (tvCurrentAvailability != null) tvCurrentAvailability.setText(result);
                    Toast.makeText(getContext(), "Availability updated", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showFeesDialog() {
        String[] fees = {"$50 - Standard Consultation", "$80 - Specialized Consultation", "$120 - Emergency/After Hours"};
        String[] feeValues = {"$50", "$80", "$120"};
        
        new AlertDialog.Builder(requireContext())
                .setTitle("Set Consultation Fee")
                .setItems(fees, (dialog, which) -> {
                    String selectedFee = feeValues[which];
                    prefs.edit().putString("doctor_fees", selectedFee).apply();
                    if (tvCurrentFees != null) tvCurrentFees.setText(selectedFee);
                    Toast.makeText(getContext(), "Fee updated to " + selectedFee, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showNotificationSettings() {
        String[] options = {"Push Notifications", "Email Alerts", "SMS Reminders"};
        boolean[] checked = {
            prefs.getBoolean("pref_push", true),
            prefs.getBoolean("pref_email", false),
            prefs.getBoolean("pref_sms", true)
        };

        new AlertDialog.Builder(requireContext())
                .setTitle("Notification Preferences")
                .setMultiChoiceItems(options, checked, (dialog, which, isChecked) -> {
                    String key = "";
                    switch (which) {
                        case 0: key = "pref_push"; break;
                        case 1: key = "pref_email"; break;
                        case 2: key = "pref_sms"; break;
                    }
                    prefs.edit().putBoolean(key, isChecked).apply();
                })
                .setPositiveButton("Done", null)
                .show();
    }

    private void showLanguageDialog() {
        String[] languages = {"English", "Swahili", "Arabic", "Somali"};
        String currentLang = prefs.getString("selected_language", "English");

        int checkedItem = 0;
        for (int i = 0; i < languages.length; i++) {
            if (languages[i].equals(currentLang)) {
                checkedItem = i;
                break;
            }
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Select Language")
                .setSingleChoiceItems(languages, checkedItem, (dialog, which) -> {
                    String selected = languages[which];
                    LocaleHelper.setLocale(requireContext(), selected);
                    Toast.makeText(getActivity(), "Language set to " + selected, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    
                    // Add a small delay to ensure preferences are saved before recreation
                    new android.os.Handler().postDelayed(() -> {
                        if (getActivity() != null) getActivity().recreate();
                    }, 100);
                })
                .show();
    }

    private void showPrivacyPolicy() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Privacy Policy")
                .setMessage("Ummah Telemedicine is committed to protecting patient data. All consultations are encrypted, and medical records are stored securely in compliance with healthcare regulations. We do not share your personal information with third parties without consent.")
                .setPositiveButton("Close", null)
                .show();
    }
}