package robot;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Scanner;

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
/**
 * @author klimesf
 */
class Client implements Runnable {

    /**
     * Nickname of the client.
     */
    private String nickname;

    /**
     * Socket of the client.
     */
    private final Socket socket;

    /**
     * Input scanner.
     */
    private final Scanner input;

    /**
     * Output stream to the client.
     */
    private final DataOutputStream output;

    /**
     * State of the machine.
     */
    private State state = new InitialState(this);

    /**
     * Constructor.
     *
     * @param socket Client's socket
     * @throws java.io.IOException
     */
    public Client(Socket socket) throws IOException {
        this.socket = socket;
        this.input = new Scanner(socket.getInputStream());
        this.output = new DataOutputStream(socket.getOutputStream());
    }

    /**
     * Disconnects client from the server.
     *
     * @throws IOException
     */
    public void disconnect() throws IOException {
        socket.close();
        System.out.printf("%s left\n", this.nickname);
    }

    /**
     * Sets nickname of the client.
     *
     * @param nickname
     */
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    /**
     * Returns nickname of the client.
     *
     * @return Client robot's nickname.
     */
    public String getNickname() {
        return nickname;
    }

    /**
     * @param state
     */
    public void setState(State state) {
        this.state = state;
    }

    /**
     * Runs the Client.
     * Runnable interface method implementation.
     * <p/>
     * <p>
     * Reads input from the client and creates corresponding Action instances.
     * First command from the client must be NICK. Supported commands are:
     * NICK, SEND, ENTER, LEAVE, CREATE and BYE.
     * </p>
     */
    @Override
    public void run() {
        try {
//
//            BufferedReader br = new BufferedReader(
//                    new InputStreamReader(
//                            socket.getInputStream()));
//            char[] charBuffer = new char[1024];
//            br.read(charBuffer);
//            System.out.println(charBuffer);

            // Server talks first
            this.state.readMessage(this.input);
            this.state.printOutput(this.output);
            this.state.setNextState();

            // Continue in loop while client talks
            while (this.input.hasNext()) {
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
            this.socket.close();

        } catch (IOException ex) {
            System.err.println("An I/O exception occurred.");
        }
    }
}


/**
 * @author klimesf
 */
interface State {

    void readMessage(Scanner input);

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

}


/**
 * @author klimesf
 */
class AwaitingLoginState extends AbstractState implements State {

    private boolean nicknameOkay;

    /**
     * @param context
     */
    public AwaitingLoginState(Client context) {
        super(context);
    }

    @Override
    public void readMessage(Scanner input) {
        String nickname = input.next();
        this.context.setNickname(nickname);
    }

    @Override
    public void printOutput(DataOutputStream output) throws IOException {
        output.writeBytes("201 PASSWORD\r\n");
    }

    @Override
    public void setNextState() throws IOException {
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
    public void readMessage(Scanner input) {
        String password = input.next();
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
            this.context.setState(new AwaitingMessageState(this.context));
        } else {
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
        // Obtain nickname
        String nickname = this.context.getNickname();

        // Check nickname
        if (!nickname.startsWith("Robot")) {
            return false;
        }

        // Calculate correct password
        int calculatedPasword = 0;
        for (char c : nickname.toCharArray()) {
            calculatedPasword += Character.getNumericValue(c);
        }

        // Compare sent and calculated password
        return calculatedPasword == Integer.parseInt(password.trim());
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
    public void readMessage(Scanner input) {
        // do nothing
    }

    @Override
    public void printOutput(DataOutputStream output) throws IOException {
        output.writeBytes("200 LOGIN\r\n");
    }

    @Override
    public void setNextState() {
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
     * @param context
     */
    public AwaitingMessageState(Client context) {
        super(context);
    }

    @Override
    public void readMessage(Scanner input) {
        String message = input.next();
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
                this.context.setState(new AwaitingMessageState(this.context));
                break;

            case BAD_SYNTAX:
            default:
                this.context.disconnect();
                break;
        }
    }

    /**
     * Syntax status of the message.
     */
    private enum SyntaxStatus {
        BAD_SYNTAX, BAD_CHECKSUM, OKAY
    }

    /**
     * Parses the given message from robot.
     *
     * @param message The message, either an INFO or FOTO message.
     * @return Syntax status of the message.
     */
    private SyntaxStatus parseMessage(String message) {
        if (message.startsWith("INFO")) {
            return this.parseInfoMessage(message);
        } else if (message.startsWith("FOTO")) {
            return this.parseFotoMessage(message);
        } else {
            return SyntaxStatus.BAD_SYNTAX;
        }
    }

    private SyntaxStatus parseInfoMessage(String message) {
        String log = message.substring(5);
        InfoLogger.getInstance().saveLog(log);
        System.out.printf("%s: %s", this.context.getNickname(), log);
        return SyntaxStatus.OKAY;
    }

    private SyntaxStatus parseFotoMessage(String message) {
        return SyntaxStatus.OKAY;
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