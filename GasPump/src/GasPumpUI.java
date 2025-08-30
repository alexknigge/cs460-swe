import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.InputStreamReader;
import java.io.Reader;

/**
 * A JavaFX application that serves as a functional mockup of a gas pump's digital touch screen.
 * It renders a UI based on string commands from Standard Input and reports button presses to Standard Output.
 */
public class GasPumpUI extends Application {

    // Grid dimensions (logical)
    private static final int NUM_ROWS = 5;
    private static final int NUM_COLS = 2;

    private GridPane gridPane;
    private final ScreenParser parser = new ScreenParser();

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Gas Pump UI Mockup");
        gridPane = createGridPane();
        Scene scene = new Scene(gridPane, 400, 600);
        primaryStage.setScene(scene);
        primaryStage.show();

        // Start a background thread to listen for input from System.in
        startInputListenerThread();
    }

    /**
     * Creates and configures the main GridPane layout.
     * The visual layout is 4 columns to accommodate side buttons,
     * but the command protocol is based on a 2-column logical model.
     * @return A configured GridPane.
     */
    private GridPane createGridPane() {
        GridPane pane = new GridPane();
        pane.setAlignment(Pos.CENTER);
        pane.setStyle("-fx-background-color: #D3D3D3;");

        // --- 4-Column Visual Layout ---
        ColumnConstraints buttonColConst = new ColumnConstraints();
        buttonColConst.setPercentWidth(15);
        ColumnConstraints textColConst = new ColumnConstraints();
        textColConst.setPercentWidth(35);
        pane.getColumnConstraints().addAll(buttonColConst, textColConst, textColConst, buttonColConst);

        for (int i = 0; i < NUM_ROWS; i++) {
            RowConstraints rowConst = new RowConstraints();
            rowConst.setPercentHeight(100.0 / NUM_ROWS);
            pane.getRowConstraints().add(rowConst);
        }
        return pane;
    }

    /**
     * Initializes and starts a daemon thread that reads messages from Standard Input.
     */
    private void startInputListenerThread() {
        Thread inputThread = new Thread(() -> {
            try (Reader reader = new InputStreamReader(System.in)) {
                StringBuilder messageBuilder = new StringBuilder();
                int c;
                while ((c = reader.read()) != -1) {
                    messageBuilder.append((char) c);
                    if (messageBuilder.toString().endsWith("//")) {
                        String message = messageBuilder.toString();
                        Platform.runLater(() -> processScreenMessage(message));
                        messageBuilder.setLength(0);
                    }
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    Label errorLabel = new Label("Error reading input:\n" + e.getMessage());
                    errorLabel.setTextFill(Color.RED);
                    StackPane pane = new StackPane(errorLabel);
                    gridPane.getChildren().clear();
                    gridPane.add(pane, 0, 0, 4, NUM_ROWS);
                });
            }
        });
        inputThread.setDaemon(true);
        inputThread.start();
    }

    /**
     * Processes a screen message by using the ScreenParser and then placing the generated UI nodes onto the grid.
     * @param message The complete message string, ending with "//".
     */
    private void processScreenMessage(String message) {
        gridPane.getChildren().clear();
        ScreenParser.ScreenLayout layout = parser.parse(message);

        // Add all created text fields to the grid's center columns (1 and 2)
        layout.textFields().forEach((cellId, node) -> {
            int cellNum = Integer.parseInt(cellId.substring(0, 1));
            int row = cellNum / NUM_COLS;
            int logicalCol = cellNum % NUM_COLS;

            if (cellId.length() > 1 && Character.isDigit(cellId.charAt(1))) { // Combined field
                GridPane.setColumnSpan(node, 2);
                gridPane.add(node, 1, row);
            } else { // Single field
                gridPane.add(node, logicalCol + 1, row);
            }
        });

        // Add all created buttons to the grid's outer columns (0 and 3)
        layout.buttons().forEach((cellId, node) -> {
            int cellNum = Integer.parseInt(cellId);
            int row = cellNum / NUM_COLS;
            int logicalCol = cellNum % NUM_COLS;
            int gridCol = (logicalCol == 0) ? 0 : 3;
            gridPane.add(node, gridCol, row);
        });
    }
}

