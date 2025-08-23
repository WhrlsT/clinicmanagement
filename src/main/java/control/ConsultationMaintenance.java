package control;

import adt.ADTInterface;
import dao.ConsultationDAO;
import dao.DoctorDAO;
import dao.PatientDAO;
import entity.*;
import utility.InputUtil;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

public class ConsultationMaintenance {
    private final ConsultationDAO consultationDAO = new ConsultationDAO();
    private final DoctorDAO doctorDAO = new DoctorDAO();
    private final PatientDAO patientDAO = new PatientDAO();

    private final ADTInterface<Consultation> consultations;
    private final ADTInterface<Doctor> doctors;
    private final ADTInterface<Patient> patients;
    // Rooms removed

    private final Scanner scanner = new Scanner(System.in);

    public ConsultationMaintenance() {
        consultations = consultationDAO.load();
        doctors = doctorDAO.retrieveFromFile();
        patients = patientDAO.retrieveFromFile();
    }

    public void run() {
        int choice;
        do {
            System.out.println("\nConsultation Menu");
            System.out.println("1. Book Consultation");
            System.out.println("2. List Consultations");
            System.out.println("3. Cancel Consultation");
            System.out.println("4. Reschedule Consultation");
            System.out.println("5. Back");
            choice = InputUtil.getIntInput(scanner, "Choose: ");
            switch (choice) {
                case 1 -> book();
                case 2 -> list();
                case 3 -> cancel();
                case 4 -> reschedule();
                case 5 -> {}
                default -> System.out.println("Invalid");
            }
    } while (choice != 5);
    }

    private void book() {
    // Always refresh in case doctors/patients changed since this instance was created
    refreshDoctorsAndPatients();
        // 1. Show doctor options
        printDoctorSummary();
        String doctorId = InputUtil.getInput(scanner, "Doctor ID: ");
        Doctor doctor = findDoctor(doctorId);
        if (doctor==null){System.out.println("Doctor not found");return;}

        // 2. Show patient options
        printPatientSummary();
        String patientId = InputUtil.getInput(scanner, "Patient ID: ");
        Patient patient = findPatient(patientId);
        if (patient==null){System.out.println("Patient not found");return;}

    // 3. Pick a date only
    LocalDate date = readDate(); if (date==null) return;


    // 4. Reason & booking
        String reason = InputUtil.getInput(scanner, "Reason: ");
        String id = nextConsultationId();
    Consultation c = new Consultation(id, patientId, doctorId, date, reason, "", Consultation.Status.ONGOING);
        consultations.add(c);
        persist();
    System.out.println("Booked consultation ID: "+id);
    }

    private void list() {
        if (consultations.size()==0){System.out.println("None.");return;}
        System.out.printf("%-8s %-8s %-8s %-12s %-10s %-10s%n","ID","Doctor","Patient","Date","Status","Reason");
        for (int i=0;i<consultations.size();i++) {
            Consultation c = consultations.get(i);
            System.out.printf("%-8s %-8s %-8s %-12s %-10s %-10s%n",c.getId(),c.getDoctorId(),c.getPatientId(),c.getDate(),c.getStatus(),c.getReason());
        }
    }

    private void cancel() {
        String id = InputUtil.getInput(scanner, "Consultation ID: ");
        for (int i=0;i<consultations.size();i++) {
            Consultation c = consultations.get(i);
            if (c.getId().equals(id)) {
                // No room schedule to update
                consultations.remove(i);
                persist();
                System.out.println("Cancelled.");
                return;
            }
        }
        System.out.println("Not found.");
    }

    private void reschedule() {
        String id = InputUtil.getInput(scanner, "Consultation ID: ");
    Consultation c = null;
    for (int i=0;i<consultations.size();i++) if (consultations.get(i).getId().equals(id)){c=consultations.get(i);break;}
        if (c==null){System.out.println("Not found.");return;}
    // Doctor retrieval no longer needed for hour-based checks
    // No room release needed
    LocalDate newDate = readDate(); if (newDate==null) return;
    // With date-only consultations, just update date
    c.setDate(newDate);
        persist();
        System.out.println("Rescheduled.");
    }


    private Doctor findDoctor(String id){ for (int i=0;i<doctors.size();i++) if (doctors.get(i).getId().equals(id)) return doctors.get(i); return null; }
    private Patient findPatient(String id){ for (int i=0;i<patients.size();i++) if (patients.get(i).getId().equals(id)) return patients.get(i); return null; }
    // Room model removed

    // hour-based checks removed

    // No room selection now

    private String nextConsultationId() {
        int max=0; for (int i=0;i<consultations.size();i++){String id=consultations.get(i).getId(); if (id!=null&&id.startsWith("C")) {try{int n=Integer.parseInt(id.substring(1)); if(n>max)max=n;}catch(Exception ignored){}}}
        return String.format("C%04d", max+1);
    }

    private LocalDate readDate() {
        String ds = InputUtil.getInput(scanner, "Date (YYYY-MM-DD): ");
        try { return LocalDate.parse(ds);} catch (DateTimeParseException e){ System.out.println("Bad date"); return null; }
    }

    private void persist() {
        consultationDAO.save(consultations);
        doctorDAO.saveToFile(doctors);
        patientDAO.saveToFile(patients);
    }

    // ---- Helper presentation methods for improved booking flow ----

    private void printDoctorSummary() {
        System.out.println("Doctors:");
        System.out.printf("%-8s %-18s %-15s%n","ID","Name","Specialty");
        for (int i=0;i<doctors.size();i++) {
            Doctor d = doctors.get(i);
            System.out.printf("%-8s %-18s %-15s%n", d.getId(), d.getName(), d.getSpecialization());
        }
    if (doctors.size()==0) System.out.println("(No doctors found)");
    }

    private void printPatientSummary() {
        System.out.println("Patients:");
        System.out.printf("%-8s %-20s%n","ID","Name");
        for (int i=0;i<patients.size();i++) {
            Patient p = patients.get(i);
            System.out.printf("%-8s %-20s%n", p.getId(), p.getName());
        }
    if (patients.size()==0) System.out.println("(No patients found)");
    }

    // availability helpers removed due to date-only consultations

    // Reload latest doctor & patient lists from file into existing ADT instances
    private void refreshDoctorsAndPatients() {
        ADTInterface<Doctor> latestDoctors = doctorDAO.retrieveFromFile();
        doctors.clear();
        for (int i=0;i<latestDoctors.size();i++) doctors.add(latestDoctors.get(i));
        ADTInterface<Patient> latestPatients = patientDAO.retrieveFromFile();
        patients.clear();
        for (int i=0;i<latestPatients.size();i++) patients.add(latestPatients.get(i));
    }
}
