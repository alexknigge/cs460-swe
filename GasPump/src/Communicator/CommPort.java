package Communicator;
/* class to represent the Communicator specialization */

import Server.ioPort;
import Server.Message;

import java.io.IOException;
import java.net.Socket;

public class CommPort {
    private ioPort port;
    private final Socket socket;

    public CommPort(Socket socket) throws IOException {
        this.socket = socket;
        this.port = new ioPort(this.socket);
    }

    // uses IoPort to send
    public void sendMessage(Message message) throws IOException {
        port.send(message);
    }

    // uses IoPort to get
    public Message getMessage(){
        return new Message(port.get());
    }
}