package SmallDevices;

import Server.DeviceConstants;
import Server.IOPortServer;
import Server.Message;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.text.DecimalFormat;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * FlowMeter simulates a fuel pump by computing the number of gallons dispensed and the total price based on elapsed time.
 * As per SRS 6.3, the flow rate is fixed and internal to this device. This version includes a StatusUI for visualization.
 */
public class FlowMeter {
    // The flow rate is fixed as per SRS 6.3.
    private static final double FLOW_RATE_GPS = 0.15; // Gallons Per Second (equivalent to 9 gal/min)
    // --- Formatters ---
    private static final DecimalFormat G = new DecimalFormat("0.000");
    private static final DecimalFormat $ = new DecimalFormat("$0.00");
    private final IOPortServer flowPort = new IOPortServer(DeviceConstants.FLOW_METER_PORT);
    private final ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
    // --- State Variables ---
    private volatile double pricePerGallon;
    private volatile String gasType = "";
    private boolean running = false;
    private long lastStartNanos;
    private double accSeconds = 0.0;
    private volatile double lastGallons = 0.0;
    private volatile double lastTotal = 0.0;

    /**
     * Initializes the FlowMeter device.
     */
    public FlowMeter() {
        // A default price, which will be updated by the controller's command.
        this.pricePerGallon = 4.59;
        // Start the background thread for command polling and ticking.
        this.exec.scheduleAtFixedRate(this::runCycle, 0, 100, TimeUnit.MILLISECONDS);
    }

    private static double getDoubleParam(String s, String key, double defVal) {
        int i = s.indexOf(key + "=");
        if (i < 0) return defVal;
        int start = i + key.length() + 1;
        int end = start;
        while (end < s.length() && (Character.isDigit(s.charAt(end)) || s.charAt(end) == '.'))
            end++;
        try {
            return Double.parseDouble(s.substring(start, end));
        } catch (Exception e) {
            return defVal;
        }
    }

    /**
     * Main entry point for running the FlowMeter device with its status UI.
     */
    public static void main(String[] args) {
        // Create the device instance and bind it to the UI.
        StatusUI.bind(new FlowMeter());
        // Launch the JavaFX UI.
        Application.launch(StatusUI.class, args);
    }

    /**
     * A single cycle of the device's operation, run periodically.
     */
    private void runCycle() {
        pollCommands();
        tick();
    }

    /**
     * Polls the IOPort for incoming messages and handles them.
     */
    private void pollCommands() {
        Message m = flowPort.get();
        if (m != null) {
            String content = m.getContent();
            if (content != null) {
                handlePortCommand(content.trim());
            }
        }
    }

    /**
     * If the pump is running, calculates the current gallons and total cost and sends an update.
     */
    private void tick() {
        if (!running) return;
        double elapsed = accSeconds + (System.nanoTime() - lastStartNanos) / 1e9;
        double gallons = elapsed * FLOW_RATE_GPS; // Use the fixed internal rate
        double total = gallons * pricePerGallon;
        lastGallons = gallons;
        lastTotal = total;
        sendPort(updateMessage(gallons, total));
    }

    /**
     * Parses and executes commands received from the main controller.
     *
     * @param msg The command string from the controller.
     */
    private void handlePortCommand(String msg) {
        if (msg.startsWith("CMD:START")) {
            // The rate is fixed. We only parse the price-per-gallon (ppg) and gas type.
            pricePerGallon = getDoubleParam(msg, "ppg", pricePerGallon);
            int gasIndex = msg.indexOf("gas=");
            if (gasIndex != -1) {
                gasType = msg.substring(gasIndex + 4).replace("//", "").trim();
            }
            start();
        } else if (msg.equals("CMD:PAUSE//")) {
            stop();
        } else if (msg.equals("CMD:RESET//")) {
            reset();
        }
    }

    /**
     * Starts the fueling process.
     */
    public void start() {
        if (running) return;
        running = true;
        lastStartNanos = System.nanoTime();
        System.out.println("Flow meter started.");
    }

