package Main;

import Server.DeviceConstants;
import Server.IOPort;
import Server.Message;

/**
 * Manages all sources of input from the customer.
 * This class abstracts the communication with the physical card reader and the on-screen buttons.
 * It provides blocking methods to wait for specific customer actions, like tapping a card or pressing a button.
 */
public class CustomerManager {

    private static final long RESPONSE_TIMEOUT_MS = 120_000; // 2 minutes for a user to do something
    private final IOPort cardReaderConnection;
    private final IOPort screenConnection;

    /**
     * Initializes the CustomerManager and connects to the card reader and screen devices.
     */
    public CustomerManager() {
        this.cardReaderConnection = new IOPort(DeviceConstants.CARD_READER_HOSTNAME, DeviceConstants.CARD_READER_PORT);
        this.screenConnection = new IOPort(DeviceConstants.SCREEN_HOSTNAME, DeviceConstants.SCREEN_PORT);
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
     * Waits for any button to be pressed on the touch screen.
     * This is a blocking call with a timeout.
     *
     * @param timeoutMillis The maximum time to wait for a button press in milliseconds.
     * @return A string representing the cell ID of the pressed button (e.g., "4"),
     * or {@code null} if the action timed out.
     */
    public String waitForButtonPress(long timeoutMillis) {
        System.out.println("Waiting for button press...");
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < timeoutMillis) {
            Message response = screenConnection.get();
            if (response != null) {
                String content = response.getContent();
                // Protocol is "b:<cell_id>//". We need to extract the cell_id.
                if (content.startsWith("b:") && content.endsWith("//")) {
                    return content.substring(2, content.length() - 2).trim();
                }
            }
            try {
                Thread.sleep(50); // Poll every 50ms
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        System.err.println("Timed out waiting for button press.");
        return null; // Timeout
    }

    /**
     * Closes connections to both the card reader and the screen.
     */
    public void close() {
        cardReaderConnection.close();
        screenConnection.close();
    }
}
