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
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class AppointmentsFragment extends Fragment {

    private AppointmentAdapter adapter;
    private RecyclerView rvAppointments;
    private DatabaseReference mDatabase;
    private ValueEventListener mValueEventListener;
    private boolean mShowingPast = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_appointments, container, false);
        
        TabLayout tabLayout = view.findViewById(R.id.tabLayout);
        rvAppointments = view.findViewById(R.id.rvAppointments);
        rvAppointments.setLayoutManager(new LinearLayoutManager(getContext()));
        
        mDatabase = FirebaseDatabase.getInstance().getReference("appointments");
        
        // Initial view: Upcoming appointments
        updateList(false);
        listenForRealtimeUpdates();

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                mShowingPast = (tab.getPosition() != 0);
                updateList(mShowingPast);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
        
        return view;
    }

    private void listenForRealtimeUpdates() {
        com.google.firebase.auth.FirebaseUser currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;
        String currentPatientId = currentUser.getUid();

        mValueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Appointment> schedule = new ArrayList<>();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Appointment appointment = data.getValue(Appointment.class);
                    if (appointment != null && appointment.isPast() == mShowingPast && currentPatientId.equals(appointment.getPatientId())) {
                        schedule.add(appointment);
                    }
                }
                
                if (!schedule.isEmpty()) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            adapter = new AppointmentAdapter(schedule);
                            rvAppointments.setAdapter(adapter);
                        });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        };
        mDatabase.addValueEventListener(mValueEventListener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mDatabase != null && mValueEventListener != null) {
            mDatabase.removeEventListener(mValueEventListener);
        }
    }

    private void updateList(boolean showPast) {
        com.google.firebase.auth.FirebaseUser currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;
        String currentPatientId = currentUser.getUid();

        Executors.newSingleThreadExecutor().execute(() -> {
            List<Appointment> filteredList;
            if (showPast) {
                filteredList = AppDatabase.getInstance(getContext()).appointmentDao().getPastAppointmentsByPatient(currentPatientId);
            } else {
                filteredList = AppDatabase.getInstance(getContext()).appointmentDao().getUpcomingAppointmentsByPatient(currentPatientId);
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    adapter = new AppointmentAdapter(filteredList);
                    rvAppointments.setAdapter(adapter);
                });
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        TabLayout tabLayout = getView().findViewById(R.id.tabLayout);
        if (tabLayout != null) {
            updateList(tabLayout.getSelectedTabPosition() != 0);
        }
    }
}