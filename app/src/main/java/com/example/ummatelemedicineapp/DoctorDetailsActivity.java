package com.example.ummatelemedicineapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.ummatelemedicineapp.database.AppDatabase;
import com.example.ummatelemedicineapp.models.Appointment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.concurrent.Executors;

public class DoctorDetailsActivity extends BaseActivity {

    private String doctorId;
    private String name;
    private String specialty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor_details);

        Toolbar toolbar = findViewById(R.id.toolbarDetails);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(""); // Let the CollapsingToolbar handle title if needed, or keep it empty for clean look
        }
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        doctorId = getIntent().getStringExtra("doctor_id");
        name = getIntent().getStringExtra("doctor_name");
        specialty = getIntent().getStringExtra("doctor_specialty");
        int imageRes = getIntent().getIntExtra("doctor_image", R.drawable.ic_doctor_avatar);

        TextView tvName = findViewById(R.id.tvDoctorNameDetail);
        TextView tvSpecialty = findViewById(R.id.tvSpecialtyDetail);
        ImageView ivDoctor = findViewById(R.id.ivDoctorDetail);
        MaterialButton btnBook = findViewById(R.id.btnBookAppointment);
        RadioGroup rgPayment = findViewById(R.id.rgPaymentMethod);
        MaterialButton btnVideoCall = findViewById(R.id.btnVideoCallDoctor);
        if (btnVideoCall != null) {
            btnVideoCall.setOnClickListener(v -> {
                Intent intent = new Intent(this, VideoConsultationActivity.class);
                intent.putExtra("appointment_id", "adhoc_" + doctorId);
                intent.putExtra("doctor_id", doctorId);
                intent.putExtra("doctor_name", name);
                startActivity(intent);
            });
        }

        MaterialButton btnChat = findViewById(R.id.btnChatWithDoctor);
        if (btnChat != null) {
            btnChat.setOnClickListener(v -> {
                Intent intent = new Intent(this, ChatActivity.class);
                intent.putExtra("doctor_id", doctorId);
                intent.putExtra("sender_name", name);
                startActivity(intent);
            });
        }

        tvName.setText(name);
        tvSpecialty.setText(specialty);
        ivDoctor.setImageResource(imageRes);

        // Apply professional styling: Icons get padding/background, Real Photos get full-bleed
        if (imageRes == R.drawable.ic_doctor_avatar) {
            ivDoctor.setBackgroundColor(androidx.core.content.ContextCompat.getColor(this, R.color.light_bg_teal));
            ivDoctor.setPadding(150, 150, 150, 150);
            ivDoctor.setScaleType(ImageView.ScaleType.FIT_CENTER);
        } else {
            ivDoctor.setPadding(0, 0, 0, 0);
            ivDoctor.setScaleType(ImageView.ScaleType.CENTER_CROP);
        }

        btnBook.setOnClickListener(v -> {
            int selectedId = rgPayment.getCheckedRadioButtonId();
            if (selectedId == R.id.rbMpesa) {
                showMpesaDialog(name, specialty);
            } else if (selectedId == R.id.rbCard) {
                showCardDialog(name, specialty);
            } else {
                finalizeBooking(name, specialty, "Pay at Clinic");
            }
        });
    }

    private void showMpesaDialog(String doctorName, String specialty) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        android.view.View view = getLayoutInflater().inflate(R.layout.dialog_mpesa_payment, null);
        builder.setView(view);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextInputEditText etPhone = view.findViewById(R.id.etMpesaPhone);
        MaterialButton btnPay = view.findViewById(R.id.btnPayMpesa);

        btnPay.setOnClickListener(v -> {
            String phone = etPhone.getText().toString();
            if (phone.isEmpty() || phone.length() < 10) {
                Toast.makeText(this, R.string.error_invalid_phone, Toast.LENGTH_SHORT).show();
            } else {
                dialog.dismiss();
                Toast.makeText(this, getString(R.string.msg_mpesa_sent, phone), Toast.LENGTH_SHORT).show();
                finalizeBooking(doctorName, specialty, getString(R.string.payment_mpesa_status));
            }
        });

        dialog.show();
    }

    private void showCardDialog(String doctorName, String specialty) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        android.view.View view = getLayoutInflater().inflate(R.layout.dialog_card_payment, null);
        builder.setView(view);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        MaterialButton btnPay = view.findViewById(R.id.btnPayCard);
        btnPay.setOnClickListener(v -> {
            dialog.dismiss();
            Toast.makeText(this, "Processing card payment...", Toast.LENGTH_SHORT).show();
            finalizeBooking(doctorName, specialty, "Paid via Card");
        });

        dialog.show();
    }

    private void finalizeBooking(String doctorName, String specialty, String paymentStatus) {
        String patientId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        
        // Fetch real patient name from SharedPreferences or Firebase
        android.content.SharedPreferences prefs = getSharedPreferences("UMMA_PREFS", android.content.Context.MODE_PRIVATE);
        String patientName = prefs.getString("user_name", null);
        
        if (patientName == null || patientName.isEmpty()) {
            patientName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
        }
        
        if (patientName == null || patientName.isEmpty()) {
            patientName = "Patient";
        }

        // Create a new appointment object using the full constructor
        Appointment newAppointment = new Appointment(
            doctorName, patientName, specialty, "Tomorrow", "10:00 AM", "Pending", false, paymentStatus
        );
        
        // Ensure IDs are correctly linked
        if (doctorId == null || doctorId.isEmpty()) {
            // Fallback for demo purposes if ID is missing from intent
            doctorId = "doc_" + doctorName.toLowerCase().replace(" ", "_");
        }
        
        newAppointment.setDoctorId(doctorId);
        newAppointment.setPatientId(patientId);

        // Generate unique ID using Firebase push key
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference("appointments");
        String pushKey = mDatabase.push().getKey();
        if (pushKey == null) pushKey = String.valueOf(System.currentTimeMillis());
        newAppointment.setId(pushKey);
        final String finalPushKey = pushKey;
        final String finalPatientName = patientName;

        Executors.newSingleThreadExecutor().execute(() -> {
            // Save to Local DB
            AppDatabase.getInstance(getApplicationContext()).appointmentDao().insert(newAppointment);
            
            // Push to Firebase Realtime Database
            mDatabase.child(finalPushKey).setValue(newAppointment);

            // Notify the doctor via Firebase (Simulated for now by writing to a notifications node)
            DatabaseReference notificationRef = FirebaseDatabase.getInstance().getReference("notifications").child(newAppointment.getDoctorId()).push();
            com.example.ummatelemedicineapp.models.Notification notification = new com.example.ummatelemedicineapp.models.Notification(
                "New Booking",
                "You have a new appointment with " + finalPatientName,
                new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(new java.util.Date()),
                false,
                true
            );
            notificationRef.setValue(notification);
        });

        Intent intent = new Intent(this, BookingSuccessActivity.class);
        intent.putExtra("doctor_name", doctorName);
        startActivity(intent);
        finish();
    }
}