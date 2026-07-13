package com.example.ummatelemedicineapp.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ummatelemedicineapp.R;
import com.example.ummatelemedicineapp.adapters.AppointmentAdapter;
import com.example.ummatelemedicineapp.database.AppDatabase;
import com.example.ummatelemedicineapp.models.Appointment;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PatientRecordsFragment extends Fragment {

    private RecyclerView rvRecords;
    private TextView tvNoRecords;
    private AppointmentAdapter adapter;
    private List<Appointment> allRecords = new ArrayList<>();
    private AppDatabase db;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_patient_records, container, false);

        Toolbar toolbar = view.findViewById(R.id.toolbarPatientRecords);
        toolbar.setNavigationOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        rvRecords = view.findViewById(R.id.rvPatientRecords);
        tvNoRecords = view.findViewById(R.id.tvNoRecords);
        rvRecords.setLayoutManager(new LinearLayoutManager(getContext()));
        
        db = AppDatabase.getInstance(requireContext());
        loadRecords();

        EditText etSearch = view.findViewById(R.id.etSearchPatients);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        return view;
    }

    private void filter(String text) {
        List<Appointment> filteredList = new ArrayList<>();
        for (Appointment item : allRecords) {
            String patientName = item.getPatientName();
            String specialty = item.getSpecialty();
            if ((patientName != null && patientName.toLowerCase().contains(text.toLowerCase())) ||
                (specialty != null && specialty.toLowerCase().contains(text.toLowerCase()))) {
                filteredList.add(item);
            }
        }
        
        if (adapter != null) {
            adapter.updateList(filteredList);
        }

        if (filteredList.isEmpty()) {
            tvNoRecords.setVisibility(View.VISIBLE);
            rvRecords.setVisibility(View.GONE);
        } else {
            tvNoRecords.setVisibility(View.GONE);
            rvRecords.setVisibility(View.VISIBLE);
        }
    }

    private void loadRecords() {
        db.appointmentDao().getAllAppointmentsLive().observe(getViewLifecycleOwner(), appointments -> {
            if (appointments != null) {
                allRecords = appointments;
                if (isAdded()) {
                    if (allRecords.isEmpty()) {
                        tvNoRecords.setVisibility(View.VISIBLE);
                        rvRecords.setVisibility(View.GONE);
                    } else {
                        tvNoRecords.setVisibility(View.GONE);
                        rvRecords.setVisibility(View.VISIBLE);
                        if (adapter == null) {
                            adapter = new AppointmentAdapter(allRecords, true, new AppointmentAdapter.OnAppointmentStatusChangeListener() {
                                @Override
                                public void onStatusChanged(Appointment appointment) {
                                    executorService.execute(() -> {
                                        db.appointmentDao().update(appointment);
                                        com.google.firebase.database.FirebaseDatabase.getInstance().getReference("appointments")
                                                .child(appointment.getId()).setValue(appointment);
                                    });
                                }

                                @Override
                                public void onAppointmentClicked(Appointment appointment) {
                                    getParentFragmentManager().beginTransaction()
                                            .replace(R.id.doctor_fragment_container, PatientDetailFragment.newInstance(appointment.getId()))
                                            .addToBackStack(null)
                                            .commit();
                                }
                            });
                            rvRecords.setAdapter(adapter);
                        } else {
                            adapter.updateList(allRecords);
                        }
                    }
                }
            }
        });
    }
}
