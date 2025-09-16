package Server;

/**
 * Abstracts a connection to another device.
 * This is intended for connections which only require the sending of incoming messages from the connection.
 */
public class ControlPort extends IOPort {
    public ControlPort(String CONNECTOR) {
        super(CONNECTOR);
    }

    public void send(Message msg) {
        super.send(msg);
    }
}
