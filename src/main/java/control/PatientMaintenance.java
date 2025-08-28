package control;

import adt.ADTInterface;
import adt.CustomADT;
import dao.ConsultationDAO;
import dao.DoctorDAO;
import dao.MedicationDAO;
import dao.PatientDAO;
import dao.TreatmentDAO;
import entity.Consultation;
import entity.Doctor;
import entity.Medication;
import entity.Patient;
import entity.Treatment;

/**
 * PatientMaintenance: business logic and data coordination only.
 * UI lives in boundary.PatientMaintenanceUI.
 */
public class PatientMaintenance {
    private final ADTInterface<Patient> patientList = new CustomADT<>();
    private final PatientDAO patientDAO = new PatientDAO();
    private final ConsultationDAO consultationDAO = new ConsultationDAO();
    private final TreatmentDAO treatmentDAO = new TreatmentDAO();
    private final MedicationDAO medicationDAO = new MedicationDAO();
    private final DoctorDAO doctorDAO = new DoctorDAO();
    private final dao.QueueDAO queueDAO = new dao.QueueDAO();

    public PatientMaintenance() {
        ADTInterface<Patient> loaded = patientDAO.retrieveFromFile();
        for (int i = 0; i < loaded.size(); i++) patientList.add(loaded.get(i));
    }

    // Queries
    public ADTInterface<Patient> getAllPatients() {
    // Do not refresh from file on every call — return the in-memory list so
    // in-place sorts (mutations) are visible to the UI, matching Medication style.
    return patientList;
    }

    public Patient findPatientById(String patientId) {
        refreshFromFile();
        if (patientId == null) return null;
        if (patientList instanceof CustomADT<?> cadt) {
            @SuppressWarnings("unchecked") CustomADT<Patient> list = (CustomADT<Patient>) cadt;
            int idx = list.findIndex(new CustomADT.ADTPredicate<Patient>(){
                public boolean test(Patient p){ return p.getId()!=null && p.getId().equalsIgnoreCase(patientId); }
            });
            return idx>=0? list.get(idx) : null;
        }
        for (int i = 0; i < patientList.size(); i++) if (patientList.get(i).getId().equalsIgnoreCase(patientId)) return patientList.get(i);
        return null;
    }

    public ADTInterface<Patient> findPatientByIdOrName(String query) {
        refreshFromFile();
        ADTInterface<Patient> results = new CustomADT<>();
        if (query == null) return results;
        String lowerQuery = query.toLowerCase();
        for (int i = 0; i < patientList.size(); i++) {
            Patient patient = patientList.get(i);
            if ((patient.getId() != null && patient.getId().equalsIgnoreCase(query)) ||
                (patient.getName() != null && patient.getName().toLowerCase().contains(lowerQuery))) {
                results.add(patient);
            }
        }
        return results;
    }

    public ADTInterface<Consultation> getConsultationsByPatient(String patientId) {
        ADTInterface<Consultation> all = consultationDAO.load();
        ADTInterface<Consultation> filtered = new CustomADT<>();
        for (int i = 0; i < all.size(); i++) {
            Consultation c = all.get(i);
            if (c.getPatientId() != null && c.getPatientId().equals(patientId)) filtered.add(c);
        }
        return filtered;
    }

    public ADTInterface<Treatment> getTreatmentsByConsultation(String consultationId) {
        ADTInterface<Treatment> all = treatmentDAO.load();
        ADTInterface<Treatment> filtered = new CustomADT<>();
        for (int i = 0; i < all.size(); i++) {
            Treatment t = all.get(i);
            if (consultationId.equals(t.getConsultationId())) filtered.add(t);
        }
        return filtered;
    }

    public ADTInterface<Medication> getAllMedications() { return medicationDAO.load(); }
    public ADTInterface<Doctor> getAllDoctors() { return doctorDAO.retrieveFromFile(); }

    public int countConsultationsForPatient(String patientId) {
        ADTInterface<Consultation> all = consultationDAO.load();
        int count = 0;
        for (int i = 0; i < all.size(); i++) if (patientId.equals(all.get(i).getPatientId())) count++;
        return count;
    }

