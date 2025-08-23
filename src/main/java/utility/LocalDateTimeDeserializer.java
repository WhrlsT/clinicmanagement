package utility;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Lenient deserializer that accepts:
 * - array form: [yyyy,MM,dd]
 * - ISO string (date or date-time)
 */
public class LocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {
    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonToken t = p.getCurrentToken();
        if (t == JsonToken.START_ARRAY) {
            // read [yyyy,MM,dd] and return at start of day
            int year = p.nextIntValue(-1);
            int month = p.nextIntValue(-1);
            int day = p.nextIntValue(-1);
            // consume END_ARRAY
            while (p.nextToken() != JsonToken.END_ARRAY) { /* skip */ }
            return LocalDate.of(year, month, day).atStartOfDay();
        } else if (t == JsonToken.VALUE_STRING) {
            String s = p.getText().trim();
            if (s.isEmpty()) return null;
            // try parse LocalDateTime then LocalDate
            try {
                return LocalDateTime.parse(s, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (Exception ex) {
                try {
                    LocalDate d = LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE);
                    return d.atStartOfDay();
                } catch (Exception ex2) {
                    // last resort: use LocalDate.parse with default formatter
                    LocalDate d2 = LocalDate.parse(s);
                    return d2.atStartOfDay();
                }
            }
        }
        // fallback to null
        return null;
    }
}
