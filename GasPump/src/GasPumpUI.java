import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A JavaFX application that serves as a functional mockup of a gas pump's digital touch screen.
 * It connects the ScreenParser and ScreenCommunicationManager to render a UI and handle interactions.
 */
public class GasPumpUI extends Application implements ScreenCommunicationManager.MessageListener {

    private static final int NUM_ROWS = 5;
    private static final int NUM_COLS = 2;
    private final ScreenParser parser = new ScreenParser();
    private GridPane gridPane;
    private ScreenCommunicationManager commManager;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        commManager = new ScreenCommunicationManager(this);
        primaryStage.setTitle("Gas Pump UI Mockup");
        gridPane = createGridPane();
        FlowMeter flowMeter = new FlowMeter(
                msg -> Platform.runLater(() -> processScreenMessage(msg)),
                0.5, 3.999
        );
        flowMeter.initLayout();
        flowMeter.start();
        Scene scene = new Scene(gridPane, 400, 600);
        primaryStage.setScene(scene);
        primaryStage.show();

        commManager.startListening();
    }

    @Override
    public void onMessageReceived(String message) {
        Platform.runLater(() -> processScreenMessage(message));
    }

    @Override
    public void onCommunicationError(Exception e) {
        Platform.runLater(() -> {
            Label errorLabel = new Label("FATAL: Communication Error:\n" + e.getMessage());
            errorLabel.setTextFill(Color.RED);
            gridPane.getChildren().clear();
            gridPane.add(new StackPane(errorLabel), 0, 0, 4, NUM_ROWS);
        });
    }

    private void processScreenMessage(String message) {
        gridPane.getChildren().clear();
        ScreenParser.ScreenLayout layout = parser.parse(message);

        // --- Place Text Fields ---
        layout.textFields().forEach(this::placeTextNode);

        // --- Process and Place Buttons ---
        List<Button> mutuallyExclusiveButtons = layout.buttons().values().stream()
                .filter(info -> info.type() == ScreenParser.BUTTON_TYPE_MUTUALLY_EXCLUSIVE)
                .map(ScreenParser.ButtonInfo::button)
                .collect(Collectors.toList());

        layout.buttons().forEach((cellId, info) -> {
            Button currentButton = info.button();

            // Set a single action handler for all buttons
            currentButton.setOnAction(event -> {
                // Always send the message
                commManager.sendMessage("b:" + cellId);

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

