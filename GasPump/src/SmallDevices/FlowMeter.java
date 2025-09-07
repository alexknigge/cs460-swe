package SmallDevices;

import Server.IOPort;
import Server.Message;
import Server.DeviceMapper;

import java.text.DecimalFormat;
import java.util.Random;
import java.util.concurrent.*;
import java.util.function.Consumer;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.paint.Color;

/**
 * FlowMeter simulates a fuel pump by computing the number of gallons dispensed and the total price based on elapsed time.
 * <p>
 * This class does not directly handle any JavaFX UI components. Instead, it emits formatted protocol strings
 * (e.g., "t:/s:/...") that are consumed by the GasPumpUI for visualization.
 * <p>
 * FlowMeter communicates with the Main application through an IOPort, which orchestrates multiple devices
 * and manages message passing. The messages sent by FlowMeter (such as FLOW:ON, FLOW:TICK, FLOW:STOP, FLOW:RESET)
 * are interpreted by Main and GasPumpUI to update the pump display accordingly.
 * <p>
 * This design cleanly separates backend logic (FlowMeter) from the UI layer (GasPumpUI), allowing FlowMeter
 * to focus solely on the pumping simulation and protocol message emission.
 * <p>
 * This class instantiates its own IOPort using the DeviceMapper ID "flowMeterToMain" (port 1236) rather than wiring it via a helper method.
 */
public class FlowMeter {
    private final Consumer<String> emit;
    private final IOPort flowPort = new IOPort(DeviceMapper.FLOW_METER_TO_MAIN);
    private volatile double rateGalPerSec;
    private volatile double pricePerGallon;
    private final ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
    private final Random rand = new Random(System.currentTimeMillis());
    private java.util.function.Consumer<Boolean> onStop;
    private boolean running = false;
    private long lastStartNanos;
    private double accSeconds = 0.0;

    // --- lightweight status values for the optional embedded GUI ---
    private volatile double lastGallons = 0.0;
    private volatile double lastTotal   = 0.0;

    private static final DecimalFormat G = new DecimalFormat("0.000");
    private static final DecimalFormat $ = new DecimalFormat("$0.000");

    public FlowMeter(Consumer<String> emit, double gps, double ppg) {
        this.emit = emit;
        this.rateGalPerSec = gps;
        this.pricePerGallon = ppg;
    }

    /** optional callback when we stop; true = auto-stop (timer), false = manual/cancel */
    public void setOnStop(java.util.function.Consumer<Boolean> onStop) {
        this.onStop = onStop;
    }

    /** show zeros in the two flow cells */
    public void initLayout() {
        sendPort("t:3/s:3/st:2/c:0/0.000 gal;"   // row 1 down -> now cell 3
                + "t:5/s:3/st:2/c:0/$0.000;");   // and price in cell 5
    }

    /** start pumping; if already running, no-op */
    public void start() {
        if (running) return;
        running = true;
        lastStartNanos = System.nanoTime();
        sendPort("FLOW:ON");

        // 10Hz updates
        exec.scheduleAtFixedRate(this::tick, 0, 100, TimeUnit.MILLISECONDS);

        // demo: random auto-stop (5–15s)
        int stopSeconds = 5 + rand.nextInt(11);
        exec.schedule(() -> { if (running) stop(true); }, stopSeconds, TimeUnit.SECONDS);
    }

    /** pause (manual) */
    public void pause() { stop(false); }

