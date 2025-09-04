package Main;

import Server.CommPort;
import Server.Message;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

// Main.Main
public class Main {
    public static void main(String[] args) throws InterruptedException, IOException {
        CommPort mainToScreen = new CommPort("MainToScreen");
        //CommPort mainToPump = new CommPort("MainToPump");
        CommPort mainToFlowMeter = new CommPort("MainToFlowMeter");
        //CommPort mainToCardReader = new CommPort("MainToCardReader");

        String initialScreenMessage = "t:01/s:3/f:2/c:0/Welcome!;" +
                "t:2/s:2/f:1/c:0/Select Grade;" +
                "t:4/s:2/f:1/c:3/87 Octane;$4.49;b:4/m;" +
                "t:5/s:2/f:1/c:4/91 Octane;$4.99;b:5/m;" +
                "t:6/s:2/f:1/c:1/93//";

        Message screenUpdate = new Message(initialScreenMessage);
        mainToScreen.send(screenUpdate);

        System.out.println("[SERVER] Waiting...");
        TimeUnit.SECONDS.sleep(5);
        System.out.println("[SERVER] Starting up flow meter...");

        mainToFlowMeter.send(new Message("CMD:START"));
        TimeUnit.SECONDS.sleep(2);
        Message flowMeter = mainToFlowMeter.get();
        while (true) {
            if (flowMeter != null) {
                if (flowMeter.toString().contains("FLOW:STOP")) {
                    break;
                }
            }
            mainToScreen.send(flowMeter);
            System.out.println(flowMeter);
            flowMeter = mainToFlowMeter.get();
        }
        System.out.println(flowMeter);
        System.out.println("Done pumping");


        /*
        if (mainToPump.get().equals(new Message("on"))) {
            String initialScreenMessage = "t:01/s:3/f:2/c:0/Welcome!;" +
                    "t:2/s:2/f:1/c:0/Select Grade;" +
                    "t:4/s:2/f:1/c:3/87 Octane;$4.49;b:4/m;" +
                    "t:5/s:2/f:1/c:4/91 Octane;$4.99;b:5/m;" +
                    "t:6/s:2/f:1/c:1/93//";

            Message screenUpdate = new Message(initialScreenMessage);
            mainToScreen.send(screenUpdate);
        }
         */
    }
}
