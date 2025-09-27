package Main;

import Server.DeviceConstants;
import Server.IOPort;
import Server.Message;

import java.text.DecimalFormat;
import java.util.List;

/**
 * Manages all display output to the customer-facing screen.
 * This class is responsible for constructing and sending well-formed screen protocol
 * messages to the GasPumpUI application. It provides methods for showing
 * pre-defined screen layouts (e.g., welcome, select grade) as well as dynamic
 * content like the real-time fueling status.
 */
public class ScreenManager {
    private static final String CMD_TERMINATOR = "//";
    // --- Formatters for consistent display ---
    private static final DecimalFormat GALS_FORMAT = new DecimalFormat("0.000");
    private static final DecimalFormat PRICE_FORMAT = new DecimalFormat("$0.00");
    private final IOPort screenConnection;

    /**
     * Initializes a new ScreenManager and establishes a connection to the screen device.
     */
    public ScreenManager() {
        this.screenConnection = new IOPort(DeviceConstants.SCREEN_HOSTNAME, DeviceConstants.SCREEN_PORT);
    }

    /**
     * Displays the initial welcome screen to the customer.
     */
    public void showWelcomeScreen() {
        String message = "t:01/s:3/f:2/c:0/Welcome!;" +
                "t:45/s:2/f:1/c:0/Please tap your card to begin.;" + CMD_TERMINATOR;
        send(message);
    }

    /**
     * Displays a message asking the customer to wait while their card is being authorized.
     */
    public void showAuthorizingScreen() {
        String message = "t:45/s:2/f:3/c:0/Authorizing, please wait...;" + CMD_TERMINATOR;
        send(message);
    }

    /**
     * Displays the fuel grade selection screen, dynamically populating it from a list of available grades.
     *
     * @param availableGrades A list of {@link FuelGrade} objects to be displayed as options.
     */
    public void showGradeSelectionScreen(List<FuelGrade> availableGrades) {
        StringBuilder sb = new StringBuilder();
        sb.append("t:01/s:3/f:2/c:0/Select Fuel Grade;");

        // Dynamically create a text field and a button for each available grade
        for (int i = 0; i < availableGrades.size(); i++) {
            FuelGrade grade = availableGrades.get(i);
            int cell = 2 + i; // Start placing grades from cell 2
            sb.append(String.format("t:%d/s:2/f:1/c:3/%s (%s);", cell, grade.name(), PRICE_FORMAT.format(grade.pricePerGallon())));
            sb.append(String.format("b:%d/m;", cell));
        }
        sb.append("t:8/s:1/f:1/c:0/Cancel;b:8/x;"); // Add a cancel button
        sb.append(CMD_TERMINATOR);
        send(sb.toString());
    }

    /**
     * Displays the main fueling screen, showing the dynamically updating volume and total cost.
     *
     * @param gradeName The name of the selected fuel grade (e.g., "Regular 87").
     * @param gallons   The current volume of fuel dispensed, in gallons.
     * @param total     The current total cost of the transaction.
     */
    public void showPumpingScreen(String gradeName, double gallons, double total) {
        String message = "t:0/s:2/f:1/c:0/Fueling: " + gradeName + ";" +
                "t:2/s:2/f:1/c:0/Gallons Dispensed:;" +
                "t:3/s:3/f:2/c:0/" + GALS_FORMAT.format(gallons) + " gal;" +
                "t:4/s:2/f:1/c:0/Total Cost:;" +
                "t:5/s:3/f:2/c:0/" + PRICE_FORMAT.format(total) + ";" +
                "t:8/s:1/f:1/c:2/Stop;b:8/x;" + CMD_TERMINATOR;
        send(message);
    }

    /**
     * Displays a "Thank You" screen at the end of a successful transaction.
     *
     * @param gallons The final volume of fuel dispensed.
     * @param total   The final total cost of the transaction.
     */
    public void showThankYouScreen(double gallons, double total) {
        String message = "t:01/s:3/f:2/c:1/Thank You!;" +
                "t:3/s:2/f:1/c:0/Total Gallons: " + GALS_FORMAT.format(gallons) + ";" +
                "t:4/s:2/f:1/c:0/Total Charge: " + PRICE_FORMAT.format(total) + ";" +
                "t:67/s:2/f:1/c:0/Your receipt will be emailed to you.;" + CMD_TERMINATOR;
        send(message);
    }

    /**
     * Displays a generic message to the user, occupying the center of the screen.
     *
     * @param messageText The text to display.
     */
    public void showMessage(String messageText) {
        String message = "t:45/s:2/f:2/c:0/" + messageText + ";" + CMD_TERMINATOR;
        send(message);
    }

    /**
     * Sends a raw, pre-formatted message string to the screen.
     * Ensures the message is correctly terminated before sending.
     *
     * @param rawMessage The complete message string to send.
     */
    private void send(String rawMessage) {
        // Ensure the message ends with the required terminator, but not duplicates.
        String finalMessage = rawMessage.endsWith(CMD_TERMINATOR) ? rawMessage : rawMessage + CMD_TERMINATOR;
        screenConnection.send(new Message(finalMessage));
    }

    /**
     * Closes the connection to the screen device.
     */
    public void close() {
        screenConnection.close();
    }
}
