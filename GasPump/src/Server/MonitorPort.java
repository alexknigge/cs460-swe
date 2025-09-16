package Server;

/**
 * Abstracts a connection to another device.
 * This is intended for connections which only require the reading and sending of incoming messages from the connection.
 */
public class MonitorPort extends IOPort {

    public MonitorPort(String CONNECTOR) {
        super(CONNECTOR);
    }

    public void send(Message msg) {
        super.send(msg);
    }


    public Message read() {
        return super.read();
    }
}

