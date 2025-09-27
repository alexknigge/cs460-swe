package Main.DeviceManagers;

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
    private final IOPort hoseConnection;

    /**
     * Initializes the manager and establishes connections to the pump, flow meter, and hose devices.
     */
    public PumpAssemblyManager() {
        this.pumpConnection = new IOPort(DeviceConstants.PUMP_HOSTNAME, DeviceConstants.PUMP_PORT);
        this.flowMeterConnection = new IOPort(DeviceConstants.FLOW_METER_HOSTNAME, DeviceConstants.FLOW_METER_PORT);
        this.hoseConnection = new IOPort(DeviceConstants.HOSE_HOSTNAME, DeviceConstants.HOSE_PORT);
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
        pumpConnection.send(new Message("on"));
        String startCommand = String.format("CMD:START ppg=%.2f gas=%s//", grade.pricePerGallon(), grade.name());
        flowMeterConnection.send(new Message(startCommand));
    }

    /**
     * Stops the fueling process.
     * Sends commands to turn off the pump motor and pause the flow meter.
     */
    public void stopPumping() {
        System.out.println("Pump Assembly: Stopping pump.");
        pumpConnection.send(new Message("off"));
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

    /**
     * Checks for and processes events from the hose sensors.
     * This is a non-blocking method that polls for new messages.
     *
     * @return A {@link HoseEvent} enum representing the latest event from the hose,
     * or {@code null} if no new event has occurred.
     */
    public HoseEvent getHoseEvent() {
        Message eventMessage = hoseConnection.get();
        if (eventMessage != null) {
            String content = eventMessage.getContent().replace("//", "").trim();
            return switch (content) {
                case "removed" -> HoseEvent.REMOVED;
                case "attached" -> HoseEvent.ATTACHED;
                case "tank-full" -> HoseEvent.TANK_FULL;
                default -> null;
            };
        }
        return null;
    }

    /**
     * Closes connections to all pump assembly devices.
     */
    public void close() {
        pumpConnection.close();
        flowMeterConnection.close();
        hoseConnection.close();
    }

    /**
     * Represents the possible events received from the hose sensors.
     */
    public enum HoseEvent {
        REMOVED,  // Nozzle removed from the pump holster
        ATTACHED, // Nozzle returned to the pump holster
        TANK_FULL // Vehicle tank is full
    }
}
