package com.example.ummatelemedicineapp.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ummatelemedicineapp.R;
import com.example.ummatelemedicineapp.adapters.AppointmentAdapter;
import com.example.ummatelemedicineapp.database.AppDatabase;
import com.example.ummatelemedicineapp.models.Appointment;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DoctorScheduleFragment extends Fragment {

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private AppDatabase db;
    private DatabaseReference mDatabase;
    private ValueEventListener mValueEventListener;
    private RecyclerView rvSchedule;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_doctor_schedule, container, false);

        db = AppDatabase.getInstance(requireContext());
        mDatabase = FirebaseDatabase.getInstance().getReference("appointments");
        rvSchedule = view.findViewById(R.id.rvDoctorSchedule);
        rvSchedule.setLayoutManager(new LinearLayoutManager(getContext()));

        loadSchedule(rvSchedule);
        listenForRealtimeUpdates();

        return view;
    }

    private void listenForRealtimeUpdates() {
        com.google.firebase.auth.FirebaseUser currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;
        String currentDoctorId = currentUser.getUid();

        mValueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot data : snapshot.getChildren()) {
                    Appointment appointment = data.getValue(Appointment.class);
                    if (appointment != null) {
                        executorService.execute(() -> db.appointmentDao().insert(appointment));
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        };
        mDatabase.orderByChild("doctorId").equalTo(currentDoctorId).addValueEventListener(mValueEventListener);
    }

    private void updateUI(List<Appointment> schedule) {
        if (isAdded()) {
            requireActivity().runOnUiThread(() -> {
                rvSchedule.setAdapter(new AppointmentAdapter(schedule, true, new AppointmentAdapter.OnAppointmentStatusChangeListener() {
                    @Override
                    public void onStatusChanged(Appointment appointment) {
                        executorService.execute(() -> {
                            db.appointmentDao().update(appointment);
                            mDatabase.child(appointment.getId()).setValue(appointment);
                            
                            // Send notification to patient about status change
                            DatabaseReference notifRef = FirebaseDatabase.getInstance().getReference("notifications").child(appointment.getPatientId()).push();
                            String title = "Appointment Update";
                            String msg = "Your appointment with " + appointment.getDoctorName() + " is now " + appointment.getStatus();
                            if ("Rescheduled".equalsIgnoreCase(appointment.getStatus())) {
                                title = "Urgent: Appointment Rescheduled";
                                msg = "Your appointment with " + appointment.getDoctorName() + " has been rescheduled. Check details in 'My Appointments'.";
                            }
                            
                            com.example.ummatelemedicineapp.models.Notification notification = new com.example.ummatelemedicineapp.models.Notification(
                                title, msg, new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(new java.util.Date()),
                                false, "Rescheduled".equalsIgnoreCase(appointment.getStatus())
                            );
                            notifRef.setValue(notification);
                        });
                    }

                    @Override
                    public void onAppointmentClicked(Appointment appointment) {
                        getParentFragmentManager().beginTransaction()
                                .replace(R.id.doctor_fragment_container, PatientDetailFragment.newInstance(appointment.getId()))
                                .addToBackStack(null)
                                .commit();
                    }
                }));
            });
        }
    }

    private void loadSchedule(RecyclerView recyclerView) {
        com.google.firebase.auth.FirebaseUser currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;
        String currentDoctorId = currentUser.getUid();

        db.appointmentDao().getUpcomingAppointmentsByDoctorLive(currentDoctorId).observe(getViewLifecycleOwner(), schedule -> {
            if (schedule != null) {
                updateUI(schedule);
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mDatabase != null && mValueEventListener != null) {
            com.google.firebase.auth.FirebaseUser currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                mDatabase.orderByChild("doctorId").equalTo(currentUser.getUid()).removeEventListener(mValueEventListener);
            }
        }
        executorService.shutdown();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getView() != null) {
            loadSchedule(getView().findViewById(R.id.rvDoctorSchedule));
        }
    }
}