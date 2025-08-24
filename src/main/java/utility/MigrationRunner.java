package utility;

import control.DoctorMaintenance;
import dao.DoctorDAO;
import adt.ADTInterface;
import entity.Doctor;

/** Runner to invoke schedule migration once at startup */
public class MigrationRunner {
    public static boolean run(DoctorMaintenance docMaint, Object unused) {
        try {
            DoctorDAO dDao = new DoctorDAO();
            ADTInterface<Doctor> doctors = dDao.retrieveFromFile();
            boolean changed = ScheduleMigration.migrateDoctors(doctors);
            if (changed) {
                dDao.saveToFile(doctors);
            }
            return changed;
        } catch (Exception e) {
            return false;
        }
    }
}
