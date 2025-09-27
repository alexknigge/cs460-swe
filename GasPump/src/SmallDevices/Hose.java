package SmallDevices;

import Server.DeviceConstants;
import Server.IOPortServer;
import Server.Message;
import javafx.animation.*;
import javafx.application.Application;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.Random;

/**
 * Simulates a physical gas hose, latch, and vehicle tank.
 * It runs as a JavaFX application and acts as a server, sending sensor status
 * messages (e.g., "removed//", "attached//", "tank-full//") to the main controller.
 */
public class Hose extends Application {

    private boolean connected = false; // Represents connection to the car, not the pump holster
    private Timeline fillTimeline;
    private IOPortServer commManager;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        // Initialize the communication port to act as a server
        this.commManager = new IOPortServer(DeviceConstants.HOSE_PORT);

        // --- UI Components ---
        double latchRadius = 50;
        double connectorW = 200;
        double connectorH = 2 * latchRadius;
        double hoseHeight = connectorH / 3;
        double hoseLength = 400;

        Rectangle hose = new Rectangle(-hoseLength, connectorH / 2 - hoseHeight / 2, hoseLength, hoseHeight);
        hose.setFill(Color.BLACK);

        Rectangle connector = new Rectangle(0, 0, connectorW, connectorH);
        connector.setArcWidth(60);
        connector.setArcHeight(60);
        connector.setFill(Color.SILVER);

        Circle latch = new Circle(latchRadius);
        latch.setFill(null);
        latch.setStroke(Color.RED);
        latch.setStrokeWidth(15);
        latch.setCenterX(connectorW - latchRadius);
        latch.setCenterY(connectorH / 2);

        Pane hoseAndLatchPane = new Pane(hose, connector, latch);
        hoseAndLatchPane.setPrefSize(connectorW, connectorH);
        hoseAndLatchPane.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        HBox centerRow = new HBox(hoseAndLatchPane);
        centerRow.setAlignment(Pos.CENTER);
        VBox hoseAndLatchBox = new VBox(centerRow);
        hoseAndLatchBox.setAlignment(Pos.CENTER);

        double tankWidth = 50, tankHeight = 150;
        Rectangle tankOutline = new Rectangle(tankWidth, tankHeight);
        tankOutline.setFill(null);
        tankOutline.setStroke(Color.BLACK);
        tankOutline.setStrokeWidth(2);

        DoubleProperty fillPercent = new SimpleDoubleProperty(new Random().nextDouble() * (2.0 / 3.0));
        Rectangle tankFill = new Rectangle(tankWidth, tankHeight * fillPercent.get());
        tankFill.setFill(Color.GOLD);
        tankFill.heightProperty().bind(fillPercent.multiply(tankHeight));

        StackPane tankPane = new StackPane(tankOutline, tankFill);
        tankPane.setAlignment(Pos.BOTTOM_CENTER);

        VBox tankBox = new VBox(5, new Text("Tank"), tankPane);
        tankBox.setAlignment(Pos.CENTER);
        tankBox.setPadding(new Insets(10));

        // --- Buttons and Event Handlers ---
        HBox buttonsRow = getButtonsHBox(latch, fillPercent);
        VBox bottomBox = new VBox(buttonsRow);
        bottomBox.setAlignment(Pos.CENTER);
        bottomBox.setPadding(new Insets(15));

        // --- Layout ---
        BorderPane root = new BorderPane();
        root.setCenter(hoseAndLatchBox);
        root.setBottom(bottomBox);
        root.setRight(tankBox);

        Scene scene = new Scene(root, 600, 360);
        primaryStage.setTitle("Gas Hose Mockup");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private HBox getButtonsHBox(Circle latch, DoubleProperty fillPercent) {
        Button connectButton = getConnectButton(latch);

        Button fillButton = new Button("Fill");
        fillButton.setOnAction(e -> {
            if (!connected) {
                System.out.println("Cannot fill: hose is disconnected from car!");
                return;
            }
            if (fillTimeline != null && fillTimeline.getStatus() == Animation.Status.PAUSED) {
                fillTimeline.play();
                return;
            }
            if (fillTimeline != null && fillTimeline.getStatus() == Animation.Status.RUNNING)
                return;

            double remaining = 1.0 - fillPercent.get();
            if (remaining <= 0.0001) return;

            double secondsFromEmptyToFull = 10.0; // flow rate
            Duration dur = Duration.seconds(secondsFromEmptyToFull * remaining);
            fillTimeline = new Timeline(new KeyFrame(dur, new KeyValue(fillPercent, 1.0, Interpolator.LINEAR)));
            fillTimeline.setOnFinished(event -> {
                // When filling is complete, send the "tank-full" message
                System.out.println("Hose sending: tank-full//");
                commManager.send(new Message("tank-full//"));
            });
            fillTimeline.play();
        });

        HBox buttonsRow = new HBox(10, connectButton, fillButton);
        buttonsRow.setAlignment(Pos.CENTER);
        return buttonsRow;
    }

    private Button getConnectButton(Circle latch) {
        Button connectButton = new Button("Attach/Remove Nozzle");
        connectButton.setOnAction(e -> {
            connected = !connected;
            latch.setStroke(connected ? Color.LIMEGREEN : Color.RED);
            // Send status message to the main controller
            String message = connected ? "removed//" : "attached//";
            System.out.println("Hose sending: " + message);
            commManager.send(new Message(message));

            if (!connected && fillTimeline != null && fillTimeline.getStatus() == Animation.Status.RUNNING) {
                fillTimeline.pause(); // pause when disconnected
            }
        });
        return connectButton;
    }
}