    /**
     * Stops the fueling process.
     */
    public void stop() {
        if (!running) return;
        accSeconds += (System.nanoTime() - lastStartNanos) / 1e9;
        running = false;
        System.out.println("Flow meter stopped.");
    }

    /**
     * Resets the flow meter's counters and state for the next transaction.
     */
    public void reset() {
        stop(); // Ensure it's stopped before resetting
        accSeconds = 0.0;
        lastGallons = 0.0;
        lastTotal = 0.0;
        sendPort(updateMessage(0, 0));
        System.out.println("Flow meter reset.");
    }

    private String updateMessage(double gallons, double total) {
        return "t:2.5/s:2/st:1/c:0/;"
                + "t:3/s:3/st:2/c:0/" + G.format(gallons) + " gal;"
                + "t:5/s:3/st:2/c:0/" + $.format(total) + ";";
    }

    private void sendPort(String s) {
        if (!s.endsWith("//")) s += "//";
        flowPort.send(new Message(s));
    }

    /**
     * A JavaFX UI for visualizing the internal state of the FlowMeter.
     */
    public static class StatusUI extends Application {
        private static volatile FlowMeter bound;
        private Label stateLbl, galLbl, totLbl, rateLbl, gasLbl;
        private Rectangle pipeFlow;
        private double flowOffset = 0;

        public static void bind(FlowMeter fm) {
            bound = fm;
        }

        @Override
        public void start(Stage stage) {
            stateLbl = new Label("Waiting...");
            galLbl = new Label("Gallons: 0.000");
            totLbl = new Label("Total:   $0.000");
            gasLbl = new Label("Gas: ");
            rateLbl = new Label("Rate: " + G.format(FLOW_RATE_GPS) + " gal/s");
            // ... (UI styles and layout setup) ...
            VBox root = setupUILayout(); // Encapsulated UI setup
            stage.setTitle("FlowMeter Status");
            stage.setScene(new Scene(root, 420, 200));
            stage.show();
            setupAnimationTimer(); // Start the UI update loop
        }

        private VBox setupUILayout() {
            stateLbl.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
            galLbl.setStyle("-fx-font-size: 16px;");
            totLbl.setStyle("-fx-font-size: 16px;");
            gasLbl.setStyle("-fx-font-size: 16px;");
            rateLbl.setStyle("-fx-font-size: 16px;");

            Rectangle pipeBase = new Rectangle(400, 32, Color.GRAY);
            pipeFlow = new Rectangle(150, 32, Color.GOLD);
            StackPane pipePane = new StackPane(pipeBase, pipeFlow);
            Rectangle clip = new Rectangle(400, 32);
            pipePane.setClip(clip);

            VBox root = new VBox(10, stateLbl, pipePane, new HBox(8, galLbl), new HBox(8, totLbl), new HBox(8, gasLbl), new HBox(8, rateLbl));
            root.setPadding(new Insets(12));
            root.setAlignment(Pos.CENTER_LEFT);
            return root;
        }

        private void setupAnimationTimer() {
            new AnimationTimer() {
                @Override
                public void handle(long now) {
                    if (bound == null) return;
                    // Reflect the bound device's current status in the UI
                    stateLbl.setText(bound.running ? "FLOWING" : "STOPPED");
                    stateLbl.setStyle(bound.running ? "-fx-text-fill: green; -fx-font-size: 18px; -fx-font-weight: bold;" : "-fx-text-fill: red; -fx-font-size: 18px; -fx-font-weight: bold;");
                    galLbl.setText("Gallons: " + G.format(bound.lastGallons));
                    totLbl.setText("Total:   " + $.format(bound.lastTotal));
                    gasLbl.setText("Gas: " + bound.gasType);
                    rateLbl.setText("Rate: " + G.format(FLOW_RATE_GPS) + " gal/s  @  " + $.format(bound.pricePerGallon) + "/gal");
                    pipeFlow.setVisible(bound.running);
                    if (bound.running) {
                        flowOffset = (flowOffset + 2) % 400;
                        pipeFlow.setTranslateX(-160 + flowOffset);
                    }
                }
            }.start();
        }
    }
}

