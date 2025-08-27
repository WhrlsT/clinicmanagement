/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package control;

import boundary.*;
import utility.MigrationRunner;
import utility.InputUtil;
/**
 *
 * @author Whrl
 */
public class ClinicMaintenance {
    private ClinicMaintenanceUI mainUI = new ClinicMaintenanceUI();
    private boundary.PatientMaintenanceUI patientUI = new boundary.PatientMaintenanceUI();
    private DoctorMaintenanceUI doctorUI = new DoctorMaintenanceUI();
    private ConsultationMaintenanceUI consultUI = new ConsultationMaintenanceUI();
    DoctorMaintenance doctorMaintenance = new DoctorMaintenance();

    public void runClinicMaintenance() {
        InputUtil.clearScreen();
        mainUI.printHeader("Welcome to the Clinic Maintenance System");
        while (true) {
            int mainChoice = mainUI.getMainMenuChoice();
            InputUtil.clearScreen();
            if (mainChoice == 1) {
                // User Side
                mainUI.runUserSideMenu();
                InputUtil.clearScreen();
                mainUI.printHeader("Welcome to the Clinic Maintenance System");
            } else if (mainChoice == 2) {
                // Admin Side menu loop
                int choice;
                do {
                    mainUI.printHeader("Welcome to the Clinic Maintenance System");
                    choice = mainUI.getMenuChoice();
                    InputUtil.clearScreen();
                    switch (choice) {
                        case 1 -> patientUI.runHub();
                        case 2 -> doctorUI.run();
                        case 3 -> consultUI.run();
                        case 4 -> new boundary.TreatmentMaintenanceUI().run();
                        case 5 -> new boundary.MedicationMaintenanceUI().run();
                        case 6 -> System.out.println("Returning to Main Menu...");
                        default -> { System.out.println("Invalid choice. Please try again."); InputUtil.pauseScreen(); }
                    }
                    if (choice != 6) {
                        InputUtil.clearScreen();
                        mainUI.printHeader("Welcome to the Clinic Maintenance System");
                    }
                } while (choice != 6);
                // Back to main menu
                InputUtil.clearScreen();
                mainUI.printHeader("Welcome to the Clinic Maintenance System");
                continue;
            } else {
                System.out.println("Invalid choice. Please try again.");
                InputUtil.pauseScreen();
                InputUtil.clearScreen();
                mainUI.printHeader("Welcome to the Clinic Maintenance System");
            }
        }
    }

    public static void main(String[] args) {
        ClinicMaintenance clinicMaintenance = new ClinicMaintenance();
        // Run schedule migration using runner
        boolean didMig = MigrationRunner.run(clinicMaintenance.doctorMaintenance, null);
        if (didMig) {
            System.out.println("[Migration] Legacy schedule entries normalized.");
        }
        clinicMaintenance.runClinicMaintenance();
    }
}
