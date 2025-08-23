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
                case 9 -> {}
                default -> {
                    System.out.println("Invalid");
                    InputUtil.pauseScreen();
                }
            }
        } while (c != 9);
        persist();
    }

    // UI helpers moved to boundary/QueueMaintenanceUI

    private void enqueue() {
        System.out.println("═".repeat(60));
        System.out.println("QUEUE MANAGEMENT - ADD PATIENT TO QUEUE");
        System.out.println("═".repeat(60));
        
        // First check if any doctors are on duty
        if (!anyDoctorOnDuty()) {
            System.out.println("(X) Cannot enqueue: No doctors are currently on duty.");
            return;
        }
        
        String patientId = InputUtil.getInput(sc,"Patient ID: ");
        if (findPatient(patientId)==null){System.out.println("Patient not found");return;}
        
        String prefDoc = InputUtil.getInput(sc,"Preferred Doctor ID (blank=any): ").trim();
        if (prefDoc.isEmpty()) {
            prefDoc = null;
        } else {
            if (findDoctor(prefDoc)==null){System.out.println("Doctor not found");return;}
            // Check if the preferred doctor is on duty
            if (!isDoctorOnDuty(prefDoc)) {
                System.out.println("(X) Cannot enqueue: Doctor " + prefDoc + " is currently off duty.");
                System.out.println("Available doctors: " + getOnDutyDoctorsList());
                return;
            }
        }
        
        String reason = InputUtil.getInput(sc,"Reason: ");
        int priority = InputUtil.getIntInput(sc,"Priority (0 normal, higher = urgent): ");
        String id = nextQueueId();
        PatientQueueEntry e = new PatientQueueEntry(id, patientId, prefDoc, reason, priority);
        // Use CustomADT's enqueue method for priority-based insertion
        queue.enqueue(e);
        persist();
        System.out.println("✅ Enqueued: "+id);
    }

    private void remove() {
        String id = InputUtil.getInput(sc,"Queue ID to remove: ");
        for (int i=0;i<queue.size();i++) if (queue.get(i).getId().equals(id)) { queue.remove(i); persist(); System.out.println("Removed."); return; }
        System.out.println("Not found.");
    }

    private void view() {
        System.out.println("═".repeat(60));
        System.out.println("CURRENT QUEUE STATUS");
        System.out.println("═".repeat(60));
        
        if (queue.size()==0){System.out.println("Queue is empty.");return;}
        System.out.printf("%-6s %-8s %-8s %-3s %-10s %-10s%n","ID","Patient","PrefDoc","Pr","Status","Reason");
        System.out.println("─".repeat(60));
        for (int i=0;i<queue.size();i++) {
            PatientQueueEntry e = queue.get(i);
            System.out.printf("%-6s %-8s %-8s %-3d %-10s %-10s%n", e.getId(), e.getPatientId(), e.getPreferredDoctorId()==null?"ANY":e.getPreferredDoctorId(), e.getPriority(), e.getStatus(), e.getReason());
        }
    }

    private void callNext(String doctorId) {
        int idx = queue.findNextIndex(doctorId);
        if (idx<0){System.out.println("No waiting entry.");return;}
        PatientQueueEntry e = queue.get(idx);
        e.setStatus(QueueStatus.IN_PROGRESS); // Automatically start when called
        e.incrementCallAttempts();
        // Move entry just after last IN_PROGRESS to keep order stable
        queue.repositionAfterCalled(idx);
        persist();
        System.out.println("✅ Called and started: "+e.getId()+ (doctorId!=null?" for doctor "+doctorId: ""));
    }

    private void complete() {
        System.out.println("═".repeat(60));
        System.out.println("QUEUE MANAGEMENT - COMPLETE PATIENT CONSULTATION");
        System.out.println("═".repeat(60));
        
        // First show only IN_PROGRESS entries
        showInProgressEntries();
        
        if (!hasInProgressEntries()) {
            System.out.println("No patients are currently in progress. Call a patient first.");
            return;
        }
        
        String id = InputUtil.getInput(sc,"Queue ID to complete: ");
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

    private void showInProgressEntries() {
        System.out.println("\n-- Patients Currently In Progress --");
        boolean hasEntries = false;
        
        for (int i = 0; i < queue.size(); i++) {
            PatientQueueEntry e = queue.get(i);
            if (e.getStatus() == QueueStatus.IN_PROGRESS) {
                if (!hasEntries) {
                    System.out.printf("%-6s %-8s %-8s %-10s%n", "ID", "Patient", "PrefDoc", "Reason");
                    System.out.println("─".repeat(40));
                    hasEntries = true;
                }
                System.out.printf("%-6s %-8s %-8s %-10s%n", 
                                 e.getId(), 
                                 e.getPatientId(), 
                                 e.getPreferredDoctorId() == null ? "ANY" : e.getPreferredDoctorId(), 
                                 e.getReason());
            }
        }
        
        if (!hasEntries) {
            System.out.println("No patients currently in progress.");
        }
        System.out.println();
    }

    private boolean hasInProgressEntries() {
        for (int i = 0; i < queue.size(); i++) {
            if (queue.get(i).getStatus() == QueueStatus.IN_PROGRESS) {
                return true;
            }
        }
        return false;
    }

    private void bump() {
        String id = InputUtil.getInput(sc,"Queue ID to bump priority: ");
        PatientQueueEntry e = queue.findEntry(id);
        if (e==null){System.out.println("Not found.");return;}
        int delta = InputUtil.getIntInput(sc,"Increase by (e.g. 1): ");
        e.setPriority(e.getPriority()+delta);
        // Reorder: bubble upward from current position
        int idx = queue.indexOf(id);
        queue.bubbleUp(idx);
        persist();
        System.out.println("Priority updated.");
    }

    private void viewRecentConsultations() {
    java.time.LocalDate today = java.time.LocalDate.now();
    System.out.println("\n-- Recent Consultations (Today: " + today + ") --");
        
        if (consultations.size() == 0) {
            System.out.println("No consultations found.");
            return;
        }
        
        boolean hasToday = false;
        System.out.printf("%-8s %-10s %-10s %-12s %-20s %-10s%n", 
                         "ID", "Patient", "Doctor", "Date", "Reason", "Status");
        System.out.println("─".repeat(80));
        
        for (int i = 0; i < consultations.size(); i++) {
            Consultation c = consultations.get(i);
            if (c.getDate().toLocalDate().equals(today)) {
                hasToday = true;
                String doctorDisplay = c.getDoctorId().equals("UNASSIGNED") ? "(!!) UNASSIGNED" : c.getDoctorId();
                System.out.printf("%-8s %-10s %-10s %-12s %-20s %-10s%n",
                                 c.getId(), c.getPatientId(), doctorDisplay,
                                 c.getDate().toString(), c.getReason(), c.getStatus());
            }
        }
        
        if (!hasToday) {
            System.out.println("No consultations for today.");
        }
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