    public int countTreatmentsForPatient(String patientId) {
        ADTInterface<Consultation> allConsults = consultationDAO.load();
        ADTInterface<Treatment> allTreats = treatmentDAO.load();
        int count = 0;
        for (int i = 0; i < allTreats.size(); i++) {
            Treatment t = allTreats.get(i);
            if (t.getConsultationId() == null) continue;
            for (int j = 0; j < allConsults.size(); j++) {
                Consultation c = allConsults.get(j);
                if (patientId.equals(c.getPatientId()) && t.getConsultationId().equals(c.getId())) { count++; break; }
            }
        }
        return count;
    }

    // Queue summary counts: [waiting, called, in_progress, skipped, completed]
    public int[] getQueueSummaryCounts() {
        int waiting=0, called=0, inprog=0, skipped=0, completed=0;
        try {
            adt.ADTInterface<entity.PatientQueueEntry> q = queueDAO.load();
            for (int i=0;i<q.size();i++) {
                entity.QueueStatus st = q.get(i).getStatus();
                switch(st){
                    case WAITING -> waiting++;
                    case CALLED -> called++;
                    case IN_PROGRESS -> inprog++;
                    case SKIPPED -> skipped++;
                    case COMPLETED -> completed++;
                }
            }
        } catch(Exception ignored) {}
        return new int[]{waiting, called, inprog, skipped, completed};
    }

    // Commands
    public Patient addPatient(String name, String gender, String phone, String email, String dob, String nationality) {
        Patient newPatient = new Patient(null, name, gender, phone, email, dob, nationality);
        newPatient.setId(generateNextPatientId());
        patientList.add(newPatient);
        persist();
        return newPatient;
    }

    public boolean updatePatient(Patient updated) {
        if (updated == null || updated.getId() == null) return false;
        for (int i = 0; i < patientList.size(); i++) {
            if (updated.getId().equals(patientList.get(i).getId())) {
                patientList.set(i, updated);
                persist();
                return true;
            }
        }
        return false;
    }

    public boolean deletePatient(String id) {
        Patient p = findPatientById(id);
        if (p == null) return false;
        patientList.remove(p);
        reassignPatientIds();
        persist();
        return true;
    }

    // Demographics DTO
    public static class DemographicsReport {
        public int totalPatients;
        public double averageAge;
        public ADTInterface<String> genderCounts;
        public ADTInterface<String> nationalityCounts;
        public ADTInterface<String> ageGroupCounts;
        public int highFrequencyPatients;
        public ADTInterface<String> highFrequencyList; // entries formatted as "ID|Name|Visits"
        public String csv;
    }

    public DemographicsReport generateDemographicsReport() {
        DemographicsReport r = new DemographicsReport();
        r.totalPatients = patientList.size();
        double ageSum = 0.0;
        ADTInterface<String> genderCounts = new CustomADT<>();
        ADTInterface<String> nationalityCounts = new CustomADT<>();
        ADTInterface<String> ageGroups = new CustomADT<>();
        ageGroups.add("0-17:0");
        ageGroups.add("18-35:0");
        ageGroups.add("36-50:0");
        ageGroups.add("51-65:0");
        ageGroups.add("66+:0");

    StringBuilder csv = new StringBuilder();
    csv.append("id,name,dateOfBirth,age,gender,phone,email,nationality,visitCount,lastVisitDate,chronicFlag\n");

    ADTInterface<Consultation> allConsults = consultationDAO.load();
    int highFreq = 0;
    ADTInterface<String> highList = new CustomADT<>();

        for (int i = 0; i < patientList.size(); i++) {
            Patient p = patientList.get(i);
            String ageStr = p.calculateAge(p.getDateOfBirth());
            int age = 0; try { age = Integer.parseInt(ageStr); } catch(Exception ignored) {}
            ageSum += age;

            incrementCount(genderCounts, safe(p.getGender()));
            incrementCount(nationalityCounts, safe(p.getNationality()));

            if (age <= 17) incrementCount(ageGroups, "0-17");
            else if (age <= 35) incrementCount(ageGroups, "18-35");
            else if (age <= 50) incrementCount(ageGroups, "36-50");
            else if (age <= 65) incrementCount(ageGroups, "51-65");
            else incrementCount(ageGroups, "66+");

            int visitCount = 0; java.time.LocalDate lastVisit = null;
            for (int j = 0; j < allConsults.size(); j++) {
                Consultation c = allConsults.get(j);
                if (p.getId().equals(c.getPatientId())) {
                    visitCount++;
                    if (c.getDate() != null) {
                        try {
                            java.time.LocalDate d = c.getDate().toLocalDate();
                            if (d != null && (lastVisit == null || d.isAfter(lastVisit))) lastVisit = d;
                        } catch(Exception ignored) {}
                    }
                }
            }
            boolean chronic = visitCount >= 5;
            if (visitCount >= 5) {
                highFreq++;
                highList.add(p.getId()+"|"+escapeCsv(p.getName())+"|"+visitCount);
            }
            String lastVisitStr = lastVisit==null?"":lastVisit.toString();
            csv.append(String.format("%s,%s,%s,%d,%s,%s,%s,%s,%d,%s,%b\n",
                p.getId(), escapeCsv(p.getName()), p.getDateOfBirth(), age, safe(p.getGender()), safe(p.getPhoneNumber()), safe(p.getEmail()), safe(p.getNationality()), visitCount, lastVisitStr, chronic));
        }

        r.averageAge = r.totalPatients==0?0.0:ageSum / r.totalPatients;
        r.highFrequencyPatients = highFreq;
        r.highFrequencyList = highList;
        r.genderCounts = genderCounts; r.nationalityCounts = nationalityCounts; r.ageGroupCounts = ageGroups; r.csv = csv.toString();
        return r;
    }

