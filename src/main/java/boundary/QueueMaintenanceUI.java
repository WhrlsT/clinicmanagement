package boundary;

import adt.ADTInterface;
import control.QueueMaintenance;
import entity.*;
import utility.InputUtil;

import java.util.Scanner;

public class QueueMaintenanceUI {
    private final Scanner sc = new Scanner(System.in);

    public void run() {
        QueueMaintenance ctl = new QueueMaintenance();
        int c;
        do {
            InputUtil.clearScreen();
            showHeader(ctl.getQueue(), ctl.getDoctors(), ctl.getPatients());
            c = menu();
            switch (c) {
                case 1 -> {
                    InputUtil.clearScreen();
                    handleEnqueue(ctl);
                    InputUtil.pauseScreen();
                }
                case 2 -> {
                    InputUtil.clearScreen();
                    displayQueue(ctl.getQueue());
                    InputUtil.pauseScreen();
                }
                case 3 -> {
                    InputUtil.clearScreen();
                    if (!ctl.anyDoctorOnDuty()) {
                        System.out.println("(X) Cannot call patient: No doctors are currently on duty.");
                    } else {
                        var e = ctl.callNext(null);
                        if (e == null) System.out.println("No waiting entry.");
                        else System.out.println("✅ Called and started: " + e.getId());
                    }
                    InputUtil.pauseScreen();
                }
                case 4 -> {
                    InputUtil.clearScreen();
                    String doc = InputUtil.getInput(sc, "Doctor ID: ");
                    if (doc == null || doc.isEmpty()) {
                        System.out.println("No doctor entered.");
                    } else if (!ctl.isDoctorOnDuty(doc)) {
                        System.out.println("(X) Cannot call patient: Doctor " + doc + " is currently off duty.");
                    } else {
                        var e = ctl.callNext(doc);
                        if (e == null) System.out.println("No waiting entry.");
                        else System.out.println("✅ Called and started: " + e.getId() + " for doctor " + doc);
                    }
                    InputUtil.pauseScreen();
                }
                case 5 -> {
                    InputUtil.clearScreen();
                    displayInProgressEntries(ctl.getQueue());
                    if (!hasInProgressEntries(ctl.getQueue())) {
                        System.out.println("No patients are currently in progress. Call a patient first.");
                    } else {
                        String id = promptComplete();
                        if (id != null && !id.isEmpty()) {
                            try {
                                var cns = ctl.complete(id);
                                System.out.println("(/) Consultation logged: " + cns.getId());
                                System.out.println("(/) Patient dequeued successfully.");
                                System.out.println("(/) Completed and dequeued.");
                            } catch (Exception ex) {
                                System.out.println("(X) " + ex.getMessage());
                            }
                        }
                    }
                    InputUtil.pauseScreen();
                }
                case 6 -> {
                    InputUtil.clearScreen();
                    String id = promptBumpId();
                    Integer delta = promptBumpDelta();
                    try {
                        int newPr = ctl.bump(id, delta);
                        System.out.println("Priority updated to " + newPr + ".");
                    } catch (Exception ex) {
                        System.out.println("(X) " + ex.getMessage());
                    }
                    InputUtil.pauseScreen();
                }
                case 7 -> {
                    InputUtil.clearScreen();
                    String id = promptRemove();
                    boolean ok = ctl.remove(id);
                    System.out.println(ok ? "Removed." : "Not found.");
                    InputUtil.pauseScreen();
                }
                case 8 -> {
                    InputUtil.clearScreen();
                    displayRecentConsultations(ctl.getConsultations());
                    InputUtil.pauseScreen();
                }
                case 9 -> {
                    InputUtil.clearScreen();
                    boolean confirm = promptClearConfirmation();
                    if (confirm) {
                        boolean changed = ctl.clearQueueAll();
                        System.out.println(changed ? "(!!) Queue cleared." : "Queue is already empty.");
                    } else {
                        System.out.println("Clear cancelled.");
                    }
                    InputUtil.pauseScreen();
                }
                case 10 -> {return;}
                default -> {
                    System.out.println("Invalid");
                    InputUtil.pauseScreen();
                }
            }
        } while (c != 10);
    }

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

    private boolean hasInProgressEntries(ADTInterface<PatientQueueEntry> queue) {
        for (int i = 0; i < (queue == null ? 0 : queue.size()); i++) {
            if (queue.get(i).getStatus() == QueueStatus.IN_PROGRESS) {
                return true;
            }
        }
        return false;
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
     * Display a concise list of doctors currently on duty (available now).
     */
    public void displayAvailableDoctors(ADTInterface<Doctor> doctors) {
        System.out.println("\n-- Available Doctors --");
        if (doctors == null || doctors.size() == 0) {
            System.out.println("(no doctors configured)");
            return;
        }
        int dayOfWeek = java.time.LocalDate.now().getDayOfWeek().getValue() - 1;
        int hour = java.time.LocalTime.now().getHour();
        boolean any = false;
        for (int i = 0; i < doctors.size(); i++) {
            Doctor d = doctors.get(i);
            if (d.getSchedule() != null && d.getSchedule().isAvailable(dayOfWeek, hour)) {
                any = true;
                System.out.printf("%s - %s%n", d.getId(), d.getName() == null ? "(no name)" : d.getName());
            }
        }
        if (!any) System.out.println("(none on duty right now)");
    }

    /**
     * Prompt the operator to create a new queue entry. Returns the created
     * PatientQueueEntry or null if the user chose to go back/cancel.
     * The caller should provide the ID to use for the new entry.
     */
    public PatientQueueEntry promptEnqueue(String id, ADTInterface<Patient> patients, ADTInterface<Doctor> doctors) {
        System.out.println("═".repeat(60));
        System.out.println("QUEUE MANAGEMENT - ADD PATIENT TO QUEUE");
        System.out.println("═".repeat(60));

        // Show existing patients to help the operator choose an ID
        if (patients != null) {
            new PatientMaintenanceUI().displayPatientsTable(patients);
        }

        String patientId = InputUtil.getInputWithBackOption(sc, "Patient ID (enter 0 to go back): ");
        if (patientId == null) return null; // user chose to go back

    // Show available doctors immediately before prompting for preferred doctor
    displayAvailableDoctors(doctors);

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

    private void handleEnqueue(QueueMaintenance ctl) {
        if (!ctl.anyDoctorOnDuty()) {
            System.out.println("(X) Cannot enqueue: No doctors are currently on duty.");
            return;
        }
        String id = ctl.generateNextQueueId();
        PatientQueueEntry e = promptEnqueue(id, ctl.getPatients(), ctl.getDoctors());
        if (e == null) return;
        try {
            ctl.enqueue(e.getId(), e.getPatientId(), e.getPreferredDoctorId(), e.getReason(), e.getPriority());
            System.out.println("✅ Enqueued: " + e.getId());
        } catch (Exception ex) {
            System.out.println("(X) " + ex.getMessage());
            System.out.println("Available doctors: " + ctl.getOnDutyDoctorsList());
        }
    }

}
