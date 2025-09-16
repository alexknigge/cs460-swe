package Server;

/**
 * Abstracts a connection to another device.
 * This is intended for connections which only require the sending and getting of incoming messages from the connection.
 */
public class CommPort extends IOPort {
    public CommPort(String CONNECTOR) {
        super(CONNECTOR);
    }

    public void send(Message msg) {
        super.send(msg);
    }

    public Message get() {
        return super.get();
    }
}
