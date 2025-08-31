/* class to represent the Status specialization */

public class StatusPort {
    private ioPort port;

    public StatusPort(String connector) {
        this.port = new ioPort(connector);
    }

    // only uses IoPort to read
    public String readStatus() {
        return port.read();
    }
}