/**
 *
 * @author Low Wai Hang
 */

package control;

import adt.ADTInterface;
import adt.CustomADT;
import dao.ConsultationDAO;
import dao.DoctorDAO;
import entity.Consultation;
import entity.Doctor;
import entity.SlotStatus;

/**
 * Control for Doctor domain (logic-only, no UI/printing).
 */
public class DoctorMaintenance {
    private final DoctorDAO doctorDAO = new DoctorDAO();
    private final ConsultationDAO consultationDAO = new ConsultationDAO();
    private final ADTInterface<Doctor> doctorList;

    public DoctorMaintenance() {
        doctorList = doctorDAO.retrieveFromFile();
    }

    // Queries
    public ADTInterface<Doctor> getAllDoctors() { return doctorList; }

    public Doctor findDoctorById(String doctorId) {
        if (doctorId == null) return null;
        for (int i = 0; i < doctorList.size(); i++) {
            Doctor d = doctorList.get(i);
            if (doctorId.equalsIgnoreCase(d.getId())) return d;
        }
        return null;
    }

    public ADTInterface<Doctor> findDoctorByIdOrName(String query) {
        ADTInterface<Doctor> results = new CustomADT<>();
        if (query == null) return results;
        String lower = query.toLowerCase();
        for (int i = 0; i < doctorList.size(); i++) {
            Doctor d = doctorList.get(i);
            if ((d.getId() != null && d.getId().equalsIgnoreCase(query)) ||
                (d.getName() != null && d.getName().toLowerCase().contains(lower))) {
                results.add(d);
            }
        }
        return results;
    }

    public ADTInterface<Consultation> getConsultationsByDoctor(String doctorId) {
        ADTInterface<Consultation> all = consultationDAO.load();
        ADTInterface<Consultation> result = new CustomADT<>();
        for (int i = 0; i < all.size(); i++) {
            Consultation c = all.get(i);
            if (c != null && doctorId != null && doctorId.equals(c.getDoctorId())) {
                result.add(c);
            }
        }
        return result;
    }

    // Commands
    public Doctor addDoctor(String name, String specialization, String phone, String email) {
        Doctor d = new Doctor(null, name, specialization, phone, email);
        d.setId(generateNextDoctorId());
        doctorList.add(d);
        persist();
        return d;
    }

    public boolean updateDoctor(Doctor updated) {
        if (updated == null || updated.getId() == null) return false;
        Doctor existing = findDoctorById(updated.getId());
        if (existing == null) return false;
        existing.setName(updated.getName());
        existing.setSpecialization(updated.getSpecialization());
        existing.setPhoneNumber(updated.getPhoneNumber());
        existing.setEmail(updated.getEmail());
        // schedule changes should be applied directly on existing.getSchedule()
        persist();
        return true;
    }

    public boolean deleteDoctor(String doctorId) {
        Doctor d = findDoctorById(doctorId);
        if (d == null) return false;
        doctorList.remove(d);
        reassignDoctorIds();
        persist();
        return true;
    }

    public boolean setDoctorAvailabilityRange(String doctorId, int day, int startHour, int endHour, SlotStatus status) {
        Doctor d = findDoctorById(doctorId);
        if (d == null || d.getSchedule() == null) return false;
        if (startHour < 0) startHour = 0;
        if (endHour > 24) endHour = 24;
        if (endHour <= startHour) return false;
        d.getSchedule().setAvailabilityRange(day, startHour, endHour, status == null ? SlotStatus.AVAILABLE : status);
        persist();
        return true;
    }

    // Helpers
    private String generateNextDoctorId() {
        int max = 0;
        for (int i = 0; i < doctorList.size(); i++) {
            Doctor d = doctorList.get(i);
            if (d.getId() != null && d.getId().startsWith("D")) {
                try { max = Math.max(max, Integer.parseInt(d.getId().substring(1))); } catch (Exception ignored) {}
            }
        }
        return String.format("D%04d", max + 1);
    }

    private void reassignDoctorIds() {
        // Keep order; re-number sequentially by current order
        for (int i = 0; i < doctorList.size(); i++) {
            Doctor d = doctorList.get(i);
            d.setId(String.format("D%04d", i + 1));
        }
    }

    public void persist() { doctorDAO.saveToFile(doctorList); }

