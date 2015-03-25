package cz.filipklimes.psi.tcp.server.states;

import cz.filipklimes.psi.tcp.server.Client;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Scanner;

/**
 * @author klimesf
 */
public interface State {

    void readMessage(Scanner input);

    void printOutput(DataOutputStream output) throws IOException;

    void setNextState() throws IOException;

}
