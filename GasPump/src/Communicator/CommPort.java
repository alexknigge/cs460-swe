package Communicator;
/* class to represent the Communicator specialization */

import Server.ioPort;
import Server.Message;

public class CommPort extends ioPort {
    public void send(Message msg) {
        super.sendMessage(msg);
    }

    public Message get() {
        return super.getMessage();
    }
}