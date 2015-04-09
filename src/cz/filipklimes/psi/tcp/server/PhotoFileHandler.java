package cz.filipklimes.psi.tcp.server;

/**
 * @author klimesf
 */
public interface PhotoFileHandler {

    /**
     * Appends char to the open file.
     *
     * @param i The byte to be appended.
     * @return true if char was appended successfully, false if not.
     */
    boolean append(int i);

    /**
     * Flushes the buffers and closes the open file.
     *
     * @return true if file was appended to and closed successfully, false if not.
     */
    boolean close();

    /**
     * Removes the opened file.
     *
     * @return true if file was removed successfully, false if not.
     */
    boolean removeFile();
}
