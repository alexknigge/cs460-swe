package Main;

/**
 * Represents all possible states of the Gas Pump's Finite State Machine (FSM).
 * Each enum value corresponds to a state in the SRS state machine diagram.
 */
public enum PumpState {
    OFF,
    STANDBY,
    IDLE,
    WAITING_FOR_AUTHORIZATION,
    NO_AUTHORIZATION,
    SELECT_GAS,
    READY_TO_PUMP,
    FUELING,
    PAUSED,
    TRANSACTION_COMPLETE
}
