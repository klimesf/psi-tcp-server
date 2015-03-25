package cz.filipklimes.psi.tcp.server.states;

import cz.filipklimes.psi.tcp.server.Client;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Scanner;

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
