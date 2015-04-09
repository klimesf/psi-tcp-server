package cz.filipklimes.psi.tcp.server;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.*;

/**
 * @author klimesf
 */
public class ClientHandlerExecutor implements Runnable {

    private Client handler;
    private Socket socket;
    private BufferedOutputStream outputStream;

    public ClientHandlerExecutor(Client handler, Socket socket) throws IOException {
        this.handler = handler;
        this.socket = socket;
        this.outputStream = new BufferedOutputStream(socket.getOutputStream());
    }

    @Override
    public void run() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        try {
            executorService.submit(this.handler).get(Robot.TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
        } catch (ExecutionException e) {
        } catch (TimeoutException e) {
            try {
                System.err.printf("[%d]: Connection timed out\n", this.handler.getClientNumber());
                this.outputStream.write("502 TIMEOUT\r\n".getBytes());
                this.outputStream.flush();
                this.socket.close();
            } catch (IOException e1) {
            }
        }
    }
}
