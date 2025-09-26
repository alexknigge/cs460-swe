package Main;

import Server.DeviceConstants;
import Server.IOPort;
import Server.Message;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

// Main.Main
public class Main {
    public static void main(String[] args) throws InterruptedException, IOException {
        System.out.println("Connecting to Devices... (Make sure devices are turned on first)");
        IOPort screenConnection = new IOPort(DeviceConstants.SCREEN_HOSTNAME,
                DeviceConstants.SCREEN_PORT);
        System.out.println("Successfully connected to Screen.");
        /*IOPort pumpConnection = new IOPort(DeviceConstants.PUMP_HOSTNAME,
                DeviceConstants.PUMP_PORT);
        System.out.println("Successfully connected to Pump."); */
        IOPort flowMeterConnection = new IOPort(DeviceConstants.FLOW_METER_HOSTNAME,
                DeviceConstants.FLOW_METER_PORT);
        System.out.println("Successfully connected to Flow Meter.");
        IOPort cardReaderConnection = new IOPort(DeviceConstants.CARD_READER_HOSTNAME,
                DeviceConstants.CARD_READER_PORT);
        System.out.println("Successfully connected to Card Reader.");

        while (true) {
            String initialScreenMessage = "t:01/s:3/f:2/c:0/Welcome!;" +
                    "t:2/s:2/f:1/c:0/Select Grade;" +
                    "t:4/s:2/f:1/c:3/Regular (87 Octane) - $4.49;b:4/m;" +
                    "t:5/s:2/f:1/c:4/Premium (91) - $4.99;b:5/m;" +
                    "t:6/s:2/f:1/c:1/Super (93) - $5.25;b:6/m//";

            Message screenUpdate = new Message(initialScreenMessage);
            screenConnection.send(screenUpdate);

            System.out.println("[SERVER] Waiting for card...");
            TimeUnit.SECONDS.sleep(5);

            screenUpdate = cardReaderConnection.get();
            screenConnection.send(screenUpdate);

            TimeUnit.SECONDS.sleep(5);

            screenUpdate = cardReaderConnection.get();
            screenConnection.send(screenUpdate);


            TimeUnit.SECONDS.sleep(5);
            System.out.println("[SERVER] Pumping gas...");
            //System.out.println("[SERVER] Starting up flow meter...");

            flowMeterConnection.send(new Message("CMD:START rate=0.1 ppg=3.25 c:3"));
            TimeUnit.SECONDS.sleep(2);
            Message flowMeter = flowMeterConnection.get();
            while (true) {
                if (flowMeter != null) {
                    if (flowMeter.toString().contains("FLOW:STOP")) {
                        break;
                    }
                }
                Message fuelingScreen = new Message(
                        "t:01/s:3/f:2/c:0/Pumping Fueling;" +
                                "t:2/s:2/f:1/c:0/Press Pause to temporarily stop;" +
                                "t:3/s:2/f:1/c:2/Cancel;b:3/m;" +
                                "t:4/s:2/f:1/c:3/Help;b:4/m;" +
                                "t:5/s:2/f:1/c:4/Pause;b:5/m//"
                );
                screenConnection.send(fuelingScreen);
                //screenConnection.send(flowMeter);
                System.out.println(flowMeter);
                flowMeter = flowMeterConnection.get();
            }
            TimeUnit.SECONDS.sleep(2);
            flowMeter = flowMeterConnection.get();
            screenConnection.send(flowMeter);
            System.out.println("Done pumping");
            TimeUnit.SECONDS.sleep(5);

            Message finalMessage = new Message("t:01/s:3/f:2/c:0/Thank You!;" +
                    "t:2/s:2/f:1/c:0/Receipt sent.;" +
                    "t:03/s:2/f:1/c:0/;" +
                    "t:04/s:2/f:1/c:0/;" +
                    "t:05/s:2/f:1/c:0/;" +
                    "t:06/s:2/f:1/c:0//");
            screenConnection.send(finalMessage);
            TimeUnit.SECONDS.sleep(5);
        }

    }
}
