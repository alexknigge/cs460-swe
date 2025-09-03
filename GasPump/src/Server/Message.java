package Server;

/* Class to handle messages across devices */
public class Message {
    private final String CONTENT;

    public Message(String CONTENT) {
        this.CONTENT = CONTENT;
    }

    // build a message from a string
    public static Message fromString(String msg) {
        return new Message(msg);
    }

    public String getContent() {
        return CONTENT;
    }

    @Override
    public String toString() {
        return CONTENT;
    }
}