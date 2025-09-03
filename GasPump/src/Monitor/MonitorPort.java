package Monitor;
import Server.*;

/* class to represent the Monitor port specialization */

public class MonitorPort extends IOPort {

    public void send(Message msg) {
        super.sendMessage(msg);
    }

    public Message read() {
        return super.readMessage();
    }
}