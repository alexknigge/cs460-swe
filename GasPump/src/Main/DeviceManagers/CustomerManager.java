package Main.DeviceManagers;

import Server.DeviceConstants;
import Server.IOPort;
import Server.Message;

import java.text.DecimalFormat;
import java.util.List;

/**
 * Manages all customer-facing interfaces, including the screen and the card reader.
 * This class consolidates all logic for sending display information to the screen,
 * receiving button presses from the screen, and handling card reader interactions.
 */
public class CustomerManager {

    private static final String CMD_TERMINATOR = "//";
    private static final DecimalFormat GALS_FORMAT = new DecimalFormat("0.000");
    private static final DecimalFormat PRICE_FORMAT = new DecimalFormat("$0.00");

    private final IOPort cardReaderConnection;
    private final IOPort screenConnection;

    /**
     * Initializes the CustomerManager and connects to both the card reader and screen devices.
     */
    public CustomerManager() {
        this.cardReaderConnection = new IOPort(DeviceConstants.CARD_READER_HOSTNAME, DeviceConstants.CARD_READER_PORT);
        this.screenConnection = new IOPort(DeviceConstants.SCREEN_HOSTNAME, DeviceConstants.SCREEN_PORT);
    }

    // --- Card Reader Methods ---

    /**
     * Waits for the customer to tap their card at the card reader.
     *
     * @param timeoutMillis The maximum time to wait.
     * @return The 16-digit card number as a string, or null on timeout/error.
     */
    public String waitForCardTap(long timeoutMillis) {
        System.out.println("Waiting for card tap...");
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < timeoutMillis) {
            Message response = cardReaderConnection.get();
            if (response != null) {
                String content = response.getContent().replace("//", "").trim();
                if ("error".equalsIgnoreCase(content)) {
                    System.err.println("Card reader reported an error.");
                    return null;
                }
                return content;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        System.err.println("Timed out waiting for card tap.");
        return null;
    }

    /**
     * Notifies the physical card reader of the transaction status.
     *
     * @param isApproved True for "approved", false for "declined".
     */
    public void notifyCardReader(boolean isApproved) {
        String message = isApproved ? "approved//" : "declined//";
        cardReaderConnection.send(new Message(message));
    }

    // --- Screen Methods ---

    /**
     * Waits for any button to be pressed on the touch screen.
     *
     * @param timeoutMillis The maximum time to wait.
     * @return The cell ID of the pressed button (e.g., "4"), or null on timeout.
     */
    public String waitForButtonPress(long timeoutMillis) {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < timeoutMillis) {
            Message response = screenConnection.get();
            if (response != null) {
                String content = response.getContent();
                if (content.startsWith("b:") && content.endsWith("//")) {
                    return content.substring(2, content.length() - 2).trim();
                }
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        return null;
    }

    public void showWelcomeScreen() {
        String message = "t:01/s:3/f:2/c:0/Welcome!;" +
                "t:45/s:2/f:1/c:0/Please tap your card to begin.;" + CMD_TERMINATOR;
        sendToScreen(message);
    }

    public void showAuthorizingScreen() {
        String message = "t:45/s:2/f:3/c:0/Authorizing, please wait...;" + CMD_TERMINATOR;
        sendToScreen(message);
    }

    public void showGradeSelectionScreen(List<FuelGrade> availableGrades) {
        StringBuilder sb = new StringBuilder();
        sb.append("t:01/s:3/f:2/c:0/Select Fuel Grade;");
        for (int i = 0; i < availableGrades.size(); i++) {
            FuelGrade grade = availableGrades.get(i);
            int cell = 2 + i;
            sb.append(String.format("t:%d/s:2/f:1/c:3/%s (%s);", cell, grade.name(), PRICE_FORMAT.format(grade.pricePerGallon())));
            sb.append(String.format("b:%d/m;", cell));
        }
        sb.append("t:8/s:1/f:1/c:0/Cancel;b:8/x;");
        sb.append(CMD_TERMINATOR);
        sendToScreen(sb.toString());
    }

    public void showPumpingScreen(String gradeName, double gallons, double total) {
        String message = "t:0/s:2/f:1/c:0/Fueling: " + gradeName + ";" +
                "t:2/s:2/f:1/c:0/Gallons Dispensed:;" +
                "t:3/s:3/f:2/c:0/" + GALS_FORMAT.format(gallons) + " gal;" +
                "t:4/s:2/f:1/c:0/Total Cost:;" +
                "t:5/s:3/f:2/c:0/" + PRICE_FORMAT.format(total) + ";" +
                "t:8/s:1/f:1/c:2/Stop;b:8/x;" + CMD_TERMINATOR;
        sendToScreen(message);
    }

    public void showThankYouScreen(double gallons, double total) {
        String message = "t:01/s:3/f:2/c:1/Thank You!;" +
                "t:3/s:2/f:1/c:0/Total Gallons: " + GALS_FORMAT.format(gallons) + ";" +
                "t:4/s:2/f:1/c:0/Total Charge: " + PRICE_FORMAT.format(total) + ";" +
                "t:67/s:2/f:1/c:0/Your receipt will be emailed to you.;" + CMD_TERMINATOR;
        sendToScreen(message);
    }

    public void showMessage(String messageText) {
        String message = "t:45/s:2/f:2/c:0/" + messageText + ";" + CMD_TERMINATOR;
        sendToScreen(message);
    }

    private void sendToScreen(String rawMessage) {
        String finalMessage = rawMessage.endsWith(CMD_TERMINATOR) ? rawMessage : rawMessage + CMD_TERMINATOR;
        screenConnection.send(new Message(finalMessage));
    }

    /**
     * Closes connections to both the card reader and the screen.
     */
    public void close() {
        if (cardReaderConnection != null) cardReaderConnection.close();
        if (screenConnection != null) screenConnection.close();
    }
}