    private String safe(String s) { return s==null?"":s; }
    private static String escapeCsv(String s) { if (s==null) return ""; return s.contains(",")?('"'+s.replace("\"","\"\"")+'"'):s; }

    // increment a 'key:count' stored as strings in ADTInterface
    private void incrementCount(ADTInterface<String> list, String key) {
        if (key == null) key = "";
        for (int i = 0; i < list.size(); i++) {
            String kv = list.get(i);
            int idx = kv.indexOf(':');
            String k = idx>=0?kv.substring(0,idx):kv;
            int v = 0; try { v = Integer.parseInt(idx>=0?kv.substring(idx+1):"0"); } catch(Exception ex) { v = 0; }
            if (k.equals(key)) { list.set(i, k + ":" + (v+1)); return; }
        }
        list.add(key + ":1");
    }

    private String generateNextPatientId() {
        int maxId = 0;
        for (int i = 0; i < patientList.size(); i++) {
            Patient p = patientList.get(i);
            String id = p.getId();
            if (id != null && id.startsWith("P")) {
                try { int num = Integer.parseInt(id.substring(1)); if (num > maxId) maxId = num; } catch (NumberFormatException ignored) {}
            }
        }
        return String.format("P%04d", maxId + 1);
    }

    private void reassignPatientIds() {
        ADTInterface<Patient> tempList = new CustomADT<>();
        for (int i = 0; i < patientList.size(); i++) tempList.add(patientList.get(i));
        if (tempList instanceof CustomADT<?> cadt) {
            @SuppressWarnings("unchecked") CustomADT<Patient> list = (CustomADT<Patient>) cadt;
            list.sort(new CustomADT.ADTComparator<Patient>(){
                public int compare(Patient a, Patient b){
                    try { int n1=Integer.parseInt(a.getId().substring(1)); int n2=Integer.parseInt(b.getId().substring(1)); return Integer.compare(n1,n2);}catch(Exception e){return 0;}
                }
            });
        }
        for (int i = 0; i < tempList.size(); i++) { tempList.get(i).setId(String.format("P%04d", i + 1)); patientList.set(i, tempList.get(i)); }
    }

    /**
     * Return patients sorted by ID using CustomADT.mergeSort when available.
     */
    public ADTInterface<Patient> getPatientsSortedById() {
        // In-place sort of the internal list by ID when using CustomADT
        if (patientList instanceof CustomADT<?> cadt) {
            @SuppressWarnings("unchecked") CustomADT<Patient> list = (CustomADT<Patient>) cadt;
            list.mergeSort(new CustomADT.ADTComparator<Patient>(){
                public int compare(Patient a, Patient b) {
                    if (a == null && b == null) return 0;
                    if (a == null) return -1;
                    if (b == null) return 1;
                    String ia = a.getId() == null ? "" : a.getId();
                    String ib = b.getId() == null ? "" : b.getId();
                    return ia.compareToIgnoreCase(ib);
                }
            });
            return list;
        }
        // Fallback: return a sorted copy if underlying ADT doesn't support in-place sort
        CustomADT<Patient> out = new CustomADT<>();
        for (int i = 0; i < patientList.size(); i++) out.add(patientList.get(i));
        out.mergeSort(new CustomADT.ADTComparator<Patient>(){
            public int compare(Patient a, Patient b) {
                if (a == null && b == null) return 0;
                if (a == null) return -1;
                if (b == null) return 1;
                String ia = a.getId() == null ? "" : a.getId();
                String ib = b.getId() == null ? "" : b.getId();
                return ia.compareToIgnoreCase(ib);
            }
        });
        return out;
    }

