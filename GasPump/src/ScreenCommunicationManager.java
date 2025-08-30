import java.io.InputStreamReader;
import java.io.Reader;

/**
 * Manages all incoming and outgoing communication for the gas pump UI.
 * This class abstracts the communication source (initially Standard I/O),
 * making it easy to switch to sockets or other methods in the future.
 */
public class ScreenCommunicationManager {

    private final MessageListener listener;

    public ScreenCommunicationManager(MessageListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("MessageListener cannot be null.");
        }
        this.listener = listener;
    }

    /**
     * Starts listening for incoming messages on a new daemon thread.
     * Currently reads from System.in.
     */
    public void startListening() {
        Thread inputThread = new Thread(() -> {
            try (Reader reader = new InputStreamReader(System.in)) {
                StringBuilder messageBuilder = new StringBuilder();
                int c;
                while ((c = reader.read()) != -1) {
                    messageBuilder.append((char) c);
                    // Check for the terminator string
                    if (messageBuilder.toString().endsWith("//")) {
                        listener.onMessageReceived(messageBuilder.toString());
                        // Reset the builder for the next message
                        messageBuilder.setLength(0);
                    }
                }
            } catch (Exception e) {
                listener.onCommunicationError(e);
            }
        });
        inputThread.setDaemon(true);
        inputThread.start();
    }

    /**
     * Sends a message to the output stream.
     * Currently writes to System.out.
     *
     * @param message The message to send, without the terminator.
     */
    public void sendMessage(String message) {
        // The protocol requires the terminator to be on all outgoing messages.
        System.out.print(message + "//");
        System.out.flush();
    }

    /**
     * An interface for listeners who want to receive messages from the communication source.
     */
    public interface MessageListener {
        void onMessageReceived(String message);

        void onCommunicationError(Exception e);
    }
}
