package mem.kitek.server.logger;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * @author RinesThaix
 */
public class ConciseFormatter extends Formatter {

    private final DateFormat date = new SimpleDateFormat(System.getProperty("mem.kitek.log-date-format", "HH:mm:ss"));

    @Override
    @SuppressWarnings("ThrowableResultIgnored")
    public String format(LogRecord record) {
        StringBuilder formatted = new StringBuilder();

        formatted.append(date.format(record.getMillis()));
        formatted.append(" [");
        formatted.append(record.getLevel().getLocalizedName());
        formatted.append("] ");
        if (record.getLoggerName() != null) {
            formatted.append("[");
            formatted.append(record.getLoggerName());
            formatted.append("] ");
        }
        formatted.append(formatMessage(record));
        formatted.append('\n');
        if (record.getThrown() != null) {
            StringWriter writer = new StringWriter();
            record.getThrown().printStackTrace(new PrintWriter(writer));
            formatted.append(writer);
        }

        return formatted.toString();
    }
}
