package Status;
import Server.*;

/* class to represent the Status specialization */

public class StatusPort extends ioPort {
    public Message read() {
        return super.readMessage();
    }
}