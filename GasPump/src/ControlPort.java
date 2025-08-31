/* class to represent the Control Port specialization */

public class ControlPort {
    private ioPort port;

    public ControlPort(String connector) {
        this.port = new ioPort(connector);
    }

    // only sends messages using IoPort
    public Message sendMessage(Message message){
        return port.send(message);
    }
}