package cz.filipklimes.psi.tcp.server.states;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * @author klimesf
 */
public interface State {

    void readMessage(BufferedInputStream input) throws IOException;

    void printOutput(DataOutputStream output) throws IOException;

    void setNextState() throws IOException;

}
