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
        int choice;
        do {
            choice = mainUI.getMenuChoice();
            InputUtil.clearScreen();
            switch (choice) {
                case 1:
                    patientUI.runHub();
                    break;
                case 2:
                    doctorUI.run();
                    break;
                case 3:
                    consultUI.run();
                    break;
                case 4:
                    new boundary.TreatmentMaintenanceUI().run();
                    break;
                case 5:
                    new boundary.MedicationMaintenanceUI().run();
                    break;
                case 6:
                    System.out.println("Exiting the Clinic Maintenance System...");
                    break;
                default:
                    System.out.println("Invalid choice. Please try again.");
                    InputUtil.pauseScreen();
            }
            if (choice != 6) {
                InputUtil.clearScreen();
                mainUI.printHeader("Welcome to the Clinic Maintenance System");
            }
    } while (choice != 6);
    return;
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
