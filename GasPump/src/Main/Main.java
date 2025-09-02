package Main;

import Communicator.CommPort;
import Devices.GasPumpUI;
import Devices.Pump;
import Devices.ScreenCommunicationManager;
import Server.Message;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

// Main.Main
public class Main extends CommPort {
    public static void main(String[] args) throws InterruptedException, IOException {
        ServerSocket serverSocket = new ServerSocket(5000);
        System.out.println("Starting up pump...");
        Socket pumpSocket = serverSocket.accept();
        Pump pump = new Pump(pumpSocket);
        System.out.println("Pump started.");

        /*
        Socket screenSocket = serverSocket.accept();
        GasPumpUI screen = new GasPumpUI();
        */

        pump.sendMessage(new Message("on"));
        System.out.println("message sent");

        while (true) {
            System.out.println(" ");
        }

    }
}
