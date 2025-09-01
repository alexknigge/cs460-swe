package Devices;
/* Connected to fuel tank, turns on and off based on screen messages */

import Status.StatusPort;

import java.io.IOException;
import java.net.Socket;

public class Pump {
    private final StatusPort statusPort;
    public Pump (Socket socket) throws IOException {
        this.statusPort = new StatusPort(socket);
        System.out.println("Pump started up...");
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
        System.out.println("Stopping pump...");
    }

    public void run() throws IOException {
        while (true) {
            String message = statusPort.readStatus();  // blocking or non-blocking depending on implementation
            if (message == null) continue;

            switch (message) {
                case "PUMP_ON":
                    pumpOn();
                    break;
                case "PUMP_OFF":
                    pumpOff();
                    break;
                default:
                    System.out.println("Unknown message: " + message);
            }
        }
    }

    public static void main(String[] args) throws IOException {
        Pump pump = new Pump(new Socket("localhost", 5000));
        pump.run();
    }
}
