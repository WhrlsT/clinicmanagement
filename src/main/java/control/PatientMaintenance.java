/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package control;

import adt.*;
import entity.Patient;
import utility.InputUtil;
import dao.PatientDAO;
import boundary.PatientMaintenanceUI;
import boundary.ClinicMaintenanceUI;
import java.util.Scanner;
/**
 *
 * @author Whrl
 */
public class PatientMaintenance {
    private ADTInterface<Patient> patientList = new CustomADT<>();
    private PatientDAO patientDAO = new PatientDAO();
    ClinicMaintenanceUI clinicUI = new ClinicMaintenanceUI();
    PatientMaintenanceUI patientUI = new PatientMaintenanceUI();
    private Scanner scanner = new Scanner(System.in);

    public PatientMaintenance() {
        patientList = patientDAO.retrieveFromFile();
    }

    public void runPatientMaintenance() {


        clinicUI.printHeader("Clinic Patient Maintenance");
        patientUI.displayPatientsTable(getAllPatients());
        int choice;
        do {
            choice = patientUI.getMenuChoice();
            switch (choice) {
                case 1:
                    addNewPatient();
                    break;
                case 2:
                    updatePatient();
                    break;
                case 3:
                    deletePatient();
                    break;
                case 4:
                    patientUI.displayPatientsTable(getAllPatients());
                    break;
                case 5:
                    searchPatient();
                    break;
                case 6:
                    System.out.println("Returning to Main Menu...");
                    break;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        } while (choice != 6);
    }

    // New hub flow as requested
    public void runPatientManagementHub() {
        int choice;
        do {
            clinicUI.printHeader("Patient Management Hub");
            patientUI.displayPatientsTable(getAllPatients());
            displayQueueSummary();
            System.out.println("1. Manage Patient Details");
            System.out.println("2. Manage Patient Queue");
            System.out.println("3. Back to Main Menu");
            choice = InputUtil.getIntInput(scanner, "Select option: ");
            switch (choice) {
                case 1 -> runPatientMaintenance();
                case 2 -> new QueueMaintenance().run();
                case 3 -> System.out.println("Returning to main menu...");
                default -> System.out.println("Invalid selection.");
            }
        } while (choice != 3);
    }

    private void displayQueueSummary() {
        // Basic summary: counts by status
        try {
            // Access queue through a fresh QueueMaintenance for up-to-date view
            dao.QueueDAO qdao = new dao.QueueDAO();
            adt.ADTInterface<entity.PatientQueueEntry> q = qdao.load();
            int waiting=0, called=0, inprog=0, skipped=0, completed=0;
            for (int i=0;i<q.size();i++) {
                entity.QueueStatus st = q.get(i).getStatus();
                switch(st){
                    case WAITING -> waiting++;
                    case CALLED -> called++;
                    case IN_PROGRESS -> inprog++;
                    case SKIPPED -> skipped++;
                    case COMPLETED -> completed++;
                }
            }
            System.out.println("Queue Summary: WAITING="+waiting+" CALLED="+called+" IN_PROGRESS="+inprog+" SKIPPED="+skipped+" COMPLETED="+completed);
        } catch(Exception e) {
            System.out.println("Queue Summary: (error loading queue)");
        }
    }

    public String getAllPatients() {
        StringBuilder outputStr = new StringBuilder();
        for (int i = 0; i < patientList.size(); i++) {
            Patient p = patientList.get(i);
            outputStr.append(String.format("%-10s|%-20s|%-10s|%-15s|%-15s|%-25s|%-15s\n",
                p.getId(),
                p.getName(),
                p.calculateAge(p.getDateOfBirth()),
                p.getGender(),
                p.getPhoneNumber(),
                p.getEmail(),
                p.getNationality()
            ));
        }
        return outputStr.toString();
    }

    public void viewAllPatients() {
        String outputStr = getAllPatients();
        patientUI.displayPatientsTable(outputStr);
    }    

    private String generateNextPatientId() {
        int maxId = 0;
        for (int i = 0; i < patientList.size(); i++) {
            Patient p = patientList.get(i);
            String id = p.getId();
            if (id != null && id.startsWith("P")) {
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
        return String.format("P%04d", maxId + 1);
    }

    public void addNewPatient() {
    String name = InputUtil.getNonEmptyInput(scanner, "Enter patient name: ");
    String gender = InputUtil.getMFChoice(scanner, "Enter patient gender", "Male", "Female");
    String phone = InputUtil.getValidatedPhone(scanner, "Enter patient phone number (digits 7-15): ");
    String email = InputUtil.getValidatedEmail(scanner, "Enter patient email: ");
    String dob = InputUtil.getValidatedDate(scanner, "Enter patient date of birth (yyyy-MM-dd): ");
    String nationality = InputUtil.getMFChoice(scanner, "Enter patient nationality (M=Malaysian, F=Foreigner)", "Malaysian", "Foreigner");
    Patient newPatient = new Patient(null, name, gender, phone, email, dob, nationality);
        newPatient.setId(generateNextPatientId());
        patientList.add(newPatient);
        patientDAO.saveToFile(patientList);
        patientUI.displayPatientAddedMessage(newPatient);
    }   

    public void updatePatient() {
        String patientId = InputUtil.getInput(scanner, "Enter patient ID to update: ");
        Patient patient = findPatientById(patientId);
        
        if (patient != null) {
            patientUI.displayPatientDetails(patient);
            // For each field: show current, allow blank to keep
            patient.setName(promptOptionalNonEmpty("Name", patient.getName()));
            patient.setGender(promptOptionalMF("Gender", patient.getGender(), "Male", "Female"));
            patient.setPhoneNumber(promptOptionalValidatedPhone("Phone Number", patient.getPhoneNumber()));
            patient.setEmail(promptOptionalValidatedEmail("Email", patient.getEmail()));
            patient.setDateOfBirth(promptOptionalDate("Date of Birth (yyyy-MM-dd)", patient.getDateOfBirth()));
            patient.setNationality(promptOptionalMF("Nationality (M=Malaysian,F=Foreigner)", patient.getNationality(), "Malaysian", "Foreigner"));

            patientDAO.saveToFile(patientList);
            patientUI.displayPatientUpdatedMessage(patient);
        } else {
            patientUI.displayNotFoundMessage(patientId);
        }
    }

    private String promptOptionalNonEmpty(String label, String current) {
        System.out.println("Current " + label + ": " + (current==null?"":current));
        while (true) {
            String inp = InputUtil.getInput(scanner, "Enter new " + label + " value (leave blank to keep): ");
            if (inp.isEmpty()) return current;
            if (!inp.isEmpty()) return inp;
        }
    }

    private String promptOptionalMF(String label, String current, String mMeaning, String fMeaning) {
        System.out.println("Current " + label + ": " + (current==null?"":current));
        while (true) {
            String inp = InputUtil.getInput(scanner, "Enter new " + label + " (M/F, blank keep): ").trim();
            if (inp.isEmpty()) return current;
            if (inp.equalsIgnoreCase("M")) return mMeaning;
            if (inp.equalsIgnoreCase("F")) return fMeaning;
            System.out.println("Please enter only M or F.");
        }
    }

    private String promptOptionalValidatedPhone(String label, String current) {
        System.out.println("Current " + label + ": " + (current==null?"":current));
        while (true) {
            String inp = InputUtil.getInput(scanner, "Enter new " + label + " value (leave blank to keep): ");
            if (inp.isEmpty()) return current;
            if (inp.matches("^[0-9]{7,15}$")) return inp;
            System.out.println("Invalid phone (digits 7-15). Try again.");
        }
    }

    private String promptOptionalValidatedEmail(String label, String current) {
        System.out.println("Current " + label + ": " + (current==null?"":current));
        while (true) {
            String inp = InputUtil.getInput(scanner, "Enter new " + label + " value (leave blank to keep): ");
            if (inp.isEmpty()) return current;
            if (inp.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) return inp;
            System.out.println("Invalid email format. Try again.");
        }
    }

    private String promptOptionalDate(String label, String current) {
        System.out.println("Current " + label + ": " + (current==null?"":current));
        while (true) {
            String inp = InputUtil.getInput(scanner, "Enter new " + label + " value (leave blank to keep): ");
            if (inp.isEmpty()) return current;
            try { java.time.LocalDate.parse(inp); return inp; } catch(Exception e){ System.out.println("Invalid date (yyyy-MM-dd). Try again."); }
        }
    }

    private Patient findPatientById(String patientId) {
        for (int i = 0; i < patientList.size(); i++) {
            Patient patient = patientList.get(i);
            if (patient.getId().equals(patientId)) {
                return patient;
            }
        }
        return null;
    }    

    public ADTInterface<Patient> findPatientByIdOrName(String query) {
        ADTInterface<Patient> results = new CustomADT<>();
        String lowerQuery = query.toLowerCase();
        for (int i = 0; i < patientList.size(); i++) {
            Patient patient = patientList.get(i);
            if (patient.getId().equalsIgnoreCase(query) ||
                patient.getName().toLowerCase().contains(lowerQuery)) {
                results.add(patient);
            }
        }
        return results;
    }

    public void searchPatient() {
        String query = InputUtil.getInput(scanner, "Enter patient ID or name to search: ");
        ADTInterface<Patient> foundPatients = findPatientByIdOrName(query);
        if (foundPatients.size() > 0) {
            // Build table string similar to getAllPatients()
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < foundPatients.size(); i++) {
                Patient p = foundPatients.get(i);
                sb.append(String.format("%-10s|%-20s|%-10s|%-15s|%-15s|%-25s|%-15s\n",
                        p.getId(),
                        p.getName(),
                        p.calculateAge(p.getDateOfBirth()),
                        p.getGender(),
                        p.getPhoneNumber(),
                        p.getEmail(),
                        p.getNationality()
                ));
            }
            patientUI.displayPatientsTable(sb.toString());
        } else {
            patientUI.displayNotFoundMessage(query);
        }
    }

    public void deletePatient() {
        String patientId = InputUtil.getInput(scanner, "Enter patient ID to delete: ");
        Patient patient = findPatientById(patientId);

        if (patient != null) {
            patientList.remove(patient);
            reassignPatientIds();
            patientDAO.saveToFile(patientList);
            patientUI.displayDeletedMessage(patientId);
        } else {
            patientUI.displayNotFoundMessage(patientId);
        }
    }

    // Reassign IDs in sequential order after deletion
    private void reassignPatientIds() {
        // Copy to a new CustomADT for sorting
        ADTInterface<Patient> tempList = new CustomADT<>();
        for (int i = 0; i < patientList.size(); i++) {
            tempList.add(patientList.get(i));
        }

        // Simple bubble sort by ID numeric part (since CustomADT has no sort)
        for (int i = 0; i < tempList.size() - 1; i++) {
            for (int j = 0; j < tempList.size() - i - 1; j++) {
                Patient p1 = tempList.get(j);
                Patient p2 = tempList.get(j + 1);
                try {
                    int n1 = Integer.parseInt(p1.getId().substring(1));
                    int n2 = Integer.parseInt(p2.getId().substring(1));
                    if (n1 > n2) {
                        // Swap
                        tempList.set(j, p2);
                        tempList.set(j + 1, p1);
                    }
                } catch (Exception e) {
                    // ignore
                }
            }
        }

        // Reassign IDs and update patientList
        for (int i = 0; i < tempList.size(); i++) {
            tempList.get(i).setId(String.format("P%04d", i + 1));
            patientList.set(i, tempList.get(i));
        }
    }

}
