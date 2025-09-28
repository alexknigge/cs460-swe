
package SmallDevices;

import Server.DeviceConstants;
import Server.IOPortServer;
import Server.Message;

import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.scene.shape.Rectangle;
import javafx.scene.paint.Color;
import javafx.scene.layout.StackPane;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.animation.AnimationTimer;
import java.util.concurrent.atomic.AtomicReference;

public class Bank {
    // Holds the current status message for the UI
    public static final AtomicReference<String> state = new AtomicReference<>("Waiting for transaction/card");

    public static void main(String[] args) {
        Bank bank = new Bank();
        StatusUI.bindBank(bank);
        new Thread(() -> {
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
        }).start();
        Application.launch(StatusUI.class, args);
    }

    private static void authorize(IOPortServer port, String cc) {
        int lastDigit = Integer.parseInt(cc.substring(cc.length() - 1));
        // If the last digit of the card number is greater than 7, it replies with Decline.
        if (lastDigit > 7) {
            state.set("Declined");
            port.send(new Message("Decline"));
        } else {
            state.set("Approved");
            port.send(new Message("Approve"));
        }
    }

    private static void charge(IOPortServer port, String cc, double dollars) {
        if (dollars > 200) {
            state.set("Charge Declined");
            port.send(new Message("Decline"));
        } else {
            state.set(String.format("Charged: $%.2f", dollars));
            port.send(new Message(String.format("Charged:%s,%.2f", cc, dollars)));
            // Reset status after 5 seconds
            new Thread(() -> {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    // ignore
                }
                // Only reset if state still shows the charged amount
                if (state.get().startsWith("Charged: $")) {
                    state.set("Waiting for transaction/card");
                }
            }).start();
        }
    }

    // Nested JavaFX UI class
    public static class StatusUI extends Application {
        private static Bank bankInstance;
        public static void bindBank(Bank bank) {
            bankInstance = bank;
        }

        @Override
        public void start(Stage stage) {
            Rectangle rect = new Rectangle(400, 120, Color.LIGHTBLUE);
            rect.setArcWidth(24);
            rect.setArcHeight(24);

            Label title = new Label("Group 6 Bank (A Bank You Can Trust)");
            title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
            Label status = new Label();
            status.setStyle("-fx-font-size: 18px;");

            VBox vbox = new VBox(16, title, status);
            vbox.setAlignment(Pos.CENTER);
            vbox.setPadding(new Insets(24, 24, 24, 24));

            StackPane root = new StackPane(rect, vbox);
            root.setPadding(new Insets(16));
            Scene scene = new Scene(root, 500, 200);

            // AnimationTimer to update status label continuously
            AnimationTimer timer = new AnimationTimer() {
                @Override
                public void handle(long now) {
                    status.setText(Bank.state.get());
                }
            };
            timer.start();

            stage.setTitle("Bank Status");
            stage.setScene(scene);
            stage.setResizable(false);
            stage.show();
        }
    }
}
