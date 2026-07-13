package com.example.ummatelemedicineapp.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "notifications")
public class Notification {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String title;
    private String message;
    private String time;
    private boolean isRead;
    private boolean isUrgent;
    private String appointmentId;
    private String doctorId; // Can be used for sender ID in chats

    public Notification() {
    }

    public Notification(String title, String message, String time, boolean isRead, boolean isUrgent) {
        this.title = title;
        this.message = message;
        this.time = time;
        this.isRead = isRead;
        this.isUrgent = isUrgent;
    }

    public String getAppointmentId() { return appointmentId; }
    public void setAppointmentId(String appointmentId) { this.appointmentId = appointmentId; }

    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }

    @androidx.room.Ignore
    public Notification(String title, String message, String time, boolean isRead) {
        this(title, message, time, isRead, false);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
    public boolean isUrgent() { return isUrgent; }
    public void setUrgent(boolean urgent) { isUrgent = urgent; }
}