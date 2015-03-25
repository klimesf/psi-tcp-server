package cz.filipklimes.psi.tcp.server.states;

import cz.filipklimes.psi.tcp.server.Client;
import cz.filipklimes.psi.tcp.server.InfoLogger;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Scanner;

/**
 * @author klimesf
 */
public class AwaitingMessageState extends AbstractState implements State {

    /**
     * Flag. Set to TRUE if the incoming message was okay, set to FALSE if not.
     */
    private SyntaxStatus messageStatus;

    /**
     * @param context
     */
    public AwaitingMessageState(Client context) {
        super(context);
    }

    @Override
    public void readMessage(Scanner input) {
        String message = input.next();
        this.messageStatus = this.parseMessage(message);
    }

    @Override
    public void printOutput(DataOutputStream output) throws IOException {
        switch (this.messageStatus) {
            case OKAY:
                output.writeBytes("202 OK\r\n");
                break;

            case BAD_CHECKSUM:
                output.writeBytes("300 BAD CHECKSUM\r\n");
                break;

            case BAD_SYNTAX:
            default:
                output.writeBytes("501 SYNTAX ERROR\r\n");
                break;
        }
    }

    @Override
    public void setNextState() throws IOException {
        switch (this.messageStatus) {
            case OKAY:
            case BAD_CHECKSUM:
                this.context.setState(new AwaitingMessageState(this.context));
                break;

            case BAD_SYNTAX:
            default:
                this.context.disconnect();
                break;
        }
    }

    /**
     * Syntax status of the message.
     */
    private enum SyntaxStatus {
        BAD_SYNTAX, BAD_CHECKSUM, OKAY
    }

    /**
     * Parses the given message from robot.
     *
     * @param message The message, either an INFO or FOTO message.
     * @return Syntax status of the message.
     */
    private SyntaxStatus parseMessage(String message) {
        if (message.startsWith("INFO")) {
            return this.parseInfoMessage(message);
        } else if (message.startsWith("FOTO")) {
            return this.parseFotoMessage(message);
        } else {
            return SyntaxStatus.BAD_SYNTAX;
        }
    }

    private SyntaxStatus parseInfoMessage(String message) {
        String log = message.substring(5);
        InfoLogger.getInstance().saveLog(log);
        System.out.printf("%s: %s", this.context.getNickname(), log);
        return SyntaxStatus.OKAY;
    }

    private SyntaxStatus parseFotoMessage(String message) {
        return SyntaxStatus.OKAY;
    }

}
