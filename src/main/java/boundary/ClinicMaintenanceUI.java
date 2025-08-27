package boundary;

import java.util.Scanner;
import utility.InputUtil;
import control.PatientMaintenance;

public class ClinicMaintenanceUI {
    private Scanner input = new Scanner(System.in);
    private final PatientMaintenance patientCtrl = new PatientMaintenance();

    public void printHeader(String headerMsg) {
        System.out.println("\n-----------------------------------------------");
        System.out.println(headerMsg);
        System.out.println("-----------------------------------------------");
    }

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

    // User Side entry: register new patient or use system as existing patient
    public void runUserSideMenu() {
        printHeader("User Portal");
        int choice = getPaitentMenuChoice();
        InputUtil.clearScreen();
        switch (choice) {
            case 1 -> handleNewPatientRegistration();
            case 2 -> handleExistingPatientLogin();
            case 3 -> { return; }
            default -> System.out.println("Invalid choice.");
        }
        if (choice != 3) InputUtil.pauseScreen();
    }

    // New Patient: registration similar to Patient Management add patient
    public void handleNewPatientRegistration() {
    printHeader("Adding a New Patient (Enter '0' to go back)");

    String name = InputUtil.getInputWithBackOption(input, "Enter patient name: ");
    if (name == null) return;
    String gender = InputUtil.getMFChoiceWithBackOption(input, "Enter patient gender", "Male", "Female");
    if (gender == null) return;
    String phone = InputUtil.getValidatedPhoneWithBackOption(input, "Enter patient phone number (digits 7-15): ");
    if (phone == null) return;
    String email = InputUtil.getValidatedEmailWithBackOption(input, "Enter patient email: ");
    if (email == null) return;
    String dob = InputUtil.getValidatedDateWithBackOption(input, "Enter patient date of birth (yyyy-MM-dd): ");
    if (dob == null) return;
    String nationality = InputUtil.getMFChoiceWithBackOption(input, "Enter patient nationality (M=Malaysian, F=Foreigner)", "Malaysian", "Foreigner");
    if (nationality == null) return;

    entity.Patient added = patientCtrl.addPatient(name, gender, phone, email, dob, nationality);
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

    // Existing Patient: check patients for the entered ID and launch user-side UI for that ID
    public void handleExistingPatientLogin() {
        printHeader("Existing Patient Login");
        String pid = InputUtil.getNonEmptyInput(input, "Enter your Patient ID: ");
        entity.Patient p = patientCtrl.findPatientById(pid);
        if (p == null) {
            System.out.println("Patient not found.");
            return;
        }
        // Launch user-side portal for the provided patient ID
        InputUtil.clearScreen();
        userSideMaintenanceUI ui = new userSideMaintenanceUI();
        ui.setPatientId(pid);
        ui.run();
    }

}
