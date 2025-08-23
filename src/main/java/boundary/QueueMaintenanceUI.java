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
        System.out.println("9. Back");
        return InputUtil.getIntInput(sc, "Choose: ");
    }

    public void showHeader(ADTInterface<PatientQueueEntry> queue,
                           ADTInterface<Doctor> doctors,
                           ADTInterface<Patient> patients) {
        printQueueSnapshot(queue);
        printQueueSummary(queue);
        showDoctorAvailability(doctors, queue);
    }

    public void printQueueSnapshot(ADTInterface<PatientQueueEntry> queue) {
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
        if (status.startsWith("Available")) return "/";
        else if (status.equals("Consulting")) return "*";
        else return "X";
    }

    private String getDoctorStatus(Doctor doctor, ADTInterface<PatientQueueEntry> queue) {
        // Check if doctor is currently consulting
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

}
