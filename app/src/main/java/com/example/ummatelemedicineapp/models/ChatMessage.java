package com.example.ummatelemedicineapp.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "chat_messages")
public class ChatMessage {
    @PrimaryKey
    @NonNull
    private String firebaseId; // Use Firebase push key as primary key for better sync

    private String conversationId; // Combination of senderId and receiverId or a unique chat ID
    private String senderId;
    private String receiverId;
    private String message;
    private long timestamp;
    private boolean isRead;
    private String status; // "sent", "delivered", "read"

    public ChatMessage() {
    }

    public ChatMessage(String senderId, String receiverId, String message, long timestamp) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.message = message;
        this.timestamp = timestamp;
        this.isRead = false;
        this.status = "sent";
        this.conversationId = generateConversationId(senderId, receiverId);
    }

    public static String generateConversationId(String id1, String id2) {
        return id1.compareTo(id2) < 0 ? id1 + "_" + id2 : id2 + "_" + id1;
    }

    public String getFirebaseId() { return firebaseId; }
    public void setFirebaseId(String firebaseId) { this.firebaseId = firebaseId; }

    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}