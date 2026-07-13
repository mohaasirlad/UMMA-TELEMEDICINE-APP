package com.example.ummatelemedicineapp.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ummatelemedicineapp.ClinicLocatorActivity;
import com.example.ummatelemedicineapp.DoctorDetailsActivity;
import com.example.ummatelemedicineapp.HomeActivity;
import com.example.ummatelemedicineapp.R;
import com.example.ummatelemedicineapp.SymptomCheckerActivity;
import com.example.ummatelemedicineapp.VideoConsultationActivity;
import com.example.ummatelemedicineapp.adapters.DoctorAdapter;
import com.example.ummatelemedicineapp.database.AppDatabase;
import com.example.ummatelemedicineapp.models.Appointment;
import com.example.ummatelemedicineapp.models.Doctor;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeFragment extends Fragment {

    private ValueEventListener mAppointmentListener;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private DatabaseReference mAppointmentsRef;
    private List<Doctor> allDoctors = new ArrayList<>();
    private DoctorAdapter doctorAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Display User Name and Dynamic Greeting
        TextView tvGreeting = view.findViewById(R.id.tvGreeting);
        TextView tvUserName = view.findViewById(R.id.tvUserName);
        SharedPreferences prefs = requireActivity().getSharedPreferences("UMMA_PREFS", Context.MODE_PRIVATE);
        String name = prefs.getString("user_name", "Patient");
        
        tvGreeting.setText(getTimeBasedGreeting());
        tvUserName.setText(getString(R.string.welcome_user, name));

        com.google.firebase.auth.FirebaseUser currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            com.google.firebase.database.FirebaseDatabase.getInstance().getReference("users")
                    .child(currentUser.getUid())
                    .child("name")
                    .get().addOnSuccessListener(dataSnapshot -> {
                        String dbName = dataSnapshot.getValue(String.class);
                        if (dbName != null) {
                            tvUserName.setText(getString(R.string.welcome_user, dbName));
                            prefs.edit().putString("user_name", dbName).apply();
                        }
                    });
        }


        // Load Profile Image
        android.widget.ImageView ivUserAvatarHome = view.findViewById(R.id.ivUserAvatarHome);
        String imageUriStr = prefs.getString("user_image_uri", null);
        if (imageUriStr != null) {
            try {
                if (imageUriStr.startsWith("/")) {
                    ivUserAvatarHome.setImageURI(android.net.Uri.fromFile(new java.io.File(imageUriStr)));
                } else {
                    ivUserAvatarHome.setImageURI(android.net.Uri.parse(imageUriStr));
                }
            } catch (Exception e) {
                ivUserAvatarHome.setImageResource(R.drawable.ic_patient_avatar);
            }
        } else {
            ivUserAvatarHome.setImageResource(R.drawable.ic_patient_avatar);
        }

        // Navigate to Profile on clicking avatar
        ivUserAvatarHome.setOnClickListener(v -> {
            if (getActivity() instanceof HomeActivity) {
                ((HomeActivity) getActivity()).navigateToTab(R.id.nav_profile);
            }
        });

        // Setup Quick Action Cards
        view.findViewById(R.id.cardBookAppointment).setOnClickListener(v -> {
            if (getActivity() instanceof HomeActivity) {
                ((HomeActivity) getActivity()).navigateToTab(R.id.nav_appointments);
            }
        });

        view.findViewById(R.id.cardEmergency).setOnClickListener(v -> showEmergencyDialog());

        view.findViewById(R.id.cardViewQueue).setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new QueueFragment())
                        .addToBackStack(null)
                        .commit();
            }
        });

        view.findViewById(R.id.cardTelehealth).setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), VideoConsultationActivity.class);
            intent.putExtra("appointment_id", "adhoc_consult_" + System.currentTimeMillis()); // Generic room for ad-hoc telehealth
            startActivity(intent);
        });

        view.findViewById(R.id.cardClinicLocator).setOnClickListener(v -> startActivity(new Intent(getActivity(), ClinicLocatorActivity.class)));

        view.findViewById(R.id.cardSymptomChecker).setOnClickListener(v -> startActivity(new Intent(getActivity(), SymptomCheckerActivity.class)));

        view.findViewById(R.id.btnSymptomChecker).setOnClickListener(v -> startActivity(new Intent(getActivity(), SymptomCheckerActivity.class)));

        // Setup Upcoming Appointment View Details click
        view.findViewById(R.id.tvViewAppointmentDetails).setOnClickListener(v -> {
            // This will be handled dynamically in loadUpcomingAppointment
        });

        // Setup Recommended Doctors RecyclerView
        RecyclerView rvDoctors = view.findViewById(R.id.rvDoctors);
        rvDoctors.setLayoutManager(new LinearLayoutManager(getContext()));

        // Setup Search Bar
        androidx.appcompat.widget.SearchView searchView = view.findViewById(R.id.searchDoctorHome);
        searchView.setIconifiedByDefault(false);
        searchView.setFocusable(false);
        searchView.clearFocus();

        searchView.setOnQueryTextListener(new androidx.appcompat.widget.SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterDoctors(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterDoctors(newText);
                return true;
            }
        });
        
        loadRealDoctors(rvDoctors);
        loadUpcomingAppointment(view);
        
        return view;
    }

    private void loadUpcomingAppointment(View view) {
        String currentUserId = FirebaseAuth.getInstance().getUid();
        if (currentUserId == null) return;

        View cardAppt = view.findViewById(R.id.cardUpcomingAppointment);
        TextView tvDocName = view.findViewById(R.id.tvUpcomingDocName);
        TextView tvSpecialty = view.findViewById(R.id.tvUpcomingDocSpecialty);
        TextView tvDateTime = view.findViewById(R.id.tvUpcomingDateTime);

        mAppointmentsRef = FirebaseDatabase.getInstance().getReference("appointments");
        
        mAppointmentListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot data : snapshot.getChildren()) {
                    Appointment appointment = data.getValue(Appointment.class);
                    if (appointment != null) {
                        executorService.execute(() -> AppDatabase.getInstance(getContext()).appointmentDao().insert(appointment));
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        mAppointmentsRef.orderByChild("patientId").equalTo(currentUserId)
                .addValueEventListener(mAppointmentListener);

        AppDatabase.getInstance(getContext()).appointmentDao().getUpcomingAppointmentsByPatientLive(currentUserId)
                .observe(getViewLifecycleOwner(), appointments -> {
                    if (appointments != null && !appointments.isEmpty()) {
                        Appointment next = appointments.get(0);
                        tvDocName.setText(next.getDoctorName());
                        tvSpecialty.setText(next.getSpecialty());
                        tvDateTime.setText(getString(R.string.date_time_format, next.getDate(), next.getTime()));
                        cardAppt.setVisibility(View.VISIBLE);

                        View btnViewDetails = view.findViewById(R.id.tvViewAppointmentDetails);
                        btnViewDetails.setOnClickListener(v -> {
                            Intent intent = new Intent(getActivity(), DoctorDetailsActivity.class);
                            intent.putExtra("doctor_id", next.getDoctorId());
                            intent.putExtra("doctor_name", next.getDoctorName());
                            intent.putExtra("doctor_specialty", next.getSpecialty());
                            intent.putExtra("doctor_image", R.drawable.ic_doctor_avatar);
                            startActivity(intent);
                        });

                        cardAppt.setOnClickListener(v -> {
                            btnViewDetails.performClick();
                        });
                        
                        // Show "Start Consultation" button if status is Confirmed
                        View btnStartConsult = view.findViewById(R.id.btnStartConsultation);
                        if (btnStartConsult != null) {
                            if ("Confirmed".equalsIgnoreCase(next.getStatus())) {
                                btnStartConsult.setVisibility(View.VISIBLE);
                                btnStartConsult.setOnClickListener(v -> {
                                    Intent intent = new Intent(getActivity(), VideoConsultationActivity.class);
                                    intent.putExtra("appointment_id", next.getId());
                                    intent.putExtra("doctor_id", next.getDoctorId());
                                    intent.putExtra("doctor_name", next.getDoctorName());
                                    startActivity(intent);
                                });
                            } else {
                                btnStartConsult.setVisibility(View.GONE);
                            }
                        }
                    } else {
                        cardAppt.setVisibility(View.GONE);
                    }
                });
    }

    private void loadRealDoctors(RecyclerView rvDoctors) {
        com.google.firebase.auth.FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            android.util.Log.e("HomeFragment", "User not authenticated, skipping doctor load");
            return;
        }

        // Initialize adapter early with empty list to avoid null checks during search
        if (doctorAdapter == null) {
            doctorAdapter = new DoctorAdapter(new ArrayList<>(allDoctors), doctor -> {
                Intent intent = new Intent(getActivity(), com.example.ummatelemedicineapp.DoctorDetailsActivity.class);
                intent.putExtra("doctor_id", doctor.getId());
                intent.putExtra("doctor_name", doctor.getName());
                intent.putExtra("doctor_specialty", doctor.getSpecialty());
                intent.putExtra("doctor_image", doctor.getImageResId());
                startActivity(intent);
            });
            rvDoctors.setAdapter(doctorAdapter);
        }

        FirebaseDatabase.getInstance().getReference("users")
                .orderByChild("role")
                .equalTo("doctor")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        allDoctors.clear();
                        for (DataSnapshot data : snapshot.getChildren()) {
                            try {
                                String id = data.getKey();
                                String name = data.child("name").getValue(String.class);
                                String specialty = data.child("specialty").getValue(String.class);
                                
                                if (name == null) continue;
                                if (specialty == null) specialty = "General Physician";
                                
                                float rating = 5.0f;
                                Object ratingObj = data.child("rating").getValue();
                                if (ratingObj instanceof Double) {
                                    rating = ((Double) ratingObj).floatValue();
                                } else if (ratingObj instanceof Long) {
                                    rating = ((Long) ratingObj).floatValue();
                                }

                                String availability = data.child("availability").getValue(String.class);
                                if (availability == null) availability = "Available";
                                
                                Doctor doctor = new Doctor(id, name, specialty, availability, rating, R.drawable.ic_doctor_avatar);
                                allDoctors.add(doctor);
                            } catch (Exception e) {
                                android.util.Log.e("HomeFragment", "Error parsing doctor data: " + e.getMessage());
                            }
                        }
                        
                        if (isAdded() && getActivity() != null) {
                            if (doctorAdapter == null) {
                                doctorAdapter = new DoctorAdapter(new ArrayList<>(allDoctors), doctor -> {
                                    Intent intent = new Intent(getActivity(), DoctorDetailsActivity.class);
                                    intent.putExtra("doctor_id", doctor.getId());
                                    intent.putExtra("doctor_name", doctor.getName());
                                    intent.putExtra("doctor_specialty", doctor.getSpecialty());
                                    intent.putExtra("doctor_image", doctor.getImageResId());
                                    startActivity(intent);
                                });
                                rvDoctors.setAdapter(doctorAdapter);
                            } else {
                                doctorAdapter.updateList(new ArrayList<>(allDoctors));
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        android.util.Log.e("FirebaseError", "Database Error: " + error.getMessage());
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Failed to load doctors: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void filterDoctors(String query) {
        List<Doctor> filteredList = new ArrayList<>();
        for (Doctor doctor : allDoctors) {
            if (doctor.getName().toLowerCase().contains(query.toLowerCase()) ||
                doctor.getSpecialty().toLowerCase().contains(query.toLowerCase())) {
                filteredList.add(doctor);
            }
        }
        if (doctorAdapter != null) {
            doctorAdapter.updateList(filteredList);
        }
    }

    private void showEmergencyDialog() {
        if (getContext() == null) return;
        
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(getContext());
        builder.setTitle(R.string.emergency_dialog_title);
        builder.setMessage(R.string.emergency_dialog_message);

        builder.setPositiveButton(R.string.emergency_call_1199, (dialog, which) -> makeCall("1199"));
        builder.setNeutralButton(R.string.emergency_call_112, (dialog, which) -> makeCall("112"));
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss());

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();
        
        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.emergency_red));
    }

    private void makeCall(String number) {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(android.net.Uri.parse("tel:" + number));
        startActivity(intent);
    }

    private String getTimeBasedGreeting() {
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        if (hour < 12) {
            return getString(R.string.greet_morning);
        } else if (hour < 16) {
            return getString(R.string.greet_afternoon);
        } else {
            return getString(R.string.greet_evening);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mAppointmentsRef != null && mAppointmentListener != null) {
            mAppointmentsRef.removeEventListener(mAppointmentListener);
        }
        executorService.shutdown();
    }
}
