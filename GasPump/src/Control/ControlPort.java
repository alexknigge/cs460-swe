package Control;/* class to represent the Control Port specialization */

import Server.*;

import java.io.IOException;

public class ControlPort {
    private ioPort port;

    public ControlPort(String connector) throws IOException {
        this.port = new ioPort(connector);
    }

    // only sends messages using IoPort
    public Message sendMessage(Message message){
        return port.send(message);
    }
}