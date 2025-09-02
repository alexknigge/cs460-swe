package Devices;

import Communicator.CommPort;
import Server.Message;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

/**
 * Manages all incoming and outgoing communication for the gas pump UI
 * using the CommPort abstraction (which wraps ioPort).
 */
public class ScreenCommunicationManager extends CommPort {

    // --- Socket Configuration ---
    private static final String HOST = "localhost";
    private static final int PORT = 5000;
    private static final int RECONNECT_DELAY_SECONDS = 3;

    private final MessageListener listener;
    private CommPort commPort;
    private Socket socket;

    public ScreenCommunicationManager(MessageListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("MessageListener cannot be null.");
        }
        this.listener = listener;
    }

    /**
     * Starts listening for incoming messages on a new daemon thread.
     * Will attempt to reconnect on failure.
     */
    public void startListening() {
        Thread commThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    System.out.println("[DEBUG] Attempting to connect to server at " + HOST + ":" + PORT + "...");
                    socket = new Socket(HOST, PORT);

                    System.out.println("[DEBUG] Connection established successfully.");

                    while (true) {
                        Message msg = getMessage();
                        if (msg != null) {
                            System.out.println("[DEBUG] Received message: " + msg);
                            listener.onMessageReceived(msg.toString());
                        }
                    }
                } catch (Exception e) {
                    System.err.println("[DEBUG] Connection error: " + e.getMessage());
                    listener.onCommunicationError(e);
                } finally {
                    System.err.println("[DEBUG] Disconnected. Will attempt to reconnect in " +
                            RECONNECT_DELAY_SECONDS + " seconds.");
                    closeConnection();
                    try {
                        TimeUnit.SECONDS.sleep(RECONNECT_DELAY_SECONDS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        });
        commThread.setDaemon(true);
        commThread.start();
    }

    /**
     * Sends a message to the output stream if the connection is active.
     */
    public synchronized void sendMessage(String message) {
        if (commPort != null) {
            System.out.println("[DEBUG] Sending message: " + message.trim());
            commPort.sendMessage(new Message(message + "//"));
        } else {
            System.err.println("[DEBUG] Cannot send message, not connected.");
        }
    }

    /**
     * Closes the socket gracefully.
     */
    private void closeConnection() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (Exception e) {
            System.err.println("[DEBUG] Error while closing socket: " + e.getMessage());
        }
        socket = null;
        commPort = null;
    }

    /**
     * An interface for listeners who want to receive messages from the communication source.
     */
    public interface MessageListener {
        void onMessageReceived(String message);

        void onCommunicationError(Exception e);
    }
}