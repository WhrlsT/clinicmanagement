package control;

import adt.ADTInterface;
import dao.*;
import entity.*;

import java.time.LocalDate;
import java.time.LocalTime;

/** Queue control: business logic only (no I/O). */
public class QueueMaintenance {
    private final QueueDAO queueDAO = new QueueDAO();
    private final ConsultationDAO consultationDAO = new ConsultationDAO();
    private final DoctorDAO doctorDAO = new DoctorDAO();
    private final PatientDAO patientDAO = new PatientDAO();

    private final ADTInterface<PatientQueueEntry> queue;
    private final ADTInterface<Consultation> consultations;
    private final ADTInterface<Doctor> doctors;
    private final ADTInterface<Patient> patients;

    public QueueMaintenance() {
        queue = queueDAO.load();
        consultations = consultationDAO.load();
        doctors = doctorDAO.retrieveFromFile();
        patients = patientDAO.retrieveFromFile();
    }

    // Data accessors for UI layer
    public ADTInterface<PatientQueueEntry> getQueue() { return queue; }
    public ADTInterface<Consultation> getConsultations() { return consultations; }
    public ADTInterface<Doctor> getDoctors() { return doctors; }
    public ADTInterface<Patient> getPatients() { return patients; }

    // Core actions
    public String generateNextQueueId() { return nextQueueId(); }

    public PatientQueueEntry enqueue(String id, String patientId, String preferredDoctorId, String reason, int priority) {
        if (!anyDoctorOnDuty()) {
            throw new IllegalStateException("No doctors are currently on duty");
        }
        if (findPatient(patientId) == null) throw new IllegalArgumentException("Patient not found");
        if (preferredDoctorId != null) {
            if (findDoctor(preferredDoctorId) == null) throw new IllegalArgumentException("Doctor not found");
            if (!isDoctorOnDuty(preferredDoctorId)) {
                throw new IllegalStateException("Preferred doctor is currently off duty");
            }
        }
        PatientQueueEntry e = new PatientQueueEntry(id, patientId, preferredDoctorId, reason, priority);
        queue.enqueue(e);
        persist();
        return e;
    }

    public boolean remove(String id) {
        for (int i = 0; i < queue.size(); i++) {
            if (queue.get(i).getId().equals(id)) {
                queue.remove(i);
                persist();
                return true;
            }
        }
        return false;
    }

    public PatientQueueEntry callNext(String doctorId) {
        int idx = findNextWaitingIndex(doctorId);
        if (idx < 0) return null;
        PatientQueueEntry e = queue.get(idx);
        // If ANY, assign a concrete on-duty doctor (prefer a free one)
        if (doctorId == null) {
            String assigned = pickRandomOnDutyDoctorPreferFree();
            if (assigned != null) e.setPreferredDoctorId(assigned);
        } else {
            // Ensure entry is assigned to the requested doctor
            e.setPreferredDoctorId(doctorId);
        }
        e.setStatus(QueueStatus.IN_PROGRESS); // start when called
        e.incrementCallAttempts();
        repositionAfterCalled(idx);
        persist();
        return e;
    }

    /** Pick a random on-duty doctor, preferring those not currently consulting */
    private String pickRandomOnDutyDoctorPreferFree() {
        LocalDate today = LocalDate.now();
        int currentHour = LocalTime.now().getHour();
        int dow = today.getDayOfWeek().getValue() - 1;

        // First collect free on-duty doctors
        adt.CustomADT<String> free = new adt.CustomADT<>();
        adt.CustomADT<String> allOnDuty = new adt.CustomADT<>();
        for (int i = 0; i < doctors.size(); i++) {
            Doctor d = doctors.get(i);
            boolean onDuty = d.getSchedule() != null && d.getSchedule().isAvailable(dow, currentHour);
            if (!onDuty) continue;
            allOnDuty.add(d.getId());

            boolean consulting = false;
            for (int j = 0; j < queue.size(); j++) {
                PatientQueueEntry e = queue.get(j);
                if (e.getStatus() == QueueStatus.IN_PROGRESS && d.getId().equals(e.getPreferredDoctorId())) {
                    consulting = true; break;
                }
            }
            if (!consulting) free.add(d.getId());
        }
        if (!free.isEmpty()) {
            int r = (int)(Math.random() * free.size());
            return free.get(r);
        }
        if (!allOnDuty.isEmpty()) {
            int r = (int)(Math.random() * allOnDuty.size());
            return allOnDuty.get(r);
        }
        return null;
    }

