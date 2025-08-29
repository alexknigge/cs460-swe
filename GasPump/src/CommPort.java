/* class to represent the Communicator specialization */

public class CommPort {
    private IoPort port;

    public CommPort(String connector) {
        this.port = new IoPort(connector);
    }

    // uses IoPort to send
    public Message sendMessage(Message message) {
        return port.send(message);
    }

    // uses IoPort to get
    public Message getMessage(){
        return port.get();
    }
}