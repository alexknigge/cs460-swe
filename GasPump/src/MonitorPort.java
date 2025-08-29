/* class to represent the Monitor port specialization */

public class MonitorPort {

    private IoPort port;

    public MonitorPort(String connector) {
        this.port = new IoPort(connector);
    }

    // uses IoPort to send
    public Message sendMessage(Message message) {
        return port.send(message);
    }

    // uses IoPort to read
    public String readMessage() {
        return port.read();
    }

}