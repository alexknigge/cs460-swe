package Server;

/* class to represent the Status specialization */

public class StatusPort extends IOPort {
    public StatusPort(int connector) {
        super(connector);
    }

    public Message read() {
        return super.read();
    }
}

