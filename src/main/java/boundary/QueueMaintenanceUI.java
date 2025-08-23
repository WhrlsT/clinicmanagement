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
        System.out.println("5. Start Entry");
        System.out.println("6. Complete Entry");
        System.out.println("7. Skip Entry");
        System.out.println("8. Bump Priority");
        System.out.println("9. Remove Entry");
        System.out.println("10. Back");
        return InputUtil.getIntInput(sc, "Choose: ");
    }

    public void showHeader(ADTInterface<PatientQueueEntry> queue,
                           ADTInterface<Doctor> doctors,
                           ADTInterface<Patient> patients) {
        printQueueSnapshot(queue);
        printQueueSummary(queue);
        showDoctors(doctors);
        showPatients(patients);
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

    private void showDoctors(ADTInterface<Doctor> doctors) {
        DoctorMaintenanceUI ui = new DoctorMaintenanceUI();
        ui.displayDoctorsTable(buildDoctorRows(doctors));
    }

    private String buildDoctorRows(ADTInterface<Doctor> doctors) {
        if (doctors==null || doctors.size()==0) return "";
        StringBuilder sb = new StringBuilder();
        for (int i=0;i<doctors.size();i++) {
            Doctor d = doctors.get(i);
            sb.append(String.format("%-10s|%-20s|%-15s|%-20s|%-20s\n",
                    nz(d.getId()), nz(d.getName()), nz(d.getSpecialization()), nz(d.getPhoneNumber()), nz(d.getEmail())));
        }
        return sb.toString();
    }

    private void showPatients(ADTInterface<Patient> patients) {
        PatientMaintenanceUI ui = new PatientMaintenanceUI();
        ui.displayPatientsTable(buildPatientRows(patients));
    }

    private String buildPatientRows(ADTInterface<Patient> patients) {
        if (patients==null || patients.size()==0) return "";
        StringBuilder sb = new StringBuilder();
        for (int i=0;i<patients.size();i++) {
            Patient p = patients.get(i);
            String age = p.calculateAge(p.getDateOfBirth());
            sb.append(String.format("%-10s|%-20s|%-10s|%-15s|%-15s|%-25s|%-15s\n",
                    nz(p.getId()), nz(p.getName()), nz(age), nz(p.getGender()), nz(p.getPhoneNumber()), nz(p.getEmail()), nz(p.getNationality())));
        }
        return sb.toString();
    }

    private String nz(String v){ return v==null?"":v; }
}
