package robot;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
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

        int clientNumber = 1;
        while (true) {
            try {
                Socket clientSocket = serverSocket.accept();
                // Start client's own thread
                Client handler = new Client(clientSocket, clientNumber++);
                new Thread(handler).start();
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

/**
 * @author klimesf
 */
class Client implements Runnable {

    /**
     * Socket of the client.
     */
    private final Socket socket;

    /**
     * Clients number.
     */
    private int clientNumber;

    /**
     * Input scanner.
     */
    private final BufferedInputStream input;

    /**
     * Output stream to the client.
     */
    private final DataOutputStream output;

    /**
     * State of the machine.
     */
    private State state = new InitialState(this);

    /**
     * Calculated password of the client.
     */
    private int calculatedPassword;

    /**
     * Constructor.
     *
     * @param socket Client's socket
     * @throws java.io.IOException
     */
    public Client(Socket socket, int clientNumber) throws IOException {
        this.socket = socket;
        this.clientNumber = clientNumber;
        this.input = new BufferedInputStream(socket.getInputStream());
        this.output = new DataOutputStream(socket.getOutputStream());
    }

    /**
     * Disconnects client from the server.
     *
     * @throws IOException
     */
    public void disconnect() throws IOException {
        System.out.printf("[%d]: Disconnecting.\n", this.getClientNumber());
        this.input.close();
        this.output.close();
        this.socket.close();
        System.out.printf("[%d] left\n", this.getClientNumber());
    }

    /**
     * Returns client's number.
     *
     * @return Number of the client.
     */
    public int getClientNumber() {
        return clientNumber;
    }

    /**
     * @param state
     */
    public void setState(State state) {
        this.state = state;
    }

    /**
     * @param calculatedPassword
     */
    public void setCalculatedPassword(int calculatedPassword) {
        this.calculatedPassword = calculatedPassword;
    }

    /**
     * @return
     */
    public int getCalculatedPassword() {
        return calculatedPassword;
    }

    /**
     * Serves the Client.
     * Runnable interface method implementation.
     */
    @Override
    public void run() {
        try {
            // Server talks first
            this.state.readMessage(this.input);
            this.state.printOutput(this.output);
            this.state.setNextState();

            // Continue in loop while client talks
            while (!this.socket.isClosed()) {
                // Server shutting down
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
                // Load next command
                this.state.readMessage(this.input);
                this.state.printOutput(this.output);
                this.state.setNextState();
            }

        } catch (SocketException ex) {
            System.err.printf("[%d]: Robot disconnected.\n", this.getClientNumber());
        } catch (IOException ex) {
            System.err.printf("[%d]: An I/O exception occurred: %s\n",
                    this.getClientNumber(),
                    ex.getMessage()
            );
            ex.printStackTrace();
        } finally {
            try {
                if (!this.socket.isClosed()) {
                    this.disconnect();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}


/**
 * @author klimesf
 */
interface State {

    void readMessage(BufferedInputStream input) throws IOException;

    void printOutput(DataOutputStream output) throws IOException;

    void setNextState() throws IOException;

}

abstract class AbstractState implements State {

    /**
     * The client context.
     */
    protected Client context;

    /**
     * @param context
     */
    public AbstractState(Client context) {
        this.context = context;
    }
}

/**
 * @author klimesf
 */
class InitialState extends AbstractState {

    /**
     * @param context
     */
    public InitialState(Client context) {
        super(context);
    }

    @Override
    public void readMessage(BufferedInputStream input) {
        // do nothing
    }

    @Override
    public void printOutput(DataOutputStream output) throws IOException {
        output.writeBytes("200 LOGIN\r\n");
        System.out.printf("[%d]: Sending 200 LOGIN answer.\n", this.context.getClientNumber());
    }

    @Override
    public void setNextState() {
        System.out.printf("[%d]: Changing state to: AwaitingLoginState.\n", this.context.getClientNumber());
        this.context.setState(new AwaitingLoginState(this.context));
    }
}


class AwaitingLoginState extends AbstractState {


    public AwaitingLoginState(Client context) {
        super(context);
    }

    @Override
    public void readMessage(BufferedInputStream input) throws IOException {

        int current;
        int last = 0;
        int calculatedPassword = 0;

        do {
            // Read the input
            current = input.read();

            // Escape sequence met, subtract '\r' value from calculated password's value
            if (last == '\r' && current == '\n') {
                calculatedPassword -= last;
                break;
            }

            // Iterate
            last = current;
            calculatedPassword += current;

        } while (current != -1);

        this.context.setCalculatedPassword(calculatedPassword);
        System.out.printf("[%d]: Calculated password: %d.\n", this.context.getClientNumber(), calculatedPassword);
    }

    @Override
    public void printOutput(DataOutputStream output) throws IOException {
        output.writeBytes("201 PASSWORD\r\n");
        System.out.printf("[%d]: Sending 201 PASSWORD answer.\n", this.context.getClientNumber());
    }

    @Override
    public void setNextState() throws IOException {
        System.out.printf("[%d]: Changing state to: AwaitingPasswordState.\n", this.context.getClientNumber());
        this.context.setState(new AwaitingPasswordState(this.context));
    }
}

/**
 * Awaits password.
 */
class AwaitingPasswordState extends AbstractState {

    private boolean passwordOkay = false;

    public AwaitingPasswordState(Client context) {
        super(context);
    }

    @Override
    public void readMessage(BufferedInputStream input) throws IOException {

        System.out.printf("[%d]: Starting to read password.\n", this.context.getClientNumber());

        int last = 0;
        int current;
        StringBuilder sb = new StringBuilder();

        do {
            // Read the input
            current = input.read();

            // Escape sequence met
            if (last == '\r' && current == '\n') {
                break;
            }

            last = current;

            if (Character.isDigit(current)) {
                sb.append((char) current);
            }

        } while (current != -1);

        try {
            int password = Integer.parseInt(sb.toString().trim());
            System.out.printf("[%d]: Accepted password: %d.\n", this.context.getClientNumber(), password);
            this.passwordOkay = this.context.getCalculatedPassword() == password && this.context.getCalculatedPassword() > 0;
        } catch (NumberFormatException ex) {
            System.out.printf("[%d]: Could not parse password string.\n", this.context.getClientNumber());
        }

    }

    @Override
    public void printOutput(DataOutputStream output) throws IOException {
        if (this.passwordOkay) {
            output.writeBytes("202 OK\r\n");
            System.out.printf("[%d]: Sending 202 OK answer.\n", this.context.getClientNumber());
        } else {
            output.writeBytes("500 LOGIN FAILED\r\n");
            System.out.printf("[%d]: Sending 500 LOGIN FAILED answer.\n", this.context.getClientNumber());
            this.context.disconnect();
        }
    }

    @Override
    public void setNextState() throws IOException {
        System.out.printf("[%d]: Changing state to: AwaitingMessageState.\n", this.context.getClientNumber());
        this.context.setState(new AwaitingMessageState(this.context));
    }
}

class AwaitingMessageState extends AbstractState {

    private Next next = Next.INVALID;

    public AwaitingMessageState(Client context) {
        super(context);
    }

    @Override
    public void readMessage(BufferedInputStream input) throws IOException {

        int current;
        StringBuilder sb = new StringBuilder();

        current = input.read();
        if (current != 'F' && current != 'I') {
            System.out.printf("[%d]: Message 1st fail.\n", this.context.getClientNumber());
            this.next = Next.INVALID;
            return;
        }
        sb.append((char) current);

        current = input.read();
        if (current != 'O' && current != 'N') {
            System.out.printf("[%d]: Message 2nd fail.\n", this.context.getClientNumber());
            this.next = Next.INVALID;
            return;
        }
        sb.append((char) current);

        current = input.read();
        if (current != 'T' && current != 'F') {
            System.out.printf("[%d]: Message 3rd fail.\n", this.context.getClientNumber());
            this.next = Next.INVALID;
            return;
        }
        sb.append((char) current);

        current = input.read();
        if (current != 'O') {
            System.out.printf("[%d]: Message 4th fail.\n", this.context.getClientNumber());
            this.next = Next.INVALID;
            return;
        }
        sb.append((char) current);

        current = input.read();
        if (current != ' ') {
            System.out.printf("[%d]: Message beginning does not have ending whitespace.\n", this.context.getClientNumber());
            this.next = Next.INVALID;
            return;
        }
        sb.append((char) current);

        String beginning = sb.toString();

        System.out.printf("[%d]: Accepted message beginning: %s.\n", this.context.getClientNumber(), beginning);

        if (beginning.equals("FOTO ")) {
            this.next = Next.FOTO;
        } else if (beginning.equals("INFO ")) {
            this.next = Next.INFO;
        } else {
            this.next = Next.INVALID;
        }
    }

    @Override
    public void printOutput(DataOutputStream output) throws IOException {
        if (this.next == Next.INVALID) {
            output.writeBytes("501 SYNTAX ERROR\r\n");
            System.out.printf("[%d]: Sending 501 SYNTAX ERROR answer.\n", this.context.getClientNumber());
            this.context.disconnect();
        }
    }

    @Override
    public void setNextState() throws IOException {
        switch (this.next) {
            case INFO:
                System.out.printf("[%d]: Changing state to: AwaitingINFOState.\n", this.context.getClientNumber());
                this.context.setState(new AwaitingINFOState(this.context));
                break;

            case FOTO:
                System.out.printf("[%d]: Changing state to: AwaitingFOTOState.\n", this.context.getClientNumber());
                this.context.setState(new AwaitingFOTOState(this.context));
                break;

            case INVALID:
                System.out.printf("[%d]: Changing state to: AwaitingMessageState.\n", this.context.getClientNumber());
                this.context.setState(new AwaitingMessageState(this.context));
                break;
        }
    }

    enum Next {INFO, FOTO, INVALID}
}

class AwaitingINFOState extends AbstractState {

    /**
     * @param context
     */
    public AwaitingINFOState(Client context) {
        super(context);
    }

    @Override
    public void readMessage(BufferedInputStream input) throws IOException {

        int current;
        int last = 0;

        do {
            // Read the input
            current = input.read();

            // Escape sequence met
            if (last == '\r' && current == '\n') {
                break;
            }

            last = current;

        } while (current != -1);

        System.out.printf("[%d]: Accepted INFO message.\n", this.context.getClientNumber());
    }

    @Override
    public void printOutput(DataOutputStream output) throws IOException {
        output.writeBytes("202 OK\r\n");
        System.out.printf("[%d]: Sending 202 OK answer.\n", this.context.getClientNumber());
    }

    public void setNextState() throws IOException {
        System.out.printf("[%d]: Changing state to: AwaitingMessageState.\n", this.context.getClientNumber());
        this.context.setState(new AwaitingMessageState(this.context));
    }
}

class AwaitingFOTOState extends AbstractState {

    private ChecksumStatus checksumStatus;

    /**
     * @param context
     */
    public AwaitingFOTOState(Client context) {
        super(context);
    }

    @Override
    public void readMessage(BufferedInputStream input) throws IOException {

        int current;
        int last = 0;
        int numberOfBytes = 0;
        int counter = 0;
        StringBuilder sb = new StringBuilder();
        int calculatedChecksum = 0;
        int checksum = 0;
this.context.disconnect();
        // Get number of bytes
        do {
            // Read the input
            current = input.read();

            // Escape sequence met
            if (Character.isWhitespace(current)) {
                break;
            }

            sb.append((char) current);

        } while (current != -1);

        try {
            numberOfBytes = Integer.parseInt(sb.toString());
        } catch (NumberFormatException ex) {
            System.out.printf("[%d]:Could not parse FOTO byte length number.\n", this.context.getClientNumber());
            this.checksumStatus = ChecksumStatus.INVALID_SYNTAX;
            return;
        }

        // Calculate checksum
        while (current != -1 && counter < numberOfBytes) {
            // Read the input
            current = input.read();
            calculatedChecksum += current;
            counter++;
        }

        // Assert checksum
        counter = 3;
        while (current != -1 && counter > -1) {
            // Read the input
            current = input.read();
            calculatedChecksum += (current * Math.pow(16, counter--));
            counter--;
        }

        if (checksum == calculatedChecksum) {
            this.checksumStatus = ChecksumStatus.OK;
        } else {
            this.checksumStatus = ChecksumStatus.BAD;
        }

        System.out.printf("[%d]: Calculated checksum: %d.\n", this.context.getClientNumber(), calculatedChecksum);
        System.out.printf("[%d]: Sent checksum: %d.\n", this.context.getClientNumber(), checksum);
        System.out.printf("[%d]: Accepted FOTO message.\n", this.context.getClientNumber());
    }

    @Override
    public void printOutput(DataOutputStream output) throws IOException {
        if (this.checksumStatus == ChecksumStatus.BAD) {
            System.out.printf("[%d]: Sending 300 BAD CHECKSUM answer.\n", this.context.getClientNumber());
            output.writeBytes("300 BAD CHECKSUM\r\n");
        } else if (this.checksumStatus == ChecksumStatus.INVALID_SYNTAX) {
            System.out.printf("[%d]: Sending 501 SYNTAX ERROR answer.\n", this.context.getClientNumber());
            output.writeBytes("501 SYNTAX ERROR\r\n");
            this.context.disconnect();
        } else {
            output.writeBytes("202 OK\r\n");
            System.out.printf("[%d]: Sending 202 OK answer.\n", this.context.getClientNumber());
        }
    }

    public void setNextState() throws IOException {
        System.out.printf("[%d]: Changing state to: AwaitingMessageState.\n", this.context.getClientNumber());
        this.context.setState(new AwaitingMessageState(this.context));
    }

    enum ChecksumStatus {OK, INVALID_SYNTAX, BAD}

}