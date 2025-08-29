public class MonitorPort {

    private IoPort port;

    public MonitorPort(String connector) {
        this.port = new IoPort(connector);
    }

    // uses IoPort to send
    public void send(Message message) {

    }

    // uses IoPort to read
    public void read(){
       // return port.read();
    }

}