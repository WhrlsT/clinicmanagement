package entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDate;
@JsonIgnoreProperties(ignoreUnknown = true)
public class Consultation {
    private String id;
    private String patientId;
    private String doctorId;
    private LocalDate date; // date only
    private String reason;
    private String notes;
    private Status status; // ONGOING, TREATED

    public Consultation() { this.status = Status.ONGOING; }

    public Consultation(String id, String patientId, String doctorId, LocalDate date, String reason, String notes, Status status) {
        this.id = id;
        this.patientId = patientId;
        this.doctorId = doctorId;
        this.date = date;
        this.reason = reason;
        this.notes = notes;
        this.status = (status == null ? Status.ONGOING : status);
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
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public enum Status { ONGOING, TREATED }
}
