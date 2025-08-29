/* class to represent the Control Port specilization */

public class ControlPort {
    private IoPort port;

    public ControlPort(String connector) {
        this.port = new IoPort(connector);
    }

    // only sends messages using IoPort
    public Message send(Message message){
        return port.send(message);
    }
}