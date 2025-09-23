package Server;

import java.io.IOException;
import java.net.Socket;

/**
 * A two-way communication port that acts as a CLIENT.
 * It connects to a specified server and then uses threaded, non-blocking I/O
 * for sending and receiving messages.
 */
public class IOPort extends AbstractIOPort {

    /**
     * Initializes the IOPort as a CLIENT, connecting to a server at the specified host and port.
     * This constructor is blocking and will not return until a connection is established or fails.
     *
     * @param host The hostname or IP address of the server to connect to.
     * @param port The port number of the server to connect to.
     */
    public IOPort(String host, int port) {
        try {
            System.out.println("[CLIENT] Connecting to server at " + host + ":" + port + "...");
            this.socket = new Socket(host, port);
            System.out.println("[CLIENT] Connection established successfully to " + socket.getInetAddress());
            initializeStreamsAndThreads();
        } catch (IOException e) {
            System.err.println("FATAL: Client IOPort failed to connect to " + host + ":" + port + ": " + e.getMessage());
            close(); // Ensure all resources are cleaned up on failure
        }
    }
}
