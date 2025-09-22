package SmallDevices;

import Server.DeviceConstants;
import Server.IOPort;
import Server.IOPortServer;
import Server.Message;

import java.io.IOException;

public class Pump { ;
    private final IOPortServer statusPort = new IOPortServer(DeviceConstants.PUMP_PORT);

    public Pump() {
        System.out.println("Pump started up...");
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        Thread pumpClientThread = new Thread(() -> {
            Pump pump = new Pump();
            while (true) {
                pump.run();
            }
        });

        pumpClientThread.start();
    }

    /**
     * Turns pump on to push gas through to car
     */
    private void pumpOn() {
        System.out.println("Pumping gas...");
    }

    /**
     * Turns pump off
     */
    private void pumpOff() {
        System.out.println("Stopping pump.");
    }

    public void run() {
        Message msg = statusPort.get();
        if (msg == null || msg.getContent() == null) {
            return; // no message yet
        }

        System.out.println("Pump received: " + msg.getContent());

        switch (msg.getContent()) {
            case "on" -> pumpOn();
            case "off" -> pumpOff();
            default ->
                    System.out.println("Unknown command: " + msg.getContent());
        }
    }
}
