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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * FlowMeter simulates a fuel pump by computing the number of gallons dispensed and the total price based on elapsed time.
 * As per SRS 6.3, the flow rate is fixed and internal to this device. This version includes a StatusUI for visualization.
 */
public class FlowMeter {
    // The flow rate is fixed as per SRS 6.3.
    private static final double FLOW_RATE_GPS = 0.15; // Gallons Per Second (equivalent to 9 gal/min)
    // --- Formatters ---
    private static final DecimalFormat GALLONS_FORMAT = new DecimalFormat("0.000");
    private static final DecimalFormat COST_FORMAT = new DecimalFormat("$0.00");
    private final IOPortServer flowPort = new IOPortServer(DeviceConstants.FLOW_METER_PORT);
    // --- State Variables ---
    private final AtomicReference<Double> pricePerGallon = new AtomicReference<>(4.59);
    private final AtomicReference<String> gasType = new AtomicReference<>("");
    private final AtomicBoolean isFueling = new AtomicBoolean(false);
    private long lastStartNanos;
    private double accumulatedSeconds = 0.0;
    private volatile double lastGallons = 0.0;
    private volatile double lastTotal = 0.0;

    /**
     * Initializes the FlowMeter device.
     */
    public FlowMeter() {
        // Start the background thread for command polling and ticking.
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(this::runCycle, 0, 100, TimeUnit.MILLISECONDS);
    }

