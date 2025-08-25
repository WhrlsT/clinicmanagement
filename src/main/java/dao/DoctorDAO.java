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

        File file = new File(FILE_PATH);
        boolean migrated = false;
        if (file.exists() && file.length() > 0) {
            try {
                Doctor[] doctors = objectMapper.readValue(file, Doctor[].class);
                for (Doctor doctor : doctors) {
                    if (doctor.getSchedule() == null) doctor.setSchedule(new DoctorSchedule());
                    doctorList.add(doctor);
                }
            } catch (IOException ex) {
                try {
                    com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(file);
                    if (root.isArray()) {
                        if (root.size() > 1 && root.get(1).isArray()) {
                            com.fasterxml.jackson.databind.JsonNode entries = root.get(1);
                            for (com.fasterxml.jackson.databind.JsonNode pair : entries) {
                                if (pair.isArray() && pair.size() > 1 && pair.get(1).isObject()) {
                                    Doctor d = objectMapper.treeToValue(pair.get(1), Doctor.class);
                                    if (d.getSchedule() == null) d.setSchedule(new DoctorSchedule());
                                    doctorList.add(d);
                                }
                            }
                            migrated = true;
                        } else {
                            Doctor[] doctors = objectMapper.treeToValue(root, Doctor[].class);
                            for (Doctor doctor : doctors) {
                                if (doctor.getSchedule() == null) doctor.setSchedule(new DoctorSchedule());
                                doctorList.add(doctor);
                            }
                            migrated = true;
                        }
                    }
                } catch (IOException ex2) {
                    System.out.println("Error reading doctor data: " + ex2.getMessage());
                }
            }
        }
        if (migrated && doctorList.size() > 0) saveToFile(doctorList);

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
