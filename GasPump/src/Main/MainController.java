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
        this.timerManager = new TimerManager();
    }

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
        currentState = PumpState.OFF;

        try {
            while (true) {
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
        }
    }

    private void handleOffState() {
        customerManager.showMessage("Pump Unavailable");
        currentState = PumpState.STANDBY;
    }

    private void handleStandbyState() {
        availableFuelGrades = gasStationManager.getAvailableFuelGrades();
        if (availableFuelGrades != null && !availableFuelGrades.isEmpty()) {
            System.out.println("Successfully fetched price list.");
            currentState = PumpState.IDLE;
        } else {
            System.err.println("Failed to fetch price list. Remaining in STANDBY.");
        }
    }

    private void handleIdleState() {
        resetSession();
        customerManager.showWelcomeScreen();
        String card = customerManager.waitForCardTap(Long.MAX_VALUE);
        if (card != null) {
            currentCardNumber = card;
            currentState = PumpState.WAITING_FOR_AUTHORIZATION;
        }
    }

    private void handleWaitingForAuthorizationState() {
        customerManager.showAuthorizingScreen();
        BankManager.AuthorizationStatus status = bankManager.authorizeCreditCard(currentCardNumber);
        customerManager.notifyCardReader(status == BankManager.AuthorizationStatus.APPROVED);

        if (status == BankManager.AuthorizationStatus.APPROVED) {
            currentState = PumpState.SELECT_GAS;
        } else {
            currentState = PumpState.NO_AUTHORIZATION;
        }
    }

    private void handleNoAuthorizationState() {
        customerManager.showMessage("Authorization Failed");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        currentState = PumpState.IDLE;
    }

    private void handleSelectGasState() {
        customerManager.showGradeSelectionScreen(availableFuelGrades);
        timerManager.setTimer(15);

        while (!timerManager.isTimedOut()) {
            String buttonId = customerManager.waitForButtonPress(100);
            if (buttonId != null) {
                try {
                    int gradeIndex = Integer.parseInt(buttonId) - 2;
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
                } catch (NumberFormatException e) {
                    // Ignore non-numeric button IDs
                }
            }
        }
        currentState = PumpState.IDLE;
    }

    private void handleReadyToPumpState() {
        customerManager.showMessage("Ready to Pump. Please remove nozzle from holster.");
        timerManager.setTimer(15);
        while (!timerManager.isTimedOut()) {
            PumpAssemblyManager.HoseEvent event = pumpAssemblyManager.getHoseEvent();
            if (event == PumpAssemblyManager.HoseEvent.REMOVED) {
                timerManager.resetTimer();
                currentState = PumpState.FUELING;
                return;
            }
        }
        currentState = PumpState.IDLE;
    }

    private void handleFuelingState() {
        pumpAssemblyManager.startPumping(selectedFuelGrade);
        customerManager.showPumpingScreen(selectedFuelGrade.name(), 0, 0);

        while (true) {
            PumpAssemblyManager.HoseEvent hoseEvent = pumpAssemblyManager.getHoseEvent();
            
            if (hoseEvent == PumpAssemblyManager.HoseEvent.TANK_FULL) {
                pumpAssemblyManager.stopPumping();
                currentState = PumpState.TRANSACTION_COMPLETE;
                return;
            }
            if (hoseEvent == PumpAssemblyManager.HoseEvent.ATTACHED) {
                currentState = PumpState.PAUSED;
                return;
            }

            FuelingUpdate update = pumpAssemblyManager.getFuelingUpdate();
            if (update != null) {
                gallonsDispensed = update.gallons();
                totalCost = update.totalCost();
                customerManager.showPumpingScreen(selectedFuelGrade.name(), gallonsDispensed, totalCost);
            }

            String buttonId = customerManager.waitForButtonPress(100);
            if (buttonId != null && buttonId.equals("8")) { // Stop button
                pumpAssemblyManager.stopPumping();
                currentState = PumpState.TRANSACTION_COMPLETE;
                return;
            }
        }
    }

    private void handlePausedState() {
        pumpAssemblyManager.pausePumping();
        customerManager.showMessage("Fueling paused — reconnect within 15 seconds");
        timerManager.setTimer(15);
        
        while (!timerManager.isTimedOut()) {
            PumpAssemblyManager.HoseEvent evt = pumpAssemblyManager.getHoseEvent();

            if (evt == PumpAssemblyManager.HoseEvent.REMOVED) {
                currentState = PumpState.FUELING;
                return;
            }
            
            try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
        }
        
        // Timeout: end the transaction with the partial amount
        pumpAssemblyManager.stopPumping();
        currentState = PumpState.TRANSACTION_COMPLETE;
    }

    private void handleTransactionCompleteState() {
        System.out.printf("Transaction complete. Charging card %s for $%.2f%n", currentCardNumber, totalCost);
        boolean chargeSuccess = bankManager.chargeCreditCard(currentCardNumber, totalCost);

        if (chargeSuccess) {
            gasStationManager.logTransaction(currentCardNumber, selectedFuelGrade, gallonsDispensed, totalCost);
            customerManager.showThankYouScreen(gallonsDispensed, totalCost);
        } else {
            customerManager.showMessage("Final charge failed. Please see attendant.");
        }

        // Reset the flow meter for the next customer.
        pumpAssemblyManager.resetFlowMeter();

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        currentState = PumpState.IDLE;
    }

    private void resetSession() {
        currentCardNumber = null;
        selectedFuelGrade = null;
        gallonsDispensed = 0.0;
        totalCost = 0.0;
        timerManager.resetTimer();
    }
}