    private static double parsePriceFromCommand(String command, double defaultValue) {
        final String key = "ppg=";
        int index = command.indexOf(key);
        if (index < 0) return defaultValue;
        int start = index + key.length();
        int end = start;
        while (end < command.length() && (Character.isDigit(command.charAt(end)) || command.charAt(end) == '.'))
            end++;
        try {
            return Double.parseDouble(command.substring(start, end));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Main entry point for running the FlowMeter device with its status UI.
     */
    public static void main(String[] args) {
        StatusUI.bind(new FlowMeter());
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
        Message message = flowPort.get();
        if (message != null) {
            String content = message.getContent();
            if (content != null) {
                handlePortCommand(content.trim());
            }
        }
    }

    /**
     * If the pump is running, calculates the current gallons and total cost and sends an update.
     */
    private void tick() {
        if (!isFueling.get()) return;
        double elapsed = accumulatedSeconds + (System.nanoTime() - lastStartNanos) / 1e9;
        double gallons = elapsed * FLOW_RATE_GPS; // Use the fixed internal rate
        double total = gallons * pricePerGallon.get();
        lastGallons = gallons;
        lastTotal = total;
        sendPort(updateMessage(gallons, total));
    }

    /**
     * Parses and executes commands received from the main controller.
     *
     * @param command The command string from the controller.
     */
    private void handlePortCommand(String command) {
        if (command.startsWith("CMD:START")) {
            pricePerGallon.set(parsePriceFromCommand(command, pricePerGallon.get()));
            int gasIndex = command.indexOf("gas=");
            if (gasIndex != -1) {
                gasType.set(command.substring(gasIndex + 4).replace("//", "").trim());
            }
            start();
        } else if (command.equals("CMD:PAUSE//")) {
            stop();
        } else if (command.equals("CMD:RESET//")) {
            reset();
        }
    }

    /**
     * Starts the fueling process.
     */
    public void start() {
        if (isFueling.compareAndSet(false, true)) {
            lastStartNanos = System.nanoTime();
            System.out.println("Flow meter started.");
        }
    }

    /**
     * Stops the fueling process.
     */
    public void stop() {
        if (isFueling.compareAndSet(true, false)) {
            accumulatedSeconds += (System.nanoTime() - lastStartNanos) / 1e9;
            System.out.println("Flow meter stopped.");
        }
    }

    /**
     * Resets the flow meter's counters and state for the next transaction.
     */
    public void reset() {
        stop(); // Ensure it's stopped before resetting
        accumulatedSeconds = 0.0;
        lastGallons = 0.0;
        lastTotal = 0.0;
        sendPort(updateMessage(0, 0));
        System.out.println("Flow meter reset.");
    }

    private String updateMessage(double gallons, double total) {
        return "t:2.5/s:2/st:1/c:0/;"
                + "t:3/s:3/st:2/c:0/" + GALLONS_FORMAT.format(gallons) + " gal;"
                + "t:5/s:3/st:2/c:0/" + COST_FORMAT.format(total) + ";";
    }

    private void sendPort(String s) {
        if (!s.endsWith("//")) s += "//";
        flowPort.send(new Message(s));
    }

    /**
     * A JavaFX UI for visualizing the internal state of the FlowMeter.
     */
    public static class StatusUI extends Application {
        private static volatile FlowMeter boundFlowMeter;

        public static void bind(FlowMeter flowMeter) {
            boundFlowMeter = flowMeter;
        }

        @Override
        public void start(Stage stage) {
            VBox root = new VBox(10);
            root.setPadding(new Insets(12));
            root.setAlignment(Pos.CENTER_LEFT);

            Label statusLabel = new Label("Waiting...");
            Label gallonsLabel = new Label("Gallons: 0.000");
            Label totalLabel = new Label("Total:   $0.000");
            Label gasTypeLabel = new Label("Gas: ");
            Label rateLabel = new Label("Rate: " + GALLONS_FORMAT.format(FLOW_RATE_GPS) + " gal/s");

            final String labelStyle = "-fx-font-size: 16px;";
            gallonsLabel.setStyle(labelStyle);
            totalLabel.setStyle(labelStyle);
            gasTypeLabel.setStyle(labelStyle);
            rateLabel.setStyle(labelStyle);

            Rectangle pipeBase = new Rectangle(400, 32, Color.GRAY);
            Rectangle pipeFlow = new Rectangle(150, 32, Color.GOLD);
            StackPane pipePane = new StackPane(pipeBase, pipeFlow);
            Rectangle clip = new Rectangle(400, 32);
            pipePane.setClip(clip);

            root.getChildren().addAll(statusLabel, pipePane, new HBox(8, gallonsLabel), new HBox(8, totalLabel), new HBox(8, gasTypeLabel), new HBox(8, rateLabel));

            stage.setTitle("FlowMeter Status");
            stage.setScene(new Scene(root, 420, 200));
            stage.show();
            setupAnimationTimer(statusLabel, gallonsLabel, totalLabel, gasTypeLabel, rateLabel, pipeFlow);
        }

        private void setupAnimationTimer(Label statusLabel, Label gallonsLabel, Label totalLabel, Label gasTypeLabel, Label rateLabel, Rectangle pipeFlow) {
            final String statusBaseStyle = "-fx-font-size: 18px; -fx-font-weight: bold;";

            new AnimationTimer() {
                private double flowOffset = 0;

                @Override
                public void handle(long now) {
                    if (boundFlowMeter == null) return;

                    boolean isFlowing = boundFlowMeter.isFueling.get();
                    statusLabel.setText(isFlowing ? "FLOWING" : "STOPPED");
                    statusLabel.setStyle(statusBaseStyle + (isFlowing ? "-fx-text-fill: green;" : "-fx-text-fill: red;"));

                    gallonsLabel.setText("Gallons: " + GALLONS_FORMAT.format(boundFlowMeter.lastGallons));
                    totalLabel.setText("Total:   " + COST_FORMAT.format(boundFlowMeter.lastTotal));
                    gasTypeLabel.setText("Gas: " + boundFlowMeter.gasType.get());
                    rateLabel.setText("Rate: " + GALLONS_FORMAT.format(FLOW_RATE_GPS) + " gal/s  @  " + COST_FORMAT.format(boundFlowMeter.pricePerGallon.get()) + "/gal");

                    pipeFlow.setVisible(isFlowing);
                    if (isFlowing) {
                        flowOffset = (flowOffset + 2) % 400;
                        pipeFlow.setTranslateX(-160 + flowOffset);
                    }
                }
            }.start();
        }
    }
}

