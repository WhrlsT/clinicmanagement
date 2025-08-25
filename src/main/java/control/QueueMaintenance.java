package control;

import adt.ADTInterface;
import dao.*;
import entity.*;
import boundary.QueueMaintenanceUI;
import utility.InputUtil;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Scanner;

/** Hybrid queue: single structure with preferredDoctorId + priority handling */
public class QueueMaintenance {
    private final QueueDAO queueDAO = new QueueDAO();
    private final ConsultationDAO consultationDAO = new ConsultationDAO();
    private final DoctorDAO doctorDAO = new DoctorDAO();
    private final PatientDAO patientDAO = new PatientDAO();

    private final ADTInterface<PatientQueueEntry> queue;
    private final ADTInterface<Consultation> consultations;
    private final ADTInterface<Doctor> doctors;
    private final ADTInterface<Patient> patients;

    private final Scanner sc = new Scanner(System.in);

    public QueueMaintenance() {
        queue = queueDAO.load();
        consultations = consultationDAO.load();
        doctors = doctorDAO.retrieveFromFile();
        patients = patientDAO.retrieveFromFile();
    }

    public void run() {
        int c;
        do {
            InputUtil.clearScreen();
            QueueMaintenanceUI ui = new QueueMaintenanceUI();
            ui.showHeader(queue, doctors, patients);
            c = ui.menu();
            switch (c) {
                case 1 -> {
                    InputUtil.clearScreen();
                    enqueue();
                    InputUtil.pauseScreen();
                }
                case 2 -> {
                    InputUtil.clearScreen();
                    view();
                    InputUtil.pauseScreen();
                }
                case 3 -> {
                    InputUtil.clearScreen();
                    if (!anyDoctorOnDuty()) {
                        System.out.println("(X) Cannot call patient: No doctors are currently on duty.");
                    } else {
                        callNext(null);
                    }
                    InputUtil.pauseScreen();
                }
                case 4 -> { 
                    InputUtil.clearScreen();
                    String doc = InputUtil.getInput(sc, "Doctor ID: ");
                    if (findDoctor(doc) == null) {
                        System.out.println("(X) Doctor not found.");
                    } else if (!isDoctorOnDuty(doc)) {
                        System.out.println("(X) Cannot call patient: Doctor " + doc + " is currently off duty.");
                    } else {
                        callNext(doc);
                    }
                    InputUtil.pauseScreen();
                }
                case 5 -> {
                    InputUtil.clearScreen();
                    complete();
                    InputUtil.pauseScreen();
                }
                case 6 -> {
                    InputUtil.clearScreen();
                    bump();
                    InputUtil.pauseScreen();
                }
                case 7 -> {
                    InputUtil.clearScreen();
                    remove();
                    InputUtil.pauseScreen();
                }
                case 8 -> {
                    InputUtil.clearScreen();
                    viewRecentConsultations();
                    InputUtil.pauseScreen();
                }
                case 9 -> {
                    InputUtil.clearScreen();
                    clearQueue();
                    InputUtil.pauseScreen();
                }
                case 10 -> {}
                default -> {
                    System.out.println("Invalid");
                    InputUtil.pauseScreen();
                }
            }
        } while (c != 10);
        persist();
    }

    private void clearQueue() {
        QueueMaintenanceUI ui = new QueueMaintenanceUI();
        if (queue.size() == 0) { System.out.println("Queue is already empty."); return; }
        boolean ok = ui.promptClearConfirmation();
        if (!ok) { System.out.println("Clear cancelled."); return; }
        // Remove all entries
        while (queue.size() > 0) queue.remove(0);
        persist();
        System.out.println("(!!) Queue cleared.");
    }

    private void enqueue() {
        // UI handles prompts; control enforces business rules and persistence
        if (!anyDoctorOnDuty()) {
            System.out.println("(X) Cannot enqueue: No doctors are currently on duty.");
            return;
        }

        QueueMaintenanceUI ui = new QueueMaintenanceUI();
        String id = nextQueueId();
        PatientQueueEntry e = ui.promptEnqueue(id, patients);
        if (e == null) return; // user cancelled/back

        // Validate patient exists
        if (findPatient(e.getPatientId()) == null) { System.out.println("Patient not found"); return; }

        // Validate preferred doctor (if provided) and duty
        if (e.getPreferredDoctorId() != null) {
            if (findDoctor(e.getPreferredDoctorId()) == null) { System.out.println("Doctor not found"); return; }
            if (!isDoctorOnDuty(e.getPreferredDoctorId())) {
                System.out.println("(X) Cannot enqueue: Doctor " + e.getPreferredDoctorId() + " is currently off duty.");
                System.out.println("Available doctors: " + getOnDutyDoctorsList());
                return;
            }
        }

        // Enqueue and persist
        queue.enqueue(e);
        persist();
        System.out.println("✅ Enqueued: " + e.getId());
    }

