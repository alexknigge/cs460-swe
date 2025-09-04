package SmallDevices;

import Server.IOPort;
import Server.Message;

import java.text.DecimalFormat;
import java.util.Random;
import java.util.concurrent.*;
import java.util.function.Consumer;

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
    private final IOPort flowPort = new IOPort("flowMeterToMain");
    private volatile double rateGalPerSec;
    private volatile double pricePerGallon;
    private final ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
    private final Random rand = new Random();
    private java.util.function.Consumer<Boolean> onStop;
    private boolean running = false;
    private long lastStartNanos;
    private double accSeconds = 0.0;

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
        send("t:3/s:3/st:2/c:0/0.000 gal;"   // row 1 down -> now cell 3
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
        if (onStop != null) {
            try { onStop.accept(auto); } catch (Exception ignored) {}
        }
    }

    /** reset counters and UI back to zero */
    public void reset() {
        running = false;
        accSeconds = 0.0;
        send(updateMessage(0, 0));
        sendPort("FLOW:RESET");
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

        send(updateMessage(gallons, total));
        sendPort("FLOW:TICK gallons=" + G.format(gallons) +
                " total=" + $.format(total).substring(1));
    }

    /**
     * Polls the IOPort for incoming messages in a non-blocking manner.
     * Drains the message queue and forwards each command string to handlePortCommand.
     */
    private void pollCommands() {
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
        if (flowPort != null) flowPort.send(new Message(s));
    }

    /**
     * Main entry point for running FlowMeter standalone for test/demo.
     */
    public static void main(String[] args){
        // Dummy message to print to console
        FlowMeter flow = new FlowMeter(msg -> System.out.println("[UI] " + msg), 0.1, 3.25);
        flow.initLayout();
    }
}