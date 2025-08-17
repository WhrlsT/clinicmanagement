package entity;

import java.time.LocalDate;

public class Consultation {
    private String id;
    private String patientId;
    private String doctorId;
    private LocalDate date; // date only, hour separate
    private int hour; // 0-23
    private String reason;
    private String notes;

    public Consultation() {}

    public Consultation(String id, String patientId, String doctorId, LocalDate date, int hour, String reason, String notes) {
        this.id = id;
        this.patientId = patientId;
        this.doctorId = doctorId;
        this.date = date;
        this.hour = hour;
        this.reason = reason;
        this.notes = notes;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }
    // Room removed from model
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public int getHour() { return hour; }
    public void setHour(int hour) { this.hour = hour; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
