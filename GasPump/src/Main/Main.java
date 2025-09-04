package Main;

import Server.CommPort;
import Server.IOPort;
import Server.Message;

import java.io.IOException;
import java.util.Scanner;

// Main.Main
public class Main {
    public static void main(String[] args) throws InterruptedException, IOException {
        CommPort mainToPump = new CommPort("MainToPump");
        CommPort mainToScreen = new CommPort("MainToScreen");
        CommPort mainToFlowMeter = new CommPort("MainToFlowMeter");
        CommPort mainToCardReader = new CommPort("MainToCardReader");
    }
}
