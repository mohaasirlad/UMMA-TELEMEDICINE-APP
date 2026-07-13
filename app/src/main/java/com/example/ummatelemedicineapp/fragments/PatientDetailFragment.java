package com.example.ummatelemedicineapp.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.example.ummatelemedicineapp.ChatActivity;
import com.example.ummatelemedicineapp.R;
import com.example.ummatelemedicineapp.database.AppDatabase;
import com.example.ummatelemedicineapp.models.Appointment;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PatientDetailFragment extends Fragment {

    private static final String ARG_APPOINTMENT_ID = "appointment_id";
    private String appointmentId;
    private Appointment appointment;
    private AppDatabase db;
    private DatabaseReference mDatabase;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private TextView tvName, tvType, tvDate, tvTime;
    private EditText etNotes;
    private Button btnSave, btnStartCall;
    private View btnChat;

    public static PatientDetailFragment newInstance(String appointmentId) {
        PatientDetailFragment fragment = new PatientDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_APPOINTMENT_ID, appointmentId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            appointmentId = getArguments().getString(ARG_APPOINTMENT_ID);
        }
        db = AppDatabase.getInstance(requireContext());
        mDatabase = FirebaseDatabase.getInstance().getReference("appointments");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_patient_detail, container, false);

        Toolbar toolbar = view.findViewById(R.id.toolbarPatientDetail);
        toolbar.setNavigationOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        tvName = view.findViewById(R.id.tvDetailPatientName);
        tvType = view.findViewById(R.id.tvDetailType);
        tvDate = view.findViewById(R.id.tvDetailDate);
        tvTime = view.findViewById(R.id.tvDetailTime);
        etNotes = view.findViewById(R.id.etMedicalNotes);
        btnSave = view.findViewById(R.id.btnSaveNotes);
        btnChat = view.findViewById(R.id.btnChatWithPatient);
        btnStartCall = view.findViewById(R.id.btnStartCall);

        loadAppointmentDetails();

        btnSave.setOnClickListener(v -> saveNotes());
        
        btnStartCall.setOnClickListener(v -> {
            if (appointment != null) {
                // Send notification to patient that call has started
                DatabaseReference notifRef = FirebaseDatabase.getInstance().getReference("notifications").child(appointment.getPatientId()).push();
                com.example.ummatelemedicineapp.models.Notification notification = new com.example.ummatelemedicineapp.models.Notification(
                    "Incoming Call",
                    "Doctor " + appointment.getDoctorName() + " is waiting for you in the virtual room.",
                    new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(new java.util.Date()),
                    false,
                    true
                );
                notification.setAppointmentId(appointment.getId());
                notifRef.setValue(notification);

                Intent intent = new Intent(getActivity(), com.example.ummatelemedicineapp.VideoConsultationActivity.class);
                intent.putExtra("appointment_id", appointment.getId());
                intent.putExtra("user_identity", appointment.getDoctorId());
                intent.putExtra("patient_name", appointment.getPatientName());
                startActivity(intent);
            }
        });
        
        btnChat.setOnClickListener(v -> {
            if (appointment != null) {
                Intent intent = new Intent(getActivity(), ChatActivity.class);
                intent.putExtra("patient_id", appointment.getPatientId());
                intent.putExtra("sender_name", appointment.getPatientName());
                startActivity(intent);
            }
        });

        return view;
    }

    private void loadAppointmentDetails() {
        executorService.execute(() -> {
            appointment = db.appointmentDao().getAppointmentById(appointmentId);

            if (appointment != null && isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    tvName.setText(appointment.getPatientName());
                    tvType.setText(appointment.getType());
                    tvDate.setText(appointment.getDate());
                    tvTime.setText(appointment.getTime());
                    etNotes.setText(appointment.getMedicalNotes());
                });
            }
        });
    }

    private void saveNotes() {
        if (appointment == null) return;

        String notes = etNotes.getText().toString().trim();
        
        if (notes.isEmpty()) {
            etNotes.setError("Notes cannot be empty");
            return;
        }

        if (notes.length() < 10) {
            etNotes.setError("Please provide more detailed notes (min 10 characters)");
            return;
        }

        appointment.setMedicalNotes(notes);
        appointment.setStatus("Completed");
        appointment.setPast(true);

        executorService.execute(() -> {
            // Update local database
            db.appointmentDao().update(appointment);
            
            // Send notification to patient
            DatabaseReference notifRef = FirebaseDatabase.getInstance().getReference("notifications").child(appointment.getPatientId()).push();
            com.example.ummatelemedicineapp.models.Notification notification = new com.example.ummatelemedicineapp.models.Notification(
                "Appointment Completed",
                "Your appointment with " + appointment.getDoctorName() + " has been marked as completed. You can view your medical notes in the app.",
                new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(new java.util.Date()),
                false,
                false
            );
            notifRef.setValue(notification);

            // Sync with Firebase
            mDatabase.child(appointment.getId()).setValue(appointment)
                .addOnSuccessListener(aVoid -> {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "Medical record updated and synced", Toast.LENGTH_SHORT).show();
                            requireActivity().getSupportFragmentManager().popBackStack();
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        requireActivity().runOnUiThread(() -> 
                            Toast.makeText(getContext(), "Local update saved, but cloud sync failed", Toast.LENGTH_SHORT).show());
                    }
                });
        });
    }
}
