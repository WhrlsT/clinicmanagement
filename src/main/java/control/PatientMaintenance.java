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

    public String getAllPatients() {
        StringBuilder outputStr = new StringBuilder();
        for (int i = 0; i < patientList.size(); i++) {
            Patient p = patientList.get(i);
            outputStr.append(String.format("%-10s|%-20s|%-10s|%-15s|%-15s|%-25s|%-15s|%-15s\n",
                p.getId(),
                p.getName(),
                p.calculateAge(p.getDateOfBirth()),
                p.getGender(),
                p.getPhoneNumber(),
                p.getEmail(),
                p.getAddress(),
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
        Patient newPatient = patientUI.inputPatientDetails();
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
            
            // Update fields
            patient.setId(InputUtil.getInput(scanner, "Enter new patient ID: "));
            patient.setName(InputUtil.getInput(scanner, "Enter new patient Name: "));
            patient.setAddress(InputUtil.getInput(scanner, "Enter new patient Address: "));
            patient.setPhoneNumber(InputUtil.getInput(scanner, "Enter new patient Phone Number: "));
            patient.setEmail(InputUtil.getInput(scanner, "Enter new patient Email: "));
            patient.setDateOfBirth(InputUtil.getInput(scanner, "Enter new patient Date of Birth (yyyy-MM-dd): "));
            patient.setNationality(InputUtil.getInput(scanner, "Enter new patient Nationality: "));

            patientDAO.saveToFile(patientList);
            patientUI.displayPatientUpdatedMessage(patient);
        } else {
            patientUI.displayNotFoundMessage(patientId);
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
            for (int i = 0; i < foundPatients.size(); i++) {
                patientUI.displayPatientDetails(foundPatients.get(i));
            }
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
