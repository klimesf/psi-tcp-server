package cz.filipklimes.psi.tcp.server.states;

import cz.filipklimes.psi.tcp.server.Client;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Scanner;

/**
 * @author klimesf
 */
public class AwaitingLoginState extends AbstractState implements State {

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
