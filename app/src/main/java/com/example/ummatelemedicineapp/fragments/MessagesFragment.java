package com.example.ummatelemedicineapp.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ummatelemedicineapp.ChatActivity;
import com.example.ummatelemedicineapp.R;
import com.example.ummatelemedicineapp.adapters.MessageAdapter;
import com.example.ummatelemedicineapp.database.AppDatabase;
import com.example.ummatelemedicineapp.database.ChatMessageDao;
import com.example.ummatelemedicineapp.models.Appointment;
import com.example.ummatelemedicineapp.models.ChatMessage;
import com.example.ummatelemedicineapp.models.Message;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;

public class MessagesFragment extends Fragment {
    private MessageAdapter adapter;
    private List<Message> messageList = new ArrayList<>();
    private ChatMessageDao chatMessageDao;
    private String currentUserId;
    private Map<String, String> nameCache = new HashMap<>();

    private DatabaseReference userConvsRef;
    private ValueEventListener convsListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_messages, container, false);
        
        chatMessageDao = AppDatabase.getInstance(getContext()).chatMessageDao();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        userConvsRef = FirebaseDatabase.getInstance().getReference("user_conversations").child(currentUserId);
        
        RecyclerView rvMessages = view.findViewById(R.id.rvMessages);
        rvMessages.setLayoutManager(new LinearLayoutManager(getContext()));
        
        adapter = new MessageAdapter(messageList, message -> {
            Intent intent = new Intent(getActivity(), ChatActivity.class);
            intent.putExtra("sender_name", message.getSenderName());
            intent.putExtra("doctor_id", message.getOtherUserId());
            intent.putExtra("conversation_id", ChatMessage.generateConversationId(currentUserId, message.getOtherUserId()));
            startActivity(intent);
        });
        rvMessages.setAdapter(adapter);

        SearchView searchView = view.findViewById(R.id.searchMessagesPatient);
        if (searchView != null) {
            searchView.setIconifiedByDefault(false);
            searchView.setSubmitButtonEnabled(true);
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    adapter.filter(query);
                    searchView.clearFocus();
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    adapter.filter(newText);
                    return true;
                }
            });
        }
        
        prepopulateNameCache();
        loadMessages();
        
        return view;
    }

    private void prepopulateNameCache() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Appointment> appointments = AppDatabase.getInstance(getContext()).appointmentDao().getAllAppointments();
            if (appointments != null) {
                for (Appointment appt : appointments) {
                    if (appt.getPatientId() != null && appt.getPatientName() != null) {
                        nameCache.put(appt.getPatientId(), appt.getPatientName());
                    }
                    if (appt.getDoctorId() != null && appt.getDoctorName() != null) {
                        nameCache.put(appt.getDoctorId(), appt.getDoctorName());
                    }
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        startListeningForConversations();
        loadMessages();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (userConvsRef != null && convsListener != null) {
            userConvsRef.removeEventListener(convsListener);
        }
        // Also remove listeners for individual chats if we had multiple, 
        // but for now, they are created in syncLastMessage without keeping references.
        // In a real app, we should track them.
    }

    private void startListeningForConversations() {
        convsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot child : snapshot.getChildren()) {
                    String otherUserId = child.getKey();
                    syncLastMessage(otherUserId);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        userConvsRef.addValueEventListener(convsListener);
    }

    private void syncLastMessage(String otherUserId) {
        String convId = ChatMessage.generateConversationId(currentUserId, otherUserId);
        FirebaseDatabase.getInstance().getReference("chats").child(convId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot data : snapshot.getChildren()) {
                            ChatMessage msg = data.getValue(ChatMessage.class);
                            if (msg != null) {
                                msg.setFirebaseId(data.getKey());
                                msg.setConversationId(convId);
                                Executors.newSingleThreadExecutor().execute(() -> {
                                    chatMessageDao.insert(msg);
                                    if (isAdded() && getActivity() != null) {
                                        getActivity().runOnUiThread(() -> loadMessages());
                                    }
                                });
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void loadMessages() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<String> conversationIds = chatMessageDao.getAllConversationIdsForUser(currentUserId);
            List<Message> updatedList = new ArrayList<>();

            for (String convId : conversationIds) {
                ChatMessage lastDbMsg = chatMessageDao.getLastMessageForConversation(convId);
                int unreadCount = chatMessageDao.getUnreadCountForConversation(convId, currentUserId);
                
                if (lastDbMsg != null) {
                    String otherUserId = lastDbMsg.getSenderId().equals(currentUserId) ? lastDbMsg.getReceiverId() : lastDbMsg.getSenderId();
                    String timeStr = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date(lastDbMsg.getTimestamp()));

                    Message message = new Message(
                            otherUserId,
                            nameCache.getOrDefault(otherUserId, "Loading..."),
                            lastDbMsg.getSenderId().equals(currentUserId) ? getString(R.string.message_prefix_you, lastDbMsg.getMessage()) : lastDbMsg.getMessage(),
                            timeStr,
                            R.drawable.ic_doctor_avatar,
                            unreadCount
                    );
                    updatedList.add(message);
                    
                    if (!nameCache.containsKey(otherUserId)) {
                        resolveName(otherUserId);
                    }
                }
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    messageList.clear();
                    messageList.addAll(updatedList);
                    adapter.updateList(messageList);
                });
            }
        });
    }

    private void resolveName(String userId) {
        FirebaseDatabase.getInstance().getReference("users").child(userId).child("name")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String name = snapshot.getValue(String.class);
                        if (name != null) {
                            nameCache.put(userId, name);
                            if (isAdded()) {
                                loadMessages(); // Reload with the real name
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }
}
