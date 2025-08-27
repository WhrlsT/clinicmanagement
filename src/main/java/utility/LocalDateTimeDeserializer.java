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
            // Accept [yyyy,MM,dd[,HH[,mm[,ss[,nano]]]]]
            int[] parts = new int[7];
            int idx = 0;
            while (p.nextToken() != JsonToken.END_ARRAY && idx < parts.length) {
                if (p.getCurrentToken().isNumeric()) {
                    parts[idx++] = p.getIntValue();
                } else {
                    // skip non-numeric tokens if any
                }
            }
            int year = (idx > 0 ? parts[0] : 1970);
            int month = (idx > 1 ? parts[1] : 1);
            int day = (idx > 2 ? parts[2] : 1);
            int hour = (idx > 3 ? parts[3] : 0);
            int minute = (idx > 4 ? parts[4] : 0);
            int second = (idx > 5 ? parts[5] : 0);
            int nano = (idx > 6 ? parts[6] : 0);
            return LocalDateTime.of(year, month, day, hour, minute, second, nano);
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
