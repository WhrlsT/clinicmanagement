package dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import adt.ADTInterface;
import adt.CustomADT;
import entity.Doctor;
import entity.DoctorSchedule;
import java.io.*;

public class DoctorDAO {
    private static final String FILE_PATH = "doctors.json";
    private ObjectMapper objectMapper;

    public DoctorDAO() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    public ADTInterface<Doctor> retrieveFromFile() {
        ADTInterface<Doctor> doctorList = new CustomADT<>();

        try {
            File file = new File(FILE_PATH);
            if (file.exists() && file.length() > 0) {
                Doctor[] doctors = objectMapper.readValue(
                    file,
                    Doctor[].class
                );

                for (Doctor doctor : doctors) {
                    // Ensure schedule not null after deserialization
                    if (doctor.getSchedule() == null) {
                        doctor.setSchedule(new DoctorSchedule());
                    }
                    doctorList.add(doctor);
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("No existing doctor data found. Starting with empty list.");
        } catch (IOException e) {
            System.out.println("Error reading doctor data: " + e.getMessage());
        }

        return doctorList;
    }

    public void saveToFile(ADTInterface<Doctor> doctorList) {
        try {
            Doctor[] doctors = new Doctor[doctorList.size()];
            for (int i = 0; i < doctorList.size(); i++) {
                doctors[i] = doctorList.get(i);
            }

            objectMapper.writerWithDefaultPrettyPrinter()
                       .writeValue(new File(FILE_PATH), doctors);

        } catch (IOException e) {
            System.out.println("Error saving doctor data: " + e.getMessage());
        }
    }
}
