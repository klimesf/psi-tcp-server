package cz.filipklimes.psi.tcp.server.states;

import cz.filipklimes.psi.tcp.server.Client;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;

/**
 * @author klimesf
 */
class AwaitingPasswordState extends AbstractState {

    private boolean passwordOkay = false;

    public AwaitingPasswordState(Client context) {
        super(context);
    }

    /**
     * {@inheritDoc}
     */
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
            this.passwordOkay = this.context.getCalculatedPassword() == password && this.context.isPasswordStartOkay();
        } catch (NumberFormatException ex) {
            System.out.printf("[%d]: Could not parse password string.\n", this.context.getClientNumber());
        }

    }

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
    @Override
    public void setNextState() throws IOException {
        System.out.printf("[%d]: Changing state to: AwaitingMessageState.\n", this.context.getClientNumber());
        this.context.setState(new AwaitingMessageState(this.context));
    }
}
