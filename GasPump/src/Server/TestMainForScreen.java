package Server;

/**
 * A simulation of the main program that controls the GasPumpUI.
 * This program acts as a SERVER, waiting for the GasPumpUI client to connect.
 * Once connected, it sends an initial screen layout message and then continuously
 * listens for and prints any input received from the UI.
 */
public class TestMainForScreen {

    public static void main(String[] args) {
        System.out.println("--- Main Program Simulation Starting ---");

        // The IOPort for the main program will act as a SERVER.
        // It listens on the PUMP_SERVER_PORT for the GasPumpUI client to connect.
        // The IOPort constructor is blocking and will wait here until a client connects.
        IOPort mainServerPort = new IOPort("MainToScreen");

        // Check if the connection failed.
        if (mainServerPort.isClosed()) {
            System.err.println("Failed to initialize server port or the client did not connect. Exiting.");
            return;
        }

        System.out.println("✅ GasPumpUI client has connected successfully.");

        try {
            // Define the message to be sent to the GasPumpUI.
            // Note: The "//" at the end is required because the ScreenParser on the client-side
            // is designed to trim the last two characters from the incoming message string.
            String initialScreenMessage = "t:01/s:3/f:2/c:0/Welcome!;" +
                    "t:2/s:2/f:1/c:0/Select Grade;" +
                    "t:4/s:2/f:1/c:3/87 Octane;$4.49;b:4/m;" +
                    "t:5/s:2/f:1/c:4/91 Octane;$4.99;b:5/m;" +
                    "t:6/s:2/f:1/c:1/93//";

            Message screenUpdate = new Message(initialScreenMessage);

            System.out.println("🚀 Sending initial screen setup message...");
            mainServerPort.send(screenUpdate);
            System.out.println("Message sent. Now listening for input from the UI...");

            // Loop indefinitely to read and print messages from the GasPumpUI.
            while (!mainServerPort.isClosed()) {
                // read() is a blocking call that waits for a message to arrive.
                Message receivedMsg = mainServerPort.get();

                if (receivedMsg != null) {
                    System.out.println("RECEIVED from UI: \"" + receivedMsg.getContent() + "\"");
                }
            }
        } finally {
            System.out.println("Closing server port.");
            mainServerPort.close();
            System.out.println("--- Main Program Simulation Finished ---");
        }
    }
}
