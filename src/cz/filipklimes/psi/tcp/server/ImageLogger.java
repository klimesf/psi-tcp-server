package cz.filipklimes.psi.tcp.server;

/**
 * @author klimesf
 */
public class ImageLogger {

    private ImageLogger instance;

    private ImageLogger() {
    }

    public ImageLogger getInstance() {
        if (this.instance == null) {
            this.instance = new ImageLogger();
        }
        return instance;
    }

    public void saveImage(String image) {

    }

}