    /**
     * Return patients sorted by name using CustomADT.mergeSort when available.
     */
    public ADTInterface<Patient> getPatientsSortedByName() {
        // In-place sort by name
        if (patientList instanceof CustomADT<?> cadt) {
            @SuppressWarnings("unchecked") CustomADT<Patient> list = (CustomADT<Patient>) cadt;
            list.mergeSort(new CustomADT.ADTComparator<Patient>(){
                public int compare(Patient a, Patient b) {
                    if (a == null && b == null) return 0;
                    if (a == null) return -1;
                    if (b == null) return 1;
                    String na = a.getName() == null ? "" : a.getName();
                    String nb = b.getName() == null ? "" : b.getName();
                    return na.compareToIgnoreCase(nb);
                }
            });
            return list;
        }
        CustomADT<Patient> out = new CustomADT<>();
        for (int i = 0; i < patientList.size(); i++) out.add(patientList.get(i));
        out.mergeSort(new CustomADT.ADTComparator<Patient>(){
            public int compare(Patient a, Patient b) {
                if (a == null && b == null) return 0;
                if (a == null) return -1;
                if (b == null) return 1;
                String na = a.getName() == null ? "" : a.getName();
                String nb = b.getName() == null ? "" : b.getName();
                return na.compareToIgnoreCase(nb);
            }
        });
        return out;
    }

    /**
     * Find a patient by ID using binary search on a list sorted by ID.
     * Returns null if not found.
     */
    public Patient binarySearchPatientById(String patientId) {
        if (patientId == null) return null;
        // Ensure the internal list is sorted by ID in-place if possible
        if (patientList instanceof CustomADT<?> cadt) {
            @SuppressWarnings("unchecked") CustomADT<Patient> list = (CustomADT<Patient>) cadt;
            list.mergeSort(new CustomADT.ADTComparator<Patient>(){
                public int compare(Patient a, Patient b) {
                    if (a == null && b == null) return 0;
                    if (a == null) return -1;
                    if (b == null) return 1;
                    String ia = a.getId() == null ? "" : a.getId();
                    String ib = b.getId() == null ? "" : b.getId();
                    return ia.compareToIgnoreCase(ib);
                }
            });
            Patient probe = new Patient(patientId, null, null, null, null, null, null);
            int idx = list.binarySearch(probe, new CustomADT.ADTComparator<Patient>(){
                public int compare(Patient a, Patient b) {
                    if (a == null && b == null) return 0;
                    if (a == null) return -1;
                    if (b == null) return 1;
                    String ia = a.getId() == null ? "" : a.getId();
                    String ib = b.getId() == null ? "" : b.getId();
                    return ia.compareToIgnoreCase(ib);
                }
            });
            return idx >= 0 ? list.get(idx) : null;
        }
        // Fallback to using a temporary sorted copy
        CustomADT<Patient> out = new CustomADT<>();
        for (int i = 0; i < patientList.size(); i++) out.add(patientList.get(i));
        out.mergeSort(new CustomADT.ADTComparator<Patient>(){
            public int compare(Patient a, Patient b) {
                if (a == null && b == null) return 0;
                if (a == null) return -1;
                if (b == null) return 1;
                String ia = a.getId() == null ? "" : a.getId();
                String ib = b.getId() == null ? "" : b.getId();
                return ia.compareToIgnoreCase(ib);
            }
        });
        Patient probe = new Patient(patientId, null, null, null, null, null, null);
        int idx = out.binarySearch(probe, new CustomADT.ADTComparator<Patient>(){
            public int compare(Patient a, Patient b) {
                if (a == null && b == null) return 0;
                if (a == null) return -1;
                if (b == null) return 1;
                String ia = a.getId() == null ? "" : a.getId();
                String ib = b.getId() == null ? "" : b.getId();
                return ia.compareToIgnoreCase(ib);
            }
        });
        return idx >= 0 ? out.get(idx) : null;
    }

