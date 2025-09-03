package Status;
import Server.*;

/* class to represent the Status specialization */

public class StatusPort extends IOPort {
    public Message read() {
        return super.readMessage();
    }
}