    // === Sorting helpers (ascending / descending) ===
    public void sortDoctorsByName() {
        if (doctorList instanceof CustomADT<?> cadt) {
            @SuppressWarnings("unchecked") CustomADT<Doctor> l = (CustomADT<Doctor>) cadt;
            l.sort(new CustomADT.ADTComparator<Doctor>() {
                public int compare(Doctor a, Doctor b) {
                    if (a == null || a.getName() == null) return -1;
                    if (b == null || b.getName() == null) return 1;
                    return a.getName().compareToIgnoreCase(b.getName());
                }
            });
        }
    }

    public void sortDoctorsByNameDesc() {
        if (doctorList instanceof CustomADT<?> cadt) {
            @SuppressWarnings("unchecked") CustomADT<Doctor> l = (CustomADT<Doctor>) cadt;
            l.sort(new CustomADT.ADTComparator<Doctor>() {
                public int compare(Doctor a, Doctor b) {
                    if (a == null || a.getName() == null) return 1;
                    if (b == null || b.getName() == null) return -1;
                    return b.getName().compareToIgnoreCase(a.getName());
                }
            });
        }
    }

    public void sortDoctorsById() {
        if (doctorList instanceof CustomADT<?> cadt) {
            @SuppressWarnings("unchecked") CustomADT<Doctor> l = (CustomADT<Doctor>) cadt;
            l.sort(new CustomADT.ADTComparator<Doctor>() {
                public int compare(Doctor a, Doctor b) {
                    if (a == null || a.getId() == null) return -1;
                    if (b == null || b.getId() == null) return 1;
                    return a.getId().compareToIgnoreCase(b.getId());
                }
            });
        }
    }

    public void sortDoctorsByIdDesc() {
        if (doctorList instanceof CustomADT<?> cadt) {
            @SuppressWarnings("unchecked") CustomADT<Doctor> l = (CustomADT<Doctor>) cadt;
            l.sort(new CustomADT.ADTComparator<Doctor>() {
                public int compare(Doctor a, Doctor b) {
                    if (a == null || a.getId() == null) return 1;
                    if (b == null || b.getId() == null) return -1;
                    return b.getId().compareToIgnoreCase(a.getId());
                }
            });
        }
    }

    public void sortDoctorsBySpecialty() {
        if (doctorList instanceof CustomADT<?> cadt) {
            @SuppressWarnings("unchecked") CustomADT<Doctor> l = (CustomADT<Doctor>) cadt;
            l.sort(new CustomADT.ADTComparator<Doctor>() {
                public int compare(Doctor a, Doctor b) {
                    if (a == null || a.getSpecialization() == null) return -1;
                    if (b == null || b.getSpecialization() == null) return 1;
                    return a.getSpecialization().compareToIgnoreCase(b.getSpecialization());
                }
            });
        }
    }

    public void sortDoctorsBySpecialtyDesc() {
        if (doctorList instanceof CustomADT<?> cadt) {
            @SuppressWarnings("unchecked") CustomADT<Doctor> l = (CustomADT<Doctor>) cadt;
            l.sort(new CustomADT.ADTComparator<Doctor>() {
                public int compare(Doctor a, Doctor b) {
                    if (a == null || a.getSpecialization() == null) return 1;
                    if (b == null || b.getSpecialization() == null) return -1;
                    return b.getSpecialization().compareToIgnoreCase(a.getSpecialization());
                }
            });
        }
    }

    // === Search helpers ===
    public ADTInterface<Doctor> findDoctorsBySpecialty(String specialty) {
        ADTInterface<Doctor> results = new CustomADT<>();
        if (specialty == null) return results;
        String lower = specialty.toLowerCase();
        for (int i = 0; i < doctorList.size(); i++) {
            Doctor d = doctorList.get(i);
            if (d != null && d.getSpecialization() != null && d.getSpecialization().toLowerCase().contains(lower)) {
                results.add(d);
            }
        }
        return results;
    }
        // Ensure this in-memory list reflects latest file content
    public void refreshFromFile() {
        try {
            ADTInterface<Doctor> loaded = doctorDAO.retrieveFromFile();
            // Simple replace contents
            doctorList.clear();
            for (int i = 0; i < loaded.size(); i++) doctorList.add(loaded.get(i));
        } catch (Exception ignored) {}
    }
}
