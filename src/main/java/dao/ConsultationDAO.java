package dao;

import adt.ADTInterface;
import adt.CustomADT;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import entity.Consultation;
import java.io.*;

public class ConsultationDAO {
    private static final String FILE = "consultations.json";
    private final ObjectMapper mapper;

    public ConsultationDAO() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
    }

    public ADTInterface<Consultation> load() {
        ADTInterface<Consultation> list = new CustomADT<>();
        try {
            File f = new File(FILE);
            if (f.exists() && f.length() > 0) {
                Consultation[] arr = mapper.readValue(f, Consultation[].class);
                for (Consultation c : arr) list.add(c);
            }
        } catch (IOException e) {
            System.out.println("Error loading consultations: " + e.getMessage());
        }
        return list;
    }

    public void save(ADTInterface<Consultation> list) {
        try {
            Consultation[] arr = new Consultation[list.size()];
            for (int i=0;i<list.size();i++) arr[i]=list.get(i);
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(FILE), arr);
        } catch (IOException e) {
            System.out.println("Error saving consultations: " + e.getMessage());
        }
    }
}
