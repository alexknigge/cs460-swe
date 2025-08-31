package Monitor;
import Server.*;

import java.io.IOException;

/* class to represent the Monitor port specialization */

public class MonitorPort {

    private ioPort port;

    public MonitorPort(String connector) throws IOException {
        this.port = new ioPort(connector);
    }

    // uses IoPort to send
    public Message sendMessage(Message message) {
        return port.send(message);
    }

    // uses IoPort to read
    public String readMessage() {
        return port.read();
    }

}