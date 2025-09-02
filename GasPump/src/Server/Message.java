package Server;

/* Class to handle messages across devices */
public class Message {
    private final String CONTENT;

    public Message(String CONTENT) {
        this.CONTENT = CONTENT;
    }

    public String getContent() {
        return CONTENT;
    }

    @Override
    public String toString() {
        return CONTENT;
    }
}