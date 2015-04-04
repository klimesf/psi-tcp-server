package cz.filipklimes.psi.tcp.server.states;

import cz.filipklimes.psi.tcp.server.Client;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;

/**
 * @author klimesf
 */
class AwaitingLoginState extends AbstractState {

    public AwaitingLoginState(Client context) {
        super(context);
    }

    /**
     * {@inheritDoc}
     */
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
        this.context.setPasswordStartOkay(sb.toString().equalsIgnoreCase("Robot"));
        System.out.printf("[%d]: Calculated password: %d.\n", this.context.getClientNumber(), calculatedPassword);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void printOutput(BufferedOutputStream output) throws IOException {
        output.write("201 PASSWORD\r\n".getBytes());
        output.flush();
        System.out.printf("[%d]: Sending 201 PASSWORD answer.\n", this.context.getClientNumber());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setNextState() throws IOException {
        System.out.printf("[%d]: Changing state to: AwaitingPasswordState.\n", this.context.getClientNumber());
        this.context.setState(new AwaitingPasswordState(this.context));
    }
}
