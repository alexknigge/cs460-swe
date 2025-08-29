/* class to represent the Monitor port specilization */

public class MonitorPort {

    private IoPort port;

    public MonitorPort(String connector) {
        this.port = new IoPort(connector);
    }

    // uses IoPort to send
    public void send(Message message) {
        port.send(message);
    }

    // uses IoPort to read
    public String read(){
        return port.read();
    }

}