    private void remove() {
    QueueMaintenanceUI ui = new QueueMaintenanceUI();
    String id = ui.promptRemove();
    for (int i = 0; i < queue.size(); i++) if (queue.get(i).getId().equals(id)) { queue.remove(i); persist(); System.out.println("Removed."); return; }
    System.out.println("Not found.");
    }

    private void view() {
    QueueMaintenanceUI ui = new QueueMaintenanceUI();
    ui.displayQueue(queue);
    }

    private void callNext(String doctorId) {
        int idx = findNextWaitingIndex(doctorId);
        if (idx<0){System.out.println("No waiting entry.");return;}
        PatientQueueEntry e = queue.get(idx);
        e.setStatus(QueueStatus.IN_PROGRESS); // Automatically start when called
        e.incrementCallAttempts();
        // Move entry just after last IN_PROGRESS to keep order stable
        repositionAfterCalled(idx);
        persist();
        System.out.println("✅ Called and started: "+e.getId()+ (doctorId!=null?" for doctor "+doctorId: ""));
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

    private void complete() {
        System.out.println("═".repeat(60));
        System.out.println("QUEUE MANAGEMENT - COMPLETE PATIENT CONSULTATION");
        System.out.println("═".repeat(60));

        // First show only IN_PROGRESS entries (UI)
        QueueMaintenanceUI ui = new QueueMaintenanceUI();
        ui.displayInProgressEntries(queue);

        if (!hasInProgressEntries()) {
            System.out.println("No patients are currently in progress. Call a patient first.");
            return;
        }

        String id = ui.promptComplete();
        PatientQueueEntry e = queue.findEntry(id);
        if (e==null){System.out.println("Not found.");return;}
        if (e.getStatus()!=QueueStatus.IN_PROGRESS){System.out.println("Patient not in progress (must be called first).");return;}
        
    // Get current time and date for consultation
    int hour = java.time.LocalTime.now().getHour();
    java.time.LocalDate date = java.time.LocalDate.now();
    String doctorId = pickDoctorForEntry(e, date, hour);
        
        // Always create a consultation record
        if (doctorId==null) {
            System.out.println("(!!) No doctor free this hour; creating consultation record without doctor assignment.");
            doctorId = "UNASSIGNED"; // Use placeholder for unassigned consultations
        }
        
    // Create and save consultation record (with date+hour)
    java.time.LocalDateTime dt = date.atTime(hour,0);
        Consultation c = new Consultation(nextConsultationId(), e.getPatientId(), doctorId, dt, e.getReason(), "Queue", Consultation.Status.ONGOING);
        // Create a calendar event when a specific doctor is assigned
        if (!"UNASSIGNED".equals(doctorId)) {
            try {
                Doctor d = findDoctor(doctorId);
                if (d != null && d.getCalendarId()!=null && !d.getCalendarId().isEmpty()) {
                    String evId = utility.GoogleCalendarService.getInstance().addConsultationEvent(d, dt, 60, "Consultation - " + e.getPatientId(), e.getReason());
                    c.setCalendarEventId(evId);
                }
            } catch (Exception ignored) {}
        }
        consultations.add(c);
        consultationDAO.save(consultations);
        System.out.println("(/) Consultation logged: "+c.getId());
        
        // Remove patient from queue (dequeue)
        int queueIndex = queue.indexOf(id);
        if (queueIndex >= 0) {
            queue.remove(queueIndex);
            System.out.println("(/) Patient dequeued successfully.");
        }
        
        persist();
        System.out.println("(/) Completed and dequeued.");
    }

    // showInProgressEntries moved to QueueMaintenanceUI

    private void bump() {
    QueueMaintenanceUI ui = new QueueMaintenanceUI();
    String id = ui.promptBumpId();
    PatientQueueEntry e = queue.findEntry(id);
    if (e==null){System.out.println("Not found.");return;}
    int delta = ui.promptBumpDelta();
    e.setPriority(e.getPriority() + delta);
        // Reorder: bubble upward from current position
        int idx = queue.indexOf(id);
        queue.bubbleUp(idx);
        persist();
        System.out.println("Priority updated.");
    }

    private void viewRecentConsultations() {
    QueueMaintenanceUI ui = new QueueMaintenanceUI();
    ui.displayRecentConsultations(consultations);
    }

    private boolean hasInProgressEntries() {
        for (int i = 0; i < queue.size(); i++) {
            if (queue.get(i).getStatus() == QueueStatus.IN_PROGRESS) {
                return true;
            }
        }
        return false;
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
    private boolean anyDoctorOnDuty() {
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
    private boolean isDoctorOnDuty(String doctorId) {
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
    private String getOnDutyDoctorsList() {
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
}
