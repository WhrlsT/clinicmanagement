package entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;
@JsonIgnoreProperties(ignoreUnknown = true)
public class Consultation {
    private String id;
    private String patientId;
    private String doctorId;
    private LocalDateTime date; // date and time
    private String reason;
    private String notes;
    private Status status; // ONGOING, TREATED
    // Track Google Calendar event id when created
    private String calendarEventId;
    // Link to a previous consultation when this is a follow-up
    private String followUpOfId;

    public Consultation() { this.status = Status.ONGOING; }

    public Consultation(String id, String patientId, String doctorId, LocalDateTime date, String reason, String notes, Status status) {
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
    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getCalendarEventId() { return calendarEventId; }
    public void setCalendarEventId(String calendarEventId) { this.calendarEventId = calendarEventId; }
    public String getFollowUpOfId() { return followUpOfId; }
    public void setFollowUpOfId(String followUpOfId) { this.followUpOfId = followUpOfId; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public enum Status { BOOKED, ONGOING, TREATED }
}
