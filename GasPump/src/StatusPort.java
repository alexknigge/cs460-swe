/* class to represent the Status specilization */

public class StatusPort {
    private IoPort port;

    public StatusPort(String connector) {
        this.port = new IoPort(connector);
    }

    // only uses IoPort to read
    public String readStatus() {
        return port.read();
    }
}