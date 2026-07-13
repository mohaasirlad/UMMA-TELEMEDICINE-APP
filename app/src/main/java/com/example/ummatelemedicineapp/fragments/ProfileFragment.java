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

import com.example.ummatelemedicineapp.ChangePasswordActivity;
import com.example.ummatelemedicineapp.EditProfileActivity;
import com.example.ummatelemedicineapp.FileUploadActivity;
import com.example.ummatelemedicineapp.LoginActivity;
import com.example.ummatelemedicineapp.NotificationSettingsActivity;
import com.example.ummatelemedicineapp.R;
import com.example.ummatelemedicineapp.utils.LocaleHelper;

import com.example.ummatelemedicineapp.database.AppDatabase;
import com.google.firebase.auth.FirebaseAuth;

import java.util.concurrent.Executors;

public class ProfileFragment extends Fragment {

    private TextView tvName, tvEmail;
    private ImageView ivProfile;
    private SharedPreferences prefs;

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        ivProfile.setImageURI(imageUri);
                        prefs.edit().putString("user_image_uri", imageUri.toString()).apply();
                        Toast.makeText(getContext(), "Profile photo updated", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        prefs = requireActivity().getSharedPreferences("UMMA_PREFS", Context.MODE_PRIVATE);

        tvName = view.findViewById(R.id.tvProfileName);
        tvEmail = view.findViewById(R.id.tvProfileEmail);
        ivProfile = view.findViewById(R.id.ivProfileImage);

        updateProfileDisplay();

        if (ivProfile != null) {
            ivProfile.setOnClickListener(v -> selectImage());
        }

        view.findViewById(R.id.btnLogout).setOnClickListener(v -> {
            // Sign out from Firebase
            FirebaseAuth.getInstance().signOut();

            // Clear local database
            Executors.newSingleThreadExecutor().execute(() -> {
                AppDatabase.getInstance(getContext()).chatMessageDao().clearAll();
            });

            // Clear registration and session data
            prefs.edit()
                .remove("is_registered")
                .remove("last_role")
                .remove("user_email")
                .remove("user_password")
                .apply();

            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        view.findViewById(R.id.btnUploadReports).setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), FileUploadActivity.class));
        });

        view.findViewById(R.id.btnEditProfile).setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), EditProfileActivity.class));
        });

        view.findViewById(R.id.btnChangePassword).setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), ChangePasswordActivity.class));
        });

        view.findViewById(R.id.btnLanguage).setOnClickListener(v -> {
            showLanguageDialog();
        });

        view.findViewById(R.id.btnNotifications).setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), NotificationSettingsActivity.class));
        });

        return view;
    }

    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(intent);
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

        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setTitle("Select Language");
        builder.setSingleChoiceItems(languages, checkedItem, (dialog, which) -> {
            String selected = languages[which];
            LocaleHelper.setLocale(requireContext(), selected);
            
            Toast.makeText(getActivity(), "Language set to " + selected, Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            
            // Restart activity to apply changes with a small delay
            new android.os.Handler().postDelayed(() -> {
                if (getActivity() != null) getActivity().recreate();
            }, 100);
        });
        builder.show();
    }

    private void updateProfileDisplay() {
        tvName.setText(prefs.getString("user_name", "Patient Name"));
        tvEmail.setText(prefs.getString("user_email", "patient@example.com"));

        String imageUriStr = prefs.getString("user_image_uri", null);
        if (imageUriStr != null) {
            try {
                ivProfile.setImageURI(Uri.parse(imageUriStr));
            } catch (Exception e) {
                ivProfile.setImageResource(R.drawable.ic_patient_avatar);
            }
        } else {
            ivProfile.setImageResource(R.drawable.ic_patient_avatar);
        }

        // Verification Badge logic
        if (getView() != null) {
            TextView tvVerified = getView().findViewById(R.id.tvPatientVerifiedBadge);
            if (tvVerified != null) {
                boolean isVerified = FirebaseAuth.getInstance().getCurrentUser() != null && 
                                    FirebaseAuth.getInstance().getCurrentUser().isEmailVerified();
                tvVerified.setVisibility(isVerified ? View.VISIBLE : View.GONE);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateProfileDisplay();
    }
}