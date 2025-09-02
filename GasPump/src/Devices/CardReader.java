package Devices;


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

    Rectangle outerRect;
    private static ScreenCommunicationManager commManager;
    private boolean guiTest = true;

    public static void main(String[] args) {
        launch(args);
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
            commManager.sendMessage(new Message(generate16()));
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
        }
        else if (message.equalsIgnoreCase("declined//")) {
            outerRect.setFill(Color.RED);
            PauseTransition delay = new PauseTransition(Duration.seconds(5));
            delay.setOnFinished(event ->
                    Platform.runLater(() -> outerRect.setFill(Color.GREEN))
            );
            delay.play();
        }
        else if (message.equalsIgnoreCase("complete//")) {
            outerRect.setFill(Color.GREEN);
        }
        else if (message.equalsIgnoreCase("error")) {
            outerRect.setFill(Color.RED);
        }
        else {
            outerRect.setFill(Color.GREEN);
        }
}


    private String generate16() {
        StringBuilder sb = new StringBuilder(16);
        java.util.Random r = new java.util.Random();
        for (int i = 0; i < 16; i++) sb.append(r.nextInt(10));
        return sb.toString();
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
}
