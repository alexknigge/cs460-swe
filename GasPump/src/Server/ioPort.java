package Server;/* This class represents an object that helps build the specializations. */

import java.io.*;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;

public class ioPort {
    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;
    private final Queue<String> buffer = new LinkedList<>();

    public ioPort(Socket socket) throws IOException {
        this.socket = socket;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);
    }

    // Called by Main to check if something is ready
    public boolean hasMessage() throws IOException {
        if (in.ready()) {
            String line = in.readLine();
            if (line != null) {
                buffer.add(line);
            }
        }
        return !buffer.isEmpty();
    }

    public String read() {
        return buffer.poll();
    }

    public String get() {
        return buffer.poll();
    }

    public void send(Message msg) {
        out.println(msg);
    }
}

