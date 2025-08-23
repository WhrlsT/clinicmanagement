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
        objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
    }

    public ADTInterface<Patient> retrieveFromFile() {
        ADTInterface<Patient> patientList = new CustomADT<>();

        try {
            File file = new File(FILE_PATH);
            if (file.exists() && file.length() > 0) {
                Patient[] patients = objectMapper.readValue(
                    file,
                    Patient[].class
                );

                for (Patient patient : patients) {
                    patientList.add(patient);
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("No existing patient data found. Starting with empty list.");
        } catch (IOException e) {
            System.out.println("Error reading patient data: " + e.getMessage());
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