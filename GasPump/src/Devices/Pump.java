package Devices;
/* Connected to fuel tank, turns on and off based on screen messages */

import Server.Message;
import Status.StatusPort;

import java.io.IOException;

public class Pump extends StatusPort {
    public Pump () throws IOException {
        System.out.println("Pump started up...");
    }

    public void handleMessage(Message msg) {
        if (msg == null || msg.getContent() == null) return;

        switch (msg.getContent()) {
            case "on" -> pumpOn();
            case "off" -> pumpOff();
            default -> System.out.println("Unknown command: " + msg.getContent());
        }
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
}
