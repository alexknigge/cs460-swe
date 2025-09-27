package Main;

import Server.DeviceConstants;
import Server.IOPort;
import Server.Message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manages all communication with the main gas station server.
 * This class is responsible for fetching up-to-date fuel prices and logging
 * completed transactions for the station's records.
 */
public class GasStationManager {

    private static final long RESPONSE_TIMEOUT_MS = 5000; // 5 seconds
    private final IOPort stationConnection;

    /**
     * Initializes a new GasStationManager and establishes a connection to the station server.
     */
    public GasStationManager() {
        this.stationConnection = new IOPort(DeviceConstants.GAS_STATION_HOSTNAME, DeviceConstants.GAS_STATION_PORT); // Using a placeholder port
    }

    /**
     * Fetches the list of available fuel grades and their current prices from the gas station server.
     * This is a blocking call that waits for the server to respond or for the request to time out.
     *
     * @return A list of {@link FuelGrade} objects. Returns an empty list if the request fails or times out.
     */
    public List<FuelGrade> getAvailableFuelGrades() {
        System.out.println("Requesting fuel prices from station server...");
        stationConnection.send(new Message("get-prices"));

        Message response = waitForResponse(RESPONSE_TIMEOUT_MS);
        if (response == null) {
            System.err.println("Error: Timed out waiting for price list from station server.");
            return Collections.emptyList();
        }

        return parsePriceList(response.getContent());
    }

    /**
     * Logs the details of a completed transaction to the gas station server.
     * This is a non-blocking, "fire-and-forget" operation.
     *
     * @param cardNumber The customer's credit card number.
     * @param grade      The {@link FuelGrade} that was dispensed.
     * @param gallons    The total volume of fuel dispensed.
     * @param totalCost  The final cost of the transaction.
     */
    public void logTransaction(String cardNumber, FuelGrade grade, double gallons, double totalCost) {
        String logMessage = String.format("log-sale:card=%s,grade=%s,gallons=%.3f,cost=%.2f",
                cardNumber, grade.name(), gallons, totalCost);
        stationConnection.send(new Message(logMessage));
        System.out.println("Transaction logged to station server.");
    }

    /**
     * Parses the raw string response from the server into a list of FuelGrade objects.
     * Expected format: "Name1,Octane1,Price1;Name2,Octane2,Price2;..."
     * Example: "Regular,87,4.59;Premium,91,4.99;Super,93,5.19"
     *
     * @param priceData The raw string data from the server.
     * @return A list of parsed {@link FuelGrade} objects.
     */
    private List<FuelGrade> parsePriceList(String priceData) {
        List<FuelGrade> grades = new ArrayList<>();
        if (priceData == null || priceData.trim().isEmpty()) {
            return grades;
        }

        String[] gradeEntries = priceData.split(";");
        for (String entry : gradeEntries) {
            String[] parts = entry.split(",");
            if (parts.length == 3) {
                try {
                    String name = parts[0].trim();
                    int octane = Integer.parseInt(parts[1].trim());
                    double price = Double.parseDouble(parts[2].trim());
                    grades.add(new FuelGrade(name, price, octane));
                } catch (NumberFormatException e) {
                    System.err.println("Error parsing price data entry: '" + entry + "' - " + e.getMessage());
                }
            }
        }
        return grades;
    }

    /**
     * Waits for a message to arrive from the IOPort's incoming queue.
     * This method polls the queue and includes a timeout to prevent indefinite blocking.
     *
     * @param timeoutMillis The maximum time to wait for a response, in milliseconds.
     * @return The received {@link Message}, or {@code null} if the timeout was reached.
     */
    private Message waitForResponse(long timeoutMillis) {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < timeoutMillis) {
            Message response = stationConnection.get();
            if (response != null) {
                return response;
            }
            try {
                Thread.sleep(20); // Poll every 20ms
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        return null; // Timeout
    }

    /**
     * Closes the connection to the gas station server.
     */
    public void close() {
        stationConnection.close();
    }
}
