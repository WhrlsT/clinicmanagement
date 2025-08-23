package dao;

import adt.ADTInterface;
import adt.CustomADT;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import entity.Treatment;
import java.io.*;

public class TreatmentDAO {
    private static final String FILE = "treatments.json";
    private final ObjectMapper mapper;

    public TreatmentDAO() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
    }

    public ADTInterface<Treatment> load() {
        ADTInterface<Treatment> list = new CustomADT<>();
        try {
            File f = new File(FILE);
            if (f.exists() && f.length() > 0) {
                Treatment[] arr = mapper.readValue(f, Treatment[].class);
                for (Treatment t : arr) list.add(t);
            }
        } catch (IOException e) {
            System.out.println("Error loading treatments: " + e.getMessage());
        }
        return list;
    }

    public void save(ADTInterface<Treatment> list) {
        try {
            Treatment[] arr = new Treatment[list.size()];
            for (int i=0;i<list.size();i++) arr[i]=list.get(i);
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(FILE), arr);
        } catch (IOException e) {
            System.out.println("Error saving treatments: " + e.getMessage());
        }
    }
}
