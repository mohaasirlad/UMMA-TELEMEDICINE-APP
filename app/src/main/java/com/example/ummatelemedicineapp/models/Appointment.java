package com.example.ummatelemedicineapp.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "appointments")
public class Appointment {
    @PrimaryKey
    @NonNull
    private String id;
    
    private String doctorName;
    private String patientName;
    private String doctorId;
    private String patientId;
    private String specialty;
    private String type;
    private String date;
    private String time;
    private String status;
    private boolean isPast;
    private String paymentStatus; // "Paid" or "Pay at Clinic"
    private String medicalNotes;

    public Appointment() {
        this.id = "";
    }

    @Ignore
    public Appointment(String doctorName, String patientName, String type, String date, String time, String status, boolean isPast, String paymentStatus) {
        this.id = "";
        this.doctorName = doctorName;
        this.patientName = patientName;
        this.type = type;
        this.specialty = type;
        this.date = date;
        this.time = time;
        this.status = status;
        this.isPast = isPast;
        this.paymentStatus = paymentStatus;
        this.medicalNotes = "";
    }

    // Compatibility constructor for existing code
    public Appointment(String doctorName, String specialty, String date, String time, String status, boolean isPast, String paymentStatus) {
        this.id = "";
        this.doctorName = doctorName;
        this.patientName = "Patient";
        this.type = specialty;
        this.specialty = specialty;
        this.date = date;
        this.time = time;
        this.status = status;
        this.isPast = isPast;
        this.paymentStatus = paymentStatus;
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getSpecialty() { return specialty; }
    public void setSpecialty(String specialty) { this.specialty = specialty; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isPast() { return isPast; }
    public void setPast(boolean past) { isPast = past; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getMedicalNotes() { return medicalNotes; }
    public void setMedicalNotes(String medicalNotes) { this.medicalNotes = medicalNotes; }
}