    /** stop pumping and fire optional callback */
    public void stop(boolean auto) {
        if (!running) return;
        accSeconds += (System.nanoTime() - lastStartNanos) / 1e9;
        running = false;
        sendPort("FLOW:STOP reason=" + (auto ? "auto" : "manual"));
        // After stopping, emit a summary screen for ScreenParser
        double gallons = lastGallons;
        double total   = lastTotal;
        String summary =
            "t:01/s:2/st:2/c:0/" + (auto ? "Sale Complete" : "Pumping Stopped") + ";" +
            "t:2/s:2/st:1/c:0/Total Dispensed;" +
            "t:3/s:3/st:2/c:0/" + G.format(gallons) + " gal;" +
            "t:4/s:2/st:1/c:0/Total Price;" +
            "t:5/s:3/st:2/c:0/" + $.format(total) + ";";
        sendPort(summary);
        if (onStop != null) {
            try { onStop.accept(auto); } catch (Exception ignored) {}
        }
        System.out.println("flow stopped");
    }

    /** reset counters and UI back to zero */
    public void reset() {
        running = false;
        accSeconds = 0.0;
        sendPort(updateMessage(0, 0));
        //sendPort("FLOW:RESET");
    }

    /**
     * Called on schedule. We:
     * 1) poll IOPort for any inbound CMD:* (non-blocking)
     * 2) if running, compute gallons/total and emit both UI + FLOW:TICK
     */
    private void tick() {
        // 1) poll inbound messages (no blocking, no threads required)
        pollCommands();

        // 2) update if running
        if (!running) return;
        double elapsed = accSeconds + (System.nanoTime() - lastStartNanos) / 1e9;
        double gallons = elapsed * rateGalPerSec;
        double total   = gallons * pricePerGallon;

        // keep lightweight copies for the embedded status UI
        lastGallons = gallons;
        lastTotal   = total;

        sendPort(updateMessage(gallons, total));
        //sendPort("FLOW:TICK gallons=" + G.format(gallons) +
        //        " total=" + $.format(total).substring(1));
    }

    /**
     * Polls the IOPort for incoming messages in a non-blocking manner.
     * Drains the message queue and forwards each command string to handlePortCommand.
     */
    public void pollCommands() {
        if (flowPort == null) return;
        Message m = flowPort.get();                 // returns null if none
        while (m != null) {
            String msg = m.getContent();
            if (msg != null) handlePortCommand(msg.trim());
            m = flowPort.get();                     // drain queue
        }
    }

    /** understand simple wire commands from Main */
    private void handlePortCommand(String msg) {
        if (msg.startsWith("CMD:START")) {
            // allow optional overrides: rate=<gps> ppg=<price>
            double r = getDoubleParam(msg, "rate", rateGalPerSec);
            double p = getDoubleParam(msg, "ppg",  pricePerGallon);
            rateGalPerSec = r;
            pricePerGallon = p;
            initLayout();
            start();
        } else if (msg.equals("CMD:PAUSE")) {
            stop(false);
        } else if (msg.equals("CMD:RESET")) {
            reset();
        }
    }

    private static double getDoubleParam(String s, String key, double defVal) {
        int i = s.indexOf(key + "=");
        if (i < 0) return defVal;
        int start = i + key.length() + 1;
        int end = start;
        while (end < s.length() && (Character.isDigit(s.charAt(end)) || s.charAt(end) == '.')) end++;
        try { return Double.parseDouble(s.substring(start, end)); }
        catch (Exception e) { return defVal; }
    }

    // updateMessage(...)
    private String updateMessage(double gallons, double total) {
        return "t:3/s:3/st:2/c:0/" + G.format(gallons) + " gal;"
                + "t:5/s:3/st:2/c:0/" + $.format(total)   + ";";
    }

    private void send(String msg) {
        if (emit != null) emit.accept(msg);
    }

    private void sendPort(String s) {
        if (flowPort != null) {
            if (!s.endsWith("//")) s += "//";
            flowPort.send(new Message(s));
        }
    }

    /**
     * Minimal in-file status UI for demo/debug.
     * Lives inside FlowMeter so the device can show it is "flowing" without affecting normal I/O behavior.
     * It polls `pollCommands()` and mirrors the last gallons/total; turns red when stopped.
     */
    public static class StatusUI extends Application {
        private static volatile FlowMeter bound;
        public static void bind(FlowMeter fm) { bound = fm; }

