package cz.filipklimes.psi.tcp.server.states;

import cz.filipklimes.psi.tcp.server.Client;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;

/**
 * @author klimesf
 */
public class InitialState extends AbstractState {

    /**
     * @param context
     */
    public InitialState(Client context) {
        super(context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readMessage(BufferedInputStream input) {
        // do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void printOutput(BufferedOutputStream output) throws IOException {
        output.write("200 LOGIN\r\n".getBytes());
        output.flush();
        System.out.printf("[%d]: Sending 200 LOGIN answer.\n", this.context.getClientNumber());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setNextState() {
        System.out.printf("[%d]: Changing state to: AwaitingLoginState.\n", this.context.getClientNumber());
        this.context.setState(new AwaitingLoginState(this.context));
    }
}