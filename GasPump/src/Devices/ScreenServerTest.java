package Devices;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * A simple command-line server to test the GasPumpUI application.
 * It listens on a specified port, accepts one client, and then relays messages
 * between the server's console and the client application.
 */
public class ScreenServerTest {

    // Must match the configuration in ScreenCommunicationManager
    private static final int PORT = 1234;

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Test Server started. Waiting for a client to connect on port " + PORT + "...");

            while (true) { // Loop to accept new clients after a disconnect
                try (Socket clientSocket = serverSocket.accept()) {
                    System.out.println("Client connected: " + clientSocket.getInetAddress().getHostAddress());
                    System.out.println("Type a screen message and press Enter to send it to the UI.");
                    System.out.println("Messages from the UI will be displayed here.\n");

                    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                    // Thread to listen for messages FROM the UI client
                    Thread clientListener = new Thread(() -> {
                        try {
                            StringBuilder messageBuilder = new StringBuilder();
                            int c;
                            while ((c = in.read()) != -1) {
                                messageBuilder.append((char) c);
                                if (messageBuilder.toString().endsWith("//")) {
                                    System.out.println("Received from UI: " + messageBuilder.toString().trim());
                                    messageBuilder.setLength(0);
                                }
                            }
                        } catch (Exception e) {
                            // This will happen when the client disconnects.
                        } finally {
                            System.out.println("Client appears to have disconnected.");
                        }
                    });
                    clientListener.start();

                    // Main thread will read from console and send TO the UI client
                    BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
                    String userInput;
                    while ((userInput = stdIn.readLine()) != null && clientSocket.isConnected()) {
                        System.out.println("Sending to UI: " + userInput.trim());
                        out.print(userInput); // The protocol is part of the string
                        out.flush();
                    }

                    // Wait for the listener thread to finish if the console input ends
                    clientListener.join();

                } catch (Exception e) {
                    System.err.println("Error during client session: " + e.getMessage());
                }
                System.out.println("\nWaiting for a new client to connect...");
            }
        } catch (Exception e) {
            System.err.println("Could not start server on port " + PORT);
            e.printStackTrace();
        }
    }
}
