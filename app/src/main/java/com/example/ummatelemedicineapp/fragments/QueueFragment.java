package com.example.ummatelemedicineapp.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.ummatelemedicineapp.R;
import com.example.ummatelemedicineapp.database.AppDatabase;
import com.example.ummatelemedicineapp.models.Appointment;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Locale;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class QueueFragment extends Fragment {

    private TextView tvQueueNumber, tvEstimatedWait, tvPatientsAhead, tvQueueDoctorName;
    private LinearProgressIndicator queueProgress;
    private AppDatabase db;
    private SharedPreferences prefs;
    private ValueEventListener mValueEventListener, mDoctorQueueListener;
    private String currentDoctorId = null;
    private String currentAppointmentDate = null;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_queue, container, false);

        db = AppDatabase.getInstance(requireContext());
        prefs = requireActivity().getSharedPreferences("UMMA_PREFS", Context.MODE_PRIVATE);

        tvQueueNumber = view.findViewById(R.id.tvQueueNumber);
        tvEstimatedWait = view.findViewById(R.id.tvEstimatedWait);
        tvPatientsAhead = view.findViewById(R.id.tvPatientsAhead);
        tvQueueDoctorName = view.findViewById(R.id.tvQueueDoctorName);
        queueProgress = view.findViewById(R.id.queueProgress);

        listenForRealtimeUpdates();
        observeQueue();

        view.findViewById(R.id.btnViewClinicMap).setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), com.example.ummatelemedicineapp.ClinicLocatorActivity.class);
            intent.putExtra("show_directions_to_main", true);
            startActivity(intent);
        });

        return view;
    }

    private void listenForRealtimeUpdates() {
        if (mValueEventListener != null) return;
        String currentUserId = FirebaseAuth.getInstance().getUid();
        if (currentUserId == null) return;

        mValueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot data : snapshot.getChildren()) {
                    Appointment appointment = data.getValue(Appointment.class);
                    if (appointment != null) {
                        executorService.execute(() -> db.appointmentDao().insert(appointment));
                        
                        // If this is a today's appointment, sync the doctor's queue for position calculation
                        if ("Today".equalsIgnoreCase(appointment.getDate())) {
                            listenForDoctorQueue(appointment.getDoctorId(), appointment.getDate());
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        FirebaseDatabase.getInstance().getReference("appointments")
                .orderByChild("patientId")
                .equalTo(currentUserId)
                .addValueEventListener(mValueEventListener);
    }

    private void listenForDoctorQueue(String doctorId, String date) {
        if (doctorId == null || date == null) return;
        if (doctorId.equals(currentDoctorId) && date.equals(currentAppointmentDate)) return;

        // Clean up previous doctor listener if it exists
        if (mDoctorQueueListener != null && currentDoctorId != null) {
            FirebaseDatabase.getInstance().getReference("appointments")
                    .orderByChild("doctorId")
                    .equalTo(currentDoctorId)
                    .removeEventListener(mDoctorQueueListener);
        }

        currentDoctorId = doctorId;
        currentAppointmentDate = date;

        mDoctorQueueListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot data : snapshot.getChildren()) {
                    Appointment appointment = data.getValue(Appointment.class);
                    if (appointment != null && date.equals(appointment.getDate())) {
                        executorService.execute(() -> db.appointmentDao().insert(appointment));
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        FirebaseDatabase.getInstance().getReference("appointments")
                .orderByChild("doctorId")
                .equalTo(doctorId)
                .addValueEventListener(mDoctorQueueListener);
    }

    private int parseTime(String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) return 0;
        try {
            String normalizedTime = timeStr.trim().toUpperCase();
            String[] parts = normalizedTime.split(" ");
            if (parts.length < 2) return 0;
            
            String timePart = parts[0];
            String ampm = parts[1];
            
            int hours, minutes = 0;
            if (timePart.contains(":")) {
                String[] hhmm = timePart.split(":");
                hours = Integer.parseInt(hhmm[0]);
                minutes = Integer.parseInt(hhmm[1]);
            } else {
                hours = Integer.parseInt(timePart);
            }
            
            if (ampm.equals("PM") && hours < 12) hours += 12;
            else if (ampm.equals("AM") && hours == 12) hours = 0;
            
            return hours * 60 + minutes;
        } catch (Exception e) {
            return 0;
        }
    }

    private void observeQueue() {
        String currentUserId = FirebaseAuth.getInstance().getUid();
        if (currentUserId == null) return;

        db.appointmentDao().getUpcomingAppointmentsLive().observe(getViewLifecycleOwner(), appointments -> {
            if (appointments == null || appointments.isEmpty()) {
                updateEmptyQueue();
                return;
            }

            // Find my active appointment for today
            Appointment myAppt = appointments.stream()
                    .filter(a -> currentUserId.equals(a.getPatientId()) && 
                            "Today".equalsIgnoreCase(a.getDate()) &&
                            ("Confirmed".equalsIgnoreCase(a.getStatus()) || 
                             "Pending".equalsIgnoreCase(a.getStatus()) || 
                             "Attended".equalsIgnoreCase(a.getStatus())))
                    .findFirst()
                    .orElse(null);

            if (myAppt == null) {
                updateEmptyQueue();
                return;
            }

            // Filter for same doctor and same date, then sort by time
            // Only include Pending/Confirmed for the waiting list
            List<Appointment> doctorQueue = appointments.stream()
                    .filter(a -> myAppt.getDoctorId().equals(a.getDoctorId()) && 
                            myAppt.getDate().equals(a.getDate()) &&
                            ("Confirmed".equalsIgnoreCase(a.getStatus()) || "Pending".equalsIgnoreCase(a.getStatus())))
                    .sorted((a1, a2) -> Integer.compare(parseTime(a1.getTime()), parseTime(a2.getTime())))
                    .collect(Collectors.toList());

            int myPosition = -1;
            for (int i = 0; i < doctorQueue.size(); i++) {
                if (myAppt.getId().equals(doctorQueue.get(i).getId())) {
                    myPosition = i + 1;
                    break;
                }
            }

            if (myPosition != -1 || "Attended".equalsIgnoreCase(myAppt.getStatus())) {
                if ("Attended".equalsIgnoreCase(myAppt.getStatus())) {
                    tvQueueNumber.setText("--");
                    tvPatientsAhead.setText(R.string.queue_status_attending);
                    tvEstimatedWait.setText(R.string.queue_in_consultation);
                    queueProgress.setProgress(100);
                } else {
                    int ahead = myPosition - 1;
                    int waitTime = ahead * 15; // 15 mins per patient estimate

                    tvQueueNumber.setText(String.format(Locale.getDefault(), "%02d", myPosition));
                    
                    String aheadText = ahead == 1 ? 
                            getString(R.string.queue_patients_ahead_singular) : 
                            getString(R.string.queue_patients_ahead_plural, ahead);
                    tvPatientsAhead.setText(aheadText);

                    String waitValue = waitTime == 0 ? 
                            getString(R.string.queue_you_are_next) : 
                            getString(R.string.queue_wait_mins, waitTime);
                    tvEstimatedWait.setText(getString(R.string.queue_wait_time, waitValue));
                    
                    int progress = Math.max(5, 100 - (ahead * 20)); 
                    queueProgress.setProgress(progress);
                }
                
                if (tvQueueDoctorName != null) {
                    tvQueueDoctorName.setText(getString(R.string.date_time_format, "Room 102", myAppt.getDoctorName()));
                }
            } else {
                updateEmptyQueue();
                tvPatientsAhead.setText(R.string.queue_no_appointment);
                tvEstimatedWait.setText(R.string.queue_book_now);
            }
        });
    }

    private void updateEmptyQueue() {
        tvQueueNumber.setText("--");
        tvPatientsAhead.setText(R.string.queue_empty);
        tvEstimatedWait.setText(R.string.queue_clinic_clear);
        if (tvQueueDoctorName != null) {
            tvQueueDoctorName.setText(R.string.no_active_appointment);
        }
        queueProgress.setProgress(0);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mValueEventListener != null) {
            String currentUserId = FirebaseAuth.getInstance().getUid();
            if (currentUserId != null) {
                FirebaseDatabase.getInstance().getReference("appointments")
                        .orderByChild("patientId")
                        .equalTo(currentUserId)
                        .removeEventListener(mValueEventListener);
            }
        }
        if (mDoctorQueueListener != null && currentDoctorId != null) {
            FirebaseDatabase.getInstance().getReference("appointments")
                    .orderByChild("doctorId")
                    .equalTo(currentDoctorId)
                    .removeEventListener(mDoctorQueueListener);
        }
        executorService.shutdown();
    }
}
