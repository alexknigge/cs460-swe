public class CommPort {
    private IoPort port;

    public CommPort(String connector) {
        this.port = new IoPort(connector);
    }

    // uses IoPort to send
    public void send(Message message) {
        // return port.send();
    }

    // uses IoPort to get
    public String get(){
        return port.get();
    }
}