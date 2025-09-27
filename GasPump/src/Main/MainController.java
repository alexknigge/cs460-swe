package Main;

import Main.DeviceManagers.*;

import java.util.List;

/**
 * The main controller for the gas pump.
 * This class implements the Finite State Machine (FSM) described in the SRS.
 * It orchestrates all the manager classes to control the pump's state,
 * handle user interactions, and manage the complete fueling process.
 */
public class MainController {

    // --- Managers ---
    private final BankManager bankManager;
    private final CustomerManager customerManager;
    private final GasStationManager gasStationManager;
    private final PumpAssemblyManager pumpAssemblyManager;
    private final ScreenManager screenManager;
    private final TimerManager timerManager;

    // --- FSM and Session State ---
    private PumpState currentState;
    private List<FuelGrade> availableFuelGrades;
    private String currentCardNumber;
    private FuelGrade selectedFuelGrade;
    private double gallonsDispensed;
    private double totalCost;

    public MainController() {
        // Initialize all the manager components
        this.bankManager = new BankManager();
        this.customerManager = new CustomerManager();
        this.gasStationManager = new GasStationManager();
        this.pumpAssemblyManager = new PumpAssemblyManager();
        this.screenManager = new ScreenManager();
        this.timerManager = new TimerManager();
    }

    /**
     * The main entry point for the entire Gas Pump application.
     */
    public static void main(String[] args) {
        MainController controller = new MainController();
        controller.run();
    }

    // --- State Handler Methods ---

