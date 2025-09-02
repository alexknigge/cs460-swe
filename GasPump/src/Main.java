import Communicator.CommPort;
import Devices.GasPumpUI;
import Devices.Pump;
import Devices.ScreenCommunicationManager;
import Server.Message;
import javafx.application.Platform;

import java.util.Scanner;

// Main
public class Main extends CommPort {
    private final Scanner scanner = new Scanner(System.in);
    public static void main(String[] args) throws Exception {
        Pump pump = new Pump();
        ScreenCommunicationManager screenPort = new ScreenCommunicationManager();

        // Inject the buffer into the UI before launching
        GasPumpUI.setCommManager(screenPort);

        // Launch JavaFX UI on the main thread
        new Thread(() -> GasPumpUI.launch(GasPumpUI.class)).start();

        // Wait a little for JavaFX to initialize
        Thread.sleep(500); // adjust as needed

        // Optional: send initial UI message
        screenPort.sendMessage(new Message(
                "t:01/s:3/f:2/c:0/Welcome!;" +
                        "t:2/s:2/f:1/c:0/Select Grade;" +
                        "t:4/s:2/f:1/c:3/87 Octane;b:4/m;" +
                        "t:5/s:2/f:1/c:4/91 Octane;b:5/m;" +
                        "t:8/s:1/f:3/c:2/Cancel;b:8/x;//",
                1001
        ));

        // Start polling loop in a separate thread
        Thread pollingThread = new Thread(() -> {
            try {
                while (true) {
                    // Poll pump messages
                    while (pump.hasMessage()) {
                        Message msg = pump.read();
                        pump.handleMessage(msg);
                    }

                    // Poll UI messages
                    while (screenPort.hasMessage()) {
                        Message msg = screenPort.get();

                        Platform.runLater(() -> {
                            GasPumpUI ui = GasPumpUI.getInstance();
                            if (ui != null) {
                                ui.processScreenMessage(msg);
                            }
                        });
                    }

                    Thread.sleep(50); // avoid busy-wait
                }
            } catch (InterruptedException e) {
                System.out.println("Polling thread interrupted");
            }
        });

        pollingThread.setDaemon(true); // stops when main exits
        pollingThread.start();
    }
}
