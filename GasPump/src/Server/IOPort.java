package Server;

/* This class represents an object that helps build the specializations. */
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class IOPort {

    private static String HOST = "localhost";
    private final int CONNECTOR;
    private final int RECONNECT_DELAY_SECONDS = 5;

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private BlockingQueue<Message> inQueue = new LinkedBlockingQueue<>();
    private BlockingQueue<Message> outQueue = new LinkedBlockingQueue<>();

    private boolean running = true;
    private MessageListener messageListener;


    public IOPort(int CONNECTOR) {
        this.CONNECTOR = CONNECTOR;
    }

    public void setListener(MessageListener listener){
        this.messageListener = listener;
    }


    // Put a message into the buffer
    public void sendMessage(Message msg) {
        outQueue.add(msg);
    }

    // Take a message (remove from buffer)
    public Message getMessage() {
        return inQueue.poll(); // returns null if empty
    }

    // Peek a message (do not remove from buffer)
    public Message readMessage() throws InterruptedException {
        return inQueue.take(); // waits until message is available
    }

    // Check if there are messages
    public boolean hasMessage() {
        return !inQueue.isEmpty();
    }

    public void run() throws IOException {
        Thread commThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    System.out.println("[DEBUG] Attempting to connect to server at " + HOST + ":" + CONNECTOR + "...");
                    socket = new Socket(HOST, CONNECTOR);
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    out = new PrintWriter(socket.getOutputStream(), true);
                    System.out.println("[DEBUG] Connection established successfully.");

                    while (running && !socket.isClosed()) {
                        // handle incoming messages
                        if (in.ready()) {
                            String message = in.readLine();
                            if (message != null) {
                                Message msg = new Message(message);
                                inQueue.add(msg);
                                if (messageListener != null) {
                                    messageListener.onMessageReceived(msg.getContent());
                                }
                                processMessage(msg);
                            }
                        }
                        // handle outgoing messages
                        Message toSend = outQueue.poll();
                        if (toSend != null) {
                            // send to socket
                            out.println(toSend.getContent());
                            System.out.println("[DEBUG] Sent: " + toSend.getContent());
                        }
                    }
                } catch (Exception e) {
                    System.err.println("[DEBUG] Connection error: " + e.getMessage());
                    messageListener.onCommunicationError(e);
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

    private void processMessage(Message msg) {
        // Overridden in subclasses for specialization
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
        in = null;
        out = null;
    }

    /**
     * An interface for listeners who want to receive messages from the communication source.
     */
    public interface MessageListener {
        void onMessageReceived(String message);

        void onCommunicationError(Exception e);
    }
}


