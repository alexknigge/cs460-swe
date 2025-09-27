package Main;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A data record representing a real-time update from the flow meter during fueling.
 * It encapsulates the gallons dispensed and the total cost at a specific moment.
 *
 * @param gallons   The total volume of fuel dispensed so far.
 * @param totalCost The total cost of the transaction so far.
 */
public record FuelingUpdate(double gallons, double totalCost) {

    // Regex to find "0.000 gal" and "$0.00" within the flow meter's screen protocol message.
    private static final Pattern GALLONS_PATTERN = Pattern.compile("(\\d+\\.\\d+)\\s+gal");
    private static final Pattern COST_PATTERN = Pattern.compile("\\$(\\d+\\.\\d+)");

    /**
     * Parses a raw message string from the FlowMeter device to extract fueling data.
     * The FlowMeter sends screen protocol messages like: "t:3/s:3/st:2/c:0/1.234 gal;t:5/s:3/st:2/c:0/$5.67;"
     * This method uses regex to find the relevant numbers within that string.
     *
     * @param rawMessage The message from the FlowMeter.
     * @return A new {@link FuelingUpdate} object, or {@code null} if parsing fails.
     */
    public static FuelingUpdate parseFrom(String rawMessage) {
        if (rawMessage == null) return null;

        try {
            Matcher gallonsMatcher = GALLONS_PATTERN.matcher(rawMessage);
            Matcher costMatcher = COST_PATTERN.matcher(rawMessage);

            if (gallonsMatcher.find() && costMatcher.find()) {
                double gallons = Double.parseDouble(gallonsMatcher.group(1));
                double totalCost = Double.parseDouble(costMatcher.group(1));
                return new FuelingUpdate(gallons, totalCost);
            }
        } catch (NumberFormatException e) {
            System.err.println("Could not parse fueling update from message: " + rawMessage);
            return null;
        }
        return null; // Return null if patterns are not found
    }
}
