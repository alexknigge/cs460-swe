package Server;
/* class to represent the Communicator specialization */

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
