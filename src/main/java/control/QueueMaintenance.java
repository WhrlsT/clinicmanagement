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
            QueueMaintenanceUI ui = new QueueMaintenanceUI();
            ui.showHeader(queue, doctors, patients);
            c = ui.menu();
            switch (c) {
                case 1 -> enqueue();
                case 2 -> view();
                case 3 -> callNext(null);
                case 4 -> { String doc = InputUtil.getInput(sc, "Doctor ID: "); callNext(doc); }
                case 5 -> start();
                case 6 -> complete();
                case 7 -> skip();
                case 8 -> bump();
                case 9 -> remove();
                case 10 -> {}
                default -> System.out.println("Invalid");
            }
        } while (c != 10);
        persist();
    }

    // UI helpers moved to boundary/QueueMaintenanceUI

    private void enqueue() {
        String patientId = InputUtil.getInput(sc,"Patient ID: ");
        if (findPatient(patientId)==null){System.out.println("Patient not found");return;}
        String prefDoc = InputUtil.getInput(sc,"Preferred Doctor ID (blank=any): ").trim();
        if (prefDoc.isEmpty()) prefDoc = null; else if (findDoctor(prefDoc)==null){System.out.println("Doctor not found");return;}
        String reason = InputUtil.getInput(sc,"Reason: ");
        int priority = InputUtil.getIntInput(sc,"Priority (0 normal, higher = urgent): ");
        String id = nextQueueId();
        PatientQueueEntry e = new PatientQueueEntry(id, patientId, prefDoc, reason, priority);
        // Insert based on priority (simple: append then bubble up by swaps)
        queue.add(e);
        bubbleUp(queue.size()-1);
        persist();
        System.out.println("Enqueued: "+id);
    }

    private void bubbleUp(int idx){
        while (idx>0) {
            int prev = idx-1;
            if (queue.get(prev).getPriority() < queue.get(idx).getPriority()) {
                // swap via manual swap (no collections)
                PatientQueueEntry tmp = queue.get(prev);
                queue.set(prev, queue.get(idx));
                queue.set(idx, tmp);
                idx = prev;
            } else break;
        }
    }

    private void view() {
        if (queue.size()==0){System.out.println("Empty.");return;}
        System.out.printf("%-6s %-8s %-8s %-3s %-10s %-10s%n","ID","Patient","PrefDoc","Pr","Status","Reason");
        for (int i=0;i<queue.size();i++) {
            PatientQueueEntry e = queue.get(i);
            System.out.printf("%-6s %-8s %-8s %-3d %-10s %-10s%n", e.getId(), e.getPatientId(), e.getPreferredDoctorId()==null?"ANY":e.getPreferredDoctorId(), e.getPriority(), e.getStatus(), e.getReason());
        }
    }

    private void callNext(String doctorId) {
        int idx = findNextIndex(doctorId);
        if (idx<0){System.out.println("No waiting entry.");return;}
        PatientQueueEntry e = queue.get(idx);
        e.setStatus(QueueStatus.CALLED);
        e.incrementCallAttempts();
        // Move called entry just after last CALLED/IN_PROGRESS to keep order stable
        repositionAfterCalled(idx);
        persist();
        System.out.println("Called: "+e.getId()+ (doctorId!=null?" for doctor "+doctorId: ""));
    }

    private void repositionAfterCalled(int idx){
        PatientQueueEntry e = queue.get(idx);
        // Find target insert position
        int target = 0;
        for (int i=0;i<queue.size();i++) {
            QueueStatus st = queue.get(i).getStatus();
            if (st==QueueStatus.CALLED || st==QueueStatus.IN_PROGRESS) target = i+1; else break;
        }
        if (idx < target) return; // already in place
        // Extract and shift
        PatientQueueEntry temp = e;
        for (int i=idx; i>target; i--) {
            queue.set(i, queue.get(i-1));
        }
        queue.set(target, temp);
    }

    private void start() {
        String id = InputUtil.getInput(sc,"Queue ID to start: ");
        PatientQueueEntry e = findEntry(id);
        if (e==null){System.out.println("Not found.");return;}
        if (e.getStatus()!=QueueStatus.CALLED){System.out.println("Must be CALLED first.");return;}
        e.setStatus(QueueStatus.IN_PROGRESS);
        persist();
        System.out.println("In progress.");
    }

    private void complete() {
        String id = InputUtil.getInput(sc,"Queue ID to complete: ");
        PatientQueueEntry e = findEntry(id);
        if (e==null){System.out.println("Not found.");return;}
        if (e.getStatus()!=QueueStatus.IN_PROGRESS){System.out.println("Not in progress.");return;}
        int hour = LocalTime.now().getHour();
        LocalDate date = LocalDate.now();
        String doctorId = pickDoctorForEntry(e, date, hour);
        if (doctorId==null) {
            System.out.println("No doctor free this hour; marking completed without consultation record.");
        } else {
            Consultation c = new Consultation(nextConsultationId(), e.getPatientId(), doctorId, date, e.getReason(), "Queue", Consultation.Status.TREATED);
            consultations.add(c);
            consultationDAO.save(consultations);
            System.out.println("Consultation logged: "+c.getId());
        }
        e.setStatus(QueueStatus.COMPLETED);
        persist();
        System.out.println("Completed.");
    }

    private void skip() {
        String id = InputUtil.getInput(sc,"Queue ID to skip: ");
        PatientQueueEntry e = findEntry(id);
        if (e==null){System.out.println("Not found.");return;}
        e.setStatus(QueueStatus.SKIPPED);
        persist();
        System.out.println("Skipped.");
    }

    private void bump() {
        String id = InputUtil.getInput(sc,"Queue ID to bump priority: ");
        PatientQueueEntry e = findEntry(id);
        if (e==null){System.out.println("Not found.");return;}
        int delta = InputUtil.getIntInput(sc,"Increase by (e.g. 1): ");
        e.setPriority(e.getPriority()+delta);
        // Reorder: bubble upward from current position
        int idx = indexOf(id);
        bubbleUp(idx);
        persist();
        System.out.println("Priority updated.");
    }

    private void remove() {
        String id = InputUtil.getInput(sc,"Queue ID to remove: ");
        for (int i=0;i<queue.size();i++) if (queue.get(i).getId().equals(id)) { queue.remove(i); persist(); System.out.println("Removed."); return; }
        System.out.println("Not found.");
    }

    private int findNextIndex(String doctorId) {
        // Priority order already handled by array order (higher priority bubbled up)
        for (int i=0;i<queue.size();i++) {
            PatientQueueEntry e = queue.get(i);
            if (e.getStatus()==QueueStatus.WAITING) {
                if (doctorId==null) return i; // any
                // match doctor or ANY
                if (e.getPreferredDoctorId()==null || e.getPreferredDoctorId().equals(doctorId)) return i;
            }
        }
        return -1;
    }

    private PatientQueueEntry findEntry(String id){ for (int i=0;i<queue.size();i++) if (queue.get(i).getId().equals(id)) return queue.get(i); return null; }
    private int indexOf(String id){ for (int i=0;i<queue.size();i++) if (queue.get(i).getId().equals(id)) return i; return -1; }
    private Doctor findDoctor(String id){ for (int i=0;i<doctors.size();i++) if (doctors.get(i).getId().equals(id)) return doctors.get(i); return null; }
    private Patient findPatient(String id){ for (int i=0;i<patients.size();i++) if (patients.get(i).getId().equals(id)) return patients.get(i); return null; }

    private boolean doctorFree(String doctorId, LocalDate date, int hour) {
        Doctor d = findDoctor(doctorId);
        if (d==null) return false;
        int dayIdx = date.getDayOfWeek().getValue()-1;
        if (!d.getSchedule().isAvailable(dayIdx,hour)) return false;
        for (int i=0;i<consultations.size();i++) {
            Consultation c = consultations.get(i);
            // date-only model: consider at most one consultation per doctor per date from queue auto-complete
            if (c.getDoctorId().equals(doctorId) && c.getDate().equals(date)) return false;
        }
        return true;
    }

    private String pickDoctorForEntry(PatientQueueEntry e, LocalDate date, int hour) {
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
}
