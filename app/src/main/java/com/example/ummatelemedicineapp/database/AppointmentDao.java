package com.example.ummatelemedicineapp.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.ummatelemedicineapp.models.Appointment;

import java.util.List;

@Dao
public interface AppointmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Appointment appointment);

    @Update
    void update(Appointment appointment);

    @Delete
    void delete(Appointment appointment);

    @Query("SELECT * FROM appointments WHERE doctorId = :doctorId ORDER BY date DESC, time DESC")
    androidx.lifecycle.LiveData<List<Appointment>> getAppointmentsByDoctorLive(String doctorId);

    @Query("SELECT * FROM appointments WHERE patientId = :patientId ORDER BY date DESC, time DESC")
    androidx.lifecycle.LiveData<List<Appointment>> getAppointmentsByPatientLive(String patientId);

    @Query("SELECT * FROM appointments WHERE doctorId = :doctorId AND isPast = 0 ORDER BY date ASC, time ASC")
    androidx.lifecycle.LiveData<List<Appointment>> getUpcomingAppointmentsByDoctorLive(String doctorId);

    @Query("SELECT * FROM appointments WHERE patientId = :patientId AND isPast = 0 ORDER BY date ASC, time ASC")
    androidx.lifecycle.LiveData<List<Appointment>> getUpcomingAppointmentsByPatientLive(String patientId);

    @Query("SELECT * FROM appointments WHERE isPast = 0 ORDER BY date ASC, time ASC")
    androidx.lifecycle.LiveData<List<Appointment>> getUpcomingAppointmentsLive();

    @Query("SELECT COUNT(*) FROM appointments WHERE doctorId = :doctorId AND date = 'Today'")
    androidx.lifecycle.LiveData<Integer> getTodayCountByDoctorLive(String doctorId);

    @Query("SELECT COUNT(*) FROM appointments WHERE doctorId = :doctorId AND status = 'Pending'")
    androidx.lifecycle.LiveData<Integer> getPendingCountByDoctorLive(String doctorId);

    @Query("SELECT * FROM appointments ORDER BY date DESC, time DESC")
    androidx.lifecycle.LiveData<List<Appointment>> getAllAppointmentsLive();

    @Query("SELECT * FROM appointments ORDER BY date DESC, time DESC")
    List<Appointment> getAllAppointments();

    @Query("SELECT * FROM appointments WHERE doctorId = :doctorId ORDER BY date DESC, time DESC")
    List<Appointment> getAppointmentsByDoctor(String doctorId);

    @Query("SELECT * FROM appointments WHERE patientId = :patientId ORDER BY date DESC, time DESC")
    List<Appointment> getAppointmentsByPatient(String patientId);

    @Query("SELECT * FROM appointments WHERE doctorId = :doctorId AND isPast = 0 ORDER BY date ASC, time ASC")
    List<Appointment> getUpcomingAppointmentsByDoctor(String doctorId);

    @Query("SELECT * FROM appointments WHERE patientId = :patientId AND isPast = 0 ORDER BY date ASC, time ASC")
    List<Appointment> getUpcomingAppointmentsByPatient(String patientId);

    @Query("SELECT * FROM appointments WHERE doctorId = :doctorId AND isPast = 1 ORDER BY date DESC, time DESC")
    List<Appointment> getPastAppointmentsByDoctor(String doctorId);

    @Query("SELECT * FROM appointments WHERE patientId = :patientId AND isPast = 1 ORDER BY date DESC, time DESC")
    List<Appointment> getPastAppointmentsByPatient(String patientId);

    @Query("SELECT COUNT(*) FROM appointments WHERE doctorId = :doctorId AND date = 'Today'")
    int getTodayCountByDoctor(String doctorId);

    @Query("SELECT COUNT(*) FROM appointments WHERE doctorId = :doctorId AND status = 'Pending'")
    int getPendingCountByDoctor(String doctorId);

    @Query("SELECT DISTINCT patientName FROM appointments WHERE doctorId = :doctorId")
    List<String> getUniquePatientNamesByDoctor(String doctorId);

    @Query("SELECT * FROM appointments WHERE id = :id")
    Appointment getAppointmentById(String id);
}