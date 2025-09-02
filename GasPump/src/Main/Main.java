package Main;

import Communicator.CommPort;
import Devices.GasPumpUI;
import Devices.Pump;
import Devices.ScreenCommunicationManager;
import Server.Message;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.util.Duration;

import java.io.IOException;
import java.util.Scanner;

public class Main extends CommPort {
    private Scanner scanner = new Scanner(System.in);
    private static ScreenCommunicationManager screenPort = new ScreenCommunicationManager();
    private static final Pump pump;

    static {
        try {
            pump = new Pump();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        // Inject comm manager into UI
        GasPumpUI.setCommManager(screenPort);

        // Launch JavaFX (blocks until UI exits)
        Application.launch(GasPumpUI.class, args);
    }

    /**
     * Called by GasPumpUI after the UI is ready.
     */
    public static void startComms() {
        // Step 1: send initial welcome message
        screenPort.sendMessage(new Message(
                "t:01/s:3/f:2/c:0/Welcome!;" +
                        "t:2/s:2/f:1/c:0/Select Grade;" +
                        "t:4/s:2/f:1/c:3/87 Octane;b:4/m;" +
                        "t:5/s:2/f:1/c:4/91 Octane;b:5/m;" +
                        "t:8/s:1/f:3/c:2/Cancel;b:8/x;//",
                1001
        ));

        // Step 2: Start Timeline polling for pump and screen messages
        Timeline poller = new Timeline(new KeyFrame(Duration.millis(50), e -> {
            // Pump messages

            while (pump.hasMessage()) {
                Message msg = pump.read();
                pump.handleMessage(msg);
            }



            // Screen messages
            while (screenPort.hasMessage()) {
                Message msg = screenPort.get();
                GasPumpUI ui = GasPumpUI.getInstance();
                if (ui != null) {
                    ui.processScreenMessage(msg);
                }
            }

            // Non-blocking console input
            try {
                if (System.in.available() > 0) {
                    Scanner scanner = new Scanner(System.in);
                    String input = scanner.nextLine();
                    if ("quit".equalsIgnoreCase(input)) {
                        Platform.exit();
                    } else {
                        screenPort.sendMessage(new Message(input, 1001));
                    }
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }));
        poller.setCycleCount(Timeline.INDEFINITE);
        poller.play();
    }
}
