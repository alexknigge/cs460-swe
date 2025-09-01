package Status;
import Server.*;

import java.io.IOException;
import java.net.Socket;

/* class to represent the Status specialization */

public class StatusPort {
    private ioPort port;
    private String status;

    public StatusPort(Socket socket) throws IOException {
        this.port = new ioPort(socket);
    }

    // only uses IoPort to read
    public String readStatus() throws IOException {
        status = port.read();
        return status;
    }
}