/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dao;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import adt.ADTInterface;
import adt.CustomADT;
import entity.Patient;
import java.io.*;

/**
 *
 * @author Whrl
 */

public class PatientDAO {
    private static final String FILE_PATH = "patients.json";
    private ObjectMapper objectMapper;

    public PatientDAO() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    public ADTInterface<Patient> retrieveFromFile() {
        ADTInterface<Patient> patientList = new CustomADT<>();

        File file = new File(FILE_PATH);
        if (file.exists() && file.length() > 0) {
                boolean migrated = false;
                try {
                    // Try normal deserialization: JSON array of Patient objects
                    Patient[] patients = objectMapper.readValue(file, Patient[].class);
                    for (Patient patient : patients) patientList.add(patient);
                } catch (IOException ex) {
                    // Fallback: handle legacy/wrapped format like ["[Lentity.Patient;", [ ["entity.Patient", {..}], ... ] ]
                    try {
                        com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(file);
                        if (root.isArray()) {
                            // If second element is an array of pairs [type, object]
                            if (root.size() > 1 && root.get(1).isArray()) {
                                com.fasterxml.jackson.databind.JsonNode entries = root.get(1);
                                for (com.fasterxml.jackson.databind.JsonNode pair : entries) {
                                    if (pair.isArray() && pair.size() > 1 && pair.get(1).isObject()) {
                                        Patient p = objectMapper.treeToValue(pair.get(1), Patient.class);
                                        patientList.add(p);
                                    }
                                }
                                migrated = true;
                            } else {
                                // Try treating root itself as array of Patient objects
                                Patient[] patients = objectMapper.treeToValue(root, Patient[].class);
                                for (Patient patient : patients) patientList.add(patient);
                                migrated = true;
                            }
                        }
                    } catch (IOException ex2) {
                        System.out.println("Error reading patient data: " + ex2.getMessage());
                    }
                }
                // If we read a legacy format, persist a clean JSON array for future runs
                if (migrated && patientList.size() > 0) {
                    saveToFile(patientList);
                }
        }

        return patientList;
    }

    public void saveToFile(ADTInterface<Patient> patientList) {
        try {
            Patient[] patients = new Patient[patientList.size()];
            for (int i = 0; i < patientList.size(); i++) {
                patients[i] = patientList.get(i);
            }

            objectMapper.writerWithDefaultPrettyPrinter()
                       .writeValue(new File(FILE_PATH), patients);

        } catch (IOException e) {
            System.out.println("Error saving patient data: " + e.getMessage());
        }
    }
}