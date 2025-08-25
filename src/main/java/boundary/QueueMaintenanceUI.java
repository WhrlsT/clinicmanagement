package boundary;

import adt.ADTInterface;
import entity.*;
import utility.InputUtil;

import java.util.Scanner;

public class QueueMaintenanceUI {
    private final Scanner sc = new Scanner(System.in);

    public int menu() {
        System.out.println("\nPatient Queue (Hybrid)");
        System.out.println("1. Enqueue");
        System.out.println("2. View Queue");
        System.out.println("3. Call Next (Any Doctor)");
        System.out.println("4. Call Next For Doctor");
        System.out.println("5. Complete Entry");
        System.out.println("6. Bump Priority");
        System.out.println("7. Remove Entry");
    System.out.println("8. View Recent Consultations");
    System.out.println("9. Clear Queue");
    System.out.println("10. Back");
        return InputUtil.getIntInput(sc, "Choose: ");
    }

    public void showHeader(ADTInterface<PatientQueueEntry> queue,
                           ADTInterface<Doctor> doctors,
                           ADTInterface<Patient> patients) {
    // Compose the top-of-screen header: snapshot, summary and doctor availability
    printQueueSnapshot(queue);
    printQueueSummary(queue);
    showDoctorAvailability(doctors, queue);
    }

    public void printQueueSnapshot(ADTInterface<PatientQueueEntry> queue) {
    // Print a compact snapshot of the current queue entries
    System.out.println("\n-- Current Queue --");
        if (queue == null || queue.size() == 0) { System.out.println("(empty)"); return; }
        System.out.printf("%-6s %-8s %-8s %-3s %-10s %-10s%n","ID","Patient","PrefDoc","Pr","Status","Reason");
        for (int i=0;i<queue.size();i++) {
            PatientQueueEntry e = queue.get(i);
            System.out.printf("%-6s %-8s %-8s %-3d %-10s %-10s%n",
                    e.getId(), e.getPatientId(), e.getPreferredDoctorId()==null?"ANY":e.getPreferredDoctorId(),
                    e.getPriority(), e.getStatus(), e.getReason());
        }
    }

    public void printQueueSummary(ADTInterface<PatientQueueEntry> queue) {
    // Summarise counts for queue statuses
    int waiting=0, called=0, inProg=0, completed=0, skipped=0;
        for (int i=0;i<(queue==null?0:queue.size());i++) {
            QueueStatus st = queue.get(i).getStatus();
            if (st==QueueStatus.WAITING) waiting++;
            else if (st==QueueStatus.CALLED) called++;
            else if (st==QueueStatus.IN_PROGRESS) inProg++;
            else if (st==QueueStatus.COMPLETED) completed++;
            else if (st==QueueStatus.SKIPPED) skipped++;
        }
        System.out.printf("Summary: WAITING=%d, CALLED=%d, IN_PROGRESS=%d, COMPLETED=%d, SKIPPED=%d%n",
                waiting, called, inProg, completed, skipped);
    }

    public void showDoctorAvailability(ADTInterface<Doctor> doctors, ADTInterface<PatientQueueEntry> queue) {
        // Display each doctor's current duty/consulting status and warn if none on duty
        System.out.println("\n-- Doctor Availability --");
        if (doctors == null || doctors.size() == 0) {
            System.out.println("(no doctors)");
            return;
        }
        
        boolean anyOnDuty = false;
        for (int i = 0; i < doctors.size(); i++) {
            Doctor doctor = doctors.get(i);
            String status = getDoctorStatus(doctor, queue);
            String statusIcon = getStatusIcon(status);
            System.out.printf("%s Doctor %-8s (%-15s) : %s\n", 
                statusIcon, doctor.getId(), 
                doctor.getName() != null ? doctor.getName() : "Unknown", 
                status);
            
            if (status.startsWith("Available") || status.equals("Consulting")) {
                anyOnDuty = true;
            }
        }
        
        if (!anyOnDuty) {
            System.out.println("\n(!!) WARNING: No doctors are currently on duty. Queue entries are disabled.");
        }
    }

    private String getStatusIcon(String status) {
        // Simple visual glyphs: available='/', consulting='*', off='X'
        if (status.startsWith("Available")) return "/";
        else if (status.equals("Consulting")) return "*";
        else return "X";
    }

