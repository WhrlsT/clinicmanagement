package dao;

import adt.ADTInterface;
import adt.CustomADT;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import entity.Medication;
import java.io.*;

public class MedicationDAO {
    private static final String FILE = "medications.json";
    private final ObjectMapper mapper;

    public MedicationDAO() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
    }

    public ADTInterface<Medication> load() {
        ADTInterface<Medication> list = new CustomADT<>();
        try {
            File f = new File(FILE);
            if (f.exists() && f.length() > 0) {
                Medication[] arr = mapper.readValue(f, Medication[].class);
                for (Medication m : arr) list.add(m);
            }
        } catch (IOException e) {
            System.out.println("Error loading medications: " + e.getMessage());
        }
        return list;
    }

    public void save(ADTInterface<Medication> list) {
        try {
            Medication[] arr = new Medication[list.size()];
            for (int i=0;i<list.size();i++) arr[i]=list.get(i);
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(FILE), arr);
        } catch (IOException e) {
            System.out.println("Error saving medications: " + e.getMessage());
        }
    }
}
