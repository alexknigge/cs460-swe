public class ControlPort {
    private IoPort port;

    public ControlPort(String connector) {
        this.port = new IoPort(connector);
    }

    // only sends messages using IoPort
    public void send(Message message){
        // return port.send();
    }
}