package Main.DeviceManagers;

import Server.DeviceConstants;
import Server.IOPort;
import Server.Message;

/**
 * Manages all communication with the bank's server.
 * This class abstracts the low-level network messaging for operations like
 * credit card authorization and final transaction charges. It handles sending
 * requests and waiting for the bank's responses.
 */
public class BankManager {

    private static final long RESPONSE_TIMEOUT_MS = 5000; // 5 seconds
    private final IOPort bankConnection;

    /**
     * Initializes a new BankManager and establishes a dedicated connection to the bank server.
     */
    public BankManager() {
        this.bankConnection = new IOPort(DeviceConstants.BANK_HOSTNAME, DeviceConstants.BANK_PORT);
    }

    /**
     * Requests authorization for a credit card transaction from the bank.
     * This method sends the card number and blocks until the bank responds or the request times out.
     *
     * @param cardNumber A string representing the 16-digit credit card number.
     * @return An {@link AuthorizationStatus} enum indicating whether the card was approved, declined, or if an error occurred.
     */
    public AuthorizationStatus authorizeCreditCard(String cardNumber) {
        String requestString = "Authorize:" + cardNumber;
        bankConnection.send(new Message(requestString));

        Message response = waitForResponse(RESPONSE_TIMEOUT_MS);

        if (response == null) {
            System.err.println("Bank authorization timed out for card: " + cardNumber);
            return AuthorizationStatus.ERROR;
        }

        return switch (response.getContent()) {
            case "Approve" -> AuthorizationStatus.APPROVED;
            case "Decline" -> AuthorizationStatus.DECLINED;
            default -> {
                System.err.println("Received unknown response from bank: " + response.getContent());
                yield AuthorizationStatus.ERROR;
            }
        };
    }

    /**
     * Charges a final amount to a credit card.
     * This method sends the charge request to the bank and blocks until a confirmation response
     * is received or the request times out.
     *
     * @param cardNumber The credit card number to charge.
     * @param amount     The transaction amount in dollars.
     * @return {@code true} if the charge was successfully confirmed, {@code false} otherwise.
     */
    public boolean chargeCreditCard(String cardNumber, double amount) {
        String requestString = String.format("Charge:%s,%.2f", cardNumber, amount);
        bankConnection.send(new Message(requestString));

        Message response = waitForResponse(RESPONSE_TIMEOUT_MS);

        if (response == null) {
            System.err.println("Bank charge confirmation timed out for card: " + cardNumber);
            return false;
        }

        // The bank simulation responds with "Charged:CARD_NUMBER,AMOUNT" on success.
        // We check if the response starts with "Charged:" as a simple confirmation.
        return response.getContent().startsWith("Charged:");
    }

    /**
     * Waits for a message to arrive from the IOPort's incoming queue.
     * This method polls the queue and includes a timeout to prevent indefinite blocking.
     *
     * @param timeoutMillis The maximum time to wait for a response, in milliseconds.
     * @return The received {@link Message}, or {@code null} if the timeout was reached.
     */
    private Message waitForResponse(long timeoutMillis) {
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < timeoutMillis) {
            Message response = bankConnection.get();
            if (response != null) {
                return response;
            }
            try {
                // Sleep briefly to prevent the loop from consuming 100% CPU.
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Preserve the interrupted status
                System.err.println("BankManager response wait was interrupted.");
                return null;
            }
        }
        return null; // Timeout occurred
    }

    /**
     * Closes the connection to the bank server.
     */
    public void close() {
        bankConnection.close();
    }

    /**
     * Represents the possible outcomes of a credit card authorization request.
     */
    public enum AuthorizationStatus {
        /**
         * The bank approved the transaction.
         */
        APPROVED,
        /**
         * The bank declined the transaction.
         */
        DECLINED,
        /**
         * An error occurred, or the request timed out.
         */
        ERROR
    }
}

