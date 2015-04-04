package cz.filipklimes.psi.tcp.server.states;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;

/**
 * @author klimesf
 */
public interface State {

    void readMessage(BufferedInputStream input) throws IOException;

    void printOutput(BufferedOutputStream output) throws IOException;

    void setNextState() throws IOException;
}