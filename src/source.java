package robot1;

import java.io.DataInputStream;
import java.io.DataOutputStream;
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

        int clientNumber = 1;
        while (true) {
            try {
                Socket clientSocket = serverSocket.accept();
                // Start client's own thread
                Client handler = new Client(clientSocket, clientNumber++);
                new Thread(handler).start();
                System.out.printf("Client accepted from: %s:%d\n",
                        clientSocket.getInetAddress().toString(),
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
    private final DataInputStream input;

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
     * Flag. Does the nickname begin with "Robot"?
     */
    private boolean beginsWithRobot;

    /**
     * Constructor.
     *
     * @param socket Client's socket
     * @throws java.io.IOException
     */
    public Client(Socket socket, int clientNumber) throws IOException {
        this.socket = socket;
        this.clientNumber = clientNumber;
        this.input = new DataInputStream(socket.getInputStream());
        this.output = new DataOutputStream(socket.getOutputStream());
    }

    /**
     * Disconnects client from the server.
     *
     * @throws IOException
     */
    public void disconnect() throws IOException {
//        this.input.close();
//        this.output.close();
        this.socket.close();
        System.out.printf("[%d]: %s left\n", this.getClientNumber());
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
     * @param robot
     */
    public void beginsWithRobot(boolean robot) {
        this.beginsWithRobot = robot;
    }

    /**
     * @return
     */
    public boolean isBeginsWithRobot() {
        return beginsWithRobot;
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

            // When client signs off, close the socket
            if (!this.socket.isClosed()) {
                this.socket.close();
            }

        } catch (IOException ex) {
            System.err.printf("[%d]: An I/O exception occurred: %s, %s\n",
                    this.getClientNumber(),
                    ex.getMessage(),
                    ex.getStackTrace().toString()
            );
        }
    }
}


/**
 * @author klimesf
 */
interface State {

    void readMessage(DataInputStream input) throws IOException;

    void printOutput(DataOutputStream output) throws IOException;

    void setNextState() throws IOException;

}


/**
 * @author klimesf
 */
abstract class AbstractState implements State {

    /**
     *
     */
    protected Client context;

    /**
     * @param context
     */
    public AbstractState(Client context) {
        this.context = context;
    }

    /**
     * Loads message from BufferedReader.
     *
     * @param input Input from client.
     * @return The message.
     * @throws IOException
     */
    public String loadMessage(DataInputStream input) throws IOException {
        return loadMessage(input, true);
    }

    /**
     * Loads message from BufferedReader.
     *
     * @param input Input from client.
     * @return The message.
     * @throws IOException
     */
    public String loadMessage(DataInputStream input, boolean skipEmpty) throws IOException {
        String message;
        StringBuilder sb = new StringBuilder(2 << 19);
        char lastChar = ' ';
        char currentChar;
        int length = 0;

        while (input.available() > 0) {

            int currentByte = input.read();
            if (currentByte == -1) {
                System.out.printf("[%d]: Current byte is -1.\n", this.context.getClientNumber());
                return null;
            }
            currentChar = (char) currentByte;
            ++length;

            if (lastChar == '\r' && currentChar == '\n') {
                break;
            }
            sb.append(currentChar);
            lastChar = currentChar;
        }
        if (length < 1024) {
            message = sb.toString();
        } else {
            message = "--too long--";
        }

        // Skip empty message
        if (skipEmpty && message.length() < 1) {
            return loadMessage(input);
        } else {
            return message.trim();
        }
    }

}


/**
 * @author klimesf
 */
class AwaitingLoginState extends AbstractState implements State {

    /**
     * @param context
     */
    public AwaitingLoginState(Client context) {
        super(context);
    }

    @Override
    public String loadMessage(DataInputStream input) throws IOException {
        char lastChar = ' ';
        char currentChar;
        int calculatedPassword = 0;
        int length = 0;
        StringBuilder sb = new StringBuilder(5);

        while (true) {
            if (input.available() > 0) {
                break;
            }
        }

        while (input.available() > 0) {

            int currentByte = input.read();

            if (currentByte == -1) {
                System.out.printf("[%d]: Current byte is -1.\n", this.context.getClientNumber());
                return null;
            }

            currentChar = (char) currentByte;
            if (length < 5) {
                sb.append(currentChar);
            }
            ++length;


            if (lastChar == '\r' && currentChar == '\n') {
                break;
            }
            if (!Character.isWhitespace(currentChar)) {
                calculatedPassword += (int) currentChar;
            }
            lastChar = currentChar;
        }
        this.context.beginsWithRobot(sb.toString().trim().equals("Robot"));
        this.context.setCalculatedPassword(calculatedPassword);
        return "";
    }

    @Override
    public void readMessage(DataInputStream input) throws IOException {
        String nickname = this.loadMessage(input);
        System.out.printf("[%d]: Accepted nickname: %s. Calculated password is %d.%n", this.context.getClientNumber(), nickname, this.context.getCalculatedPassword());
    }

    @Override
    public void printOutput(DataOutputStream output) throws IOException {
        output.writeBytes("201 PASSWORD\r\n");
    }

    @Override
    public void setNextState() throws IOException {
        System.out.printf("[%d]: Changing state to: AwaitingPasswordState\n", this.context.getClientNumber());
        this.context.setState(new AwaitingPasswordState(this.context));
    }
}


/**
 * @author klimesf
 */
class AwaitingPasswordState extends AbstractState implements State {

    /**
     * Flag. TRUE if password is okay, FALSE if not.
     */
    private boolean passwordOkay;

    /**
     * @param context
     */
    public AwaitingPasswordState(Client context) {
        super(context);
    }

    @Override
    public void readMessage(DataInputStream input) throws IOException {
        String password = this.loadMessage(input);
        System.out.printf("[%d]: Accepted password: %s%n", this.context.getClientNumber(), password);
        this.passwordOkay = this.checkPassword(password);
    }

    @Override
    public void printOutput(DataOutputStream output) throws IOException {
        if (this.passwordOkay) {
            output.writeBytes("202 OK\r\n");
        } else {
            output.writeBytes("500 LOGIN FAILED\r\n");
        }
    }

    @Override
    public void setNextState() throws IOException {
        if (this.passwordOkay) {
            System.out.printf("[%d]: Changing state to: AwaitingMessageState.\n", this.context.getClientNumber());
            this.context.setState(new AwaitingMessageState(this.context));
        } else {
            System.out.printf("[%d]: Couldn't change to next state, wrong password.\n", this.context.getClientNumber());
            this.context.disconnect();
        }
    }

    /**
     * Checks if the nickname and password is okay.
     *
     * @param password The password.
     * @return TRUE if okay, FALSE if not.
     */
    private boolean checkPassword(String password) {
        System.out.printf("[%d]: Calculated password: %d\n", this.context.getClientNumber(), this.context.getCalculatedPassword());
        System.out.printf("[%d]: Sent password: %s\n", this.context.getClientNumber(), password);

        // Compare sent and calculated password
        try {
            return this.context.isBeginsWithRobot() && this.context.getCalculatedPassword() == Integer.parseInt(password.trim());
        } catch (NumberFormatException ex) {
            return false;
        }
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
    public void readMessage(DataInputStream input) {
        // do nothing
    }

    @Override
    public void printOutput(DataOutputStream output) throws IOException {
        System.out.printf("[%d]: Sending 200 LOGIN message.\n", this.context.getClientNumber());
        output.writeBytes("200 LOGIN\r\n");
    }

    @Override
    public void setNextState() {
        System.out.printf("[%d]: Changing state to: AwaitingLoginState.\n", this.context.getClientNumber());
        this.context.setState(new AwaitingLoginState(this.context));
    }
}


/**
 * @author klimesf
 */
class AwaitingMessageState extends AbstractState implements State {

    /**
     * Flag. Set to TRUE if the incoming message was okay, set to FALSE if not.
     */
    private SyntaxStatus messageStatus;

    /**
     * Flag. Is the checksum okay?
     */
    private boolean checksumOkay;

    /**
     * Flag. Is FOTO syntax okay?
     */
    private boolean syntaxOkay;

    /**
     * @param context
     */
    public AwaitingMessageState(Client context) {
        super(context);
    }

    @Override
    public String loadMessage(DataInputStream input) throws IOException {
        String message;
        StringBuilder sb = new StringBuilder(2 << 19);

        char currentChar;
        int length = 0;

        while (length < 5 && input.available() > 0) {

            int currentByte = input.read();
            if (currentByte == -1) {
                System.out.printf("[%d]: Current byte is -1.\n", this.context.getClientNumber());
                return null;
            }

            currentChar = (char) currentByte;
            if (length > 0 || !Character.isWhitespace(currentChar)) {
                ++length;
                sb.append(currentChar);
            }
        }

        message = sb.toString();

        System.out.printf("[%d]: message beginning: %s\n", this.context.getClientNumber(), message);

        if (message.equals("FOTO ")) {
            System.out.printf("[%d]: parsing as FOTO message.\n", this.context.getClientNumber());
            return message + this.loadFotoMessage(input);
        } else {
            System.out.printf("[%d]: parsing as INFO message.\n", this.context.getClientNumber());
            return message + this.loadInfoMessage(input);
        }
    }

    /**
     * @param input
     * @return
     * @throws IOException
     */
    private String loadInfoMessage(DataInputStream input) throws IOException {
        char lastChar = ' ';
        char currentChar;
//        StringBuilder sb = new StringBuilder();
        int length = 0;
        while (input.available() > 0) {
            int currentByte = input.read();
            if (currentByte == -1) {
                System.out.printf("[%d]: Current byte is -1.\n", this.context.getClientNumber());
                return null;
            }
            currentChar = (char) currentByte;
            if (lastChar == '\r' && currentChar == '\n') {
                break;
            }
            ++length;
//            sb.append(currentChar);
            lastChar = currentChar;
        }

//        if(length > 1024) {
            return "-- too long --";
//        } else {
//            return sb.toString();
//        }
    }

    /**
     * @param input
     * @return
     * @throws IOException
     */
    private String loadFotoMessage(DataInputStream input) throws IOException {
        char lastChar = 'x';
        char currentChar;
        int length;

        StringBuilder numberOfBytesBuilder = new StringBuilder();
        while (input.available() > 0 && !Character.isWhitespace(lastChar)) {
            currentChar = (char) input.read();
            numberOfBytesBuilder.append(currentChar);
            lastChar = currentChar;
        }
        int messageLength = Integer.parseInt(numberOfBytesBuilder.toString().trim());

        if(messageLength < 1) {
            this.syntaxOkay = false;
        }

        System.out.printf("[%d]: FOTO message length is %d.\n", this.context.getClientNumber(), messageLength);

        // Load the image and calc checksum
        int checksum = 0;
        length = 0;
        while (length < messageLength && input.available() > 0) {

            int currentByte = input.read();
            if (currentByte == -1) {
                System.out.printf("[%d]: Current byte is -1.\n", this.context.getClientNumber());
                return null;
            }

            currentChar = (char) currentByte;
            checksum += (int) currentChar;
            ++length;
            // TODO save image?
        }

        // Checksum
        System.out.printf("[%d]: Calculated checksum is %d.\n", this.context.getClientNumber(), checksum);

        // Check if the checksum is correct
        length = 4;
        int sentChecksum = 0;
        while (length > 0 && input.available() > 0) {

            int currentByte = input.read();
            if (currentByte == -1) {
                System.out.printf("[%d]: Current byte is -1.\n", this.context.getClientNumber());
                return null;
            }

            currentChar = (char) currentByte;
            checksum += ((int) currentChar) * (Math.pow(16, length));
            --length;
        }
        System.out.printf("[%d]: Sent checksum is %d.\n", this.context.getClientNumber(), sentChecksum);
        this.checksumOkay = checksum == sentChecksum;

        return "";
    }

    @Override
    public void readMessage(DataInputStream input) throws IOException {
        String message = this.loadMessage(input);
        System.out.printf("[%d]: Accepted new message(length = %d): %s\n", this.context.getClientNumber(), message.length(), message);
        this.messageStatus = this.parseMessage(message);
    }

    @Override
    public void printOutput(DataOutputStream output) throws IOException {
        switch (this.messageStatus) {
            case OKAY:
                output.writeBytes("202 OK\r\n");
                break;

            case BAD_CHECKSUM:
                output.writeBytes("300 BAD CHECKSUM\r\n");
                break;

            case EMPTY:
                break;

            case BAD_SYNTAX:
            default:
                output.writeBytes("501 SYNTAX ERROR\r\n");
                break;
        }
    }

    @Override
    public void setNextState() throws IOException {
        switch (this.messageStatus) {
            case OKAY:
            case BAD_CHECKSUM:
                System.out.printf("[%d]: Changing state to: AwaitingMessageState.\n", this.context.getClientNumber());
            case EMPTY:
                this.context.setState(new AwaitingMessageState(this.context));
                break;


            case BAD_SYNTAX:
            default:
                System.out.printf("[%d]: Disconnecting, bad syntax.\n", this.context.getClientNumber());
                this.context.disconnect();
                break;
        }
    }

    /**
     * Syntax status of the message.
     */
    private enum SyntaxStatus {
        BAD_SYNTAX, BAD_CHECKSUM, EMPTY, OKAY
    }

    /**
     * Parses the given message from robot.
     *
     * @param message The message, either an INFO or FOTO message.
     * @return Syntax status of the message.
     */
    private SyntaxStatus parseMessage(String message) {
        if (message.startsWith("INFO ")) {
            return this.parseInfoMessage(message);
        } else if (message.startsWith("FOTO ")) {
            return this.parseFotoMessage(message);
        } else if (message.length() < 1) {
            return SyntaxStatus.EMPTY;
        } else {
            return SyntaxStatus.BAD_SYNTAX;
        }
    }

    private SyntaxStatus parseInfoMessage(String message) {
        String log = message.substring(5);
        InfoLogger.getInstance().saveLog(log);
        System.out.printf("[%d]: LOG: %s\n", this.context.getClientNumber(), log);
        return SyntaxStatus.OKAY;
    }

    private SyntaxStatus parseFotoMessage(String message) {
        if(this.syntaxOkay) {
            return this.checksumOkay ? SyntaxStatus.OKAY : SyntaxStatus.BAD_CHECKSUM;
        } else {
            return SyntaxStatus.BAD_SYNTAX;
        }
    }
}


/**
 * @author klimesf
 */
class InfoLogger {

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