package Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

/**
 * An abstract base class for a robust, two-way communication port.
 * It handles the common logic of message queuing and threaded I/O.
 * Subclasses are responsible for establishing the connection as either a client or a server.
 */
abstract class AbstractIOPort {

    private final AtomicReference<Message> latestMessage = new AtomicReference<>();
    private final BlockingQueue<Message> outQueue = new LinkedBlockingQueue<>();

    protected Socket socket;
    protected PrintWriter out;
    protected BufferedReader in;

    private Thread readerThread;
    private Thread writerThread;

    /**
     * Initializes the input/output streams and starts the dedicated reader and writer threads.
     * This method should be called by a subclass constructor once the socket is connected.
     *
     * @throws IOException if an I/O error occurs when creating the streams.
     */
    protected void initializeStreamsAndThreads() throws IOException {
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        this.readerThread = new Thread(this::readFromSocket);
        this.writerThread = new Thread(this::writeToSocket);
        this.readerThread.setDaemon(true);
        this.writerThread.setDaemon(true);
        this.readerThread.start();
        this.writerThread.start();
    }

    /**
     * Reads lines from the socket, converts them to Message objects, and places them in the
     * incoming message queue. This method runs in its own thread.
     */
    private void readFromSocket() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                latestMessage.set(Message.fromString(line));
            }
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
     * Takes a message object from the outgoing queue, converts it to a string,
     * and sends it over the socket. This method runs in its own thread.
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
     * Queues a message to be sent asynchronously.
     *
     * @param message The Message object to send.
     */
    public void send(Message message) {
        if (!isClosed() && message != null) {
            outQueue.add(message);
        }
    }

    /**
     * Atomically retrieves the latest received message and clears it.
     * This ensures that for high-frequency updates, only the most recent message is processed.
     *
     * @return The latest Message object, or null if no new message has arrived.
     */
    public Message get() {
        return latestMessage.getAndSet(null);
    }

    /**
     * Retrieves, but does not remove, the latest received message.
     *
     * @return The latest Message object at the head of the queue, or null if empty.
     */
    public Message read() {
        return latestMessage.get();
    }

    /**
     * Checks if the port's socket connection is closed or not initialized.
     *
     * @return true if the connection is closed, false otherwise.
     */
    private boolean isClosed() {
        return socket == null || socket.isClosed();
    }

    /**
     * Closes the connection and all associated resources cleanly. (Safe to call multiple times.)
     */
    public void close() {
        if (writerThread != null) {
            writerThread.interrupt();
        }
        if (readerThread != null) {
            readerThread.interrupt();
        }

        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            // Ignore errors on close.
        }
    }
}
