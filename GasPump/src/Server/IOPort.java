package Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * A robust, two-way communication port that can act as either a client or a server
 * based on the connector port's mapping in DeviceMapper.
 * Uses dedicated, blocking threads for efficient I/O.
 */
public class IOPort {

    private final BlockingQueue<Message> inQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<Message> outQueue = new LinkedBlockingQueue<>();
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Thread readerThread;
    private Thread writerThread;

    /**
     * Creates an IOPort that dynamically determines its role as a server or client.
     * If the connectorPort maps to the "main host" in DeviceMapper, it acts as a CLIENT.
     * Otherwise, it acts as a SERVER, listening on the connectorPort.
     *
     * @param connectorPort The port number that defines the device and its role.
     */
    // TODO: update javadoc
    public IOPort(String deviceID) {
        try {
            int portNum = DeviceMapper.getDevicePort(deviceID);
            if (DeviceMapper.shouldIDBeAServer(deviceID)) {
                startAsServer(portNum);
            } else {
                startAsClient("localhost", portNum);
            }
        } catch (IllegalArgumentException e) {
            System.err.println("FATAL: Configuration error - " + e.getMessage());
            close();
        }
    }


    /**
     * Initialized the IOPort using a SERVER socket, listening on the specified port.
     * NOTE: This is blocking and will NOT return until a client connects.
     *
     * @param port The socket's port number
     */
    private void startAsServer(int port) {
        // Using try-with-resources for the ServerSocket to ensure it's always closed.
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("[SERVER] Device listening on port " + port + "...");
            this.socket = serverSocket.accept(); // Blocks until a client connects
            System.out.println("[SERVER] Main host client connected from " + socket.getInetAddress());
            initializeStreamsAndThreads();
        } catch (IOException e) {
            System.err.println("FATAL: Server IOPort failed to initialize on port " + port + ": " + e.getMessage());
            close(); // Ensure all resources are cleaned up on failure
        }
    }

    /**
     * Initialized the IOPort using a CLIENT socket, listening on the specified port.
     * NOTE: This is blocking and will NOT return until a client connects.
     *
     * @param host The socket's hostname
     * @param port The socket's port number
     */
    private void startAsClient(String host, int port) {
        try {
            System.out.println("[CLIENT] Main host connecting to server at " + host + ":" + port + "...");
            this.socket = new Socket(host, port);
            System.out.println("[CLIENT] Connection established successfully.");
            initializeStreamsAndThreads();
        } catch (IOException e) {
            System.err.println("FATAL: Client IOPort failed to connect to " + host + ":" + port + ": " + e.getMessage());
            close(); // Ensure all resources are cleaned up on failure
        }
    }


    private void initializeStreamsAndThreads() throws IOException {
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        this.readerThread = new Thread(this::readFromSocket);
        this.writerThread = new Thread(this::writeToSocket);
        this.readerThread.setDaemon(true); // Daemon threads won't prevent JVM shutdown
        this.writerThread.setDaemon(true);
        this.readerThread.start();
        this.writerThread.start();
    }


    /**
     * Reads lines from the socket, and converts them to Message objects, placing them in the
     * incoming message queue.
     */
    private void readFromSocket() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                inQueue.put(Message.fromString(line));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Preserve the interrupted status
            System.err.println("Reader thread interrupted.");
        } catch (Exception e) {
            // This is expected when the connection is closed by either party.
            if (!isClosed()) {
                System.err.println("Connection lost while reading: " + e.getMessage());
            }
        } finally {
            close();
        }
    }

    /**
     * Takes a message object from the outgoing queue, and converts them to strings,
     * sending them over the socket.
     */
    private void writeToSocket() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Message msg = outQueue.take(); // Blocks until a message is available
                out.println(msg);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Preserve the interrupted status
            System.err.println("Writer thread interrupted.");
        }
    }


    /**
     * Send an asynchronous message. This will be queued and send by a background thread.
     *
     * @param message The Message object to send.
     */
    public void send(Message message) {
        if (!isClosed() && message != null) {
            outQueue.add(message);
        }
    }


    /**
     * Retrieves and removes the earliest,
     * or returns null if this no messages are present in the queue.
     *
     * @return A Message object, or null if no message is available.
     */
    public Message get() {
        return inQueue.poll();
    }

    /**
     * Retrieves, but does not remove, the earliest received message,
     * or returns null if this no messages are present in the queue.
     *
     * @return The Message object from the head of the queue, or null if interrupted.
     */
    public Message read() {
        return inQueue.peek();
    }

    /**
     * Checks if the port's underlying socket is closed or not initialized.
     *
     * @return true if the connection is closed, false otherwise.
     */
    public boolean isClosed() {
        return socket == null || socket.isClosed();
    }

    /**
     * Closes the connection and all associated resources cleanly.
     * This method is idempotent and safe to call multiple times.
     */
    public void close() {
        // Interrupt the I/O threads first to stop them from blocking.
        if (writerThread != null) {
            writerThread.interrupt();
        }
        if (readerThread != null) {
            readerThread.interrupt();
        }

        // Close streams and socket in a try-catch block to suppress errors on close.
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            // This is acceptable, as we are tearing down the connection.
        }
    }
}

