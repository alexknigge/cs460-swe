package Server;

/* Class to handle messages across devices */
public class Message {
    private final String CONTENT;
    private final int DEVICE;

    public Message(String CONTENT, int DEVICE) {
        this.CONTENT = CONTENT;
        this.DEVICE = DEVICE;
    }

    public String getContent() {
        return CONTENT;
    }

    public int getDevice() {
        return DEVICE;
    }

    @Override
    public String toString() {
        return CONTENT;
    }
}