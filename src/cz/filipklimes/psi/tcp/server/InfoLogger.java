package cz.filipklimes.psi.tcp.server;

/**
 * @author klimesf
 */
public class InfoLogger {

    private static InfoLogger instance;

    private InfoLogger() {
    }

    public static InfoLogger getInstance() {
        if (instance == null) {
            instance = new InfoLogger();
        }
        return instance;
    }

    public void saveLog(String log) {

    }

}
