package boundary;

import java.util.Scanner;
import utility.InputUtil;
import entity.Doctor;

public class DoctorMaintenanceUI {

    private Scanner scanner = new Scanner(System.in);

    public int getMenuChoice() {
        System.out.println("Please select an option:");
        System.out.println("1. Add Doctor");
        System.out.println("2. Update Doctor");
        System.out.println("3. Delete Doctor");
        System.out.println("4. View Doctor");
        System.out.println("5. Search Doctor");
    System.out.println("6. View Doctor Schedule");
    System.out.println("7. Set Doctor Availability Range");
    System.out.println("8. Exit");
        System.out.print("Select an option: ");
        return InputUtil.getIntInput(scanner, "Enter your choice: ");
    }

    public void displayDoctorsTable(String outputStr) {
        System.out.println("\n----------------------------------------------------------------------------------------------------------------------------------------");
        System.out.println("Doctor List");
        System.out.println("----------------------------------------------------------------------------------------------------------------------------------------");
    System.out.printf("%-10s|%-20s|%-15s|%-20s|%-20s\n", "ID", "Name", "Specialty", "Phone", "Email");
    System.out.println("----------------------------------------------------------------------------------------------------------------------------------------");
        if (outputStr == null || outputStr.trim().isEmpty()) {
            System.out.println("No records found.\n");
        } else {
            System.out.println(outputStr);
        }
    }

    public Doctor inputDoctorDetails() {
        String name = InputUtil.getInput(scanner, "Enter doctor name: ");
        String specialty = InputUtil.getInput(scanner, "Enter doctor specialty: ");
        String phoneNumber = InputUtil.getInput(scanner, "Enter doctor phone number: ");
    // Address field removed from Doctor entity and listing; omit address input.
        String email = InputUtil.getInput(scanner, "Enter doctor email: ");
        // Add other fields as needed

        return new Doctor(null, name, specialty, phoneNumber, email);
    }

    public void displayDoctorDetails(Doctor doctor) {
        System.out.println("\nDoctor Details");
        System.out.println("-----------------------------------------------");
        System.out.printf("ID: %s\n", doctor.getId());
        System.out.printf("Name: %s\n", doctor.getName());
        System.out.printf("Specialty: %s\n", doctor.getSpecialization());
        System.out.printf("Phone: %s\n", doctor.getPhoneNumber());
        System.out.printf("Email: %s\n", doctor.getEmail());
        System.out.println("-----------------------------------------------");
    }

    public void displayDoctorAddedMessage(Doctor doctor){
        System.out.println("Doctor : " + doctor.getName() + " added successfully:");
        displayDoctorDetails(doctor);
    }

    public void displayDoctorUpdatedMessage(Doctor doctor){
        System.out.println("Doctor : " + doctor.getName() + " updated successfully:");
        displayDoctorDetails(doctor);
    }

    public void displayNotFoundMessage(String doctorId) {
        System.out.println("Doctor with ID: " + doctorId + " not found.");
    }

    public void displayDeletedMessage(String doctorId) {
        System.out.println("Doctor with ID: " + doctorId + " deleted successfully.");
    }
}