    /**
     * Generic sorter: field can be "id","name","age","gender","nationality".
     * Returns a new sorted ADT; does not mutate the internal patientList.
     */
    public ADTInterface<Patient> getPatientsSortedBy(String field, boolean ascending) {
        // In-place generic sort (mutates patientList) when using CustomADT
        final String ffield = field == null ? "id" : field.toLowerCase();
        final boolean asc = ascending;
        if (patientList instanceof CustomADT<?> cadt) {
            @SuppressWarnings("unchecked") CustomADT<Patient> list = (CustomADT<Patient>) cadt;
            list.mergeSort(new CustomADT.ADTComparator<Patient>(){
                public int compare(Patient a, Patient b) {
                    int res = 0;
                    switch(ffield) {
                        case "name": {
                            String na = a==null||a.getName()==null?"":a.getName();
                            String nb = b==null||b.getName()==null?"":b.getName();
                            res = na.compareToIgnoreCase(nb);
                            break;
                        }
                        case "age": {
                            int ia = 0, ib = 0;
                            try { ia = Integer.parseInt(a.calculateAge(a.getDateOfBirth())); } catch(Exception ignored) {}
                            try { ib = Integer.parseInt(b.calculateAge(b.getDateOfBirth())); } catch(Exception ignored) {}
                            res = Integer.compare(ia, ib);
                            break;
                        }
                        case "gender": {
                            String ga = a==null||a.getGender()==null?"":a.getGender();
                            String gb = b==null||b.getGender()==null?"":b.getGender();
                            res = ga.compareToIgnoreCase(gb);
                            break;
                        }
                        case "nationality": {
                            String na = a==null||a.getNationality()==null?"":a.getNationality();
                            String nb = b==null||b.getNationality()==null?"":b.getNationality();
                            res = na.compareToIgnoreCase(nb);
                            break;
                        }
                        case "id":
                        default: {
                            String ia = a==null||a.getId()==null?"":a.getId();
                            String ib = b==null||b.getId()==null?"":b.getId();
                            res = ia.compareToIgnoreCase(ib);
                            break;
                        }
                    }
                    return asc ? res : -res;
                }
            });
            return list;
        }
        // Fallback: return a sorted copy
        CustomADT<Patient> out = new CustomADT<>();
        for (int i = 0; i < patientList.size(); i++) out.add(patientList.get(i));
        out.mergeSort(new CustomADT.ADTComparator<Patient>(){
            public int compare(Patient a, Patient b) {
                int res = 0;
                switch(ffield) {
                    case "name": {
                        String na = a==null||a.getName()==null?"":a.getName();
                        String nb = b==null||b.getName()==null?"":b.getName();
                        res = na.compareToIgnoreCase(nb);
                        break;
                    }
                    case "age": {
                        int ia = 0, ib = 0;
                        try { ia = Integer.parseInt(a.calculateAge(a.getDateOfBirth())); } catch(Exception ignored) {}
                        try { ib = Integer.parseInt(b.calculateAge(b.getDateOfBirth())); } catch(Exception ignored) {}
                        res = Integer.compare(ia, ib);
                        break;
                    }
                    case "gender": {
                        String ga = a==null||a.getGender()==null?"":a.getGender();
                        String gb = b==null||b.getGender()==null?"":b.getGender();
                        res = ga.compareToIgnoreCase(gb);
                        break;
                    }
                    case "nationality": {
                        String na = a==null||a.getNationality()==null?"":a.getNationality();
                        String nb = b==null||b.getNationality()==null?"":b.getNationality();
                        res = na.compareToIgnoreCase(nb);
                        break;
                    }
                    case "id":
                    default: {
                        String ia = a==null||a.getId()==null?"":a.getId();
                        String ib = b==null||b.getId()==null?"":b.getId();
                        res = ia.compareToIgnoreCase(ib);
                        break;
                    }
                }
                return asc ? res : -res;
            }
        });
        return out;
    }

    private void persist() { patientDAO.saveToFile(patientList); }

    // Ensure this in-memory list reflects latest file content
    public void refreshFromFile() {
        try {
            ADTInterface<Patient> loaded = patientDAO.retrieveFromFile();
            // Simple replace contents
            patientList.clear();
            for (int i = 0; i < loaded.size(); i++) patientList.add(loaded.get(i));
        } catch (Exception ignored) {}
    }

    /**
     * Public wrapper so UI can request a one-time reload from file.
     * This avoids reloading on every getAllPatients() call while allowing
     * the UI to refresh when entering the patient screen.
     */
    public void reloadFromFile() { refreshFromFile(); }
}
