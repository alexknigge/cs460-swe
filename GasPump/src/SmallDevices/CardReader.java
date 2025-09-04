package SmallDevices;


import Server.IOPort;
import Server.Message;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.concurrent.TimeUnit;

/**
 * @Author: Dustin Ferguson
 * </p>
 * Card reader is designed to receive 4 types of messages:
 * (1) The string "approved//" from the control device when a transaction is
 * approved;
 * (2) The string "declined//" from the control device when a transaction is
 * approved;
 * (3) The String "complete//" from the control device when a transaction is
 * complete;
 * (4) The String "error//" from the control device when a transaction is
 * complete;
 * The color of the physical card reader will change based on messages received.
 * </p>
 * Card reader will send only 2 types of messages to main control unit:
 * (1) The string of 16 ints representing the credit card number followed by "//".
 * (2) The string "error//" if there is a card read error.
 */

public class CardReader extends Application {

    private static IOPort commManager;
    Rectangle outerRect;

    public static void main(String[] args) throws InterruptedException {
        // launch(args);
        // TODO: Change this, basically just for demo purposes
        commManager = new IOPort("cardReaderToMain");
        while (true) {
            String initCard = "t:01/s:3/f:2/c:0/Tap to Pay Now;" +
                    "t:2/s:2/f:1/c:0/Cancel;" + "b:1/m;" +
                    "t:3/s:2/f:1/c:0/Help;" + "b:2/m;" +
                    "t:04/s:2/f:1/c:0/;" + "//";
            commManager.send(new Message(initCard));
            String paymentAccepted = "t:01/s:3/f:2/c:0/Payment Accepted;" +
                    "t:2/s:2/f:1/c:0/Thank you!;" +
                    "t:3/s:2/f:1/c:0/You may begin fueling now;" +
                    "t:04/s:2/f:1/c:0/;" + "//";
            commManager.send(new Message(paymentAccepted));
            TimeUnit.SECONDS.sleep(8);
        }
    }

    @Override
    public void start(Stage primaryStage) {
        BorderPane root = createUI();
        Scene scene = new Scene(root, 600, 300);
        primaryStage.setTitle("Card Reader UI Mockup");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private BorderPane createUI() {

        // Outer rectangle
        outerRect = new Rectangle(300, 200, Color.GREEN);
        outerRect.setArcWidth(30);
        outerRect.setArcHeight(30);

        // Inner rectangle
        Rectangle innerRect = new Rectangle(200, 150, Color.YELLOW);
        innerRect.setArcWidth(30);
        innerRect.setArcHeight(30);

        // Load tapHere image
        Image img = new Image("file:GasPump/resources/tapHere.png");
        ImageView imageView = new ImageView(img);
        imageView.setFitWidth(180);
        imageView.setFitHeight(180);
        imageView.setPreserveRatio(true);

        // Side Panel
        Button payButton = new Button("(Simulate Card Tap)");
        payButton.setOnAction(e -> {
            commManager.send(new Message(generate16() + "//"));
            outerRect.setFill(Color.ORANGE);
        });
        VBox buttonBox = new VBox(30, payButton);
        buttonBox.setPadding(new Insets(20));
        buttonBox.setAlignment(Pos.CENTER_RIGHT);


        // Main.Main layout
        BorderPane root = new BorderPane();
        StackPane stack = new StackPane(outerRect, innerRect, imageView);
        root.setCenter(stack);
        root.setRight(buttonBox);
        return root;

    }

    public void processMessage(String message) {
        if (message.equalsIgnoreCase("approved//")) {
            outerRect.setFill(Color.LIMEGREEN);
        } else if (message.equalsIgnoreCase("declined//")) {
            outerRect.setFill(Color.RED);
            PauseTransition delay = new PauseTransition(Duration.seconds(5));
            delay.setOnFinished(event ->
                    Platform.runLater(() -> outerRect.setFill(Color.GREEN))
            );
            delay.play();
        } else if (message.equalsIgnoreCase("complete//")) {
            outerRect.setFill(Color.GREEN);
        } else if (message.equalsIgnoreCase("error")) {
            outerRect.setFill(Color.RED);
        } else {
            outerRect.setFill(Color.GREEN);
        }
    }

    private String generate16() {
        StringBuilder sb = new StringBuilder(16);
        java.util.Random r = new java.util.Random();
        for (int i = 0; i < 16; i++) sb.append(r.nextInt(10));
        return sb.toString();
    }

}
