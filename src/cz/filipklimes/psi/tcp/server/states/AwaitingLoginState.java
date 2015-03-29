package cz.filipklimes.psi.tcp.server.states;

import cz.filipklimes.psi.tcp.server.Client;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * @author klimesf
 */
public class AwaitingLoginState extends AbstractState implements State {

    /**
     * @param context
     */
    public AwaitingLoginState(Client context) {
        super(context);
    }

    @Override
    public void readMessage(BufferedInputStream input) throws IOException {
        String message = this.loadMessage(input);
        this.context.setNickname(message);
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