    private String getDoctorStatus(Doctor doctor, ADTInterface<PatientQueueEntry> queue) {
        // Determine if the doctor is currently consulting or has waiting patients.
        boolean hasInProgressPatient = false;
        int waitingForThisDoctor = 0;
        
        for (int i = 0; i < queue.size(); i++) {
            PatientQueueEntry entry = queue.get(i);
            
            // Count IN_PROGRESS entries that could be with this doctor
            if (entry.getStatus() == QueueStatus.IN_PROGRESS) {
                if (entry.getPreferredDoctorId() == null || entry.getPreferredDoctorId().equals(doctor.getId())) {
                    hasInProgressPatient = true;
                }
            }
            
            // Count patients waiting specifically for this doctor
            if (entry.getStatus() == QueueStatus.WAITING && 
                doctor.getId().equals(entry.getPreferredDoctorId())) {
                waitingForThisDoctor++;
            }
        }
        
        // If doctor has an in-progress patient, they're consulting
        if (hasInProgressPatient) {
            return "Consulting";
        }
        
        // Check if doctor is scheduled to work now
        java.time.LocalTime now = java.time.LocalTime.now();
        java.time.LocalDate today = java.time.LocalDate.now();
        int currentHour = now.getHour();
        int dayOfWeek = today.getDayOfWeek().getValue() - 1; // 0 = Monday
        
        boolean isScheduledNow = (doctor.getSchedule() != null && 
                                 doctor.getSchedule().isAvailable(dayOfWeek, currentHour));
        
        if (isScheduledNow) {
            if (waitingForThisDoctor > 0) {
                return String.format("Available (%d waiting)", waitingForThisDoctor);
            } else {
                return "Available";
            }
        } else {
            return "Off Duty";
        }
    }

    /** Display patients currently IN_PROGRESS */
    public void displayInProgressEntries(ADTInterface<PatientQueueEntry> queue) {
        System.out.println("\n-- Patients Currently In Progress --");
        boolean hasEntries = false;

        for (int i = 0; i < (queue == null ? 0 : queue.size()); i++) {
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

    /** Prompt for Queue ID to complete and return the entered ID (or null) */
    public String promptComplete() {
        return InputUtil.getInput(sc, "Queue ID to complete: ");
    }

    /** Prompt for Queue ID to bump and return entered ID */
    public String promptBumpId() { return InputUtil.getInput(sc, "Queue ID to bump priority: "); }

    /** Prompt for bump delta and return entered integer */
    public int promptBumpDelta() { return InputUtil.getIntInput(sc, "Increase by (e.g. 1): "); }

    /** Display recent consultations (UI-only) */
    public void displayRecentConsultations(ADTInterface<Consultation> consultations) {
        java.time.LocalDate today = java.time.LocalDate.now();
        System.out.println("\n-- Recent Consultations (Today: " + today + ") --");

        if (consultations == null || consultations.size() == 0) {
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

    /** Ask the operator to confirm clearing the entire queue. Returns true if confirmed. */
    public boolean promptClearConfirmation() {
        String v = InputUtil.getInputWithBackOption(sc, "Type CLEAR to confirm clearing the entire queue (0=back): ");
        if (v == null) return false;
        return v.equalsIgnoreCase("CLEAR");
    }

    /**
     * Prompt the operator to create a new queue entry. Returns the created
     * PatientQueueEntry or null if the user chose to go back/cancel.
     * The caller should provide the ID to use for the new entry.
     */
    public PatientQueueEntry promptEnqueue(String id, ADTInterface<Patient> patients) {
        System.out.println("═".repeat(60));
        System.out.println("QUEUE MANAGEMENT - ADD PATIENT TO QUEUE");
        System.out.println("═".repeat(60));

        // Show existing patients to help the operator choose an ID
        if (patients != null) {
            new PatientMaintenanceUI().displayPatientsTable(patients);
        }

        String patientId = InputUtil.getInputWithBackOption(sc, "Patient ID (enter 0 to go back): ");
        if (patientId == null) return null; // user chose to go back

        String prefDoc = InputUtil.getInputWithBackOption(sc, "Preferred Doctor ID (blank=any, 0=back): ");
        if (prefDoc == null) return null; // back
        prefDoc = prefDoc.trim();
        if (prefDoc.isEmpty()) prefDoc = null;

        String reason = InputUtil.getInputWithBackOption(sc, "Reason: ");
        if (reason == null) return null;

        Integer priorityObj = InputUtil.getValidatedHourWithBackOption(sc, "Priority (0 normal, higher = urgent): ");
        int priority = (priorityObj == null) ? 0 : priorityObj;

        return new PatientQueueEntry(id, patientId, prefDoc, reason, priority);
    }

    /**
     * Prompt for the queue ID to remove and return the entered ID.
     */
    public String promptRemove() {
        return InputUtil.getInput(sc, "Queue ID to remove: ");
    }

    /**
     * Display the full queue listing (same format as the previous view()).
     */
    public void displayQueue(ADTInterface<PatientQueueEntry> queue) {
        System.out.println("═".repeat(60));
        System.out.println("CURRENT QUEUE STATUS");
        System.out.println("═".repeat(60));

        if (queue == null || queue.size() == 0) { System.out.println("Queue is empty."); return; }
        System.out.printf("%-6s %-8s %-8s %-3s %-10s %-10s%n","ID","Patient","PrefDoc","Pr","Status","Reason");
        System.out.println("─".repeat(60));
        for (int i = 0; i < queue.size(); i++) {
            PatientQueueEntry e = queue.get(i);
            System.out.printf("%-6s %-8s %-8s %-3d %-10s %-10s%n",
                    e.getId(), e.getPatientId(), e.getPreferredDoctorId()==null?"ANY":e.getPreferredDoctorId(), e.getPriority(), e.getStatus(), e.getReason());
        }
    }

}
