package Devices;
/* Connected to fuel tank, turns on and off based on screen messages */

import Server.Message;
import Status.StatusPort;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;

public class Pump extends StatusPort {
    public Pump (Socket socket) throws IOException {
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
        System.out.println("Stopping pump.");
    }

    public void run() throws IOException {
        while (true) {
            Message msg = getMessage();
            if (msg == null || msg.getContent() == null) {
                continue; // no message yet
            }

            System.out.println("Pump received: " + msg.getContent());

            switch (msg.getContent()) {
                case "on" -> pumpOn();
                case "off" -> pumpOff();
                default -> System.out.println("Unknown command: " + msg.getContent());
            }
        }
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        Pump pump = new Pump(new Socket("localhost", 5000));
        pump.run();
    }
}
