package Devices;


/**
 * @Author: Dustin Ferguson
 * </p>
 * Card reader is designed to receive 4 types of messages:
 * (1) a string of ints representing a card number;
 * (2) The string "approved" from the control device when a transaction is approved;
 * (3) The string "declined" from the control device when a transaction is approved;
 * (4) The String "complete" from the control device when a transaction is complete.
 * The color of the physical card reader will change based on messages received.
 * </p>
 * Card reader will send only 2 types of messages to main control unit:
 * (1) The string of ints received representing the credit card number.
 * (2) The string "error" if there is a card read error.
 */

public class CardReader implements ScreenCommunicationManager.MessageListener{

    private boolean cardObtained;
    private boolean cardApproved;
    private ScreenCommunicationManager commManager;

    public CardReader() {
        commManager = new ScreenCommunicationManager(this);
    }

    public static boolean isInteger(String s) {
        if (s == null || s.isEmpty()) return false;
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public void onMessageReceived(String message) {
        if (isInteger(message)) {
            commManager.sendMessage(message);
            cardObtained = true;
        }
        else {
            if (message.equalsIgnoreCase("approved")) cardApproved =
                    true;
            else if (message.equalsIgnoreCase("declined")) cardObtained = false;
            else if (message.equalsIgnoreCase("complete")) {
                cardObtained = false;
                cardApproved = false;
            }
            else commManager.sendMessage("errpr");
        }
    }

    @Override
    public void onCommunicationError(Exception e) {
        commManager.sendMessage("CardReader error");
    }
}
