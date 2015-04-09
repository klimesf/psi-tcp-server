package cz.filipklimes.psi.tcp.server;

import java.io.*;

/**
 * Implementation of the PhotoFileHandler interface.
 *
 * @author klimesf
 */
public class PhotoFileHandlerImpl implements PhotoFileHandler {

    private FileOutputStream fileOutputStream;
    private String fileName;

    /**
     * Creates a new PhotoFileHandlerImpl which will write to the given file.
     */
    public PhotoFileHandlerImpl(String fileName) {
        this.fileName = fileName;

        try {
            // If file was opened previously, close the streams
            if (this.fileOutputStream != null) {
                this.close();
            }

            this.fileOutputStream = new FileOutputStream(fileName);

        } catch (IOException e) {
            throw new RuntimeException("Could not open photo file");
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean append(int i) {
        try {
            if (fileOutputStream != null) {
                fileOutputStream.write(i);
                return true;
            } else {
                return false;
            }
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean close() {
        try {
            if (this.fileOutputStream != null) {
                this.fileOutputStream.close();
                return true;
            } else {
                return false;
            }
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeFile() {
        File file = new File(this.fileName);
        return file.delete();
    }
}
