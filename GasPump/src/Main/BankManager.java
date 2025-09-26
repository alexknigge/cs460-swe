package Main;

import Server.DeviceConstants;
import Server.IOPort;
import Server.Message;

public class BankManager {
    private final IOPort bankConnection;

    public BankManager() {
        this.bankConnection = new IOPort(DeviceConstants.BANK_HOSTNAME, DeviceConstants.BANK_PORT);
    }

    /**
     * Checks whether a credit card is authorized to be transacted with by communicating with the Bank.
     * Note: This function is blocking until a response is received from the BANK
     *
     * @param cardNumber A string representation of the credit card number, to be sent to the bank
     * @return True if the transaction is authorized, false otherwise.
     */
    public boolean getCreditCardAuthorization(String cardNumber) {
        // Construct the authorization request message string
        String requestString = "Authorize:" + cardNumber;

        // Send the message to the bank server
        bankConnection.send(new Message(requestString));
        Message response;
        do {
            response = bankConnection.get();
            if (response == null) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } while (response == null);

        return response.toString().equals("Approve");
    }

    /**
     * Charges a credit card a certain amount. This function blocks until a response is received.
     *
     * @param cardNumber The credit card number to charge.
     * @param amount     The amount to charge in dollars.
     */
    public void chargeCreditCard(String cardNumber, double amount) {
        String requestString = String.format("Charge:%s,%.2f", cardNumber, amount);
        bankConnection.send(new Message(requestString));
    }
}
