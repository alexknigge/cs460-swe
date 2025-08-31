package Devices;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

/**
 * Manages all incoming and outgoing communication for the gas pump UI via a socket connection.
 * This class abstracts the communication source, making it easy to switch to other methods in the future.
 */
public class ScreenCommunicationManager {

    // --- Socket Configuration ---
    private static final String HOST = "localhost";
    private static final int PORT = 1234;
    private static final int RECONNECT_DELAY_SECONDS = 3;

    private final MessageListener listener;
    private PrintWriter out;
    private Socket clientSocket;

    public ScreenCommunicationManager(MessageListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("MessageListener cannot be null.");
        }
        this.listener = listener;
    }

    /**
     * Starts listening for incoming messages on a new daemon thread.
     * This method will continuously attempt to connect to the server.
     */
    public void startListening() {
        Thread commThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    System.out.println("[DEBUG] Attempting to connect to server at " + HOST + ":" + PORT + "...");
                    clientSocket = new Socket(HOST, PORT);
                    out = new PrintWriter(clientSocket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                    System.out.println("[DEBUG] Connection established successfully.");

                    StringBuilder messageBuilder = new StringBuilder();
                    int c;
                    while ((c = in.read()) != -1) {
                        messageBuilder.append((char) c);
                        if (messageBuilder.toString().endsWith("//")) {
                            String message = messageBuilder.toString();
                            System.out.println("[DEBUG] Received message: " + message.trim());
                            listener.onMessageReceived(message);
                            messageBuilder.setLength(0); // Reset for next message
                        }
                    }
                } catch (Exception e) {
                    System.err.println("[DEBUG] Connection error: " + e.getMessage());
                    listener.onCommunicationError(e);
                } finally {
                    System.err.println("[DEBUG] Disconnected from server. Will attempt to reconnect in " + RECONNECT_DELAY_SECONDS + " seconds.");
                    closeConnection();
                    try {
                        TimeUnit.SECONDS.sleep(RECONNECT_DELAY_SECONDS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt(); // Preserve interrupt status
                    }
                }
            }
        });
        commThread.setDaemon(true);
        commThread.start();
    }

    /**
     * Sends a message to the output stream if the connection is active.
     *
     * @param message The message to send, without the terminator.
     */
    public synchronized void sendMessage(String message) {
        if (out != null) {
            String fullMessage = message + "//";
            System.out.println("[DEBUG] Sending message: " + fullMessage.trim());
            out.print(fullMessage);
            out.flush();
        } else {
            System.err.println("[DEBUG] Cannot send message, not connected to server.");
        }
    }

    /**
     * Closes the socket and streams gracefully.
     */
    private void closeConnection() {
        try {
            if (out != null) out.close();
            if (clientSocket != null && !clientSocket.isClosed())
                clientSocket.close();
        } catch (Exception e) {
            System.err.println("[DEBUG] Error while closing connection: " + e.getMessage());
        }
        out = null;
        clientSocket = null;
    }

    /**
     * An interface for listeners who want to receive messages from the communication source.
     */
    public interface MessageListener {
        void onMessageReceived(String message);

        void onCommunicationError(Exception e);
    }
}

