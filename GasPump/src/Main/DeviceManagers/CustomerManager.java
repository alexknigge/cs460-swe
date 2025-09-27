package Main.DeviceManagers;

import Server.DeviceConstants;
import Server.IOPort;
import Server.Message;

/**
 * Manages all customer input from the physical card reader.
 * This class abstracts the communication with the card reader device, providing
 * blocking methods to wait for a card tap and to notify the device of the
 * transaction's authorization status.
 */
public class CustomerManager {

    private final IOPort cardReaderConnection;

    /**
     * Initializes the CustomerManager and connects to the card reader device.
     */
    public CustomerManager() {
        this.cardReaderConnection = new IOPort(DeviceConstants.CARD_READER_HOSTNAME, DeviceConstants.CARD_READER_PORT);
    }

    /**
     * Waits for the customer to tap their card at the card reader.
     * This is a blocking call with a timeout.
     *
     * @param timeoutMillis The maximum time to wait for a card tap in milliseconds.
     * @return A string containing the 16-digit card number, or {@code null} if the action timed out or an error occurred.
     */
    public String waitForCardTap(long timeoutMillis) {
        System.out.println("Waiting for card tap...");
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < timeoutMillis) {
            Message response = cardReaderConnection.get();
            if (response != null) {
                String content = response.getContent().replace("//", "").trim();
                // The card reader spec says it can send "error//".
                if ("error".equalsIgnoreCase(content)) {
                    System.err.println("Card reader reported an error.");
                    return null;
                }
                return content;
            }
            try {
                Thread.sleep(50); // Poll every 50ms
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        System.err.println("Timed out waiting for card tap.");
        return null; // Timeout
    }

    /**
     * Notifies the physical card reader of the transaction status.
     *
     * @param isApproved {@code true} to send an "approved" message, {@code false} for "declined".
     */
    public void notifyCardReader(boolean isApproved) {
        String message = isApproved ? "approved//" : "declined//";
        cardReaderConnection.send(new Message(message));
    }

    /**
     * Closes the connection to the card reader.
     */
    public void close() {
        cardReaderConnection.close();
    }
}
