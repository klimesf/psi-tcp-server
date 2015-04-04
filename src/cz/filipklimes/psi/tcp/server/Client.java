package cz.filipklimes.psi.tcp.server;

import cz.filipklimes.psi.tcp.server.states.InitialState;
import cz.filipklimes.psi.tcp.server.states.State;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

/**
 * Client represents a thread serving the Robot on the other side of the socket.
 *
 * @author klimesf
 */
public class Client implements Runnable {

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

    /**
     * Does the password start with "Robot"?
     */
    private boolean passwordStartOkay = false;

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
     * @return The calculated password. Might not be initialized.
     */
    public int getCalculatedPassword() {
        return calculatedPassword;
    }

    /**
     * @param passwordStartOkay
     */
    public void setPasswordStartOkay(boolean passwordStartOkay) {
        this.passwordStartOkay = passwordStartOkay;
    }

    /**
     * @return true if the password starts with "Robot", false if not. Might not be initialized.
     */
    public boolean isPasswordStartOkay() {
        return passwordStartOkay;
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