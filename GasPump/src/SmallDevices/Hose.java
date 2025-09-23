package SmallDevices;

import javafx.animation.*;
import javafx.application.Application;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.Random;

public class Hose extends Application {
    
    private boolean connected = false;
    private Timeline fillTimeline;
    
    @Override
    public void start(Stage primaryStage) {
        // Sizes
        double latchRadius = 50;
        double connectorW = 200;
        double connectorH = 2 * latchRadius;
        double hoseHeight = connectorH/3;
        double hoseLength = 400;
        
        // Hose, connector, and latch /////////////////////////////////////////
        // Black hose:
        Rectangle hose = new Rectangle(-hoseLength, connectorH/2 - hoseHeight/2,
                hoseLength,
                hoseHeight);
        hose.setFill(Color.BLACK);
        
        // Silver connector
        Rectangle connector = new Rectangle(0, 0, connectorW, connectorH);
        connector.setArcWidth(60);
        connector.setArcHeight(60);
        connector.setFill(Color.SILVER);
        
        // Color-changing latch
        Circle latch = new Circle(latchRadius);
        latch.setFill(null);
        latch.setStroke(Color.RED);
        latch.setStrokeWidth(15);
        latch.setCenterX(connectorW - latchRadius);
        latch.setCenterY(connectorH / 2);
        
        // Hose and latch pane
        Pane hoseAndLatchPane = new Pane(hose, connector, latch);
        hoseAndLatchPane.setPrefSize(connectorW, connectorH);
        hoseAndLatchPane.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        // Center hose and latch pane
        HBox centerRow = new HBox(hoseAndLatchPane);
        centerRow.setAlignment(Pos.CENTER);
        VBox hoseAndLatchBox = new VBox(centerRow);
        hoseAndLatchBox.setAlignment(Pos.CENTER);
        
        // Tank bar ///////////////////////////////////////////////////////////
        double tankWidth = 50, tankHeight = 150;
        Rectangle tankOutline = new Rectangle(tankWidth, tankHeight);
        tankOutline.setFill(null);
        tankOutline.setStroke(Color.BLACK);
        tankOutline.setStrokeWidth(2);
        
        // Set initial tank fill
        DoubleProperty fillPercent = new SimpleDoubleProperty(new Random().nextDouble() * (2.0 / 3.0));
        Rectangle tankFill = new Rectangle(tankWidth, tankHeight * fillPercent.get());
        tankFill.setFill(Color.GREEN);
        tankFill.heightProperty().bind(fillPercent.multiply(tankHeight));
        
        // Tank Pane
        StackPane tankPane = new StackPane(tankOutline, tankFill);
        tankPane.setAlignment(Pos.BOTTOM_CENTER);
        
        VBox tankBox = new VBox(5, new Text("Tank"), tankPane);
        tankBox.setAlignment(Pos.CENTER);
        tankBox.setPadding(new Insets(10));
        
        // Buttons ////////////////////////////////////////////////////////////
        // Connect button
        Button connectButton = new Button("Connect/Disconnect");
        connectButton.setOnAction(e -> {
            connected = !connected;
            latch.setStroke(connected ? Color.LIMEGREEN : Color.RED);
            
            if (!connected && fillTimeline != null && fillTimeline.getStatus() == Animation.Status.RUNNING) {
                fillTimeline.pause(); // pause when disconnected
            }
        });
        
        // Fill button
        Button fillButton = new Button("Fill");
        fillButton.setOnAction(e -> {
            if (!connected) {
                System.out.println("Cannot fill: hose is disconnected!");
                return;
            }
            if (fillTimeline != null && fillTimeline.getStatus() == Animation.Status.PAUSED) {
                fillTimeline.play();
                return;
            }
            if (fillTimeline != null && fillTimeline.getStatus() == Animation.Status.RUNNING) return;
            
            double remaining = 1.0 - fillPercent.get();
            if (remaining <= 0.0001) return;
            
            double secondsFromEmptyToFull = 4.0; // flow rate
            Duration dur = Duration.seconds(secondsFromEmptyToFull * remaining);
            fillTimeline = new Timeline(new KeyFrame(dur, new KeyValue(fillPercent, 1.0, Interpolator.LINEAR)));
            fillTimeline.play();
        });
        
        // Button Box (Bottom)
        HBox buttonsRow = new HBox(10, connectButton, fillButton);
        buttonsRow.setAlignment(Pos.CENTER);
        VBox bottomBox = new VBox(buttonsRow);
        bottomBox.setAlignment(Pos.CENTER);
        bottomBox.setPadding(new Insets(15));

        // Layout /////////////////////////////////////////////////////////////
        // root layout
        BorderPane root = new BorderPane();
        root.setCenter(hoseAndLatchBox);
        root.setBottom(bottomBox);
        root.setRight(tankBox);
        
        // Set scene
        Scene scene = new Scene(root, 600, 360);
        primaryStage.setTitle("Gas Hose Mockup");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
    
    public static void main(String[] args) { launch(args); }
}