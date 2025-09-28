package SmallDevices;


import Server.DeviceConstants;
import Server.IOPortServer;
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

import java.io.File;
import java.net.MalformedURLException;
import java.util.Objects;

/**
 * Simulates a physical card reader device. It runs as a JavaFX application
 * and listens for a connection from the main controller. When the user simulates
 * a card tap, it sends a 16-digit number to the controller. It also changes color
 * based on "approved" or "declined" messages it receives back.
 */
public class CardReader extends Application {

    private IOPortServer commManager;
    private Rectangle outerRect;

    /**
     * The main entry point for the CardReader application.
     * This simply launches the JavaFX UI.
     */
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        // The IOPort must be initialized here, within the JavaFX application thread.
        this.commManager = new IOPortServer(DeviceConstants.CARD_READER_PORT);

        // Start a separate thread to listen for messages from the MainController
        // This prevents the UI from freezing while waiting for network input.
        Thread messageListenerThread = new Thread(this::listenForMessages);
        messageListenerThread.setDaemon(true); // Allows the app to exit cleanly
        messageListenerThread.start();

        // Create and display the UI
        BorderPane root = createUI();
        Scene scene = new Scene(root, 500, 250);
        primaryStage.setTitle("Card Reader UI Mockup");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * Continuously listens for incoming messages from the MainController and
     * processes them on the JavaFX application thread.
     */
    private void listenForMessages() {
        while (true) {
            Message msg = commManager.get();
            if (msg != null) {
                // UI updates must be run on the JavaFX application thread.
                Platform.runLater(() -> processMessage(msg.getContent()));
            }
            try {
                // A brief pause to prevent the loop from consuming 100% CPU.
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
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
        ImageView tapIcon = new ImageView(
                new Image(
                        Objects.requireNonNull(CardReader.class.getResource("/tapHere.png"),
                                "Missing resource: /tapHere.png"
                        ).toExternalForm()
                )
        );
        
        tapIcon.setFitWidth(180);
        tapIcon.setFitHeight(180);
        tapIcon.setPreserveRatio(true);

        // Side Panel
        Button payButton = new Button("(Simulate Card Tap)");
        payButton.setOnAction(e -> {
            commManager.send(new Message(generate16DigitNumber() + "//"));
            outerRect.setFill(Color.ORANGE); // Change color to indicate processing
        });
        VBox buttonBox = new VBox(30, payButton);
        buttonBox.setPadding(new Insets(20));
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        // Main layout
        BorderPane root = new BorderPane();
        StackPane stack = new StackPane(outerRect, innerRect, tapIcon);
        root.setCenter(stack);
        root.setRight(buttonBox);
        return root;
    }

    /**
     * Processes messages received from the MainController to update the UI color.
     *
     * @param message The raw message content (e.g., "approved//").
     */
    private void processMessage(String message) {
        String cleanMessage = message.replace("//", "").trim().toLowerCase();
        switch (cleanMessage) {
            case "approved" -> outerRect.setFill(Color.LIMEGREEN);
            case "declined" -> {
                outerRect.setFill(Color.RED);
                // After 5 seconds, revert the color back to green.
                PauseTransition delay = new PauseTransition(Duration.seconds(5));
                delay.setOnFinished(event -> outerRect.setFill(Color.GREEN));
                delay.play();
            }
            case "complete" -> outerRect.setFill(Color.GREEN);
            case "error" -> outerRect.setFill(Color.RED);
            default -> // Default to green if the message is unrecognized
                    outerRect.setFill(Color.GREEN);
        }
    }

    /**
     * Generates a random 16-digit string to simulate a credit card number.
     *
     * @return A 16-character string of numbers.
     */
    private String generate16DigitNumber() {
        StringBuilder sb = new StringBuilder(16);
        java.util.Random r = new java.util.Random();
        for (int i = 0; i < 16; i++) {
            sb.append(r.nextInt(10));
        }
        return sb.toString();
    }
}
