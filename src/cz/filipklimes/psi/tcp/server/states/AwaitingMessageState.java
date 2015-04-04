package cz.filipklimes.psi.tcp.server.states;

import cz.filipklimes.psi.tcp.server.Client;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;

/**
 * @author klimesf
 */
class AwaitingMessageState extends AbstractState {

    private Next next = Next.INVALID;

    public AwaitingMessageState(Client context) {
        super(context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readMessage(BufferedInputStream input) throws IOException {

        int current;
        StringBuilder sb = new StringBuilder();

        current = input.read();
        if (current != 'F' && current != 'I') {
            System.out.printf("[%d]: Message 1st fail.\n", this.context.getClientNumber());
            this.next = Next.INVALID;
            return;
        }
        sb.append((char) current);

        current = input.read();
        if (current != 'O' && current != 'N') {
            System.out.printf("[%d]: Message 2nd fail.\n", this.context.getClientNumber());
            this.next = Next.INVALID;
            return;
        }
        sb.append((char) current);

        current = input.read();
        if (current != 'T' && current != 'F') {
            System.out.printf("[%d]: Message 3rd fail.\n", this.context.getClientNumber());
            this.next = Next.INVALID;
            return;
        }
        sb.append((char) current);

        current = input.read();
        if (current != 'O') {
            System.out.printf("[%d]: Message 4th fail.\n", this.context.getClientNumber());
            this.next = Next.INVALID;
            return;
        }
        sb.append((char) current);

        current = input.read();
        if (current != ' ') {
            System.out.printf("[%d]: Message beginning does not have ending whitespace.\n", this.context.getClientNumber());
            this.next = Next.INVALID;
            return;
        }
        sb.append((char) current);

        String beginning = sb.toString();

        System.out.printf("[%d]: Accepted message beginning: %s.\n", this.context.getClientNumber(), beginning);

        if (beginning.equals("FOTO ")) {
            this.next = Next.FOTO;
        } else if (beginning.equals("INFO ")) {
            this.next = Next.INFO;
        } else {
            this.next = Next.INVALID;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void printOutput(BufferedOutputStream output) throws IOException {
        if (this.next == Next.INVALID) {
            output.write("501 SYNTAX ERROR\r\n".getBytes());
            output.flush();
            System.out.printf("[%d]: Sending 501 SYNTAX ERROR answer.\n", this.context.getClientNumber());
            this.context.disconnect();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setNextState() throws IOException {
        switch (this.next) {
            case INFO:
                System.out.printf("[%d]: Changing state to: AwaitingINFOState.\n", this.context.getClientNumber());
                this.context.setState(new AwaitingINFOState(this.context));
                break;

            case FOTO:
                System.out.printf("[%d]: Changing state to: AwaitingFOTOState.\n", this.context.getClientNumber());
                this.context.setState(new AwaitingFOTOState(this.context));
                break;

            case INVALID:
                System.out.printf("[%d]: Changing state to: AwaitingMessageState.\n", this.context.getClientNumber());
                this.context.setState(new AwaitingMessageState(this.context));
                break;
        }
    }

    /**
     * What should the next state be?
     */
    enum Next {
        INFO, FOTO, INVALID
    }
}
