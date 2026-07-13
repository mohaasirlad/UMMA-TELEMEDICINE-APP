package com.example.ummatelemedicineapp.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.ummatelemedicineapp.models.Appointment;
import com.example.ummatelemedicineapp.models.ChatMessage;
import com.example.ummatelemedicineapp.models.Notification;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Database(entities = {Appointment.class, ChatMessage.class, Notification.class}, version = 13)
public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase instance;
    private static final int NUMBER_OF_THREADS = 4;
    public static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    public abstract AppointmentDao appointmentDao();
    public abstract ChatMessageDao chatMessageDao();
    public abstract NotificationDao notificationDao();

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                    AppDatabase.class, "umma_telemedicine_v13")
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
}