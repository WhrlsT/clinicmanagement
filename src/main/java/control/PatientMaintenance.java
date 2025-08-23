/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package control;

import adt.*;
import entity.*;
import utility.InputUtil;
import dao.*;
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
    private ConsultationDAO consultationDAO = new ConsultationDAO();
    private TreatmentDAO treatmentDAO = new TreatmentDAO();
    private MedicationDAO medicationDAO = new MedicationDAO();
    private DoctorDAO doctorDAO = new DoctorDAO();
    
    ClinicMaintenanceUI clinicUI = new ClinicMaintenanceUI();
    PatientMaintenanceUI patientUI = new PatientMaintenanceUI();
    private Scanner scanner = new Scanner(System.in);

    public PatientMaintenance() {
        patientList = patientDAO.retrieveFromFile();
    }

    public void runPatientMaintenance() {
        InputUtil.clearScreen();
        clinicUI.printHeader("Clinic Patient Maintenance");
        patientUI.displayPatientsTable(getAllPatients());
        int choice;
        do {
            choice = patientUI.getMenuChoice();
            switch (choice) {
                case 1:
                    InputUtil.clearScreen();
                    addNewPatient();
                    break;
                case 2:
                    InputUtil.clearScreen();
                    updatePatient();
                    break;
                case 3:
                    InputUtil.clearScreen();
                    deletePatient();
                    break;
                case 4:
                    InputUtil.clearScreen();
                    clinicUI.printHeader("Clinic Patient Maintenance");
                    patientUI.displayPatientsTable(getAllPatients());
                    break;
                case 5:
                    InputUtil.clearScreen();
                    searchPatient();
                    break;
                case 6:
                    InputUtil.clearScreen();
                    viewPatientVisitRecords();
                    break;
                case 7:
                    System.out.println("Returning to Main Menu...");
                    break;
                default:
                    System.out.println("Invalid choice. Please try again.");
                    InputUtil.pauseScreen();
            }
            if (choice != 7 && choice != 4) {
                InputUtil.pauseScreen();
                InputUtil.clearScreen();
                clinicUI.printHeader("Clinic Patient Maintenance");
                patientUI.displayPatientsTable(getAllPatients());
            }
        } while (choice != 7);
    }

    // New hub flow as requested
    public void runPatientManagementHub() {
        int choice;
        do {
            InputUtil.clearScreen();
            clinicUI.printHeader("Patient Management Hub");
            patientUI.displayPatientsTable(getAllPatients());
            displayQueueSummary();
            System.out.println("1. Manage Patient Details");
            System.out.println("2. Manage Patient Queue");
            System.out.println("3. Back to Main Menu");
            choice = InputUtil.getIntInput(scanner, "Select option: ");
            switch (choice) {
                case 1 -> runPatientMaintenance();
                case 2 -> {
                    InputUtil.clearScreen();
                    new QueueMaintenance().run();
                }
                case 3 -> System.out.println("Returning to main menu...");
                default -> {
                    System.out.println("Invalid selection.");
                    InputUtil.pauseScreen();
                }
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
        clinicUI.printHeader("Clinic Patient Maintenance");
        System.out.println("Adding a New Patient (Enter '0' to go back)");
        System.out.println("─".repeat(50));
        
        String name = getInputWithBackOption("Enter patient name: ");
        if (name == null) return; // User chose to go back
        
        String gender = getMFChoiceWithBackOption("Enter patient gender", "Male", "Female");
        if (gender == null) return;
        
        String phone = getValidatedPhoneWithBackOption("Enter patient phone number (digits 7-15): ");
        if (phone == null) return;
        
        String email = getValidatedEmailWithBackOption("Enter patient email: ");
        if (email == null) return;
        
        String dob = getValidatedDateWithBackOption("Enter patient date of birth (yyyy-MM-dd): ");
        if (dob == null) return;
        
        String nationality = getMFChoiceWithBackOption("Enter patient nationality (M=Malaysian, F=Foreigner)", "Malaysian", "Foreigner");
        if (nationality == null) return;
        
        Patient newPatient = new Patient(null, name, gender, phone, email, dob, nationality);
        newPatient.setId(generateNextPatientId());
        patientList.add(newPatient);
        patientDAO.saveToFile(patientList);
        patientUI.displayPatientAddedMessage(newPatient);
    }   

    public void updatePatient() {
        clinicUI.printHeader("Clinic Patient Maintenance");
        System.out.println("Updating Patient Details (Enter '0' to go back)");
        System.out.println("─".repeat(50));
        
        String patientId = InputUtil.getInput(scanner, "Enter patient ID to update: ");
        if (patientId.equals("0")) {
            return; // Go back
        }
        
        Patient patient = findPatientById(patientId);
        
        if (patient != null) {
            InputUtil.clearScreen();
            clinicUI.printHeader("Clinic Patient Maintenance");
            System.out.println("Updating Patient: " + patient.getName() + " (" + patient.getId() + ")");
            System.out.println("─".repeat(50));
            patientUI.displayPatientDetails(patient);
            System.out.println("Enter new values (leave blank to keep current, '0' to cancel):");
            System.out.println("─".repeat(50));
            
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
        clinicUI.printHeader("Clinic Patient Maintenance");
        System.out.println("Search for a Patient (Enter '0' to go back)");
        System.out.println("─".repeat(50));
        
        String query = InputUtil.getInput(scanner, "Enter patient ID or name to search: ");
        if (query.equals("0")) {
            return; // Go back
        }
        
        ADTInterface<Patient> foundPatients = findPatientByIdOrName(query);
        if (foundPatients.size() > 0) {
            InputUtil.clearScreen();
            clinicUI.printHeader("Clinic Patient Maintenance");
            System.out.println("Search Results for: \"" + query + "\"");
            System.out.println("─".repeat(50));
            
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
        clinicUI.printHeader("Clinic Patient Maintenance");
        System.out.println("Deleting a Patient (Enter '0' to go back)");
        System.out.println("─".repeat(50));
        
        String patientId = InputUtil.getInput(scanner, "Enter patient ID to delete: ");
        if (patientId.equals("0")) {
            return; // Go back
        }
        
        Patient patient = findPatientById(patientId);

        if (patient != null) {
            System.out.println("\nPatient found:");
            patientUI.displayPatientDetails(patient);
            System.out.println("─".repeat(50));
            
            String confirmation = InputUtil.getInput(scanner, "Are you sure you want to delete this patient? (y/N): ");
            if (confirmation.equalsIgnoreCase("y") || confirmation.equalsIgnoreCase("yes")) {
                patientList.remove(patient);
                reassignPatientIds();
                patientDAO.saveToFile(patientList);
                patientUI.displayDeletedMessage(patientId);
            } else {
                System.out.println("Delete operation cancelled.");
            }
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
    
    private void viewPatientVisitRecords() {
        clinicUI.printHeader("Clinic Patient Maintenance");
        System.out.println("View Patient Visit Records (Enter '0' to go back)");
        System.out.println("─".repeat(50));
        
        // Show available patients
        patientUI.displayPatientsTable(getAllPatients());
        
        String patientId = InputUtil.getInput(scanner, "Enter Patient ID to view visit records: ");
        if (patientId.equals("0")) {
            return; // Go back
        }
        
        // Find patient
        Patient patient = findPatientById(patientId);
        if (patient == null) {
            System.out.println("Patient not found.");
            return;
        }
        
        InputUtil.clearScreen();
        
        // Load consultations and treatments
        ADTInterface<Consultation> consultations = consultationDAO.load();
        ADTInterface<Treatment> treatments = treatmentDAO.load();
        ADTInterface<Medication> medications = medicationDAO.load();
        ADTInterface<Doctor> doctors = doctorDAO.retrieveFromFile();
        
        // Display patient info
        System.out.println("\nPatient: " + patient.getName() + " (" + patient.getId() + ")");
        System.out.println("═".repeat(80));
        
        // Find consultations for this patient
        ADTInterface<Consultation> patientConsultations = new CustomADT<>();
        for (int i = 0; i < consultations.size(); i++) {
            Consultation c = consultations.get(i);
            if (c.getPatientId().equals(patientId)) {
                patientConsultations.add(c);
            }
        }
        
        if (patientConsultations.size() == 0) {
            System.out.println("No visit records found for this patient.");
            return;
        }
        
        // Display visit records table
        System.out.println("\nVisit Records:");
        System.out.printf("%-12s | %-12s | %-20s | %-12s | %-10s%n", 
                         "ConsultationID", "Date", "Reason", "Doctor", "Status");
        System.out.println("─".repeat(80));
        
        for (int i = 0; i < patientConsultations.size(); i++) {
            Consultation c = patientConsultations.get(i);
            String doctorName = getDoctorName(doctors, c.getDoctorId());
            System.out.printf("%-12s | %-12s | %-20s | %-12s | %-10s%n",
                             c.getId(), 
                             c.getDate().toString(), 
                             c.getReason(), 
                             doctorName, 
                             c.getStatus());
        }
        
        // Prompt for detailed view
        System.out.println("\nEnter consultation ID to view details (or press Enter to return): ");
        String consultationId = InputUtil.getInput(scanner, "").trim();
        
        if (!consultationId.isEmpty()) {
            InputUtil.clearScreen();
            viewConsultationDetails(consultationId, consultations, treatments, medications, doctors);
        }
    }
    
    private void viewConsultationDetails(String consultationId, ADTInterface<Consultation> consultations, 
                                        ADTInterface<Treatment> treatments, ADTInterface<Medication> medications,
                                        ADTInterface<Doctor> doctors) {
        
        // Find consultation
        Consultation consultation = null;
        for (int i = 0; i < consultations.size(); i++) {
            if (consultations.get(i).getId().equals(consultationId)) {
                consultation = consultations.get(i);
                break;
            }
        }
        
        if (consultation == null) {
            System.out.println("Consultation not found.");
            return;
        }
        
        // Display consultation details
        System.out.println("\n" + "═".repeat(60));
        System.out.println("CONSULTATION DETAILS");
        System.out.println("═".repeat(60));
        System.out.println("Consultation ID: " + consultation.getId());
        System.out.println("Date: " + consultation.getDate());
        System.out.println("Patient ID: " + consultation.getPatientId());
        System.out.println("Doctor: " + getDoctorName(doctors, consultation.getDoctorId()));
        System.out.println("Reason: " + consultation.getReason());
        if (consultation.getNotes() != null && !consultation.getNotes().isEmpty()) {
            System.out.println("Notes: " + consultation.getNotes());
        }
        System.out.println("Status: " + consultation.getStatus());
        
        // Find and display treatments for this consultation
        ADTInterface<Treatment> consultationTreatments = new CustomADT<>();
        for (int i = 0; i < treatments.size(); i++) {
            Treatment t = treatments.get(i);
            if (consultationId.equals(t.getConsultationId())) {
                consultationTreatments.add(t);
            }
        }
        
        if (consultationTreatments.size() > 0) {
            System.out.println("\n" + "─".repeat(60));
            System.out.println("TREATMENTS:");
            System.out.println("─".repeat(60));
            
            for (int i = 0; i < consultationTreatments.size(); i++) {
                Treatment t = consultationTreatments.get(i);
                System.out.println("\nTreatment ID: " + t.getId());
                System.out.println("Type: " + t.getType());
                if (t.getName() != null && !t.getName().isEmpty()) {
                    System.out.println("Name: " + t.getName());
                }
                if (t.getInstructions() != null && !t.getInstructions().isEmpty()) {
                    System.out.println("Instructions: " + t.getInstructions());
                }
                if (t.getNotes() != null && !t.getNotes().isEmpty()) {
                    System.out.println("Notes: " + t.getNotes());
                }
                System.out.println("Status: " + t.getStatus());
                if (t.getOrderedDate() != null) {
                    System.out.println("Ordered Date: " + t.getOrderedDate());
                }
                if (t.getCost() != null) {
                    System.out.println("Cost: $" + String.format("%.2f", t.getCost()));
                }
                
                // Display linked medications
                if (t.getMedicationIds() != null && t.getMedicationIds().length > 0) {
                    System.out.println("Medications:");
                    for (String medId : t.getMedicationIds()) {
                        Medication med = findMedicationById(medications, medId);
                        if (med != null) {
                            System.out.println("  - " + med.getName() + " (" + medId + ")");
                            if (med.getDosage() != null) {
                                System.out.println("    Dosage: " + med.getDosage());
                            }
                            if (med.getFrequency() != null) {
                                System.out.println("    Frequency: " + med.getFrequency());
                            }
                            if (med.getInstructions() != null) {
                                System.out.println("    Instructions: " + med.getInstructions());
                            }
                        }
                    }
                }
                System.out.println("─".repeat(40));
            }
        } else {
            System.out.println("\nNo treatments recorded for this consultation.");
        }
        
        InputUtil.pauseScreen();
    }
    
    private String getDoctorName(ADTInterface<Doctor> doctors, String doctorId) {
        if (doctorId == null || doctorId.equals("UNASSIGNED")) {
            return "UNASSIGNED";
        }
        
        for (int i = 0; i < doctors.size(); i++) {
            Doctor d = doctors.get(i);
            if (d.getId().equals(doctorId)) {
                return d.getName();
            }
        }
        return doctorId; // Return ID if name not found
    }
    
    private Medication findMedicationById(ADTInterface<Medication> medications, String medicationId) {
        for (int i = 0; i < medications.size(); i++) {
            Medication m = medications.get(i);
            if (m.getId().equals(medicationId)) {
                return m;
            }
        }
        return null;
    }
    
    // Helper methods for input with back option
    private String getInputWithBackOption(String prompt) {
        while (true) {
            String input = InputUtil.getInput(scanner, prompt);
            if (input.equals("0")) {
                return null; // Signal to go back
            }
            if (!input.trim().isEmpty()) {
                return input.trim();
            }
            System.out.println("Input cannot be empty. Please try again or enter '0' to go back.");
        }
    }
    
    private String getMFChoiceWithBackOption(String prompt, String mMeaning, String fMeaning) {
        while (true) {
            String input = InputUtil.getInput(scanner, prompt + " (M/F, 0=back): ").trim();
            if (input.equals("0")) {
                return null; // Signal to go back
            }
            if (input.equalsIgnoreCase("M")) {
                return mMeaning;
            }
            if (input.equalsIgnoreCase("F")) {
                return fMeaning;
            }
            System.out.println("Please enter M, F, or 0 to go back.");
        }
    }
    
    private String getValidatedPhoneWithBackOption(String prompt) {
        while (true) {
            String input = InputUtil.getInput(scanner, prompt);
            if (input.equals("0")) {
                return null; // Signal to go back
            }
            if (input.matches("^[0-9]{7,15}$")) {
                return input;
            }
            System.out.println("Invalid phone number (7-15 digits only). Please try again or enter '0' to go back.");
        }
    }
    
    private String getValidatedEmailWithBackOption(String prompt) {
        while (true) {
            String input = InputUtil.getInput(scanner, prompt);
            if (input.equals("0")) {
                return null; // Signal to go back
            }
            if (input.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                return input;
            }
            System.out.println("Invalid email format. Please try again or enter '0' to go back.");
        }
    }
    
    private String getValidatedDateWithBackOption(String prompt) {
        while (true) {
            String input = InputUtil.getInput(scanner, prompt);
            if (input.equals("0")) {
                return null; // Signal to go back
            }
            try {
                java.time.LocalDate.parse(input);
                return input;
            } catch (Exception e) {
                System.out.println("Invalid date format (use yyyy-MM-dd). Please try again or enter '0' to go back.");
            }
        }
    }

}
