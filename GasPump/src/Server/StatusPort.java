package Server;

/* class to represent the Status specialization */

public class StatusPort extends IOPort {
    public StatusPort(int port) {
        super(port);
    }

    public Message read() {
        return super.read();
    }
}

