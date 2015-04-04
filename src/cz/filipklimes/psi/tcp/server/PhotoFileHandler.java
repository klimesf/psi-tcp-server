package cz.filipklimes.psi.tcp.server;

/**
 * @author klimesf
 */
public interface PhotoFileHandler {

    /**
     * Opens file to be appended. If the file does not exist, creates a new one.
     * <p/>
     * <p>
     * If a file was opened previously, another call of this function
     * closes the output streams and opens new ones.
     * </p>
     *
     * @param fileName Name of the file to be created.
     * @return true if file was opened successfully, false if not.
     */
    boolean createFile(String fileName);

    /**
     * Appends char to the open file.
     *
     * @param c The char to be appended.
     * @return true if char was appended successfully, false if not.
     */
    boolean appendChar(char c);

    /**
     * Flushes the buffers and closes the open file.
     *
     * @return true if file was appended to and closed successfully, false if not.
     */
    public boolean close();

}
