/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package boundary;

import java.util.Scanner;
import utility.InputUtil;
import entity.Patient;
/**
 *
 * @author Whrl
 */
public class PatientMaintenanceUI {
    
    private Scanner scanner = new Scanner(System.in);

    public int getMenuChoice() {
        System.out.println("Please select an option:");
        System.out.println("1. Add Patient");
        System.out.println("2. Update Patient");
        System.out.println("3. Delete Patient");
        System.out.println("4. Search Patient");
        System.out.println("5. View Visit Records");
        System.out.println("6. Exit");
        System.out.print("Select an option: ");
        return InputUtil.getIntInput(scanner, "Enter your choice: ");
    }

    public void displayPatientsTable(String outputStr) {
    System.out.println("\n----------------------------------------------------------------------------------------------------------------------");
        System.out.println("Patient List");
    System.out.println("----------------------------------------------------------------------------------------------------------------------");
    System.out.printf("%-10s|%-20s|%-10s|%-15s|%-15s|%-25s|%-15s\n", "ID", "Name", "Age", "Gender", "Phone", "Email", "Nationality");
    System.out.println("----------------------------------------------------------------------------------------------------------------------");
        if (outputStr == null || outputStr.trim().isEmpty()) {
            System.out.println("No records found.\n");
        } else {
            System.out.println(outputStr);
        }
    }

    public Patient inputPatientDetails() {
        String name = InputUtil.getInput(scanner, "Enter patient name: ");
        String gender = InputUtil.getInput(scanner, "Enter patient gender: ");
        String phoneNumber = InputUtil.getInput(scanner, "Enter patient phone number: ");
        String email = InputUtil.getInput(scanner, "Enter patient email: ");
        String dateOfBirth = InputUtil.getInput(scanner, "Enter patient date of birth (yyyy-MM-dd): ");
        String nationality = InputUtil.getInput(scanner, "Enter patient nationality: ");

    return new Patient(null, name, gender, phoneNumber, email, dateOfBirth, nationality);
    }

    public void displayPatientDetails(Patient patient) {
        System.out.println("\nPatient Details");
        System.out.println("-----------------------------------------------");
        System.out.printf("ID: %s\n", patient.getId());
        System.out.printf("Name: %s\n", patient.getName());
        System.out.printf("Gender: %s\n", patient.getGender());
        System.out.printf("Phone: %s\n", patient.getPhoneNumber());
        System.out.printf("Email: %s\n", patient.getEmail());
        System.out.printf("Nationality: %s\n", patient.getNationality());
        System.out.printf("Date of Birth: %s\n", patient.getDateOfBirth());
        System.out.printf("Age: %s\n", patient.calculateAge(patient.getDateOfBirth()));
        System.out.println("-----------------------------------------------");
    }

    public void displayPatientAddedMessage(Patient patient){
        System.out.println("Patient : " + patient.getName() + " added successfully:");
        displayPatientDetails(patient);
    }

    public void displayPatientUpdatedMessage(Patient patient){
        System.out.println("Patient : " + patient.getName() + " updated successfully:");
        displayPatientDetails(patient);
    }

    public void displayNotFoundMessage(String patientId) {
        System.out.println("Patient with ID: " + patientId + " not found.");
    }

    public void displayDeletedMessage(String patientId) {
        System.out.println("Patient with ID: " + patientId + " deleted successfully.");
    }
    
}