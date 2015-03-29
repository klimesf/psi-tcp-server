package cz.filipklimes.psi.tcp.server.states;

import cz.filipklimes.psi.tcp.server.Client;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;

/**
 * @author klimesf
 */
abstract public class AbstractState implements State {

    /**
     *
     */
    protected Client context;

    /**
     * @param context
     */
    public AbstractState(Client context) {
        this.context = context;
    }

    /**
     * Loads message from BufferedReader.
     *
     * @param input Input from client.
     * @return The message.
     * @throws IOException
     */
    public String loadMessage(BufferedInputStream input) throws IOException {
        StringBuilder sb = new StringBuilder(2 << 19);
        char lastChar = ' ';
        char currentChar;
        int length = 0;
        while (input.available() > 0) {
            int currentByte = input.read();
            if (currentByte == -1) {
                return null;
            }
            currentChar = (char) currentByte;
            ++length;

            if (lastChar != '\r' && currentChar != '\n') {
                break;
            }
            sb.append(currentChar);
            lastChar = currentChar;
        }
        String message = sb.toString();
        System.out.printf("Acquired message: %s\n", message);
        return message;
    }
}
