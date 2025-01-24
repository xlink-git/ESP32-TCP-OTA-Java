import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.*;

public class log {
    private static Logger logger = Logger.getLogger(OtaMain.class.getName());
    private static boolean init_flag = false;

    private static class MyLogFormatter extends Formatter {

        @Override
        public String format(LogRecord record) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            Date date = new Date(record.getMillis());

            StringBuffer sb = new StringBuffer(1000);
            sb.append(dateFormat.format(date));
            sb.append(" [");
            sb.append(record.getLevel());
            sb.append("] ");
            sb.append(record.getMessage());
            sb.append("\n");
            return sb.toString();
        }
    }

    public static void init() {
        if(init_flag) return;

        logger.setLevel(Level.FINE);
        // remove default log handler
        logger.setUseParentHandlers(false);

        // add new log handler
        Handler handler = new ConsoleHandler();
        handler.setFormatter(new MyLogFormatter());
        logger.addHandler(handler);
        init_flag = true;
    }

    public static void debug(String str) {
        if(!init_flag) init();
        logger.fine(str);
    }

    public static void info(String str) {
        if(!init_flag) init();
        logger.info(str);
    }

    public static void warn(String str) {
        if(!init_flag) init();
        logger.warning(str);
    }

    public static void error(String str) {
        if(!init_flag) init();
        logger.severe(str);
    }
}
