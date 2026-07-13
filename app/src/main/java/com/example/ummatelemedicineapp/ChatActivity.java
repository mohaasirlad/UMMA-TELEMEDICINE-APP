package com.example.ummatelemedicineapp;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ummatelemedicineapp.adapters.ChatAdapter;
import com.example.ummatelemedicineapp.database.AppDatabase;
import com.example.ummatelemedicineapp.database.ChatMessageDao;
import com.example.ummatelemedicineapp.models.ChatMessage;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class ChatActivity extends BaseActivity {

    private ChatAdapter chatAdapter;
    private List<ChatMessage> chatMessages;
    private RecyclerView rvChat;
    private EditText etMessage;
    private String conversationId;
    private String receiverId;
    private String receiverName;
    private String currentUserId;
    private ChatMessageDao chatMessageDao;
    private DatabaseReference chatRef;
    private ValueEventListener chatListener;
    private LiveData<List<ChatMessage>> messagesLiveData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        receiverId = getIntent().getStringExtra("doctor_id");
        if (receiverId == null) receiverId = getIntent().getStringExtra("patient_id");
        receiverName = getIntent().getStringExtra("sender_name");
        
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        
        if (receiverId == null) {
            // Check if we came from a notification where we only have conversationId
            conversationId = getIntent().getStringExtra("conversation_id");
            if (conversationId != null) {
                String[] parts = conversationId.split("_");
                receiverId = parts[0].equals(currentUserId) ? parts[1] : parts[0];
            }
        }

        if (receiverId == null) {
            Toast.makeText(this, "Error: User ID missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (receiverName == null) {
            resolveReceiverName(receiverId);
        }

        conversationId = ChatMessage.generateConversationId(currentUserId, receiverId);
        
        Toolbar toolbar = findViewById(R.id.toolbarChat);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(receiverName != null ? receiverName : "Chat");
        }
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        chatMessageDao = AppDatabase.getInstance(this).chatMessageDao();
        chatRef = FirebaseDatabase.getInstance().getReference("chats").child(conversationId);

        rvChat = findViewById(R.id.rvChatMessages);
        etMessage = findViewById(R.id.etChatMessage);
        FloatingActionButton btnSend = findViewById(R.id.btnSendMessage);

        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatMessages, currentUserId);
        rvChat.setLayoutManager(new LinearLayoutManager(this));
        rvChat.setAdapter(chatAdapter);
        
        setupMessagesObserver();
        listenForMessages();
        
        btnSend.setOnClickListener(v -> sendMessage());
        
        markAsReadInRoom();
    }

    private void setupMessagesObserver() {
        messagesLiveData = chatMessageDao.getMessagesLiveData(conversationId);
        messagesLiveData.observe(this, messages -> {
            if (messages != null) {
                chatMessages.clear();
                chatMessages.addAll(messages);
                chatAdapter.notifyDataSetChanged();
                if (!chatMessages.isEmpty()) {
                    rvChat.scrollToPosition(chatMessages.size() - 1);
                }
            }
        });
    }

    private void markAsReadInRoom() {
        Executors.newSingleThreadExecutor().execute(() -> {
            chatMessageDao.markAsRead(conversationId, currentUserId);
        });
    }

    private void resolveReceiverName(String userId) {
        FirebaseDatabase.getInstance().getReference("users").child(userId).child("name")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        receiverName = snapshot.getValue(String.class);
                        if (receiverName != null && getSupportActionBar() != null) {
                            getSupportActionBar().setTitle(receiverName);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void listenForMessages() {
        chatListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot data : snapshot.getChildren()) {
                    ChatMessage msg = data.getValue(ChatMessage.class);
                    if (msg != null) {
                        msg.setFirebaseId(data.getKey()); // Set the Firebase ID
                        msg.setConversationId(conversationId);
                        
                        // Save to local Room DB in background
                        final ChatMessage finalMsg = msg;
                        Executors.newSingleThreadExecutor().execute(() -> {
                            chatMessageDao.insert(finalMsg);
                        });
                    }
                }

                // Mark as read in Firebase if I'm the receiver
                for (DataSnapshot data : snapshot.getChildren()) {
                    ChatMessage msg = data.getValue(ChatMessage.class);
                    if (msg != null && msg.getReceiverId().equals(currentUserId) && !msg.isRead()) {
                        data.getRef().child("read").setValue(true);
                        data.getRef().child("status").setValue("read");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        chatRef.addValueEventListener(chatListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (chatRef != null && chatListener != null) {
            chatRef.removeEventListener(chatListener);
        }
    }

    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (!text.isEmpty()) {
            long timestamp = System.currentTimeMillis();
            ChatMessage newMessage = new ChatMessage(currentUserId, receiverId, text, timestamp);
            newMessage.setConversationId(conversationId);
            
            // Push to Firebase
            DatabaseReference newMsgRef = chatRef.push();
            newMessage.setFirebaseId(newMsgRef.getKey()); // Set the key before pushing
            newMsgRef.setValue(newMessage);
            
            // Save to Local immediately
            Executors.newSingleThreadExecutor().execute(() -> {
                chatMessageDao.insert(newMessage);
            });
            
            // Track this conversation for both users
            DatabaseReference userConvsRef = FirebaseDatabase.getInstance().getReference("user_conversations");
            userConvsRef.child(currentUserId).child(receiverId).setValue(timestamp);
            userConvsRef.child(receiverId).child(currentUserId).setValue(timestamp);
            
            // Send Notification for new message
            DatabaseReference notifRef = FirebaseDatabase.getInstance().getReference("notifications").child(receiverId).push();
            
            // Get current user's name to show in notification
            String senderName = getSharedPreferences("UMMA_PREFS", MODE_PRIVATE).getString("user_name", "Someone");
            if ("doctor".equals(getSharedPreferences("UMMA_PREFS", MODE_PRIVATE).getString("last_role", ""))) {
                senderName = getSharedPreferences("UMMA_PREFS", MODE_PRIVATE).getString("doctor_name", "Doctor");
            }

            com.example.ummatelemedicineapp.models.Notification notification = new com.example.ummatelemedicineapp.models.Notification(
                "Message from " + senderName,
                text,
                new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date()),
                false,
                false
            );
            notification.setAppointmentId("CHAT"); 
            notification.setDoctorId(currentUserId); // Pass sender ID so receiver can reply
            notifRef.setValue(notification);
            
            etMessage.setText("");
        }
    }
}