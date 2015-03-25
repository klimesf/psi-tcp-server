package cz.filipklimes.psi.tcp.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Iterator;

/**
 * @author klimesf
 */
public class Robot {

    /**
     * Launches the Server and listens on given port.
     * When a new client connects, starts a new thread which then communicates with the client.
     *
     * @param args Command line arguments.
     */
    public static void main(String[] args) {
        ServerSocket serverSocket;
        int port = Robot.parsePort(args); // Get port number from arguments

        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException ex) {
            System.err.printf("Could not listen on port: %s\r", port);
            System.exit(1);
            return;
        }


        while (true) {
            try {
                Socket clientSocket = serverSocket.accept();
                // Start client's own thread
                Client handler = new Client(clientSocket);
                new Thread(handler).start();
                System.out.printf("Client accepted from: %n:%n\n",
                        clientSocket.getInetAddress(),
                        clientSocket.getLocalPort());
            } catch (IOException ex) {
                System.err.println("Accept failed.");
            }
        }
    }

    /**
     * Parses port from the command line arguments
     *
     * @param args Command line arguments
     * @return
     */
    private static int parsePort(String[] args) {
        int port = 3000; // Default value
        Iterator<String> iterator = Arrays.asList(args).iterator();
        if (iterator.hasNext()) {
            port = Integer.parseInt(iterator.next());
        }
        return port;
    }

}
