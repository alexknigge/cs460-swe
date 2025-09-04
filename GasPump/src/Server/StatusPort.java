package Server;

/* class to represent the Status specialization */

public class StatusPort extends IOPort {
    public StatusPort(String connector) {
        super(connector);
    }

    public Message read() {
        return super.read();
    }
}

