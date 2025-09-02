package Server;
/* This class represents an object that helps build the specializations. */

import java.util.LinkedList;
import java.util.Queue;

public class ioPort {
    protected Queue<Message> buffer = new LinkedList<>();

    // Put a message into the buffer
    public void sendMessage(Message msg) {
        buffer.add(msg);
    }

    // Take a message (remove from buffer)
    protected Message getMessage() {
        return buffer.poll(); // returns null if empty
    }

    // Peek a message (do not remove from buffer)
    protected Message readMessage() {
        return buffer.peek();
    }

    // Check if there are messages
    public boolean hasMessage() {
        return !buffer.isEmpty();
    }
}


