/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package control;

import boundary.ClinicMaintenanceUI;
import utility.MigrationRunner;
import utility.InputUtil;
/**
 *
 * @author Whrl
 */
public class ClinicMaintenance {
    private ClinicMaintenanceUI mainUI = new ClinicMaintenanceUI();
    private PatientMaintenance patientMaintenance = new PatientMaintenance();
    private DoctorMaintenance doctorMaintenance = new DoctorMaintenance();

    public void runClinicMaintenance() {
        InputUtil.clearScreen();
        mainUI.printHeader("Welcome to the Clinic Maintenance System");
        int choice;
        do {
            choice = mainUI.getMenuChoice();
            InputUtil.clearScreen();
            switch (choice) {
                case 1:
                    patientMaintenance.runPatientManagementHub();
                    break;
                case 2:
                    doctorMaintenance.runDoctorMaintenance();
                    break;
                case 3:
                    new ConsultationMaintenance().run();
                    break;
                case 4:
                    new QueueMaintenance().run();
                    break;
                case 5:
                    new TreatmentMaintenance().run();
                    break;
                case 6:
                    new MedicationMaintenance().run();
                    break;
                case 7:
                    InputUtil.clearScreen();
                    System.out.println("Exiting the Clinic Maintenance System...");
                    break;
                default:
                    System.out.println("Invalid choice. Please try again.");
                    InputUtil.pauseScreen();
            }
            if (choice != 7) {
                InputUtil.clearScreen();
                mainUI.printHeader("Welcome to the Clinic Maintenance System");
            }
    } while (choice != 7);

    }

    public static void main(String[] args) {
        ClinicMaintenance clinicMaintenance = new ClinicMaintenance();
        // Run schedule migration using runner
    boolean didMig = MigrationRunner.run(clinicMaintenance.doctorMaintenance, null);
        if (didMig) System.out.println("[Migration] Legacy schedule entries normalized.");
        clinicMaintenance.runClinicMaintenance();
    }
}
