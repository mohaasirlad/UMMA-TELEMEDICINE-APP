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
import com.example.ummatelemedicineapp.adapters.NotificationAdapter;
import com.example.ummatelemedicineapp.database.AppDatabase;

import java.util.concurrent.Executors;

public class NotificationsFragment extends Fragment {

    private NotificationAdapter adapter;
    private com.google.firebase.database.DatabaseReference mNotificationDatabase;
    private com.google.firebase.database.ValueEventListener mNotificationEventListener;
    private com.example.ummatelemedicineapp.database.AppDatabase db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notifications, container, false);
        
        db = AppDatabase.getInstance(requireContext());
        RecyclerView rvNotifications = view.findViewById(R.id.rvNotifications);
        rvNotifications.setLayoutManager(new LinearLayoutManager(getContext()));

        listenForRealtimeNotifications();
        
        db.notificationDao().getAllNotificationsLive().observe(getViewLifecycleOwner(), notifications -> {
            if (notifications != null && !notifications.isEmpty()) {
                adapter = new NotificationAdapter(notifications, notification -> {
                    if (!notification.isRead()) {
                        Executors.newSingleThreadExecutor().execute(() -> {
                            db.notificationDao().markAsRead(notification.getId());
                        });
                    }
                    if (notification.getTitle().contains("Call") && notification.getAppointmentId() != null) {
                        android.content.Intent intent = new android.content.Intent(getActivity(), com.example.ummatelemedicineapp.TelehealthLobbyActivity.class);
                        intent.putExtra("appointment_id", notification.getAppointmentId());
                        intent.putExtra("display_name", "Ongoing Call");
                        intent.putExtra("display_role", "Telehealth Room");
                        startActivity(intent);
                    } else if ("CHAT".equals(notification.getAppointmentId())) {
                        // Directly open ChatActivity from notification
                        android.content.Intent chatIntent = new android.content.Intent(getActivity(), com.example.ummatelemedicineapp.ChatActivity.class);
                        chatIntent.putExtra("sender_name", notification.getTitle().replace("Message from ", ""));
                        // Re-use doctorId field as the senderId for chat
                        if (notification.getDoctorId() != null) {
                            String role = getContext().getSharedPreferences("UMMA_PREFS", android.content.Context.MODE_PRIVATE).getString("last_role", "");
                            if ("doctor".equals(role)) {
                                chatIntent.putExtra("patient_id", notification.getDoctorId());
                            } else {
                                chatIntent.putExtra("doctor_id", notification.getDoctorId());
                            }
                        }
                        startActivity(chatIntent);
                    }
                });
                rvNotifications.setAdapter(adapter);
                view.findViewById(R.id.tvNoNotifications).setVisibility(View.GONE);
                view.findViewById(R.id.tvMarkAllRead).setVisibility(View.VISIBLE);
            } else {
                view.findViewById(R.id.tvNoNotifications).setVisibility(View.VISIBLE);
                view.findViewById(R.id.tvMarkAllRead).setVisibility(View.GONE);
                rvNotifications.setAdapter(null);
            }
        });

        view.findViewById(R.id.tvMarkAllRead).setOnClickListener(v -> {
            Executors.newSingleThreadExecutor().execute(() -> {
                db.notificationDao().markAllAsRead();
            });
        });
        
        return view;
    }

    private void listenForRealtimeNotifications() {
        com.google.firebase.auth.FirebaseUser currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;
        String currentUserId = currentUser.getUid();

        mNotificationDatabase = com.google.firebase.database.FirebaseDatabase.getInstance().getReference("notifications").child(currentUserId);
        mNotificationEventListener = new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                for (com.google.firebase.database.DataSnapshot data : snapshot.getChildren()) {
                    com.example.ummatelemedicineapp.models.Notification notification = data.getValue(com.example.ummatelemedicineapp.models.Notification.class);
                    if (notification != null) {
                        Executors.newSingleThreadExecutor().execute(() -> {
                            db.notificationDao().insert(notification);
                            data.getRef().removeValue();
                        });
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
            }
        };
        mNotificationDatabase.addValueEventListener(mNotificationEventListener);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mNotificationDatabase != null && mNotificationEventListener != null) {
            mNotificationDatabase.removeEventListener(mNotificationEventListener);
        }
    }
}
