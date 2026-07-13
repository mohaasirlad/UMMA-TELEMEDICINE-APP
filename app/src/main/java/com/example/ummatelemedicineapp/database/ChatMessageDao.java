package com.example.ummatelemedicineapp.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.ummatelemedicineapp.models.ChatMessage;

import java.util.List;

@Dao
public interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    List<ChatMessage> getMessagesForConversation(String conversationId);

    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    LiveData<List<ChatMessage>> getMessagesLiveData(String conversationId);

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    void insert(ChatMessage message);

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    void insertAll(List<ChatMessage> messages);

    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY timestamp DESC LIMIT 1")
    ChatMessage getLastMessageForConversation(String conversationId);

    @Query("SELECT COUNT(*) FROM chat_messages WHERE conversationId = :conversationId AND isRead = 0 AND receiverId = :currentUserId")
    int getUnreadCountForConversation(String conversationId, String currentUserId);

    @Query("UPDATE chat_messages SET isRead = 1 WHERE conversationId = :conversationId AND receiverId = :currentUserId")
    void markAsRead(String conversationId, String currentUserId);

    @Query("UPDATE chat_messages SET status = :status WHERE firebaseId = :firebaseId")
    void updateMessageStatus(String firebaseId, String status);

    @Query("SELECT DISTINCT conversationId FROM chat_messages WHERE senderId = :userId OR receiverId = :userId")
    List<String> getAllConversationIdsForUser(String userId);

    @Query("SELECT DISTINCT conversationId FROM chat_messages")
    List<String> getAllConversationIds();

    @Query("DELETE FROM chat_messages")
    void clearAll();
}