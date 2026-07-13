package com.example.ummatelemedicineapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ummatelemedicineapp.R;
import com.example.ummatelemedicineapp.models.Appointment;

import java.util.List;

public class AppointmentAdapter extends RecyclerView.Adapter<AppointmentAdapter.AppointmentViewHolder> {

    public interface OnAppointmentStatusChangeListener {
        void onStatusChanged(Appointment appointment);
        default void onAppointmentClicked(Appointment appointment) {}
    }

    private List<Appointment> appointments;
    private boolean isStaffView;
    private OnAppointmentStatusChangeListener statusChangeListener;

    public AppointmentAdapter(List<Appointment> appointments) {
        this(appointments, false, null);
    }

    public AppointmentAdapter(List<Appointment> appointments, boolean isStaffView) {
        this(appointments, isStaffView, null);
    }

    public AppointmentAdapter(List<Appointment> appointments, boolean isStaffView, OnAppointmentStatusChangeListener listener) {
        this.appointments = appointments;
        this.isStaffView = isStaffView;
        this.statusChangeListener = listener;
    }

    @NonNull
    @Override
    public AppointmentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new AppointmentViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.item_appointment, parent, false
                )
        );
    }

    @Override
    public void onBindViewHolder(@NonNull AppointmentViewHolder holder, int position) {
        holder.setAppointmentData(appointments.get(position), isStaffView, statusChangeListener);
    }

    @Override
    public int getItemCount() {
        return appointments.size();
    }

    public void updateList(List<Appointment> newList) {
        this.appointments = newList;
        notifyDataSetChanged();
    }

    static class AppointmentViewHolder extends RecyclerView.ViewHolder {
        private TextView tvDoctor, tvSpecialty, tvStatus, tvDate, tvTime, tvPayment;
        private View layoutActions;
        private Button btnAttend, btnNotAttend, btnReschedule, btnConfirm, btnChatNow;

        AppointmentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDoctor = itemView.findViewById(R.id.tvAppointDoctor);
            tvSpecialty = itemView.findViewById(R.id.tvAppointSpecialty);
            tvStatus = itemView.findViewById(R.id.tvAppointStatus);
            tvDate = itemView.findViewById(R.id.tvAppointDate);
            tvTime = itemView.findViewById(R.id.tvAppointTime);
            tvPayment = itemView.findViewById(R.id.tvPaymentBadge);
            layoutActions = itemView.findViewById(R.id.layoutActionButtons);
            btnAttend = itemView.findViewById(R.id.btnAttend);
            btnNotAttend = itemView.findViewById(R.id.btnNotAttend);
            btnReschedule = itemView.findViewById(R.id.btnReschedule);
            btnConfirm = itemView.findViewById(R.id.btnConfirm);
            btnChatNow = itemView.findViewById(R.id.btnChatNow);
        }

        void setAppointmentData(Appointment appointment, boolean isStaff, OnAppointmentStatusChangeListener listener) {
            if (isStaff) {
                tvDoctor.setText(appointment.getPatientName());
                tvSpecialty.setText(itemView.getContext().getString(R.string.consultation_type_label, appointment.getType()));
            } else {
                tvDoctor.setText(appointment.getDoctorName());
                tvSpecialty.setText(appointment.getSpecialty());
            }
            tvStatus.setText(appointment.getStatus());
            tvDate.setText(appointment.getDate());
            tvTime.setText(appointment.getTime());
            
            // Payment Badge logic
            String payment = appointment.getPaymentStatus();
            tvPayment.setText(payment);
            if (payment != null && payment.startsWith("Paid")) {
                tvPayment.setBackgroundResource(R.drawable.bg_status_confirmed);
                tvPayment.setTextColor(itemView.getContext().getResources().getColor(android.R.color.holo_green_dark));
            } else {
                tvPayment.setBackgroundResource(R.drawable.bg_status_pending);
                tvPayment.setTextColor(itemView.getContext().getResources().getColor(R.color.primary_blue));
            }

            // Status Badge logic
            updateStatusBadge(appointment.getStatus());

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onAppointmentClicked(appointment);
                }
            });

            // Join Telehealth Button
            Button btnJoinTelehealth = itemView.findViewById(R.id.btnJoinTelehealth);
            if (!appointment.isPast() && "Confirmed".equalsIgnoreCase(appointment.getStatus())) {
                btnJoinTelehealth.setVisibility(View.VISIBLE);
                btnJoinTelehealth.setOnClickListener(v -> {
                    android.content.Intent intent = new android.content.Intent(itemView.getContext(), com.example.ummatelemedicineapp.TelehealthLobbyActivity.class);
                    intent.putExtra("appointment_id", appointment.getId());
                    if (isStaff) {
                        intent.putExtra("display_name", appointment.getPatientName());
                        intent.putExtra("display_role", "Patient | Virtual Consultation");
                    } else {
                        intent.putExtra("display_name", appointment.getDoctorName());
                        intent.putExtra("display_role", appointment.getSpecialty() + " | Virtual Room");
                    }
                    itemView.getContext().startActivity(intent);
                });
            } else {
                btnJoinTelehealth.setVisibility(View.GONE);
            }

            // Staff Actions logic
            if (isStaff && !appointment.isPast()) {
                layoutActions.setVisibility(View.VISIBLE);
                
                if ("Pending".equalsIgnoreCase(appointment.getStatus())) {
                    btnConfirm.setVisibility(View.VISIBLE);
                    btnAttend.setVisibility(View.GONE);
                    btnNotAttend.setVisibility(View.GONE);
                    btnReschedule.setVisibility(View.VISIBLE);
                    
                    btnConfirm.setOnClickListener(v -> {
                        appointment.setStatus("Confirmed");
                        updateStatusBadge("Confirmed");
                        if (listener != null) listener.onStatusChanged(appointment);
                        Toast.makeText(itemView.getContext(), "Appointment Confirmed", Toast.LENGTH_SHORT).show();
                    });
                } else if ("Confirmed".equalsIgnoreCase(appointment.getStatus())) {
                    btnConfirm.setVisibility(View.GONE);
                    btnAttend.setVisibility(View.VISIBLE);
                    btnNotAttend.setVisibility(View.VISIBLE);
                    btnReschedule.setVisibility(View.VISIBLE);
                } else {
                    layoutActions.setVisibility(View.GONE);
                }
                
                btnAttend.setOnClickListener(v -> {
                    appointment.setStatus("Attended");
                    updateStatusBadge("Attended");
                    layoutActions.setVisibility(View.GONE);
                    if (listener != null) {
                        listener.onStatusChanged(appointment);
                        listener.onAppointmentClicked(appointment);
                    }
                    Toast.makeText(itemView.getContext(), "Attendance Confirmed", Toast.LENGTH_SHORT).show();
                });

                btnNotAttend.setOnClickListener(v -> {
                    appointment.setStatus("No Show");
                    updateStatusBadge("No Show");
                    layoutActions.setVisibility(View.GONE);
                    if (listener != null) listener.onStatusChanged(appointment);
                    Toast.makeText(itemView.getContext(), "Marked as No Show", Toast.LENGTH_SHORT).show();
                });

                btnChatNow.setOnClickListener(v -> {
                    android.content.Intent intent = new android.content.Intent(itemView.getContext(), com.example.ummatelemedicineapp.ChatActivity.class);
                    intent.putExtra("patient_id", appointment.getPatientId());
                    intent.putExtra("sender_name", appointment.getPatientName());
                    String currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
                    intent.putExtra("conversation_id", com.example.ummatelemedicineapp.models.ChatMessage.generateConversationId(currentUserId, appointment.getPatientId()));
                    itemView.getContext().startActivity(intent);
                });

                btnReschedule.setOnClickListener(v -> {
                    // Simulate Emergency Reschedule
                    String newDate = "Rescheduled (TBD)";
                    appointment.setDate(newDate);
                    appointment.setStatus("Rescheduled");
                    
                    tvDate.setText(newDate);
                    updateStatusBadge("Rescheduled");
                    layoutActions.setVisibility(View.GONE);
                    
                    if (listener != null) listener.onStatusChanged(appointment);
                    Toast.makeText(itemView.getContext(), "Emergency Reschedule Initiated. Patient will be notified.", Toast.LENGTH_LONG).show();
                });
            } else {
                layoutActions.setVisibility(View.GONE);
            }
        }

        private void updateStatusBadge(String status) {
            tvStatus.setText(status);
            if ("Confirmed".equalsIgnoreCase(status)) {
                tvStatus.setBackgroundResource(R.drawable.bg_status_confirmed);
                tvStatus.setTextColor(itemView.getContext().getResources().getColor(android.R.color.holo_green_dark));
            } else if ("Attended".equalsIgnoreCase(status) || "Completed".equalsIgnoreCase(status)) {
                tvStatus.setBackgroundResource(R.drawable.bg_status_confirmed);
                tvStatus.setTextColor(itemView.getContext().getResources().getColor(android.R.color.holo_green_dark));
            } else if ("Rescheduled".equalsIgnoreCase(status)) {
                tvStatus.setBackgroundResource(R.drawable.bg_status_pending);
                tvStatus.setTextColor(itemView.getContext().getResources().getColor(android.R.color.holo_orange_dark));
            } else {
                tvStatus.setBackgroundResource(R.drawable.bg_status_pending);
                tvStatus.setTextColor(itemView.getContext().getResources().getColor(R.color.primary_blue));
            }
        }
    }
}