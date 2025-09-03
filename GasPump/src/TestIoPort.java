// class to test the IoPort and Messages

import Server.Message;
import Server.IOPort;

public class TestIoPort {
    public static void main(String[] args) throws Exception {
        IOPort port = new IOPort(12345);
        port.run();

        // Give time for connection
        Thread.sleep(1000);

        // Send a few messages
        port.sendMessage(new Message("Hello Server!"));
        port.sendMessage(new Message("Second message"));

        // Wait a bit to receive echoes
        Thread.sleep(2000);

        // Retrieve received messages
        while (port.hasMessage()) {
            Message msg = port.getMessage();
            System.out.println("Client received: " + msg.getContent());
        }
    }
}

