package utility;

import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.*;
import entity.Doctor;

import java.io.*;
import java.time.*;
import java.util.Collections;

public class GoogleCalendarService {
    private static final String APPLICATION_NAME = "ClinicManagement";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIR = "tokens";
    private static final String CREDENTIALS_FILE_PATH = "credentials.json"; // default name

    private static GoogleCalendarService INSTANCE;
    private final Calendar service;

    private GoogleCalendarService() throws Exception {
        final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        service = new Calendar.Builder(httpTransport, JSON_FACTORY, getCredentials(httpTransport))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public static GoogleCalendarService getInstance() throws Exception {
        if (INSTANCE == null) INSTANCE = new GoogleCalendarService();
        return INSTANCE;
    }

    private static com.google.api.client.auth.oauth2.Credential getCredentials(final NetHttpTransport httpTransport) throws IOException {
        try (InputStream in = locateCredentials()) {
            GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, JSON_FACTORY, clientSecrets, Collections.singleton(CalendarScopes.CALENDAR))
                .setDataStoreFactory(new FileDataStoreFactory(new File(TOKENS_DIR)))
                .setAccessType("offline")
                .build();
            LocalServerReceiver receiver = buildReceiverWithFallback();
            return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
        }
    }

    // Try multiple locations / mechanisms to find credentials.json
    private static InputStream locateCredentials() throws FileNotFoundException {
        // 1. Environment variable pointing directly to file
        String envPath = System.getenv("GOOGLE_CREDENTIALS_PATH");
        if (envPath != null && !envPath.isBlank()) {
            File f = new File(envPath);
            if (f.exists()) return new FileInputStream(f);
        }
        // 2. Working directory
        File wd = new File(CREDENTIALS_FILE_PATH);
        if (wd.exists()) return new FileInputStream(wd);
        // 3. Conventional resources folder (if run from IDE)
        File res = new File("src/main/resources/" + CREDENTIALS_FILE_PATH);
        if (res.exists()) return new FileInputStream(res);
        // 4. User home config directory
        File home = new File(System.getProperty("user.home"), ".clinic/" + CREDENTIALS_FILE_PATH);
        if (home.exists()) return new FileInputStream(home);
        // 5. Classpath resource
        InputStream cp = GoogleCalendarService.class.getClassLoader().getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (cp != null) return cp;
        throw new FileNotFoundException("credentials.json not found. Checked: working dir (" + wd.getAbsolutePath() + "), src/main/resources, env GOOGLE_CREDENTIALS_PATH, ~/.clinic/");
    }

    private static LocalServerReceiver buildReceiverWithFallback() {
        // Allow override
        String env = System.getenv("OAUTH_CALLBACK_PORT");
        if (env != null) {
            try { int p = Integer.parseInt(env); return new LocalServerReceiver.Builder().setPort(p).build(); } catch (Exception ignored) {}
        }
        int[] candidates = {8888, 8889, 8890, 9000, 9999};
        for (int port : candidates) {
            if (isPortFree(port)) {
                return new LocalServerReceiver.Builder().setPort(port).build();
            }
        }
        // Fallback: random ephemeral (pick in high range and hope free)
        for (int port = 49152; port < 50000; port++) {
            if (isPortFree(port)) {
                return new LocalServerReceiver.Builder().setPort(port).build();
            }
        }
        // Last resort original 8888 (will likely fail but we report) 
        return new LocalServerReceiver.Builder().setPort(8888).build();
    }

    private static boolean isPortFree(int port) {
        try (java.net.ServerSocket ss = new java.net.ServerSocket(port)) { ss.setReuseAddress(true); return true; } catch (IOException e) { return false; }
    }

    // Ensure doctor has a duty calendar
    public void ensureCalendar(Doctor doctor) throws IOException {
        if (doctor.getCalendarId()!=null && !doctor.getCalendarId().isEmpty()) return;
        String desiredSummary = "Duty - " + doctor.getName();
        CalendarList cl = service.calendarList().list().execute();
        for (CalendarListEntry entry : cl.getItems()) {
            if (desiredSummary.equalsIgnoreCase(entry.getSummary())) {
                doctor.setCalendarId(entry.getId());
                return;
            }
        }
        com.google.api.services.calendar.model.Calendar cal = new com.google.api.services.calendar.model.Calendar();
        cal.setSummary(desiredSummary);
        cal.setTimeZone(ZoneId.systemDefault().toString());
        com.google.api.services.calendar.model.Calendar created = service.calendars().insert(cal).execute();
        doctor.setCalendarId(created.getId());
    }

    public String addDutyHour(Doctor doctor, LocalDate date, int hour) throws IOException {
        if (doctor.getCalendarId()==null) throw new IllegalStateException("No calendarId");
        if (findDutyEvent(doctor,date,hour)!=null) return null; // already
        ZoneId zone = ZoneId.systemDefault();
        ZonedDateTime start = date.atTime(hour,0).atZone(zone);
        ZonedDateTime end = start.plusHours(1);
        Event event = new Event().setSummary("Duty").setDescription("Duty Hour")
                .setStart(new EventDateTime().setDateTime(new com.google.api.client.util.DateTime(start.toInstant().toEpochMilli())).setTimeZone(zone.toString()))
                .setEnd(new EventDateTime().setDateTime(new com.google.api.client.util.DateTime(end.toInstant().toEpochMilli())).setTimeZone(zone.toString()));
        Event created = service.events().insert(doctor.getCalendarId(), event).execute();
        return created.getId();
    }

    public boolean removeDutyHour(Doctor doctor, LocalDate date, int hour) throws IOException {
        Event e = findDutyEvent(doctor,date,hour);
        if (e==null) return false;
        service.events().delete(doctor.getCalendarId(), e.getId()).execute();
        return true;
    }

    public boolean isDutyHour(Doctor doctor, LocalDate date, int hour) throws IOException {
        return findDutyEvent(doctor,date,hour)!=null;
    }

    private Event findDutyEvent(Doctor doctor, LocalDate date, int hour) throws IOException {
        if (doctor.getCalendarId()==null) return null;
        ZoneId zone = ZoneId.systemDefault();
        ZonedDateTime start = date.atTime(hour,0).atZone(zone);
        ZonedDateTime end = start.plusHours(1);
        Events events = service.events().list(doctor.getCalendarId())
                .setTimeMin(new com.google.api.client.util.DateTime(start.toInstant().toEpochMilli()))
                .setTimeMax(new com.google.api.client.util.DateTime(end.toInstant().toEpochMilli()))
                .setSingleEvents(true)
                .execute();
        for (Event ev : events.getItems()) if ("Duty".equalsIgnoreCase(ev.getSummary())) return ev;
        return null;
    }
}
