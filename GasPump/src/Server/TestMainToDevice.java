package Server;

public class TestMainToDevice {

    public static void main(String[] args) {
        System.out.println("--- MainToDeviceTest Starting ---");

        // Thread for the peripheral device (e.g., a pump), which will act as the SERVER.
        Thread deviceServerThread = new Thread(() -> {
            // Instantiating with the pump's port, which maps to "pump", so it becomes a server.
            IOPort devicePort = new IOPort("cardReaderToMain");
            if (devicePort.isClosed()) {
                System.err.println("Device Server Thread: Failed to initialize port. Exiting.");
                return;
            }

            try {
                System.out.println("Device Server Thread: Waiting to receive a message from the main host...");
                // Blocking call to wait for a message
                Message receivedMsg = devicePort.read();

                if (receivedMsg != null) {
                    System.out.println("Device Server Thread: Received message: \"" + receivedMsg.getContent() + "\"");
                    // Send a reply back to the main host
                    Message replyMsg = new Message("Hello Main Host, message received!");
                    System.out.println("Device Server Thread: Sending reply...");
                    devicePort.send(replyMsg);
                    // Give a moment for the message to be sent before closing
                    Thread.sleep(500);
                } else {
                    System.err.println("Device Server Thread: Read operation was interrupted.");
                }

            } catch (InterruptedException e) {
                System.err.println("Device Server Thread: Was interrupted during sleep.");
                Thread.currentThread().interrupt();
            } finally {
                System.out.println("Device Server Thread: Closing port.");
                devicePort.close();
            }
        });

        // Thread for the main host, which will act as the CLIENT.
        Thread mainHostClientThread = new Thread(() -> {
            // Instantiating with port 20000, which maps to "main host", so it becomes a client.
            IOPort mainPort = new IOPort("MainToCardReader");
            if (mainPort.isClosed()) {
                System.err.println("Main Host Client Thread: Failed to initialize port. Exiting.");
                return;
            }

            try {
                // Send an initial message to the device server
                Message initialMsg = new Message("Hello Pump, this is the Main Host.");
                System.out.println("Main Host Client Thread: Sending initial message...");
                mainPort.send(initialMsg);

                System.out.println("Main Host Client Thread: Waiting for a reply from the device...");
                // Blocking call to wait for the reply
                Message replyMsg = mainPort.read();

                if (replyMsg != null) {
                    System.out.println("Main Host Client Thread: Received reply: \"" + replyMsg.getContent() + "\"");
                } else {
                    System.err.println("Main Host Client Thread: Read operation was interrupted.");
                }

            } finally {
                System.out.println("Main Host Client Thread: Closing port.");
                mainPort.close();
            }
        });

        // --- Start the simulation ---
        deviceServerThread.start();

        try {
            // Give the server thread a moment to start up and listen before the client tries to connect.
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        mainHostClientThread.start();

        // Wait for both threads to complete their execution
        try {
            deviceServerThread.join();
            mainHostClientThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("--- MainToDeviceTest Finished ---");
    }
}

