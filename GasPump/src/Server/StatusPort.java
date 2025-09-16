package Server;

/* class to represent the Status specialization */

/**
 * Abstracts a connection to another device.
 * This is intended for connections which only require the reading of incoming messages from the connection.
 */
public class StatusPort extends IOPort {
    public StatusPort(String connector) {
        super(connector);
    }

    public Message read() {
        return super.read();
    }
}

