import Devices.GasPumpUI;
import Server.Message;
import Server.ioPort;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

// Main
public class Main {
    private final List<ioPort> ports = new ArrayList<>();
    public static void main(String[] args) throws Exception {
        new Main().run();
    }

    /**
     * The run method waits for each interface to start up and connect
     * before moving on to the next
     * The current order is Screen (UI) -> Pump
     * @throws Exception
     */
    public void run() throws Exception {
        ServerSocket server = new ServerSocket(5000);

        System.out.println("Waiting for UI...");
        Socket uiSocket = server.accept();
        ioPort uiPort = new ioPort(uiSocket);
        ports.add(uiPort);
        System.out.println("UI connected.");

        System.out.println("Waiting for Pump...");
        Socket pumpSocket = server.accept();
        ioPort pumpPort = new ioPort(pumpSocket);
        ports.add(pumpPort);
        System.out.println("Pump connected.");

        while (true) {
            for (ioPort port : ports) {
                if (port.hasMessage()) {
                    String msg = port.read();
                    System.out.println("Received: " + msg);
                    routeMessage(msg, port, uiPort, pumpPort);
                }
            }
            Thread.sleep(50);
        }

    }

    private void routeMessage(String msg, ioPort from, ioPort uiPort, ioPort pumpPort) {
        if (from == uiPort) {
            // Forward UI message to pump
            pumpPort.send(new Message("UI->Pump: " + msg));
        } else if (from == pumpPort) {
            // Forward pump response to UI
            uiPort.send(new Message("Pump->UI: " + msg));
        }
    }
}
