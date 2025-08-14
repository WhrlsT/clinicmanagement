package control;

import adt.*;
import entity.Doctor;
import utility.InputUtil;
import dao.DoctorDAO;
import boundary.DoctorMaintenanceUI;
import java.util.Scanner;

public class DoctorMaintenance {
    private ADTInterface<Doctor> doctorList = new CustomADT<>();
    private DoctorDAO doctorDAO = new DoctorDAO();
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
                    System.out.println("Returning to Main Menu...");
                    break;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        } while (choice != 6);
    }

    public String getAllDoctors() {
        StringBuilder outputStr = new StringBuilder();
        for (int i = 0; i < doctorList.size(); i++) {
            Doctor d = doctorList.get(i);
            outputStr.append(String.format("%-10s|%-20s|%-15s|%-20s|%-20s|%-25s\n",
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
        Doctor newDoctor = doctorUI.inputDoctorDetails();
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

            // Update fields
            doctor.setId(InputUtil.getInput(scanner, "Enter new doctor ID: "));
            doctor.setName(InputUtil.getInput(scanner, "Enter new doctor Name: "));
            doctor.setSpecialization(InputUtil.getInput(scanner, "Enter new doctor Specialty: "));
            doctor.setPhoneNumber(InputUtil.getInput(scanner, "Enter new doctor Phone Number: "));
            doctor.setEmail(InputUtil.getInput(scanner, "Enter new doctor Email: "));

            doctorDAO.saveToFile(doctorList);
            doctorUI.displayDoctorUpdatedMessage(doctor);
        } else {
            doctorUI.displayNotFoundMessage(doctorId);
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
}
