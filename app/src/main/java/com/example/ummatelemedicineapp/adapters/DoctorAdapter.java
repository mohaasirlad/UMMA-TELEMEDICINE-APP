package com.example.ummatelemedicineapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ummatelemedicineapp.R;
import com.example.ummatelemedicineapp.models.Doctor;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.List;

public class DoctorAdapter extends RecyclerView.Adapter<DoctorAdapter.DoctorViewHolder> {

    public interface OnDoctorClickListener {
        void onDoctorClick(Doctor doctor);
    }

    private List<Doctor> doctors;
    private OnDoctorClickListener listener;

    public DoctorAdapter(List<Doctor> doctors, OnDoctorClickListener listener) {
        this.doctors = doctors;
        this.listener = listener;
    }

    public void updateList(List<Doctor> newDoctors) {
        this.doctors = newDoctors;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DoctorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new DoctorViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.item_doctor, parent, false
                ),
                listener,
                doctors
        );
    }

    @Override
    public void onBindViewHolder(@NonNull DoctorViewHolder holder, int position) {
        holder.setDoctorData(doctors.get(position));
    }

    @Override
    public int getItemCount() {
        return doctors.size();
    }

    static class DoctorViewHolder extends RecyclerView.ViewHolder {
        private ShapeableImageView ivDoctor;
        private TextView tvName, tvSpecialty, tvRating, tvAvailability;

        DoctorViewHolder(@NonNull View itemView, OnDoctorClickListener listener, List<Doctor> doctors) {
            super(itemView);
            ivDoctor = itemView.findViewById(R.id.ivDoctor);
            tvName = itemView.findViewById(R.id.tvDoctorName);
            tvSpecialty = itemView.findViewById(R.id.tvSpecialty);
            tvRating = itemView.findViewById(R.id.tvRating);
            tvAvailability = itemView.findViewById(R.id.tvAvailability);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onDoctorClick(doctors.get(position));
                }
            });
        }

        void setDoctorData(Doctor doctor) {
            tvName.setText(doctor.getName());
            tvSpecialty.setText(doctor.getSpecialty());
            tvRating.setText(String.valueOf(doctor.getRating()));
            tvAvailability.setText(doctor.getAvailability());
            ivDoctor.setImageResource(doctor.getImageResId());
        }
    }
}