    /**
     * Starts the main loop of the gas pump controller.
     */
    public void run() {
        System.out.println("Gas Pump Controller starting up...");
        // The initial state as per the SRS diagram is OFF.
        currentState = PumpState.OFF;

        try {
            // The main FSM loop
            while (true) {
                // The logic for each state is handled in a separate method for clarity.
                switch (currentState) {
                    case OFF -> handleOffState();
                    case STANDBY -> handleStandbyState();
                    case IDLE -> handleIdleState();
                    case WAITING_FOR_AUTHORIZATION ->
                            handleWaitingForAuthorizationState();
                    case NO_AUTHORIZATION -> handleNoAuthorizationState();
                    case SELECT_GAS -> handleSelectGasState();
                    case READY_TO_PUMP -> handleReadyToPumpState();
                    case FUELING -> handleFuelingState();
                    case PAUSED -> handlePausedState();
                    case TRANSACTION_COMPLETE ->
                            handleTransactionCompleteState();
                }
                // A brief pause to prevent the loop from consuming 100% CPU
                Thread.sleep(100);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Main controller loop was interrupted.");
        } finally {
            // Clean up connections on exit
            bankManager.close();
            customerManager.close();
            gasStationManager.close();
            pumpAssemblyManager.close();
            screenManager.close();
        }
    }

    private void handleOffState() {
        screenManager.showMessage("Pump Unavailable");
        // In a real system, we'd check for a signal from the gas station server.
        // For simulation, we'll assume it's always "on" and move to STANDBY.
        currentState = PumpState.STANDBY;
    }

    private void handleStandbyState() {
        availableFuelGrades = gasStationManager.getAvailableFuelGrades();
        if (availableFuelGrades != null && !availableFuelGrades.isEmpty()) {
            System.out.println("Successfully fetched price list.");
            currentState = PumpState.IDLE;
        } else {
            System.err.println("Failed to fetch price list. Remaining in STANDBY.");
            // In a real system, it would retry after a delay.
        }
    }

    private void handleIdleState() {
        resetSession();
        screenManager.showWelcomeScreen();
        // The waitForCardTap call is blocking, so we pass a very long timeout.
        // The state won't advance until a card is tapped.
        String card = customerManager.waitForCardTap(Long.MAX_VALUE);
        if (card != null) {
            currentCardNumber = card;
            currentState = PumpState.WAITING_FOR_AUTHORIZATION;
        }
    }

    private void handleWaitingForAuthorizationState() {
        screenManager.showAuthorizingScreen();
        BankManager.AuthorizationStatus status = bankManager.authorizeCreditCard(currentCardNumber);
        customerManager.notifyCardReader(status == BankManager.AuthorizationStatus.APPROVED);

        if (status == BankManager.AuthorizationStatus.APPROVED) {
            currentState = PumpState.SELECT_GAS;
        } else {
            currentState = PumpState.NO_AUTHORIZATION;
        }
    }

    private void handleNoAuthorizationState() {
        screenManager.showMessage("Authorization Failed");
        try {
            Thread.sleep(5000); // Display message for 5 seconds
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        currentState = PumpState.IDLE;
    }

    private void handleSelectGasState() {
        screenManager.showGradeSelectionScreen(availableFuelGrades);
        timerManager.setTimer(15);

        while (!timerManager.isTimedOut()) {
            String buttonId = screenManager.waitForButtonPress(100); // Poll for 100ms
            if (buttonId != null) {
                // Check if the pressed button corresponds to a fuel grade
                int gradeIndex = Integer.parseInt(buttonId) - 2; // Assuming buttons 2, 3, 4 map to grades 0, 1, 2
                if (gradeIndex >= 0 && gradeIndex < availableFuelGrades.size()) {
                    selectedFuelGrade = availableFuelGrades.get(gradeIndex);
                    timerManager.resetTimer();
                    currentState = PumpState.READY_TO_PUMP;
                    return;
                } else if (buttonId.equals("8")) { // Cancel button
                    timerManager.resetTimer();
                    currentState = PumpState.IDLE;
                    return;
                }
            }
        }
        // If the loop finishes, it means a timeout occurred
        currentState = PumpState.IDLE;
    }

    private void handleReadyToPumpState() {
        screenManager.showMessage("Ready to Pump. Please remove nozzle from holster.");
        timerManager.setTimer(15);
        while (!timerManager.isTimedOut()) {
            PumpAssemblyManager.HoseEvent event = pumpAssemblyManager.getHoseEvent();
            if (event == PumpAssemblyManager.HoseEvent.REMOVED) {
                timerManager.resetTimer();
                currentState = PumpState.FUELING;
                return;
            }
        }
        // If timeout occurs, go back to IDLE
        currentState = PumpState.IDLE;
    }

    private void handleFuelingState() {
        pumpAssemblyManager.startPumping(selectedFuelGrade);
        screenManager.showPumpingScreen(selectedFuelGrade.name(), 0, 0);

        while (true) {
            // Check for hose events first
            PumpAssemblyManager.HoseEvent hoseEvent = pumpAssemblyManager.getHoseEvent();
            if (hoseEvent == PumpAssemblyManager.HoseEvent.ATTACHED || hoseEvent == PumpAssemblyManager.HoseEvent.TANK_FULL) {
                pumpAssemblyManager.stopPumping();
                currentState = PumpState.TRANSACTION_COMPLETE;
                return;
            }

            // Check for fueling updates
            FuelingUpdate update = pumpAssemblyManager.getFuelingUpdate();
            if (update != null) {
                gallonsDispensed = update.gallons();
                totalCost = update.totalCost();
                screenManager.showPumpingScreen(selectedFuelGrade.name(), gallonsDispensed, totalCost);
            }

            // Check for stop button press
            String buttonId = screenManager.waitForButtonPress(100);
            if (buttonId != null && buttonId.equals("8")) { // Stop button
                pumpAssemblyManager.stopPumping();
                currentState = PumpState.TRANSACTION_COMPLETE;
                return;
            }
        }
    }

    private void handlePausedState() {
        // As per the SRS, this state would handle pausing and resuming.
        // For this implementation, we simplify and go directly to completion.
        currentState = PumpState.TRANSACTION_COMPLETE;
    }

    private void handleTransactionCompleteState() {
        System.out.printf("Transaction complete. Charging card %s for $%.2f%n", currentCardNumber, totalCost);
        boolean chargeSuccess = bankManager.chargeCreditCard(currentCardNumber, totalCost);

        if (chargeSuccess) {
            gasStationManager.logTransaction(currentCardNumber, selectedFuelGrade, gallonsDispensed, totalCost);
            screenManager.showThankYouScreen(gallonsDispensed, totalCost);
        } else {
            screenManager.showMessage("Final charge failed. Please see attendant.");
        }

        try {
            Thread.sleep(10000); // Display final screen for 10 seconds
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        currentState = PumpState.IDLE;
    }

    /**
     * Resets all session-specific variables to their default state.
     */
    private void resetSession() {
        currentCardNumber = null;
        selectedFuelGrade = null;
        gallonsDispensed = 0.0;
        totalCost = 0.0;
        timerManager.resetTimer();
    }
}

