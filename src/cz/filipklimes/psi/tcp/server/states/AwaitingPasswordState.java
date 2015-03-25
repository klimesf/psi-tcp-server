package cz.filipklimes.psi.tcp.server.states;

import cz.filipklimes.psi.tcp.server.Client;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Scanner;

/**
 * @author klimesf
 */
public class AwaitingPasswordState extends AbstractState implements State {

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
