package Server;

/* class to represent the Monitor port specialization */

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

