/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package control;

import boundary.*;
import utility.MigrationRunner;
/**
 *
 * @author Whrl
 */
public class ClinicMaintenance {
    private ClinicMaintenanceUI mainUI = new ClinicMaintenanceUI();
    DoctorMaintenance doctorMaintenance = new DoctorMaintenance();

    public static void main(String[] args) {
        ClinicMaintenance clinicMaintenance = new ClinicMaintenance();
        // Run schedule migration using runner
        boolean didMig = MigrationRunner.run(clinicMaintenance.doctorMaintenance, null);
        if (didMig) {
            System.out.println("[Migration] Legacy schedule entries normalized.");
        }
        clinicMaintenance.mainUI.run();
    }
}
