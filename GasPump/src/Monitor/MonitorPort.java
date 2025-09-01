package Monitor;
import Server.*;

import java.io.IOException;
import java.net.Socket;

/* class to represent the Monitor port specialization */

public class MonitorPort {

    private ioPort port;

    public MonitorPort(Socket socket) throws IOException {
        this.port = new ioPort(socket);
    }

    // uses IoPort to send
    public void sendMessage(Message message) throws IOException {
        port.send(message);
    }

    // uses IoPort to read
    public String readMessage() throws IOException {
        return port.read();
    }

}