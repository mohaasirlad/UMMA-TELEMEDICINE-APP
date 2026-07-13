package com.example.ummatelemedicineapp.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ProfileViewModel extends ViewModel {
    private final MutableLiveData<String> doctorName = new MutableLiveData<>();
    private final MutableLiveData<String> profileImageUri = new MutableLiveData<>();
    private final MutableLiveData<String> doctorShift = new MutableLiveData<>();

    public void setDoctorName(String name) {
        doctorName.setValue(name);
    }

    public LiveData<String> getDoctorName() {
        return doctorName;
    }

    public void setProfileImageUri(String uri) {
        profileImageUri.setValue(uri);
    }

    public LiveData<String> getProfileImageUri() {
        return profileImageUri;
    }

    public void setDoctorShift(String shift) {
        doctorShift.setValue(shift);
    }

    public LiveData<String> getDoctorShift() {
        return doctorShift;
    }
}