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
    private final BufferedOutputStream output;

    /**
     * State of the machine.
     */
    private State state = new InitialState(this);

    /**
     * Calculated password of the client.
     */
    private int calculatedPassword;
    private boolean passwordStartsWithRobot;

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
        this.output = new BufferedOutputStream(socket.getOutputStream());
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
     * @param passwordStartsWithRobot
     */
    public void setPasswordStartsWithRobot(boolean passwordStartsWithRobot) {
        this.passwordStartsWithRobot = passwordStartsWithRobot;
    }

    /**
     * @return
     */
    public boolean isPasswordStartsWithRobot() {
        return passwordStartsWithRobot;
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

    void printOutput(BufferedOutputStream output) throws IOException;

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
    public void printOutput(BufferedOutputStream output) throws IOException {
        output.write("200 LOGIN\r\n".getBytes());
        output.flush();
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
        int counter = 0;
        StringBuilder sb = new StringBuilder(5);

        do {
            // Read the input
            current = input.read();

            // Save the first 5 chars of the password
            if (counter++ < 5) {
                sb.append((char) current);
            }

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
        this.context.setPasswordStartsWithRobot(sb.toString().equalsIgnoreCase("Robot"));
        System.out.printf("[%d]: Calculated password: %d.\n", this.context.getClientNumber(), calculatedPassword);
    }

    @Override
    public void printOutput(BufferedOutputStream output) throws IOException {
        output.write("201 PASSWORD\r\n".getBytes());
        output.flush();
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
            this.passwordOkay = this.context.getCalculatedPassword() == password && this.context.isPasswordStartsWithRobot();
        } catch (NumberFormatException ex) {
            System.out.printf("[%d]: Could not parse password string.\n", this.context.getClientNumber());
        }

    }

    @Override
    public void printOutput(BufferedOutputStream output) throws IOException {
        if (this.passwordOkay) {
            output.write("202 OK\r\n".getBytes());
            output.flush();
            System.out.printf("[%d]: Sending 202 OK answer.\n", this.context.getClientNumber());
        } else {
            output.write("500 LOGIN FAILED\r\n".getBytes());
            output.flush();
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
    public void printOutput(BufferedOutputStream output) throws IOException {
        if (this.next == Next.INVALID) {
            output.write("501 SYNTAX ERROR\r\n".getBytes());
            output.flush();
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
    public void printOutput(BufferedOutputStream output) throws IOException {
        output.write("202 OK\r\n".getBytes());
        output.flush();
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

    /**
     * {@inheritDoc}
     */
    @Override
    public void readMessage(BufferedInputStream input) throws IOException {
        int numberOfBytes;
        long calculatedChecksum;

        numberOfBytes = readNumberOfBytes(input);
        if (numberOfBytes < 0) return;

        calculatedChecksum = readPhoto(input, numberOfBytes);
        assertChecksum(input, calculatedChecksum);
    }

    /**
     * Reads info about how many bytes the photo will have.
     *
     * @param input Input stream.
     * @return Number of bytes or -1 if invalid data were provided.
     * @throws IOException
     */
    private int readNumberOfBytes(BufferedInputStream input) throws IOException {

        int current;
        StringBuilder sb = new StringBuilder();

        // Get number of bytes
        do {
            // Read the input
            current = input.read();

            // Escape sequence met
            if (current == ' ' || !Character.isDigit(current)) {
                break;
            }

            sb.append((char) current);

        } while (current != -1);

        try {
            int numberOfBytes = Integer.parseInt(sb.toString().trim());
            System.out.printf("[%d]: Length of FOTO will be: %d\n", this.context.getClientNumber(), numberOfBytes);
            return numberOfBytes;
        } catch (NumberFormatException ex) {
            System.out.printf("[%d]: Could not parse FOTO byte length number: %s\n", this.context.getClientNumber(), sb.toString().trim());
            this.checksumStatus = ChecksumStatus.INVALID_SYNTAX;
            return -1;
        }
    }

    /**
     * Reads the photo and saves it to a file.
     *
     * @param input         Input stream.
     * @param numberOfBytes Number of bytes of the photo.
     * @return Calculated checksum of the photo.
     * @throws IOException
     */
    private long readPhoto(BufferedInputStream input, int numberOfBytes) throws IOException {

        int counter = 0;
        int current = 0;
        long calculatedChecksum = 0;

        // Prepare file
        PhotoFileHandler photoFileHandler = SingletonPhotoFileHandlerImpl.getInstance();
        photoFileHandler.createFile("photo.jpg");

        // Calculate checksum and save the photo to file
        while (current != -1 && counter < numberOfBytes) {
            // Read the input
            current = input.read();
            calculatedChecksum += current;
            counter++;
            photoFileHandler.appendChar((char) current);
        }
        photoFileHandler.close();
        return calculatedChecksum;
    }

    /**
     * Reads and asserts the checksum of the photo.
     *
     * @param input              Input stream.
     * @param calculatedChecksum Calculated checksum of the accepted photo.
     * @throws IOException
     */
    private void assertChecksum(BufferedInputStream input, long calculatedChecksum) throws IOException {
        int counter;
        int current = 0;
        long checksum;// Assert checksum
        counter = 3;
        StringBuilder checksumStringBuilder = new StringBuilder();

        while (current != -1 && counter > -1) {
            // Read the input
            current = input.read();
            checksumStringBuilder.append(Integer.toHexString(current));
            System.out.printf("[%d]: Checksum byte: %d = %c.\n", this.context.getClientNumber(), current, (char) current);
            counter--;
        }

        try {
            checksum = Integer.parseInt(checksumStringBuilder.toString().trim(), 16);
        } catch (NumberFormatException ex) {
            checksum = -1;
        }

        if (checksum == calculatedChecksum) {
            this.checksumStatus = ChecksumStatus.OK;
        } else {
            this.checksumStatus = ChecksumStatus.BAD;
        }

        System.out.printf("[%d]: Calculated checksum: %d.\n", this.context.getClientNumber(), calculatedChecksum);
        System.out.printf("[%d]: Sent checksum: %d.\n", this.context.getClientNumber(), checksum);
        System.out.printf("[%d]: Accepted checksum string: %s.\n", this.context.getClientNumber(), checksumStringBuilder.toString());
        System.out.printf("[%d]: Accepted FOTO message.\n", this.context.getClientNumber());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void printOutput(BufferedOutputStream output) throws IOException {
        if (this.checksumStatus == ChecksumStatus.BAD) {
            System.out.printf("[%d]: Sending 300 BAD CHECKSUM answer.\n", this.context.getClientNumber());
            output.write("300 BAD CHECKSUM\r\n".getBytes());
            output.flush();
        } else if (this.checksumStatus == ChecksumStatus.INVALID_SYNTAX) {
            System.out.printf("[%d]: Sending 501 SYNTAX ERROR answer.\n", this.context.getClientNumber());
            output.write("501 SYNTAX ERROR\r\n".getBytes());
            output.flush();
            this.context.disconnect();
        } else {
            output.write("202 OK\r\n".getBytes());
            output.flush();
            System.out.printf("[%d]: Sending 202 OK answer.\n", this.context.getClientNumber());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setNextState() throws IOException {
        System.out.printf("[%d]: Changing state to: AwaitingMessageState.\n", this.context.getClientNumber());
        this.context.setState(new AwaitingMessageState(this.context));
    }

    /**
     * Status of the incoming message.
     */
    enum ChecksumStatus {
        OK, INVALID_SYNTAX, BAD
    }
}

interface PhotoFileHandler {

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

class SingletonPhotoFileHandlerImpl implements PhotoFileHandler {

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