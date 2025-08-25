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
        File f = new File(FILE);
        boolean migrated = false;
        if (f.exists() && f.length() > 0) {
            try {
                Medication[] arr = mapper.readValue(f, Medication[].class);
                for (Medication m : arr) list.add(m);
            } catch (IOException ex) {
                try {
                    com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(f);
                    if (root.isArray()) {
                        if (root.size() > 1 && root.get(1).isArray()) {
                            com.fasterxml.jackson.databind.JsonNode entries = root.get(1);
                            for (com.fasterxml.jackson.databind.JsonNode pair : entries) {
                                if (pair.isArray() && pair.size() > 1 && pair.get(1).isObject()) {
                                    Medication m = mapper.treeToValue(pair.get(1), Medication.class);
                                    list.add(m);
                                }
                            }
                            migrated = true;
                        } else {
                            Medication[] arr = mapper.treeToValue(root, Medication[].class);
                            for (Medication m : arr) list.add(m);
                            migrated = true;
                        }
                    }
                } catch (IOException ex2) {
                    System.out.println("Error loading medications: " + ex2.getMessage());
                }
            }
        }
        if (migrated && list.size() > 0) save(list);
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
