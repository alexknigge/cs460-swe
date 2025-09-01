package Control;/* class to represent the Control Port specialization */

import Server.*;

import java.io.IOException;
import java.net.Socket;

public class ControlPort {
    private ioPort port;

    public ControlPort(Socket socket) throws IOException {
        this.port = new ioPort(socket);
    }

    // only sends messages using IoPort
    public void sendMessage(Message message) throws IOException {
        port.send(message);
    }
}