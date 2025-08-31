package Communicator;
/* class to represent the Communicator specialization */

import Server.ioPort;
import Server.Message;

import java.io.IOException;

public class CommPort {
    private ioPort port;

    public CommPort(String connector) throws IOException {
        this.port = new ioPort(connector);
    }

    // uses IoPort to send
    public Message sendMessage(Message message) {
        return port.send(message);
    }

    // uses IoPort to get
    public Message getMessage(){
        return port.get();
    }
}