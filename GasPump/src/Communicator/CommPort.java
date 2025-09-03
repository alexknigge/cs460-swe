package Communicator;
/* class to represent the Communicator specialization */

import Server.IOPort;
import Server.Message;

public class CommPort extends IOPort {
    public void send(Message msg) {
        super.sendMessage(msg);
    }

    public Message get() {
        return super.getMessage();
    }
}