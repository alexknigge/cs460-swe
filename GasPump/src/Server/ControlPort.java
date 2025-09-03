package Server;
/* class to represent the Control Port specialization */

public class ControlPort extends IOPort {
    public ControlPort(int CONNECTOR) {
        super(CONNECTOR);
    }

    public void send(Message msg) {
        super.send(msg);
    }
}
