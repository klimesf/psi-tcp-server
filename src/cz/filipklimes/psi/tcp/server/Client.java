package cz.filipklimes.psi.tcp.server;

import cz.filipklimes.psi.tcp.server.states.InitialState;
import cz.filipklimes.psi.tcp.server.states.State;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.CharBuffer;
import java.util.Scanner;

/**
 * @author klimesf
 */
public class Client implements Runnable {

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