package Devices;

import java.text.DecimalFormat;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.Random;

public class FlowMeter {
    private final double rateGalPerSec;
    private final double pricePerGallon;
    private final ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
    private final Consumer<String> emit;
    private final Random rand = new Random();
    private java.util.function.Consumer<Boolean> onStop; // true = auto-stop, false = manual stop

    private boolean running = false;
    private long lastStartNanos;
    private double accSeconds = 0.0;

    private static final DecimalFormat G = new DecimalFormat("0.000");
    private static final DecimalFormat $ = new DecimalFormat("$0.000");

    /**
     * Constructs a Devices.FlowMeter instance.
     *
     * @param emit A Consumer that receives display messages (typically for UI updates).
     * @param gps The flow rate in gallons per second.
     * @param ppg The price per gallon.
     */
    public FlowMeter(Consumer<String> emit, double gps, double ppg) {
        this.emit = emit;
        this.rateGalPerSec = gps;
        this.pricePerGallon = ppg;
    }

    /** Optional: set a callback invoked when pumping stops. Argument is true for auto-stop, false for manual/cancel. */
    public void setOnStop(java.util.function.Consumer<Boolean> onStop) {
        this.onStop = onStop;
    }

    /**
     * Initializes the display layout for the flow meter,
     * setting initial values for gallons and price.
     */
    public void initLayout() {
        send("t:1/s:3/st:2/c:0/0.000 gal;"   // cell 1 = gallons (big + bold)
                + "t:3/s:3/st:2/c:0/$0.000;");   // cell 3 = price   (big + bold)
    }

    /**
     * Starts the flow meter simulation, causing it to begin tracking the elapsed time,
     * gallons dispensed, and total price. If already running, does nothing.
     */
    public void start() {
        if (running) return;
        running = true;
        lastStartNanos = System.nanoTime();
        exec.scheduleAtFixedRate(this::tick, 0, 100, TimeUnit.MILLISECONDS);
        // Demo: auto-stop after a random duration (simulates pump sensor ending)
        int stopSeconds = 5 + rand.nextInt(11); // 5–15 seconds
        exec.schedule(() -> { if (running) stop(true); }, stopSeconds, TimeUnit.SECONDS);
    }

    /**
     * Pauses the flow meter simulation, accumulating the elapsed time so far.
     * If not running, does nothing.
     */
    public void pause() { stop(false); }

    /**
     * Stops the flow meter, finalizes elapsed time, and triggers an optional onStop callback.
     * @param auto true if this was an automatic/random stop; false if manual (e.g., cancel)
     */
    public void stop(boolean auto) {
        if (!running) return;
        accSeconds += (System.nanoTime() - lastStartNanos) / 1e9;
        running = false;
        if (onStop != null) {
            try { onStop.accept(auto); } catch (Exception ignored) {}
        }
    }

    /**
     * Resets the flow meter, clearing all accumulated time, gallons, and price,
     * and updates the display to show zero values.
     */
    public void reset() {
        running = false;
        accSeconds = 0.0;
        send(updateMessage(0, 0));
    }

    /**
     * Periodically called to update the display with the latest gallons dispensed and total price.
     * Does nothing if the flow meter is not running.
     */
    private void tick() {
        if (!running) return;
        double elapsed = accSeconds + (System.nanoTime() - lastStartNanos) / 1e9;
        double gallons = elapsed * rateGalPerSec;
        double total = gallons * pricePerGallon;
        send(updateMessage(gallons, total));
    }

    /**
     * Creates a formatted display message showing the specified gallons and total price.
     * The message string format uses codes for display cell configuration:
     * - t: text cell ID
     * - s: size (1 small, 2 medium, 3 large)
     * - st: style (1 regular, 2 bold, 3 italic)
     * - c: color (0 default, 1 purple, 2 red, 3 green, 4 blue)
     * - followed by the display text (gallons or price)
     *
     * @param gallons The number of gallons dispensed.
     * @param total The total price for the dispensed gallons.
     * @return A formatted string suitable for display.
     */
    private String updateMessage(double gallons, double total) {
        return "t:1/s:3/st:2/c:0/" + G.format(gallons) + " gal;"
                + "t:3/s:3/st:2/c:0/" + $.format(total)   + ";";
    }

    /**
     * Sends a message to the display consumer.
     *
     * @param msg The message to send.
     */
    private void send(String msg) { emit.accept(msg); }
}