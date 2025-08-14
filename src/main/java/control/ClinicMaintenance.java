/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package control;

import boundary.ClinicMaintenanceUI;
import control.*;
/**
 *
 * @author Whrl
 */
public class ClinicMaintenance {
    private ClinicMaintenanceUI mainUI = new ClinicMaintenanceUI();
    private PatientMaintenance patientMaintenance = new PatientMaintenance();
    private DoctorMaintenance doctorMaintenance = new DoctorMaintenance();

    public void runClinicMaintenance() {
        mainUI.printHeader("Welcome to the Clinic Maintenance System");
        int choice;
        do {
            choice = mainUI.getMenuChoice();
            switch (choice) {
                case 1:
                    patientMaintenance.runPatientMaintenance();
                    break;
                case 2:
                    doctorMaintenance.runDoctorMaintenance();
                    break;
                case 3:
                    //consultationMaintenance.runPatientMaintenance();
                    break;
                case 4:
                    //medicalTreatmentMaintenance.runPatientMaintenance();
                    break;
                case 5:
                    //pharmacyMaintenance.runPatientMaintenance();
                    break;
                case 6:
                    System.out.println("Exiting the Clinic Maintenance System...");
                    break;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        } while (choice != 6);

    }

    public static void main(String[] args) {
        ClinicMaintenance clinicMaintenance = new ClinicMaintenance();
        clinicMaintenance.runClinicMaintenance();
    }
}
