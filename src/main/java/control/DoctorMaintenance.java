package control;

import adt.*;
import entity.Doctor;
import entity.Consultation;
import utility.InputUtil;
import dao.DoctorDAO;
import dao.ConsultationDAO;
import boundary.DoctorMaintenanceUI;
import java.util.Scanner;

public class DoctorMaintenance {
    private ADTInterface<Doctor> doctorList = new CustomADT<>();
    private DoctorDAO doctorDAO = new DoctorDAO();
    private ConsultationDAO consultationDAO = new ConsultationDAO();
    DoctorMaintenanceUI doctorUI = new DoctorMaintenanceUI();
    private Scanner scanner = new Scanner(System.in);

    public DoctorMaintenance() {
        doctorList = doctorDAO.retrieveFromFile();
    }

    public void runDoctorMaintenance() {
        doctorUI.displayDoctorsTable(getAllDoctors());
        int choice;
        do {
            choice = doctorUI.getMenuChoice();
            switch (choice) {
                case 1:
                    addNewDoctor();
                    break;
                case 2:
                    updateDoctor();
                    break;
                case 3:
                    deleteDoctor();
                    break;
                case 4:
                    doctorUI.displayDoctorsTable(getAllDoctors());
                    break;
                case 5:
                    searchDoctor();
                    break;
                case 6:
                    viewDoctorSchedule();
                    break;
                case 7:
                    setDoctorAvailabilityRange();
                    break;
                case 8:
                    viewDoctorConsultations();
                    break;
                case 9:
                    System.out.println("Returning to Main Menu...");
                    break;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        } while (choice != 9);
    }

    public String getAllDoctors() {
        StringBuilder outputStr = new StringBuilder();
        for (int i = 0; i < doctorList.size(); i++) {
            Doctor d = doctorList.get(i);
                outputStr.append(String.format("%-10s|%-20s|%-15s|%-20s|%-20s\n",
                d.getId(),
                d.getName(),
                d.getSpecialization(),
                d.getPhoneNumber(),
                d.getEmail()
            ));
        }
        return outputStr.toString();
    }

    private String generateNextDoctorId() {
        int maxId = 0;
        for (int i = 0; i < doctorList.size(); i++) {
            Doctor d = doctorList.get(i);
            String id = d.getId();
            if (id != null && id.startsWith("D")) {
                try {
                    int num = Integer.parseInt(id.substring(1));
                    if (num > maxId) {
                        maxId = num;
                    }
                } catch (NumberFormatException e) {
                    // ignore invalid format
                }
            }
        }
        return String.format("D%04d", maxId + 1);
    }

    public void addNewDoctor() {
    String name = InputUtil.getInput(scanner, "Enter doctor name: ");
    String specialization = InputUtil.getInput(scanner, "Enter doctor specialty: ");
    String phone = InputUtil.getValidatedPhone(scanner, "Enter doctor phone number (digits 7-15): ");
    String email = InputUtil.getValidatedEmail(scanner, "Enter doctor email: ");
    Doctor newDoctor = new Doctor(null, name, specialization, phone, email);
        newDoctor.setId(generateNextDoctorId());
        doctorList.add(newDoctor);
        doctorDAO.saveToFile(doctorList);
        doctorUI.displayDoctorAddedMessage(newDoctor);
    }

    public void updateDoctor() {
        String doctorId = InputUtil.getInput(scanner, "Enter doctor ID to update: ");
        Doctor doctor = findDoctorById(doctorId);

        if (doctor != null) {
            doctorUI.displayDoctorDetails(doctor);

            // Update fields with optional blank to keep & validation
            doctor.setName(promptOptional("Name", doctor.getName()));
            doctor.setSpecialization(promptOptional("Specialty", doctor.getSpecialization()));
            doctor.setPhoneNumber(promptOptionalValidatedPhone("Phone Number", doctor.getPhoneNumber()));
            doctor.setEmail(promptOptionalValidatedEmail("Email", doctor.getEmail()));

            doctorDAO.saveToFile(doctorList);
            doctorUI.displayDoctorUpdatedMessage(doctor);
        } else {
            doctorUI.displayNotFoundMessage(doctorId);
        }
    }

    private String promptOptional(String label, String current) {
        System.out.println("Current " + label + ": " + (current==null?"":current));
        String inp = InputUtil.getInput(scanner, "Enter new " + label + " (blank keep): ");
        return inp.isEmpty()? current : inp;
    }

    private String promptOptionalValidatedPhone(String label, String current) {
        System.out.println("Current " + label + ": " + (current==null?"":current));
        while (true) {
            String inp = InputUtil.getInput(scanner, "Enter new " + label + " (blank keep): ");
            if (inp.isEmpty()) return current;
            if (inp.matches("^[0-9]{7,15}$")) return inp;
            System.out.println("Invalid phone (digits 7-15). Try again.");
        }
    }

    private String promptOptionalValidatedEmail(String label, String current) {
        System.out.println("Current " + label + ": " + (current==null?"":current));
        while (true) {
            String inp = InputUtil.getInput(scanner, "Enter new " + label + " (blank keep): ");
            if (inp.isEmpty()) return current;
            if (inp.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) return inp;
            System.out.println("Invalid email format. Try again.");
        }
    }

    private Doctor findDoctorById(String doctorId) {
        for (int i = 0; i < doctorList.size(); i++) {
            Doctor doctor = doctorList.get(i);
            if (doctor.getId().equals(doctorId)) {
                return doctor;
            }
        }
        return null;
    }

    private void viewDoctorSchedule() {
        String doctorId = InputUtil.getInput(scanner, "Enter doctor ID to view schedule: ");
        Doctor doctor = findDoctorById(doctorId);
        if (doctor == null) {
            doctorUI.displayNotFoundMessage(doctorId);
            return;
        }
        System.out.println("Schedule for Dr. " + doctor.getName());
        System.out.println("1. Full 24h table\n2. Compact (hours as columns)\n3. Compact cropped to active hours");
        int mode = InputUtil.getIntInput(scanner, "Choose display mode: ");
        switch (mode) {
            case 1: doctor.getSchedule().printScheduleTable(); break;
            case 2: doctor.getSchedule().printCompactScheduleTable(false); break;
            case 3: doctor.getSchedule().printCompactScheduleTable(true); break;
            default: doctor.getSchedule().printScheduleTable();
        }
    }

    private void setDoctorAvailabilityRange() {
        String doctorId = InputUtil.getInput(scanner, "Enter doctor ID: ");
        Doctor doctor = findDoctorById(doctorId);
        if (doctor == null) {
            doctorUI.displayNotFoundMessage(doctorId);
            return;
        }
        System.out.println("Select day (0=Mon .. 6=Sun)");
        int day = InputUtil.getIntInput(scanner, "Day index: ");
        int startHour = InputUtil.getIntInput(scanner, "Start hour (0-23): ");
        int endHour = InputUtil.getIntInput(scanner, "End hour (1-24): ");
        System.out.println("Status: 1=AVAILABLE, 2=NOT_AVAILABLE, 3=CLEAR BOOKINGS TO AVAILABLE");
        int statusChoice = InputUtil.getIntInput(scanner, "Choose status: ");
        entity.SlotStatus status = entity.SlotStatus.AVAILABLE;
        switch (statusChoice) {
            case 1: status = entity.SlotStatus.AVAILABLE; break;
            case 2: status = entity.SlotStatus.NOT_AVAILABLE; break;
            case 3: status = entity.SlotStatus.AVAILABLE; break;
            default: System.out.println("Invalid status, defaulting to AVAILABLE");
        }
        if (startHour < 0) startHour = 0;
        if (endHour > 24) endHour = 24;
        if (endHour <= startHour) { System.out.println("End hour must be > start hour"); return; }
        // If marking NOT_AVAILABLE, ensure no booked slots would be overridden
    // BOOKED state removed; no need to guard against overriding bookings in template
        doctor.getSchedule().setAvailabilityRange(day, startHour, endHour, status);
        doctorDAO.saveToFile(doctorList);
        System.out.println("Availability updated.");
    }

    public ADTInterface<Doctor> findDoctorByIdOrName(String query) {
        ADTInterface<Doctor> results = new CustomADT<>();
        String lowerQuery = query.toLowerCase();
        for (int i = 0; i < doctorList.size(); i++) {
            Doctor doctor = doctorList.get(i);
            if (doctor.getId().equalsIgnoreCase(query) ||
                doctor.getName().toLowerCase().contains(lowerQuery)) {
                results.add(doctor);
            }
        }
        return results;
    }

    public void searchDoctor() {
        String query = InputUtil.getInput(scanner, "Enter doctor ID or name to search: ");
        ADTInterface<Doctor> foundDoctors = findDoctorByIdOrName(query);
        if (foundDoctors.size() > 0) {
            for (int i = 0; i < foundDoctors.size(); i++) {
                doctorUI.displayDoctorDetails(foundDoctors.get(i));
            }
        } else {
            doctorUI.displayNotFoundMessage(query);
        }
    }

    public void deleteDoctor() {
        String doctorId = InputUtil.getInput(scanner, "Enter doctor ID to delete: ");
        Doctor doctor = findDoctorById(doctorId);

        if (doctor != null) {
            doctorList.remove(doctor);
            reassignDoctorIds();
            doctorDAO.saveToFile(doctorList);
            doctorUI.displayDeletedMessage(doctorId);
        } else {
            doctorUI.displayNotFoundMessage(doctorId);
        }
    }

    // Reassign IDs in sequential order after deletion
    private void reassignDoctorIds() {
        ADTInterface<Doctor> tempList = new CustomADT<>();
        for (int i = 0; i < doctorList.size(); i++) {
            tempList.add(doctorList.get(i));
        }

        // Bubble sort by ID numeric part
        for (int i = 0; i < tempList.size() - 1; i++) {
            for (int j = 0; j < tempList.size() - i - 1; j++) {
                Doctor d1 = tempList.get(j);
                Doctor d2 = tempList.get(j + 1);
                try {
                    int n1 = Integer.parseInt(d1.getId().substring(1));
                    int n2 = Integer.parseInt(d2.getId().substring(1));
                    if (n1 > n2) {
                        tempList.swap(j, j + 1);
                    }
                } catch (Exception e) {
                    // ignore
                }
            }
        }

        for (int i = 0; i < tempList.size(); i++) {
            tempList.get(i).setId(String.format("D%04d", i + 1));
            doctorList.set(i, tempList.get(i));
        }
    }

    private void viewDoctorConsultations() {
        String doctorId = InputUtil.getInput(scanner, "Enter doctor ID to view consultations: ");
        Doctor doctor = findDoctorById(doctorId);
        if (doctor == null) {
            doctorUI.displayNotFoundMessage(doctorId);
            return;
        }

        // Load consultations and filter by doctor ID
        ADTInterface<Consultation> allConsultations = consultationDAO.load();
        StringBuilder consultationsOutput = new StringBuilder();
        
        for (int i = 0; i < allConsultations.size(); i++) {
            Consultation consultation = allConsultations.get(i);
            if (consultation.getDoctorId().equals(doctorId)) {
                consultationsOutput.append(String.format("%-10s %-10s %-12s %-10s %-30s\n",
                    consultation.getId(),
                    consultation.getPatientId(),
                    consultation.getDate(),
                    consultation.getStatus(),
                    consultation.getReason()
                ));
            }
        }

        doctorUI.displayConsultations(doctor.getName(), consultationsOutput.toString());
    }
}
