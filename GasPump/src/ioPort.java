/* This class represents an object that helps build the specializations. */

public class IoPort {
    // represents the network ports
    private String connector;

    public IoPort(String connector) {
        this.connector = connector;
    }

    public Message send(Message messages) {
        System.out.println("Sending: " + messages.getContent());
        return messages;
    }

    public Message get() {
        return new Message("this is getting a message from: " + connector);
    }

    public String read() {
        return "reading message from: " + connector;
    }
}

