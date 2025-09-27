package Main;

import Server.DeviceConstants;
import Server.IOPort;
import Server.Message;

/**
 * Manages the physical components of the fuel dispensing system.
 * This class abstracts the low-level control of the pump motor, flow meter,
 * and hose sensors, providing a simple, unified interface for the main
 * control logic to start, stop, and monitor the fueling process.
 */
public class PumpAssemblyManager {

    private final IOPort pumpConnection;
    private final IOPort flowMeterConnection;
    // private IOPort hoseConnection; // Placeholder for future hose sensor integration

    /**
     * Initializes the manager and establishes connections to the pump and flow meter devices.
     */
    public PumpAssemblyManager() {
        this.pumpConnection = new IOPort(DeviceConstants.PUMP_HOSTNAME, DeviceConstants.PUMP_PORT);
        this.flowMeterConnection = new IOPort(DeviceConstants.FLOW_METER_HOSTNAME, DeviceConstants.FLOW_METER_PORT);
        // this.hoseConnection = new IOPort("localhost", HOSE_PORT); // Example
    }

    /**
     * Starts the fueling process.
     * This method sends the command to turn on the pump motor and instructs the
     * flow meter to begin measuring the dispensed fuel.
     *
     * @param grade The selected {@link FuelGrade} to be dispensed.
     */
    public void startPumping(FuelGrade grade) {
        System.out.println("Pump Assembly: Starting pump for " + grade.name());
        // Command the physical pump motor to turn on.
        pumpConnection.send(new Message("on"));

        // Command the flow meter to start its measurement cycle.
        // The FlowMeter simulation uses CMD:START with parameters for rate, ppg, and gas type.
        // For this simulation, we'll use the price from the FuelGrade object.
        String startCommand = String.format("CMD:START ppg=%.2f gas=%s//",
                grade.pricePerGallon(), grade.name());
        flowMeterConnection.send(new Message(startCommand));
    }

    /**
     * Stops the fueling process.
     * Sends commands to turn off the pump motor and pause the flow meter.
     */
    public void stopPumping() {
        System.out.println("Pump Assembly: Stopping pump.");
        // Turn off the physical pump motor.
        pumpConnection.send(new Message("off"));
        // Pause the flow meter's counting.
        flowMeterConnection.send(new Message("CMD:PAUSE//"));
    }

    /**
     * Checks for and processes real-time updates from the flow meter.
     * This is a non-blocking method that polls for new messages.
     *
     * @return A {@link FuelingUpdate} containing the latest gallons and total cost,
     * or {@code null} if no new update is available.
     */
    public FuelingUpdate getFuelingUpdate() {
        Message updateMessage = flowMeterConnection.get();
        if (updateMessage != null) {
            return FuelingUpdate.parseFrom(updateMessage.getContent());
        }
        return null;
    }

    // --- Placeholder methods for hose sensor integration ---

    /**
     * Checks if the hose is properly attached to the vehicle.
     * NOTE: This is a placeholder. The Hose simulation currently has no network interface.
     *
     * @return Always returns {@code true} in the current simulation.
     */
    public boolean isHoseAttached() {
        // Future logic: Read from hoseConnection and return status.
        return true;
    }

    /**
     * Checks if the vehicle's tank is full.
     * NOTE: This is a placeholder. The Hose/Tank simulation currently has no network interface.
     *
     * @return Always returns {@code false} in the current simulation.
     */
    public boolean isTankFull() {
        // Future logic: Read a "tank-full" message from a sensor.
        return false;
    }


    /**
     * Closes connections to all pump assembly devices.
     */
    public void close() {
        pumpConnection.close();
        flowMeterConnection.close();
    }
}
