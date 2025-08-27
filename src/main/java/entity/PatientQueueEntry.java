package entity;
import java.time.LocalDateTime;

public class PatientQueueEntry {
    private String id;
    private String patientId;
    private String preferredDoctorId; // null means any
    private String reason;
    private QueueStatus status = QueueStatus.WAITING;
    private LocalDateTime enqueuedAt = LocalDateTime.now();
    private int priority = 0; // 0 normal, higher = higher priority
    private int callAttempts = 0;

    public PatientQueueEntry() {}

    public PatientQueueEntry(String id, String patientId, String preferredDoctorId, String reason, int priority) {
        this.id = id;
        this.patientId = patientId;
        this.preferredDoctorId = preferredDoctorId;
        this.reason = reason;
        this.priority = priority;
    }

    public String getId() { return id; }
    public String getPatientId() { return patientId; }
    public String getPreferredDoctorId() { return preferredDoctorId; }
    public String getReason() { return reason; }
    public QueueStatus getStatus() { return status; }
    public LocalDateTime getEnqueuedAt() { return enqueuedAt; }
    public int getPriority() { return priority; }
    public int getCallAttempts() { return callAttempts; }

    public void setStatus(QueueStatus status) { this.status = status; }
    public void setPriority(int priority) { this.priority = priority; }
    public void incrementCallAttempts(){ this.callAttempts++; }
    public void setPreferredDoctorId(String preferredDoctorId) { this.preferredDoctorId = preferredDoctorId; }
}
