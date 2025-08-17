package control;

import adt.ADTInterface;
import dao.ConsultationDAO;
import dao.DoctorDAO;
import dao.PatientDAO;
import entity.*;
import utility.GoogleCalendarService;
import utility.InputUtil;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
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
            System.out.println("5. Doctor Next 5 Free Slots");
            System.out.println("6. Back");
            choice = InputUtil.getIntInput(scanner, "Choose: ");
            switch (choice) {
                case 1 -> book();
                case 2 -> list();
                case 3 -> cancel();
                case 4 -> reschedule();
                case 5 -> nextFreeSlots();
                case 6 -> {}
                default -> System.out.println("Invalid");
            }
    } while (choice != 6);
    }

    private void book() {
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

        // 3. Show upcoming availability for doctor (next 14 days)
        printUpcomingAvailability(doctor, 14, 7);
        LocalDate date = readDate(); if (date==null) return;

        // 4. Show free hours for chosen date
        List<Integer> freeHours = getFreeHoursForDoctorDate(doctor, date);
        if (freeHours.isEmpty()) { System.out.println("No free hours that date."); return; }
        System.out.print("Free hours: "); freeHours.forEach(h-> System.out.print(h+" ")); System.out.println();
        int hour = InputUtil.getIntInput(scanner, "Hour (choose from list): ");
        if (!freeHours.contains(hour)) { System.out.println("Hour not in free list"); return; }

    int dayIdx = date.getDayOfWeek().getValue()-1;

        // 5. Reason & booking
        String reason = InputUtil.getInput(scanner, "Reason: ");
        String id = nextConsultationId();
    Consultation c = new Consultation(id, patientId, doctorId, date, hour, reason, "");
        consultations.add(c);
        persist();
        try {
            GoogleCalendarService gcal = GoogleCalendarService.getInstance();
            gcal.ensureCalendar(doctor);
            gcal.addDutyHour(doctor,date,hour);
        } catch (Exception e) {
            System.out.println("Calendar sync failed: "+e.getMessage());
        }
    System.out.println("Booked consultation ID: "+id);
    }

    private void list() {
        if (consultations.size()==0){System.out.println("None.");return;}
    System.out.printf("%-8s %-8s %-8s %-12s %-4s %-10s %-15s%n","ID","Doctor","Patient","Date","Hr","Reason","Notes");
        for (int i=0;i<consultations.size();i++) {
            Consultation c = consultations.get(i);
            System.out.printf("%-8s %-8s %-8s %-12s %-4d %-10s %-15s%n",c.getId(),c.getDoctorId(),c.getPatientId(),c.getDate(),c.getHour(),c.getReason(),c.getNotes());
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
        Doctor doctor = findDoctor(c.getDoctorId());
    // No room release needed
        LocalDate newDate = readDate(); if (newDate==null) return; int newHour = InputUtil.getIntInput(scanner,"New hour (0-23): ");
        int newDay = newDate.getDayOfWeek().getValue()-1;
        if (!doctor.getSchedule().isAvailable(newDay,newHour) || !isDoctorHourFree(c.getDoctorId(),newDate,newHour)) {
            System.out.println("Doctor not free.");
            // re-book old
            // No re-book doctor template needed
            // no room rollback
            return;
        }
        c.setDate(newDate); c.setHour(newHour);
        persist();
        System.out.println("Rescheduled.");
    }

    private void nextFreeSlots() {
        String doctorId = InputUtil.getInput(scanner, "Doctor ID: ");
        Doctor doctor = findDoctor(doctorId); if (doctor==null){System.out.println("Not found");return;}
        int count=0; LocalDate date = LocalDate.now();
        while (count<5) {
            int dayIdx = date.getDayOfWeek().getValue()-1;
            for (int h=0; h<24 && count<5; h++) {
                if (doctor.getSchedule().isAvailable(dayIdx,h) && isDoctorHourFree(doctorId,date,h)) {
                    System.out.println(date+" " + h+":00");
                    count++;
                }
            }
            date = date.plusDays(1);
            if (date.isAfter(LocalDate.now().plusDays(30))) break; // limit search horizon
        }
        if (count==0) System.out.println("No free slots in next 30 days.");
    }


    private Doctor findDoctor(String id){ for (int i=0;i<doctors.size();i++) if (doctors.get(i).getId().equals(id)) return doctors.get(i); return null; }
    private Patient findPatient(String id){ for (int i=0;i<patients.size();i++) if (patients.get(i).getId().equals(id)) return patients.get(i); return null; }
    // Room model removed

    private boolean isDoctorHourFree(String doctorId, LocalDate date, int hour) {
        for (int i=0;i<consultations.size();i++) {
            Consultation c = consultations.get(i);
            if (c.getDoctorId().equals(doctorId) && c.getDate().equals(date) && c.getHour()==hour) return false;
        }
        return true;
    }

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
    }

    private void printPatientSummary() {
        System.out.println("Patients:");
        System.out.printf("%-8s %-20s%n","ID","Name");
        for (int i=0;i<patients.size();i++) {
            Patient p = patients.get(i);
            System.out.printf("%-8s %-20s%n", p.getId(), p.getName());
        }
    }

    private List<Integer> getFreeHoursForDoctorDate(Doctor doctor, LocalDate date) {
        List<Integer> list = new ArrayList<>();
        int dayIdx = date.getDayOfWeek().getValue() - 1;
        for (int h=0; h<24; h++) {
            if (doctor.getSchedule().isAvailable(dayIdx,h) && isDoctorHourFree(doctor.getId(), date, h)) {
                list.add(h);
            }
        }
        return list;
    }

    private void printUpcomingAvailability(Doctor doctor, int scanDays, int maxLines) {
        System.out.println("Upcoming availability (A=free with room):");
        int printed = 0; LocalDate date = LocalDate.now();
        while (printed < maxLines && date.isBefore(LocalDate.now().plusDays(scanDays))) {
            List<Integer> free = getFreeHoursForDoctorDate(doctor, date);
            if (!free.isEmpty()) {
                System.out.print(date+" ");
                for (int h: free) System.out.print(h+" ");
                System.out.println();
                printed++;
            }
            date = date.plusDays(1);
        }
        if (printed==0) System.out.println("(No availability in next "+scanDays+" days)");
    }
}
