package com.example.ummatelemedicineapp.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.ummatelemedicineapp.models.Notification;

import java.util.List;

@Dao
public interface NotificationDao {
    @Insert
    void insert(Notification notification);

    @Update
    void update(Notification notification);

    @Delete
    void delete(Notification notification);

    @Query("SELECT * FROM notifications ORDER BY id DESC")
    LiveData<List<Notification>> getAllNotificationsLive();

    @Query("SELECT * FROM notifications ORDER BY id DESC")
    List<Notification> getAllNotifications();

    @Query("SELECT * FROM notifications WHERE isUrgent = 1 AND isRead = 0 ORDER BY id DESC")
    LiveData<List<Notification>> getUrgentNotificationsLive();

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :notificationId")
    void markAsRead(int notificationId);

    @Query("UPDATE notifications SET isRead = 1")
    void markAllAsRead();

    @Query("SELECT COUNT(*) FROM notifications WHERE isRead = 0")
    LiveData<Integer> getUnreadCountLive();

    @Query("DELETE FROM notifications")
    void deleteAll();
}