        private Label stateLbl, galLbl, totLbl;
        private Label rateLbl;
        private Rectangle pipeBase, pipeFlow;
        private double flowOffset = 0;

        @Override
        public void start(Stage stage) {
            stateLbl = new Label("Waiting...");
            galLbl   = new Label("Gallons: 0.000");
            totLbl   = new Label("Total:   $0.000");
            rateLbl = new Label("Rate: 0.000 gal/s");

            stateLbl.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
            galLbl.setStyle("-fx-font-size: 16px;");
            totLbl.setStyle("-fx-font-size: 16px;");
            rateLbl.setStyle("-fx-font-size: 16px;");

            // Simple pipe (a gray tube) with gas flowing
            pipeBase = new Rectangle(400, 32, Color.GRAY);
            pipeBase.setArcWidth(12);
            pipeBase.setArcHeight(12);

            pipeFlow = new Rectangle(150, 32, Color.GOLD);
            pipeFlow.setArcWidth(12);
            pipeFlow.setArcHeight(12);

            StackPane pipePane = new StackPane(pipeBase, pipeFlow);
            pipePane.setMaxWidth(400);
            pipePane.setMinWidth(400);
            pipePane.setPrefWidth(400);
            Rectangle clip = new Rectangle(400, 32);
            clip.setArcWidth(12);
            clip.setArcHeight(12);
            pipePane.setClip(clip);

            VBox root = new VBox(10,
                    stateLbl,
                    pipePane,
                    new HBox(8, galLbl),
                    new HBox(8, totLbl),
                    new HBox(8, rateLbl)
            );
            root.setPadding(new Insets(12));
            root.setAlignment(Pos.CENTER_LEFT);

            stage.setTitle("FlowMeter Status");
            stage.setScene(new Scene(root, 280, 140));
            stage.show();

            AnimationTimer timer = new AnimationTimer() {
                @Override public void handle(long now) {
                    FlowMeter fm = bound;
                    if (fm == null) return;

                    // let the device consume any commands from Main
                    fm.pollCommands();

                    // reflect current status
                    if (fm.running) {
                        stateLbl.setText("FLOWING");
                        stateLbl.setStyle("-fx-text-fill: green; -fx-font-size: 18px; -fx-font-weight: bold;");
                    } else {
                        stateLbl.setText("STOPPED");
                        stateLbl.setStyle("-fx-text-fill: red; -fx-font-size: 18px; -fx-font-weight: bold;");
                    }
                    galLbl.setText("Gallons: " + G.format(fm.lastGallons));
                    totLbl.setText("Total:   " + $.format(fm.lastTotal));

                    // animate pipe when flowing
                    if (fm.running) {
                        pipeFlow.setVisible(true);
                        flowOffset += 2; // pixels per frame
                        if (flowOffset > 400) flowOffset = 0;
                        pipeFlow.setTranslateX(-160 + flowOffset);
                    } else {
                        pipeFlow.setVisible(false);
                    }
                    rateLbl.setText("Rate: " + G.format(fm.rateGalPerSec) + " gal/s  @  " + $.format(fm.pricePerGallon) + "/gal");
                }
            };
            timer.start();
        }
    }

    /**
     * Main entry point for running FlowMeter standalone for test/demo.
     * Creates the device, binds the optional StatusUI, and launches JavaFX.
     * NOTE: In the integrated system, Main should send "CMD:START ..." over IOPort; this demo does not auto-start.
     */
    public static void main(String[] args){
        // Device sends screen-protocol messages out over its port to Main; here we also log them for visibility.
        FlowMeter flow = new FlowMeter(msg -> System.out.println("[UI] " + msg), 0.1, 3.25);

        // Bind the optional in-file status window (safe no-op if JavaFX not desired)
        StatusUI.bind(flow);
        javafx.application.Application.launch(StatusUI.class, args);
    }
}