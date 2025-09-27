package Main;

/**
 * A non-blocking timer manager for handling inactivity timeouts in the gas pump's state machine.
 * This class allows the main control loop to set a timer for a specific duration and then
 * periodically check if that timer has expired, without halting execution.
 */
public class TimerManager {

    private long startTimeMillis;
    private long durationMillis;
    private boolean isRunning;

    /**
     * Initializes a new TimerManager.
     */
    public TimerManager() {
        this.isRunning = false;
        this.startTimeMillis = 0;
        this.durationMillis = 0;
    }

    /**
     * Sets and starts a new timer for a specified duration.
     * If a timer is already running, it will be reset and started with the new duration.
     *
     * @param seconds The duration of the timer in seconds. Must be a positive integer.
     */
    public void setTimer(int seconds) {
        if (seconds <= 0) {
            System.err.println("Timer duration must be positive.");
            return;
        }
        this.durationMillis = seconds * 1000L;
        this.startTimeMillis = System.currentTimeMillis();
        this.isRunning = true;
        System.out.println("Timer set for " + seconds + " seconds.");
    }

    /**
     * Resets and stops the current timer.
     * If no timer is running, this method does nothing.
     */
    public void resetTimer() {
        if (isRunning) {
            this.isRunning = false;
            this.startTimeMillis = 0;
            System.out.println("Timer reset.");
        }
    }

    /**
     * Checks if the currently running timer has expired.
     * This is a non-blocking check. If the timer has expired, this method will
     * continue to return true on subsequent calls until the timer is reset.
     *
     * @return {@code true} if a timer is running and its duration has elapsed, {@code false} otherwise.
     */
    public boolean isTimedOut() {
        if (!isRunning) {
            return false;
        }

        long elapsed = System.currentTimeMillis() - startTimeMillis;
        if (elapsed >= durationMillis) {
            System.out.println("Timer timed out after " + (durationMillis / 1000) + " seconds.");
            // We keep isRunning = true so that it continuously reports as timed out
            // until it is explicitly reset. This prevents race conditions in the main loop.
            return true;
        }

        return false;
    }

    /**
     * Checks if a timer is currently active.
     *
     * @return {@code true} if the timer has been set and is running, {@code false} otherwise.
     */
    public boolean isRunning() {
        return isRunning;
    }
}