    // Domain-specific queue helpers (kept out of the generic ADT)
    private int findNextWaitingIndex(String doctorId) {
        // Prefer a waiting patient who prefers this doctor (if provided)
        for (int i = 0; i < queue.size(); i++) {
            PatientQueueEntry e = queue.get(i);
            if (e.getStatus() != QueueStatus.WAITING) continue;
            if (doctorId == null) return i;
            if (doctorId.equals(e.getPreferredDoctorId())) return i;
        }
        // Fallback: any waiting patient
        for (int i = 0; i < queue.size(); i++) {
            if (queue.get(i).getStatus() == QueueStatus.WAITING) return i;
        }
        return -1;
    }

    private void repositionAfterCalled(int index) {
        if (index < 0 || index >= queue.size()) return;
        int target = 0;
        // Find last IN_PROGRESS index + 1
        for (int i = 0; i < queue.size(); i++) {
            if (queue.get(i).getStatus() == QueueStatus.IN_PROGRESS) target = i + 1; else break;
        }
        if (index < target) return; // already within in-progress block
        PatientQueueEntry temp = queue.remove(index);
        queue.add(target, temp);
    }

    public Consultation complete(String queueId) {
        PatientQueueEntry e = queue.findEntry(queueId);
        if (e == null) throw new IllegalArgumentException("Queue entry not found");
        if (e.getStatus() != QueueStatus.IN_PROGRESS) throw new IllegalStateException("Patient is not in progress");

        int hour = java.time.LocalTime.now().getHour();
        java.time.LocalDate date = java.time.LocalDate.now();
        String doctorId = pickDoctorForEntry(e, date, hour);

        if (doctorId == null) {
            doctorId = "UNASSIGNED"; // create consultation without doctor assignment
        }

        java.time.LocalDateTime dt = date.atTime(hour, 0);
        Consultation c = new Consultation(nextConsultationId(), e.getPatientId(), doctorId, dt, e.getReason(), "Queue", Consultation.Status.ONGOING);
        if (!"UNASSIGNED".equals(doctorId)) {
            try {
                Doctor d = findDoctor(doctorId);
                if (d != null && d.getCalendarId() != null && !d.getCalendarId().isEmpty()) {
                    String evId = utility.GoogleCalendarService.getInstance().addConsultationEvent(d, dt, 60, "Consultation - " + e.getPatientId(), e.getReason());
                    c.setCalendarEventId(evId);
                }
            } catch (Exception ignored) {}
        }
        consultations.add(c);
        consultationDAO.save(consultations);

        int queueIndex = queue.indexOf(queueId);
        if (queueIndex >= 0) {
            queue.remove(queueIndex);
        }
        persist();
        return c;
    }

    // showInProgressEntries moved to UI

    public int bump(String id, int delta) {
        PatientQueueEntry e = queue.findEntry(id);
        if (e == null) throw new IllegalArgumentException("Queue entry not found");
        e.setPriority(e.getPriority() + delta);
        int idx = queue.indexOf(id);
        queue.bubbleUp(idx);
        persist();
        return e.getPriority();
    }

    private Doctor findDoctor(String id){ for (int i=0;i<doctors.size();i++) if (doctors.get(i).getId().equals(id)) return doctors.get(i); return null; }
    private Patient findPatient(String id){ for (int i=0;i<patients.size();i++) if (patients.get(i).getId().equals(id)) return patients.get(i); return null; }

