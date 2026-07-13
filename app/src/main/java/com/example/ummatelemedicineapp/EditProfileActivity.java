package com.example.ummatelemedicineapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class EditProfileActivity extends BaseActivity {

    private TextInputEditText etName, etEmail;
    private ImageView ivProfile;
    private MaterialButton btnSave;
    private SharedPreferences prefs;
    private Uri selectedImageUri;

    private final ActivityResultLauncher<PickVisualMediaRequest> pickMedia =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    ivProfile.setImageURI(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        Toolbar toolbar = findViewById(R.id.toolbarEditProfile);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        etName = findViewById(R.id.etEditName);
        etEmail = findViewById(R.id.etEditEmail);
        ivProfile = findViewById(R.id.ivEditProfileImage);
        btnSave = findViewById(R.id.btnSaveProfile);
        prefs = getSharedPreferences("UMMA_PREFS", MODE_PRIVATE);

        // Load current data
        etName.setText(prefs.getString("user_name", ""));
        etEmail.setText(prefs.getString("user_email", ""));
        
        String savedImageUri = prefs.getString("user_image_uri", null);
        if (savedImageUri != null) {
            if (savedImageUri.startsWith("/")) {
                ivProfile.setImageURI(Uri.fromFile(new File(savedImageUri)));
            } else {
                selectedImageUri = Uri.parse(savedImageUri);
                ivProfile.setImageURI(selectedImageUri);
            }
        }

        findViewById(R.id.fabChangePhoto).setOnClickListener(v -> {
            pickMedia.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });

        btnSave.setOnClickListener(v -> {
            String newName = etName.getText().toString();
            String newEmail = etEmail.getText().toString();

            if (newName.isEmpty() || newEmail.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            } else {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("user_name", newName);
                editor.putString("user_email", newEmail);
                if (selectedImageUri != null) {
                    String savedPath = saveImageToInternalStorage(selectedImageUri);
                    if (savedPath != null) {
                        editor.putString("user_image_uri", savedPath);
                    }
                }
                editor.apply();
                
                Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }
        });
    }

    private String saveImageToInternalStorage(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            File file = new File(getFilesDir(), "profile_pic.jpg");
            OutputStream outputStream = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            outputStream.flush();
            outputStream.close();
            inputStream.close();
            return file.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}