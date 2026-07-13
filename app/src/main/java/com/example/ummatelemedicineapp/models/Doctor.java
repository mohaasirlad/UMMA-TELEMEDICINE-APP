package com.example.ummatelemedicineapp.models;

public class Doctor {
    private String id;
    private String name;
    private String specialty;
    private String availability;
    private float rating;
    private int imageResId;
    private String email;

    public Doctor() {
        // Required for Firebase
    }

    public Doctor(String id, String name, String specialty, String availability, float rating, int imageResId) {
        this.id = id;
        this.name = name;
        this.specialty = specialty;
        this.availability = availability;
        this.rating = rating;
        this.imageResId = imageResId;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public String getSpecialty() { return specialty; }
    public String getAvailability() { return availability; }
    public float getRating() { return rating; }
    public int getImageResId() { return imageResId; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}