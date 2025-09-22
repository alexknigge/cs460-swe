package Main;

import Server.IOPort;
import Server.Message;

import java.io.IOException;
import java.sql.SQLOutput;
import java.util.concurrent.TimeUnit;

// Main.Main
public class Main {
    public static void main(String[] args) throws InterruptedException, IOException {
        IOPort mainToScreen = new IOPort("MainToScreen");
        //CommPort mainToPump = new CommPort("MainToPump");
        IOPort mainToFlowMeter = new IOPort("MainToFlowMeter");
        IOPort mainToCardReader = new IOPort("MainToCardReader");

        while (true) {
            String initialScreenMessage = "t:01/s:3/f:2/c:0/Welcome!;" +
                    "t:2/s:2/f:1/c:0/Select Grade;" +
                    "t:4/s:2/f:1/c:3/87 Octane;$4.49;b:4/m;" +
                    "t:5/s:2/f:1/c:4/91 Octane;$4.99;b:5/m;" +
                    "t:6/s:2/f:1/c:1/93;b:6/m//";

            Message screenUpdate = new Message(initialScreenMessage);
            mainToScreen.send(screenUpdate);

            System.out.println("[SERVER] Waiting for card...");
            TimeUnit.SECONDS.sleep(5);

            screenUpdate = mainToCardReader.get();
            mainToScreen.send(screenUpdate);

            TimeUnit.SECONDS.sleep(5);

            screenUpdate = mainToCardReader.get();
            mainToScreen.send(screenUpdate);


            TimeUnit.SECONDS.sleep(5);
            System.out.println("[SERVER] Pumping gas...");
            //System.out.println("[SERVER] Starting up flow meter...");

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
            TimeUnit.SECONDS.sleep(2);
            flowMeter = mainToFlowMeter.get();
            mainToScreen.send(flowMeter);
            System.out.println("Done pumping");
            TimeUnit.SECONDS.sleep(5);

            Message finalMessage = new Message("t:01/s:3/f:2/c:0/Thank You!;" +
                    "t:2/s:2/f:1/c:0/Receipt sent.;" +
                    "t:03/s:2/f:1/c:0/;" +
                    "t:04/s:2/f:1/c:0/;" +
                    "t:05/s:2/f:1/c:0/;" +
                    "t:06/s:2/f:1/c:0//");
            mainToScreen.send(finalMessage);
            TimeUnit.SECONDS.sleep(5);
        }

    }
}
