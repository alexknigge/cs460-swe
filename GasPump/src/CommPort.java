/* class to represent the Communicator specilization */

public class CommPort {
    private IoPort port;

    public CommPort(String connector) {
        this.port = new IoPort(connector);
    }

    // uses IoPort to send
    public Message send(Message message) {
        return port.send(message);
    }

    // uses IoPort to get
    public Message get(){
        return port.get();
    }
}