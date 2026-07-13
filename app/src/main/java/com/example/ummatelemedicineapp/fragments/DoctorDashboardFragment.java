package com.example.ummatelemedicineapp.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ummatelemedicineapp.DoctorHomeActivity;
import com.example.ummatelemedicineapp.R;
import com.example.ummatelemedicineapp.adapters.AppointmentAdapter;
import com.example.ummatelemedicineapp.database.AppDatabase;
import com.example.ummatelemedicineapp.models.Appointment;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.lifecycle.ViewModelProvider;
import com.example.ummatelemedicineapp.viewmodels.ProfileViewModel;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class DoctorDashboardFragment extends Fragment {

    private TextView tvTodayCount, tvPendingCount, tvNextPatientStatus, tvDoctorName, tvDoctorShiftTime, tvDoctorGreeting;
    private ImageView ivDoctorAvatar;
    private RecyclerView rvAppointments;
    private AppDatabase db;
    private ProfileViewModel profileViewModel;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private DatabaseReference mDatabase;
    private ValueEventListener mValueEventListener;
    private DatabaseReference mNotificationDatabase;
    private ValueEventListener mNotificationEventListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_doctor_dashboard, container, false);

        db = AppDatabase.getInstance(requireContext());
        profileViewModel = new ViewModelProvider(requireActivity()).get(ProfileViewModel.class);

        tvTodayCount = view.findViewById(R.id.tvDoctorTodayCount);
        tvPendingCount = view.findViewById(R.id.tvDoctorPendingCount);
        tvNextPatientStatus = view.findViewById(R.id.tvNextPatientStatus);
        tvDoctorName = view.findViewById(R.id.tvDoctorDashboardTitle);
        tvDoctorGreeting = view.findViewById(R.id.tvDoctorGreeting);
        tvDoctorShiftTime = view.findViewById(R.id.tvDoctorShiftTime);
        ivDoctorAvatar = view.findViewById(R.id.ivDoctorAvatarDashboard);
        
        rvAppointments = view.findViewById(R.id.rvDoctorAppointments);
        rvAppointments.setLayoutManager(new LinearLayoutManager(getContext()));

        mDatabase = FirebaseDatabase.getInstance().getReference("appointments");
        mNotificationDatabase = FirebaseDatabase.getInstance().getReference("notifications");
        listenForRealtimeAppointments();
        listenForRealtimeNotifications();

        // Initial Load from Prefs
        SharedPreferences prefs = requireActivity().getSharedPreferences("UMMA_PREFS", Context.MODE_PRIVATE);
        String docName = prefs.getString("doctor_name", "Sarah Wilson");
        String profileUri = prefs.getString("doctor_profile_uri", null);
        String shift = prefs.getString("doctor_shift", "08:00 AM - 04:00 PM");

        com.google.firebase.auth.FirebaseUser currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            com.google.firebase.database.FirebaseDatabase.getInstance().getReference("users")
                    .child(currentUser.getUid())
                    .child("name")
                    .get().addOnSuccessListener(dataSnapshot -> {
                        String dbName = dataSnapshot.getValue(String.class);
                        if (dbName != null) {
                            tvDoctorName.setText(getString(R.string.welcome_doctor, dbName));
                            prefs.edit().putString("doctor_name", dbName).apply();
                        }
                    });
        }

        updateHeader(docName, profileUri, shift);


        // Observe ViewModel for changes from ProfileFragment
        profileViewModel.getDoctorName().observe(getViewLifecycleOwner(), name -> {
            if (name != null) tvDoctorName.setText(getString(R.string.welcome_doctor, name));
        });

        profileViewModel.getProfileImageUri().observe(getViewLifecycleOwner(), uri -> {
            if (uri != null && ivDoctorAvatar != null) {
                try {
                    ivDoctorAvatar.setImageURI(Uri.parse(uri));
                } catch (Exception e) {
                    ivDoctorAvatar.setImageResource(R.drawable.ic_doctor_avatar);
                }
            }
        });

        profileViewModel.getDoctorShift().observe(getViewLifecycleOwner(), newShift -> {
            if (newShift != null && tvDoctorShiftTime != null) {
                tvDoctorShiftTime.setText(getString(R.string.current_shift_label, newShift));
            }
        });

        setupObservers();

        // Quick Actions Logic
        view.findViewById(R.id.cardPatientRecords).setOnClickListener(v -> showPatientRecords());
        view.findViewById(R.id.cardDirectMessages).setOnClickListener(v -> {
            if (getActivity() instanceof DoctorHomeActivity) {
                ((DoctorHomeActivity) getActivity()).navigateToTab(R.id.nav_doctor_messages);
            }
        });
        view.findViewById(R.id.cardEarningsReports).setOnClickListener(v -> showEarningsReports());
        view.findViewById(R.id.cardManageSchedule).setOnClickListener(v -> {
            if (getActivity() instanceof DoctorHomeActivity) {
                ((DoctorHomeActivity) getActivity()).navigateToTab(R.id.nav_doctor_appointments);
            }
        });

        view.findViewById(R.id.cardDoctorAlerts).setOnClickListener(v -> showNotifications());

        return view;
    }

    private void listenForRealtimeAppointments() {
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

    private void listenForRealtimeNotifications() {
        com.google.firebase.auth.FirebaseUser currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;
        String currentDoctorId = currentUser.getUid();

        mNotificationEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot data : snapshot.getChildren()) {
                    com.example.ummatelemedicineapp.models.Notification notification = data.getValue(com.example.ummatelemedicineapp.models.Notification.class);
                    if (notification != null) {
                        executorService.execute(() -> {
                            db.notificationDao().insert(notification);
                            // Once saved locally, we can remove from Firebase to avoid re-fetching
                            data.getRef().removeValue();
                        });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        };
        mNotificationDatabase.child(currentDoctorId).addValueEventListener(mNotificationEventListener);
    }

    private void updateAppointmentUI(List<Appointment> appointments) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                rvAppointments.setAdapter(new AppointmentAdapter(appointments, true, new AppointmentAdapter.OnAppointmentStatusChangeListener() {
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
                if (!appointments.isEmpty()) {
                    tvNextPatientStatus.setText(getString(R.string.next_patient_status_label, appointments.get(0).getPatientName(), appointments.get(0).getTime()));
                } else {
                    tvNextPatientStatus.setText(R.string.no_more_patients);
                }
            });
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        com.google.firebase.auth.FirebaseUser currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (mDatabase != null && mValueEventListener != null) {
            if (currentUser != null) {
                mDatabase.orderByChild("doctorId").equalTo(currentUser.getUid()).removeEventListener(mValueEventListener);
            }
        }
        if (mNotificationDatabase != null && mNotificationEventListener != null) {
            if (currentUser != null) {
                mNotificationDatabase.child(currentUser.getUid()).removeEventListener(mNotificationEventListener);
            }
        }
    }

    private void updateHeader(String name, String uri, String shift) {
        if (tvDoctorGreeting != null) tvDoctorGreeting.setText(getTimeBasedGreeting());
        if (tvDoctorName != null) tvDoctorName.setText(getString(R.string.welcome_doctor, name));
        if (tvDoctorShiftTime != null) tvDoctorShiftTime.setText(getString(R.string.current_shift_label, shift));
        if (uri != null && ivDoctorAvatar != null) {
            try {
                ivDoctorAvatar.setImageURI(Uri.parse(uri));
            } catch (Exception e) {
                ivDoctorAvatar.setImageResource(R.drawable.ic_doctor_avatar);
            }
        }
    }

    private String getTimeBasedGreeting() {
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        if (hour >= 0 && hour < 12) {
            return getString(R.string.greet_morning);
        } else if (hour >= 12 && hour < 16) {
            return getString(R.string.greet_afternoon);
        } else {
            return getString(R.string.greet_evening);
        }
    }

    private void setupObservers() {
        com.google.firebase.auth.FirebaseUser currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;
        String currentDoctorId = currentUser.getUid();

        db.appointmentDao().getTodayCountByDoctorLive(currentDoctorId).observe(getViewLifecycleOwner(), count -> {
            tvTodayCount.setText(String.format(Locale.getDefault(), "%02d", count != null ? count : 0));
        });

        db.appointmentDao().getPendingCountByDoctorLive(currentDoctorId).observe(getViewLifecycleOwner(), count -> {
            tvPendingCount.setText(String.format(Locale.getDefault(), "%02d", count != null ? count : 0));
        });

        db.appointmentDao().getUpcomingAppointmentsByDoctorLive(currentDoctorId).observe(getViewLifecycleOwner(), appointments -> {
            if (appointments != null) {
                updateAppointmentUI(appointments);
            }
        });

        db.notificationDao().getUnreadCountLive().observe(getViewLifecycleOwner(), count -> {
            View fragmentView = getView();
            if (fragmentView == null) return;
            
            TextView tvBadge = fragmentView.findViewById(R.id.tvAlertBadgeCount);
            if (tvBadge != null) {
                if (count != null && count > 0) {
                    tvBadge.setText(String.valueOf(count));
                    tvBadge.setVisibility(View.VISIBLE);
                } else {
                    tvBadge.setVisibility(View.GONE);
                }
            }
        });

        db.notificationDao().getAllNotificationsLive().observe(getViewLifecycleOwner(), notifications -> {
            View fragmentView = getView();
            if (fragmentView == null) return;
            
            TextView tvAlert = fragmentView.findViewById(R.id.tvLatestNotification);
            if (tvAlert != null) {
                if (notifications != null && !notifications.isEmpty()) {
                    tvAlert.setText(notifications.get(0).getMessage());
                } else {
                    tvAlert.setText(R.string.no_new_notifications);
                }
            }
        });
    }

    private void showPatientRecords() {
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.doctor_fragment_container, new PatientRecordsFragment())
                    .addToBackStack(null)
                    .commit();
        }
    }

    private void showEarningsReports() {
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.doctor_fragment_container, new EarningsReportsFragment())
                    .addToBackStack(null)
                    .commit();
        }
    }

    private void showNotifications() {
        executorService.execute(() -> {
            List<com.example.ummatelemedicineapp.models.Notification> notifications = db.notificationDao().getAllNotifications();
            if (notifications == null || notifications.isEmpty()) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> Toast.makeText(getContext(), R.string.no_new_notifications, Toast.LENGTH_SHORT).show());
                }
                return;
            }

            String[] items = new String[notifications.size()];
            for (int i = 0; i < notifications.size(); i++) {
                com.example.ummatelemedicineapp.models.Notification n = notifications.get(i);
                items[i] = (n.isRead() ? "" : "● ") + n.getTitle() + ": " + n.getMessage();
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Doctor Notifications")
                            .setItems(items, (dialog, which) -> {
                                com.example.ummatelemedicineapp.models.Notification selected = notifications.get(which);
                                executorService.execute(() -> db.notificationDao().markAsRead(selected.getId()));
                                Toast.makeText(getContext(), "Marked as read", Toast.LENGTH_SHORT).show();
                            })
                            .setPositiveButton("Clear All", (dialog, which) -> {
                                executorService.execute(() -> db.notificationDao().deleteAll());
                            })
                            .setNegativeButton("Close", null)
                            .show();
                });
            }
        });
    }
}
