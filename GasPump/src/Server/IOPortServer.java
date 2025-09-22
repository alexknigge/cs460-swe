package Server;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * A, two-way communication port that acts as a SERVER.
 * It listens on a specified port for a single client connection and then uses
 * threaded, non-blocking I/O for sending and receiving messages.
 */
public class IOPortServer extends AbstractIOPort {

    /**
     * Initializes the IOPort as a SERVER, listening on the specified port.
     * NOTE: This constructor is blocking and will NOT return until a client connects.
     *
     * @param port The port number to listen on.
     */
    public IOPortServer(int port) {
        // Using try-with-resources for the ServerSocket to ensure it's always closed
        // after a connection is made or an error occurs.
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("[SERVER] Listening on port " + port + "...");
            this.socket = serverSocket.accept(); // Blocks until a client connects
            System.out.println("[SERVER] Client connected from " + socket.getInetAddress());
            initializeStreamsAndThreads();
        } catch (IOException e) {
            System.err.println("FATAL: Server IOPort failed to initialize on port " + port + ": " + e.getMessage());
            close(); // Ensure all resources are cleaned up on failure
        }
    }
}
