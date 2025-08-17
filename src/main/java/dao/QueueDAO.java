package dao;

import adt.ADTInterface;
import adt.CustomADT;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import entity.PatientQueueEntry;
import java.io.File;
import java.util.List;

public class QueueDAO {
    private static final String FILE = "queue.json";
    private final ObjectMapper mapper = new ObjectMapper();

    public QueueDAO(){
        mapper.registerModule(new JavaTimeModule());
    }

    public ADTInterface<PatientQueueEntry> load(){
        ADTInterface<PatientQueueEntry> list = new CustomADT<>();
        try {
            File f = new File(FILE);
            if (f.exists() && f.length()>0) {
                List<PatientQueueEntry> data = mapper.readValue(f, new TypeReference<List<PatientQueueEntry>>(){});
                for (PatientQueueEntry e: data) list.add(e);
            }
        } catch(Exception e){ System.out.println("Error loading queue: "+e.getMessage()); }
        return list;
    }

    public void save(ADTInterface<PatientQueueEntry> queue){
        try {
            PatientQueueEntry[] arr = new PatientQueueEntry[queue.size()];
            for (int i=0;i<queue.size();i++) arr[i]=queue.get(i);
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(FILE), arr);
        } catch(Exception e){ System.out.println("Error saving queue: "+e.getMessage()); }
    }
}