    private boolean doctorFree(String doctorId, java.time.LocalDate date, int hour) {
        Doctor d = findDoctor(doctorId);
        if (d==null) return false;
        int dayIdx = date.getDayOfWeek().getValue()-1;
        if (!d.getSchedule().isAvailable(dayIdx,hour)) return false;
        for (int i=0;i<consultations.size();i++) {
            Consultation c = consultations.get(i);
        // consider a consultation booked at the same date+hour
        if (c.getDoctorId().equals(doctorId) && c.getDate().equals(date.atTime(hour,0))) return false;
        }
        return true;
    }

    private String pickDoctorForEntry(PatientQueueEntry e, java.time.LocalDate date, int hour) {
        if (e.getPreferredDoctorId()!=null && doctorFree(e.getPreferredDoctorId(), date, hour)) return e.getPreferredDoctorId();
        // fallback first free
        for (int i=0;i<doctors.size();i++) if (doctorFree(doctors.get(i).getId(), date, hour)) return doctors.get(i).getId();
        return null;
    }

    private String nextQueueId(){
        int max=0; for (int i=0;i<queue.size();i++){ String id=queue.get(i).getId(); if (id!=null && id.startsWith("Q")) { try{ int n=Integer.parseInt(id.substring(1)); if (n>max) max=n; } catch(Exception ignored){} } }
        return String.format("Q%04d", max+1);
    }

    private String nextConsultationId(){
        int max=0; for (int i=0;i<consultations.size();i++){ String id=consultations.get(i).getId(); if (id!=null && id.startsWith("C")) { try{ int n=Integer.parseInt(id.substring(1)); if (n>max) max=n; } catch(Exception ignored){} } }
        return String.format("C%04d", max+1);
    }

    private void persist(){
        queueDAO.save(queue);
        consultationDAO.save(consultations);
        doctorDAO.saveToFile(doctors);
        patientDAO.saveToFile(patients);
    }

    /**
     * Check if any doctor is currently on duty
     */
    public boolean anyDoctorOnDuty() {
        LocalDate today = LocalDate.now();
        int currentHour = LocalTime.now().getHour();
        int dayOfWeek = today.getDayOfWeek().getValue() - 1; // 0 = Monday
        
        for (int i = 0; i < doctors.size(); i++) {
            Doctor doctor = doctors.get(i);
            if (doctor.getSchedule() != null && 
                doctor.getSchedule().isAvailable(dayOfWeek, currentHour)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if a specific doctor is currently on duty
     */
    public boolean isDoctorOnDuty(String doctorId) {
        Doctor doctor = findDoctor(doctorId);
        if (doctor == null) return false;
        
        LocalDate today = LocalDate.now();
        int currentHour = LocalTime.now().getHour();
        int dayOfWeek = today.getDayOfWeek().getValue() - 1; // 0 = Monday
        
        return doctor.getSchedule() != null && 
               doctor.getSchedule().isAvailable(dayOfWeek, currentHour);
    }

    /**
     * Get a comma-separated list of doctors currently on duty
     */
    public String getOnDutyDoctorsList() {
        LocalDate today = LocalDate.now();
        int currentHour = LocalTime.now().getHour();
        int dayOfWeek = today.getDayOfWeek().getValue() - 1; // 0 = Monday
        
        StringBuilder onDutyDoctors = new StringBuilder();
        for (int i = 0; i < doctors.size(); i++) {
            Doctor doctor = doctors.get(i);
            if (doctor.getSchedule() != null && 
                doctor.getSchedule().isAvailable(dayOfWeek, currentHour)) {
                if (onDutyDoctors.length() > 0) {
                    onDutyDoctors.append(", ");
                }
                onDutyDoctors.append(doctor.getId());
                if (doctor.getName() != null) {
                    onDutyDoctors.append(" (").append(doctor.getName()).append(")");
                }
            }
        }
        return onDutyDoctors.length() > 0 ? onDutyDoctors.toString() : "None";
    }

    public boolean clearQueueAll() {
        if (queue.size() == 0) return false;
        while (queue.size() > 0) queue.remove(0);
        persist();
        return true;
    }
}
