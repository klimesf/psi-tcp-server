package cz.filipklimes.psi.tcp.server;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Singleton implementation of the PhotoFileHandler interface.
 *
 * @author klimesf
 */
public class SingletonPhotoFileHandlerImpl implements PhotoFileHandler {

    private static SingletonPhotoFileHandlerImpl instance;
    private FileWriter fw;
    private BufferedWriter bw;

    /**
     * Private constructor following Singleton pattern.
     */
    private SingletonPhotoFileHandlerImpl() {
    }

    /**
     * Returns instance of the PhotoFileHandler singleton.
     * The instance is lazy loaded.
     *
     * @return The instance.
     */
    public static SingletonPhotoFileHandlerImpl getInstance() {
        if (instance == null) {
            instance = new SingletonPhotoFileHandlerImpl();
        }
        return instance;
    }

    /**
     * {@inheritDoc}
     */
    public boolean createFile(String fileName) {
        try {
            // If file was opened previously, close the streams
            if (bw != null) {
                this.close();
            }

            File file = new File(fileName);
            if (!file.exists()) {
                file.createNewFile();
            }

            fw = new FileWriter(file.getAbsoluteFile());
            bw = new BufferedWriter(fw, 2 << 19);

            return true;

        } catch (IOException e) {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean appendChar(char c) {
        try {
            if (bw != null) {
                bw.write(c);
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
            if (bw != null) {
                bw.flush();
                bw.close();
                return true;
            } else {
                return false;
            }
        } catch (IOException e) {
            return false;
        }
    }
}
