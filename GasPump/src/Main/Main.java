package Main;

import SmallDevices.CardReader;
import Server.Message;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

// Main.Main
public class Main {
    public static void main(String[] args) throws InterruptedException, IOException {
        ServerSocket serverSocket = new ServerSocket(5000);
        System.out.println("Starting up pump...");
        Socket pumpSocket = serverSocket.accept();
        CardReader.Pump pump = new CardReader.Pump(pumpSocket);
        System.out.println("Pump started.");

        /*
        Socket screenSocket = serverSocket.accept();
        GasPumpUI screen = new GasPumpUI();
        */

        pump.send(new Message("on"));
        System.out.println("message sent");

        while (true) {
            System.out.println(" ");
        }

    }
}
