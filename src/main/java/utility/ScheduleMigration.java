package utility;

import adt.ADTInterface;
import entity.Doctor;
import entity.SlotStatus;

/**
 * One-time migration helper to clear any legacy BOOKED flags from schedules
 * after refactor to date-based booking (consultations only).
 */
public class ScheduleMigration {
    public static boolean migrateDoctors(ADTInterface<Doctor> doctors) {
        boolean changed = false;
        for (int i=0;i<doctors.size();i++) {
            Doctor d = doctors.get(i);
            var avail = d.getSchedule().getAvailability();
            for (int day=0; day<7; day++) {
                for (int h=0; h<24; h++) {
                    if (avail[day][h] != SlotStatus.AVAILABLE && avail[day][h] != SlotStatus.NOT_AVAILABLE) {
                        avail[day][h] = SlotStatus.AVAILABLE; // default to available
                        changed = true;
                    }
                }
            }
        }
        return changed;
    }

}
