package Devices;

import Server.Message;
import Server.IOPort;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.stage.Stage;

import java.util.List;

/**
 * A JavaFX application that serves as a functional mockup of a gas pump's digital touch screen.
 * It uses an IOPort to handle all communication and a ScreenParser to render the UI.
 */
public class GasPumpUI extends Application {

    private static final int NUM_ROWS = 5;
    private static final int NUM_COLS = 2;
    private final ScreenParser parser = new ScreenParser();
    private GridPane gridPane;
    private IOPort mainIOPort;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        // Instantiate the IOPort for device ID 0. It starts running automatically.
        mainIOPort = new IOPort("0");

        primaryStage.setTitle("Gas Pump UI Mockup");
        gridPane = createGridPane();
        Scene scene = new Scene(gridPane, 400, 600);
        primaryStage.setScene(scene);
        primaryStage.show();

        // Start a timer to poll for new messages from the IOPort.
        startMessagePolling();
    }

    /**
     * Creates and starts an AnimationTimer that runs on every frame, checking the IOPort
     * for new messages to process, and updates the screen as new messages are processed.
     */
    private void startMessagePolling() {
        AnimationTimer messagePoller = new AnimationTimer() {
            @Override
            public void handle(long now) {
                String newMessage = mainIOPort.readMessage();
                if (newMessage != null) {
                    processScreenMessage(newMessage);
                }
            }
        };
        messagePoller.start();
    }

    /**
     * Updates the screen given a String Message
     *
     * @param message The formatted string message
     */
    private void processScreenMessage(String message) {
        gridPane.getChildren().clear();
        ScreenParser.ScreenLayout layout = parser.parse(message);

        // --- Place Text Fields ---
        layout.textFields().forEach(this::placeTextNode);

        // --- Process and Place Buttons ---
        List<Button> mutuallyExclusiveButtons = layout.buttons().values().stream()
                .filter(info -> info.type() == ScreenParser.BUTTON_TYPE_MUTUALLY_EXCLUSIVE)
                .map(ScreenParser.ButtonInfo::button)
                .toList();

        layout.buttons().forEach((cellId, info) -> {
            Button currentButton = info.button();

            // Set a single action handler for all buttons
            currentButton.setOnAction(event -> {
                // Send the button press message through the IOPort
                mainIOPort.sendMessage(new Message("b:" + cellId + "//"));

                // If the button is mutually exclusive, handle the style change
                if (info.type() == ScreenParser.BUTTON_TYPE_MUTUALLY_EXCLUSIVE) {
                    for (Button btn : mutuallyExclusiveButtons) {
                        btn.setStyle(ScreenParser.STYLE_BUTTON_DEFAULT);
                    }
                    currentButton.setStyle(ScreenParser.STYLE_BUTTON_SELECTED);
                }
            });

            // Place the button on the grid
            placeButtonNode(cellId, currentButton);
        });
    }

    private void placeTextNode(String cellId, Node node) {
        int cellNum = Integer.parseInt(cellId.substring(0, 1));
        int row = cellNum / NUM_COLS;
        int logicalCol = cellNum % NUM_COLS;

        if (cellId.length() > 1 && Character.isDigit(cellId.charAt(1))) { // Combined field
            GridPane.setColumnSpan(node, 2);
            gridPane.add(node, 1, row);
        } else { // Single field
            gridPane.add(node, logicalCol + 1, row);
        }
    }

    private void placeButtonNode(String cellId, Node node) {
        int cellNum = Integer.parseInt(cellId);
        int row = cellNum / NUM_COLS;
        int logicalCol = cellNum % NUM_COLS;
        int gridCol = (logicalCol == 0) ? 0 : 3;
        gridPane.add(node, gridCol, row);
    }

    private GridPane createGridPane() {
        GridPane pane = new GridPane();
        pane.setAlignment(Pos.CENTER);
        pane.setStyle("-fx-background-color: #D3D3D3;");
        ColumnConstraints buttonCol = new ColumnConstraints();
        buttonCol.setPercentWidth(15);
        ColumnConstraints textCol = new ColumnConstraints();
        textCol.setPercentWidth(35);
        pane.getColumnConstraints().addAll(buttonCol, textCol, textCol, buttonCol);
        for (int i = 0; i < NUM_ROWS; i++) {
            RowConstraints rowConst = new RowConstraints();
            rowConst.setPercentHeight(100.0 / NUM_ROWS);
            pane.getRowConstraints().add(rowConst);
        }
        return pane;
    }
}

