package Server;/* This class represents an object that helps build the specializations. */

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ioPort {
    // represents the network ports
    private String connector;
    private ServerSocket serverSocket;
    public static final int PORT = 8080;
    public static final String STOP_STRING = "stop";
    DataInputStream dataInputStream;

    public ioPort(String connector) throws IOException {
        serverSocket = new ServerSocket(PORT);
        initConnections();
        this.connector = connector;
    }

    private void initConnections() throws IOException {
        Socket clientSocket = serverSocket.accept();
        dataInputStream = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream()));
        read();
        close();
    }

    public Message send(Message messages) {
        System.out.println("Sending: " + messages.getContent());
        return messages;
    }

    public Message get() {
        return new Message("this is getting a message from: " + connector);
    }

    public String read() {
        String line = "";
        return "reading message from: " + connector;
    }

    private void close() throws IOException {
        dataInputStream.close();
        serverSocket.close();
    }
}

