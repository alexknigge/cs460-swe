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
    
    private boolean connected = false;
    private Timeline fillTimeline;
    private IOPortServer commManager;
    private final java.util.Random rng = new java.util.Random();
    private volatile boolean systemFueling = false;
    
    public static void main(String[] args) {
        launch(args);
    }
    
    @Override
    public void start(Stage primaryStage) {
        
        // Initialize the communication port
        this.commManager = new IOPortServer(DeviceConstants.HOSE_PORT);
        
        // --- Hose assembly UI Components ---
        double latchRadius = 50;
        double connectorW = 200;
        double connectorH = 2 * latchRadius;
        double hoseHeight = connectorH / 3;
        double hoseLength = 400;
        
        // --- Hose and connector ---
        Rectangle hose = new Rectangle(-hoseLength, connectorH / 2 - hoseHeight / 2, hoseLength, hoseHeight);
        hose.setFill(Color.BLACK);
        Rectangle connector = new Rectangle(0, 0, connectorW, connectorH);
        connector.setArcWidth(60);
        connector.setArcHeight(60);
        connector.setFill(Color.SILVER);
        
        // --- Circular Latch ---
        Circle latch = new Circle(latchRadius);
        latch.setFill(null);
        latch.setStroke(Color.RED);
        latch.setStrokeWidth(15);
        latch.setCenterX(connectorW - latchRadius);
        latch.setCenterY(connectorH / 2);
        
        // --- Hose and latch pane ---
        Pane hoseAndLatchPane = new Pane(hose, connector, latch);
        hoseAndLatchPane.setPrefSize(connectorW, connectorH);
        hoseAndLatchPane.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        HBox centerRow = new HBox(hoseAndLatchPane);
        centerRow.setAlignment(Pos.CENTER);
        VBox hoseAndLatchBox = new VBox(centerRow);
        hoseAndLatchBox.setAlignment(Pos.CENTER);
        
        // --- Tank fill meter ---
        // Animation components:
        DoubleProperty fillPercent = new SimpleDoubleProperty(new Random().nextDouble() * (2.0 / 3.0));
        AnimationTimer commandPoller = new AnimationTimer() {
            @Override public void handle(long now) {
                Message m = commManager.get();
                if (m == null) return;
                String cmd = m.getContent() == null ? "" : m.getContent().trim();
                
                if ("CMD:FUELING:START//".equals(cmd)) {
                    systemFueling = true;
                    if (connected) tryStartOrResume(fillPercent);
                } else if ("CMD:FUELING:STOP//".equals(cmd)) {
                    systemFueling = false;
                    if (fillTimeline != null && fillTimeline.getStatus() == Animation.Status.RUNNING) {
                        fillTimeline.pause();
                    }
                }
            }
        };
        commandPoller.start();
        // Tank:
        double tankWidth = 50, tankHeight = 150;
        Rectangle tankOutline = new Rectangle(tankWidth, tankHeight);
        tankOutline.setFill(null);
        tankOutline.setStroke(Color.BLACK);
        tankOutline.setStrokeWidth(2);
        // Fill meter:
        Rectangle tankFill = new Rectangle(tankWidth, tankHeight * fillPercent.get());
        tankFill.setFill(Color.GOLD);
        tankFill.heightProperty().bind(fillPercent.multiply(tankHeight));
        // Pane:
        StackPane tankPane = new StackPane(tankOutline, tankFill);
        tankPane.setAlignment(Pos.BOTTOM_CENTER);
        VBox tankBox = new VBox(5, new Text("Tank"), tankPane);
        tankBox.setAlignment(Pos.CENTER);
        tankBox.setPadding(new Insets(10));
        
        // --- Connect button ---
        HBox buttonsRow = buttonHBox(latch, fillPercent);
        VBox bottomBox = new VBox(buttonsRow);
        bottomBox.setAlignment(Pos.CENTER);
        bottomBox.setPadding(new Insets(15));
        
        // --- Overall layout ---
        BorderPane root = new BorderPane();
        root.setCenter(hoseAndLatchBox);
        root.setBottom(bottomBox);
        root.setRight(tankBox);
        Scene scene = new Scene(root, 600, 360);
        primaryStage.setTitle("Gas Hose Mockup");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    
    
    private HBox buttonHBox(Circle latch, DoubleProperty fillPercent) {
        Button connectButton = connectButton(latch, fillPercent);
        HBox buttonsRow = new HBox(10, connectButton);
        buttonsRow.setAlignment(Pos.CENTER);
        return buttonsRow;
    }
    
    
    private Button connectButton(Circle latch, DoubleProperty fillPercent) {
        Button connectButton = new Button("Attach/Remove Nozzle");
        connectButton.setOnAction(e -> {
            connected = !connected;
            latch.setStroke(connected ? Color.LIMEGREEN : Color.RED);
            
            // Notify main controller
            String message = connected ? "removed//" : "attached//";
            System.out.println("Hose sending: " + message);
            commManager.send(new Message(message));
            
            if (!connected) {
                
                // Pause while disconnected
                if (fillTimeline != null && fillTimeline.getStatus() == Animation.Status.RUNNING) {
                    fillTimeline.pause();
                }
                
                if (fillTimeline != null
                        && fillTimeline.getStatus() != Animation.Status.RUNNING
                        && fillPercent.get() >= 0.9999) {
                    fillTimeline.stop();
                    fillTimeline = null;
                    
                    double newStart = rng.nextDouble() * (2.0 / 3.0);
                    fillPercent.set(newStart);
                } else {
                    if (systemFueling) {
                        tryStartOrResume(fillPercent);
                    }
                }
            }
        });
        return connectButton;
    }
    
    
    private void tryStartOrResume(DoubleProperty fillPercent) {
        if (!systemFueling) return;
        if (fillPercent.get() >= 0.9999) return;
        
        if (fillTimeline != null) {
            if (fillTimeline.getStatus() == Animation.Status.PAUSED) {
                fillTimeline.play();
                return;
            }
            if (fillTimeline.getStatus() == Animation.Status.RUNNING) return;
        }
        
        double remaining = 1.0 - fillPercent.get();
        if (remaining <= 0.0001) return;
        
        Duration dur = Duration.seconds(20.0 * remaining);
        fillTimeline = new Timeline(new KeyFrame(dur,
                new KeyValue(fillPercent, 1.0, Interpolator.LINEAR)));
        fillTimeline.setOnFinished(e -> {
            System.out.println("Hose sending: tank-full//");
            commManager.send(new Message("tank-full//"));
        });
        fillTimeline.play();
    }
}
