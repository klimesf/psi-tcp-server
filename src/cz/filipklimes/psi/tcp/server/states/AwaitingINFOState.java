package cz.filipklimes.psi.tcp.server.states;

import cz.filipklimes.psi.tcp.server.Client;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;

/**
 * @author klimesf
 */
public class AwaitingINFOState extends AbstractState {

    /**
     * @param context
     */
    public AwaitingINFOState(Client context) {
        super(context);
    }

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
    @Override
    public void printOutput(BufferedOutputStream output) throws IOException {
        output.write("202 OK\r\n".getBytes());
        output.flush();
        System.out.printf("[%d]: Sending 202 OK answer.\n", this.context.getClientNumber());
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
