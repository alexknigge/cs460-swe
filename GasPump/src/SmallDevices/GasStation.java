package SmallDevices;

import Server.DeviceConstants;
import Server.IOPortServer;
import Server.Message;

/**
 * A simulation of the central Gas Station server.
 * This server listens for connections from gas pump controllers and responds to commands.
 * It provides fuel prices and logs sales data.
 */
public class GasStation {

    // Hardcoded price list for the simulation.
    // Format: "Name,Octane,Price;..."
    private static final String PRICE_LIST_RESPONSE = "Regular,87,4.59;Premium,91,4.99;Super,93,5.19";

    public static void main(String[] args) {
        // The server listens on a dedicated port.
        IOPortServer serverPort = new IOPortServer(DeviceConstants.GAS_STATION_PORT);
        System.out.println("[Gas Station Server] Now running and listening for a connection...");

        while (true) {
            // Check for incoming messages from the connected pump controller.
            Message request = serverPort.get();
            if (request != null) {
                String command = request.getContent();
                System.out.println("[Gas Station Server] Received command: " + command);

                if ("get-prices".equals(command)) {
                    // If the pump asks for prices, send the hardcoded list.
                    serverPort.send(new Message(PRICE_LIST_RESPONSE));
                    System.out.println("[Gas Station Server] Sent price list to pump.");
                } else if (command.startsWith("log-sale:")) {
                    // If the pump sends a sale log, just print it to the console.
                    // In a real system, this would write to a database or file.
                    System.out.println("[Gas Station Server] Logged transaction: " + command.substring(9));
                }
            }

            try {
                // Pause briefly to prevent the loop from running at max speed.
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        System.out.println("[Gas Station Server] Shutting down.");
        serverPort.close();
    }
}
