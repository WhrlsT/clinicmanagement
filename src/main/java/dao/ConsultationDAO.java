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
    // Register JavaTimeModule for standard ISO handling and a lenient deserializer for LocalDateTime
    com.fasterxml.jackson.datatype.jsr310.JavaTimeModule jtm = new JavaTimeModule();
    jtm.addDeserializer(java.time.LocalDateTime.class, new utility.LocalDateTimeDeserializer());
    mapper.registerModule(jtm);
    // Prefer ISO-8601 strings for dates instead of numeric arrays
    mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public ADTInterface<Consultation> load() {
        ADTInterface<Consultation> list = new CustomADT<>();
        File f = new File(FILE);
        boolean migrated = false;
        if (f.exists() && f.length() > 0) {
            try {
                Consultation[] arr = mapper.readValue(f, Consultation[].class);
                for (Consultation c : arr) list.add(c);
            } catch (IOException ex) {
                try {
                    com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(f);
                    if (root.isArray()) {
                        if (root.size() > 1 && root.get(1).isArray()) {
                            com.fasterxml.jackson.databind.JsonNode entries = root.get(1);
                            for (com.fasterxml.jackson.databind.JsonNode pair : entries) {
                                if (pair.isArray() && pair.size() > 1 && pair.get(1).isObject()) {
                                    Consultation c = mapper.treeToValue(pair.get(1), Consultation.class);
                                    list.add(c);
                                }
                            }
                            migrated = true;
                        } else {
                            Consultation[] arr = mapper.treeToValue(root, Consultation[].class);
                            for (Consultation c : arr) list.add(c);
                            migrated = true;
                        }
                    }
                } catch (IOException ex2) {
                    System.out.println("Error loading consultations: " + ex2.getMessage());
                }
            }
        }
        if (migrated && list.size() > 0) save(list);
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
