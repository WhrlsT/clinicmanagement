package boundary;

import java.util.Scanner;
import utility.InputUtil;
import control.PatientMaintenance;

public class ClinicMaintenanceUI {
    private Scanner input = new Scanner(System.in);
    private final PatientMaintenance patientCtrl = new PatientMaintenance();
    private final PatientMaintenanceUI patientUI = new PatientMaintenanceUI();
    private final DoctorMaintenanceUI doctorUI = new DoctorMaintenanceUI();
    private final ConsultationMaintenanceUI consultUI = new ConsultationMaintenanceUI();

    public void printHeader(String headerMsg) {
        System.out.println("\n-----------------------------------------------");
        System.out.println(headerMsg);
        System.out.println("-----------------------------------------------");
    }

    // Basic IO wrappers to keep IO concerns inside UI
    public void clearScreen() { InputUtil.clearScreen(); }
    public void pause() { InputUtil.pauseScreen(); }

    public int getMenuChoice() {
        System.out.println("Please select an option:");
        System.out.println("1. Patient Management");
        System.out.println("2. Doctor Management");
    System.out.println("3. Consultation Management");
    System.out.println("4. Medical Treatment Management");
    System.out.println("5. Pharmacy Management");
    System.out.println("6. Exit");
        System.out.println("-----------------------------------------------");
        return InputUtil.getIntInput(input, "Enter your choice: ");
    }

    public int getMainMenuChoice() {
        System.out.println("Please select an option:");
        System.out.println("1. User Side");
        System.out.println("2. Admin Side");
        System.out.println("-----------------------------------------------");
        return InputUtil.getIntInput(input, "Enter your choice: ");
    }

    public int getPaitentMenuChoice() {
        System.out.println("Please select an option:");
        System.out.println("1. New Patient");
        System.out.println("2. Existing Patient");
    System.out.println("3. Return to Main Menu");
        return InputUtil.getIntInput(input, "Enter your choice: ");
    }

    // User Side UI helpers (IO only)
    public void printUserPortalHeader() { printHeader("User Portal"); }

    // Gather all registration inputs; return null if user chose to go back
    public String[] promptNewPatientRegistration() {
        printHeader("Adding a New Patient (Enter '0' to go back)");
        String name = InputUtil.getInputWithBackOption(input, "Enter patient name: ");
        if (name == null) return null;
        String gender = InputUtil.getMFChoiceWithBackOption(input, "Enter patient gender", "Male", "Female");
        if (gender == null) return null;
        String phone = InputUtil.getValidatedPhoneWithBackOption(input, "Enter patient phone number (digits 7-15): ");
        if (phone == null) return null;
        String email = InputUtil.getValidatedEmailWithBackOption(input, "Enter patient email: ");
        if (email == null) return null;
        String dob = InputUtil.getValidatedDateWithBackOption(input, "Enter patient date of birth (yyyy-MM-dd): ");
        if (dob == null) return null;
        String nationality = InputUtil.getMFChoiceWithBackOption(input, "Enter patient nationality (M=Malaysian, F=Foreigner)", "Malaysian", "Foreigner");
        if (nationality == null) return null;
        return new String[]{ name, gender, phone, email, dob, nationality };
    }

    public void displayNewPatientSummary(entity.Patient added) {
        System.out.println("\nPatient added successfully.");
        System.out.println("-----------------------------------------------");
        System.out.printf("ID: %s\n", added.getId());
        System.out.printf("Name: %s\n", added.getName());
        System.out.printf("Gender: %s\n", added.getGender());
        System.out.printf("Phone: %s\n", added.getPhoneNumber());
        System.out.printf("Email: %s\n", added.getEmail());
        System.out.printf("Nationality: %s\n", added.getNationality());
        System.out.printf("Date of Birth: %s\n", added.getDateOfBirth());
        System.out.printf("Age: %s\n", added.calculateAge(added.getDateOfBirth()));
        System.out.println("-----------------------------------------------");
        System.out.println("Your Patient ID can be used to log in as an existing patient.");
    }

    public String promptExistingPatientId() {
        printHeader("Existing Patient Login");
        return InputUtil.getNonEmptyInput(input, "Enter your Patient ID: ");
    }

    public void displayPatientNotFound(String pid) {
        System.out.println("Patient not found for ID: " + pid);
    }

    public void showInvalidChoice() { System.out.println("Invalid choice."); }

    // === Top-level run loop (moved from control.ClinicMaintenance) ===
    public void run() {
        clearScreen();
        printHeader("Welcome to the Clinic Maintenance System");
        while (true) {
            int mainChoice = getMainMenuChoice();
            clearScreen();
            if (mainChoice == 1) {
                // User Side
                runUserSideMenu();
                clearScreen();
                printHeader("Welcome to the Clinic Maintenance System");
            } else if (mainChoice == 2) {
                // Admin Side menu loop
                int choice;
                do {
                    printHeader("Welcome to the Clinic Maintenance System");
                    choice = getMenuChoice();
                    clearScreen();
                    switch (choice) {
                        case 1 -> patientUI.runHub();
                        case 2 -> doctorUI.run();
                        case 3 -> consultUI.run();
                        case 4 -> new TreatmentMaintenanceUI().run();
                        case 5 -> new MedicationMaintenanceUI().run();
                        case 6 -> System.out.println("Returning to Main Menu...");
                        default -> { System.out.println("Invalid choice. Please try again."); pause(); }
                    }
                    if (choice != 6) {
                        clearScreen();
                        printHeader("Welcome to the Clinic Maintenance System");
                    }
                } while (choice != 6);
                // Back to main menu
                clearScreen();
                printHeader("Welcome to the Clinic Maintenance System");
                continue;
            } else {
                System.out.println("Invalid choice. Please try again.");
                pause();
                clearScreen();
                printHeader("Welcome to the Clinic Maintenance System");
            }
        }
    }

    // === User Side entry: register new patient or use system as existing patient ===
    public void runUserSideMenu() {
        printUserPortalHeader();
        int choice = getPaitentMenuChoice();
        clearScreen();
        switch (choice) {
            case 1 -> handleNewPatientRegistration();
            case 2 -> handleExistingPatientLogin();
            case 3 -> { return; }
            default -> showInvalidChoice();
        }
        if (choice != 3) pause();
    }

    // New Patient: registration similar to Patient Management add patient
    private void handleNewPatientRegistration() {
        String[] inputs = promptNewPatientRegistration();
        if (inputs == null) return; // user backed out
        entity.Patient added = patientCtrl.addPatient(inputs[0], inputs[1], inputs[2], inputs[3], inputs[4], inputs[5]);
        displayNewPatientSummary(added);
    }

    // Existing Patient: check patients for the entered ID and launch user-side UI for that ID
    private void handleExistingPatientLogin() {
        String pid = promptExistingPatientId();
        entity.Patient p = patientCtrl.findPatientById(pid);
        if (p == null) {
            displayPatientNotFound(pid);
            return;
        }
        // Launch user-side portal for the provided patient ID
        clearScreen();
        userSideMaintenanceUI ui = new userSideMaintenanceUI();
        ui.setPatientId(pid);
        ui.run();
    }

}
