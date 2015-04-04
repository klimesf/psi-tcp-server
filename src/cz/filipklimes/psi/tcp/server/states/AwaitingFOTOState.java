package cz.filipklimes.psi.tcp.server.states;

import cz.filipklimes.psi.tcp.server.Client;
import cz.filipklimes.psi.tcp.server.PhotoFileHandler;
import cz.filipklimes.psi.tcp.server.SingletonPhotoFileHandlerImpl;

import java.io.*;

/**
 * @author klimesf
 */
public class AwaitingFOTOState extends AbstractState {

    private ChecksumStatus checksumStatus;

    /**
     * @param context
     */
    public AwaitingFOTOState(Client context) {
        super(context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readMessage(BufferedInputStream input) throws IOException {
        int numberOfBytes;
        long calculatedChecksum;

        numberOfBytes = readNumberOfBytes(input);
        if (numberOfBytes < 0) return;

        calculatedChecksum = readPhoto(input, numberOfBytes);
        assertChecksum(input, calculatedChecksum);
    }

    /**
     * Reads info about how many bytes the photo will have.
     *
     * @param input Input stream.
     * @return Number of bytes or -1 if invalid data were provided.
     * @throws IOException
     */
    private int readNumberOfBytes(BufferedInputStream input) throws IOException {

        int current;
        StringBuilder sb = new StringBuilder();

        // Get number of bytes
        do {
            // Read the input
            current = input.read();

            // Escape sequence met
            if (current == ' ' || !Character.isDigit(current)) {
                break;
            }

            sb.append((char) current);

        } while (current != -1);

        try {
            int numberOfBytes = Integer.parseInt(sb.toString().trim());
            System.out.printf("[%d]: Length of FOTO will be: %d\n", this.context.getClientNumber(), numberOfBytes);
            return numberOfBytes;
        } catch (NumberFormatException ex) {
            System.out.printf("[%d]: Could not parse FOTO byte length number: %s\n", this.context.getClientNumber(), sb.toString().trim());
            this.checksumStatus = ChecksumStatus.INVALID_SYNTAX;
            return -1;
        }
    }

    /**
     * Reads the photo and saves it to a file.
     *
     * @param input         Input stream.
     * @param numberOfBytes Number of bytes of the photo.
     * @return Calculated checksum of the photo.
     * @throws IOException
     */
    private long readPhoto(BufferedInputStream input, int numberOfBytes) throws IOException {

        int counter = 0;
        int current = 0;
        long calculatedChecksum = 0;

        // Prepare file
        PhotoFileHandler photoFileHandler = SingletonPhotoFileHandlerImpl.getInstance();
        photoFileHandler.createFile("photo.jpg");

        // Calculate checksum and save the photo to file
        while (current != -1 && counter < numberOfBytes) {
            // Read the input
            current = input.read();
            calculatedChecksum += current;
            counter++;
            photoFileHandler.appendChar((char) current);
        }
        photoFileHandler.close();
        return calculatedChecksum;
    }

    /**
     * Reads and asserts the checksum of the photo.
     *
     * @param input              Input stream.
     * @param calculatedChecksum Calculated checksum of the accepted photo.
     * @throws IOException
     */
    private void assertChecksum(BufferedInputStream input, long calculatedChecksum) throws IOException {
        int counter;
        int current = 0;
        long checksum;// Assert checksum
        counter = 3;
        StringBuilder checksumStringBuilder = new StringBuilder();

        while (current != -1 && counter > -1) {
            // Read the input
            current = input.read();
            checksumStringBuilder.append(Integer.toHexString(current));
            System.out.printf("[%d]: Checksum byte: %d = %c.\n", this.context.getClientNumber(), current, (char) current);
            counter--;
        }

        try {
            checksum = Integer.parseInt(checksumStringBuilder.toString().trim(), 16);
        } catch (NumberFormatException ex) {
            checksum = -1;
        }

        if (checksum == calculatedChecksum) {
            this.checksumStatus = ChecksumStatus.OK;
        } else {
            this.checksumStatus = ChecksumStatus.BAD;
        }

        System.out.printf("[%d]: Calculated checksum: %d.\n", this.context.getClientNumber(), calculatedChecksum);
        System.out.printf("[%d]: Sent checksum: %d.\n", this.context.getClientNumber(), checksum);
        System.out.printf("[%d]: Accepted checksum string: %s.\n", this.context.getClientNumber(), checksumStringBuilder.toString());
        System.out.printf("[%d]: Accepted FOTO message.\n", this.context.getClientNumber());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void printOutput(BufferedOutputStream output) throws IOException {
        if (this.checksumStatus == ChecksumStatus.BAD) {
            System.out.printf("[%d]: Sending 300 BAD CHECKSUM answer.\n", this.context.getClientNumber());
            output.write("300 BAD CHECKSUM\r\n".getBytes());
            output.flush();
        } else if (this.checksumStatus == ChecksumStatus.INVALID_SYNTAX) {
            System.out.printf("[%d]: Sending 501 SYNTAX ERROR answer.\n", this.context.getClientNumber());
            output.write("501 SYNTAX ERROR\r\n".getBytes());
            output.flush();
            this.context.disconnect();
        } else {
            output.write("202 OK\r\n".getBytes());
            output.flush();
            System.out.printf("[%d]: Sending 202 OK answer.\n", this.context.getClientNumber());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setNextState() throws IOException {
        System.out.printf("[%d]: Changing state to: AwaitingMessageState.\n", this.context.getClientNumber());
        this.context.setState(new AwaitingMessageState(this.context));
    }

    /**
     * Status of the incoming message.
     */
    enum ChecksumStatus {
        OK, INVALID_SYNTAX, BAD
    }
}
