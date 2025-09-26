package SmallDevices;

import Server.DeviceConstants;
import Server.IOPortServer;
import Server.Message;

public class Bank {
    public static void main(String[] args) {
        IOPortServer bankPort = new IOPortServer(DeviceConstants.BANK_PORT);
        System.out.println("Bank is running.");
        while (true) {
            Message msg = bankPort.get();
            if (msg != null) {
                System.out.println(msg);
                String content = msg.toString();
                if (content.startsWith("Authorize:")) {
                    String cc = content.substring("Authorize:".length());
                    authorize(bankPort, cc);
                } else if (content.startsWith("Charge:")) {
                    String chargeData = content.substring("Charge:".length());
                    String[] parts = chargeData.split(",");
                    String cc = parts[0];
                    double dollars = Double.parseDouble(parts[1]);
                    charge(bankPort, cc, dollars);
                }
            }
        }
    }

    private static void authorize(IOPortServer port, String cc) {
        int lastDigit = Integer.parseInt(cc.substring(cc.length() - 1));
        //If the last digit of the card number is greater than 7, it replies with Decline.
        if (lastDigit > 7) {
            port.send(new Message("Decline"));
        } else {
            port.send(new Message("Approve"));
        }
    }

    private static void charge(IOPortServer port, String cc, double dollars) {
        if (dollars > 200) {
            port.send(new Message("Decline"));
        } else {
            port.send(new Message(String.format("Charged:%s,%.2f", cc, dollars)));
        }
    }
}
