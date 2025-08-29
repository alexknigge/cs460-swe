// class to test the IoPort and Messages

public class TestIoPort {
    public static void main(String[] args) {
        IoPort ioport = new IoPort("Test Connector");

        // testing the IoPort
        System.out.println("Test IoPort");
        // testing send
        Message send = new Message("test message :)");
        ioport.send(send);
        System.out.println("Test sent -> " + send.getContent());

        // testing get
        Message receive  = ioport.get();
        System.out.println("Test get -> " + receive.getContent());

        // testing read
        String str = ioport.read();
        System.out.println("Test read -> "+ str);
        System.out.println();

        // test CommPort
        System.out.println("Testing CommPort");
        CommPort network = new CommPort("ethernet");
        // test send
        network.sendMessage(new Message("the price of gas is $3.00"));
        // test get
        Message reply = network.getMessage();
        System.out.println("Communication get -> " + reply.getContent());
        System.out.println();

        // test ControlPort
        System.out.println("Testing ControlPort");
        ControlPort  pump = new ControlPort("pump");
        // test send
        Message pumpSend = pump.sendMessage(new Message("pump is on"));
        System.out.println("Control Port sent -> " + pumpSend.getContent());
        System.out.println();

        // test MonitorPort
        System.out.println("Testing MonitorPort");
        MonitorPort screen = new MonitorPort("screen");
        // test send
        Message screenSend = pump.sendMessage(new Message("Screen is being tested"));
        System.out.println("Monitor Port sent -> " + screenSend.getContent());
        // test read
        String screenReply = screen.readMessage();
        System.out.println("Communication read -> " + screenReply);
        System.out.println();

        // test Status Port
        System.out.println("Testing StatusPort");
        StatusPort status = new StatusPort("status");
        String statusReply = status.readStatus();
        System.out.println("Status Port read -> " + statusReply);
        System.out.println();
    }
}
