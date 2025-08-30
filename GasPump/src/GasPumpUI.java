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
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

/**
 * A JavaFX application that serves as a functional mockup of a gas pump's digital touch screen.
 * It renders a UI based on string commands from Standard Input and reports button presses to Standard Output.
 */
public class GasPumpUI extends Application {

    // --- Style and Layout Constants ---

    // Grid dimensions (logical)
    private static final int NUM_ROWS = 5;
    private static final int NUM_COLS = 2;

    // Font Size Mapping
    private static final int FONT_SIZE_SMALL = 1;
    private static final int FONT_SIZE_MEDIUM = 2;
    private static final int FONT_SIZE_LARGE = 3;

    // Font Style Mapping
    private static final int STYLE_REGULAR = 1;
    private static final int STYLE_BOLD = 2;
    private static final int STYLE_ITALIC = 3;

    // Background Color Mapping
    private static final int COLOR_DEFAULT = 0;
    private static final int COLOR_PURPLE = 1;
    private static final int COLOR_RED = 2;
    private static final int COLOR_GREEN = 3;
    private static final int COLOR_BLUE = 4;

    private GridPane gridPane;

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
        // Column 0: Left buttons (15% width)
        ColumnConstraints buttonColConst = new ColumnConstraints();
        buttonColConst.setPercentWidth(15);

        // Columns 1 & 2: Center text display (35% width each)
        ColumnConstraints textColConst = new ColumnConstraints();
        textColConst.setPercentWidth(35);

        // Column 3: Right buttons (15% width)
        pane.getColumnConstraints().addAll(buttonColConst, textColConst, textColConst, buttonColConst);


        // Configure row constraints to be of equal height
        for (int i = 0; i < NUM_ROWS; i++) {
            RowConstraints rowConst = new RowConstraints();
            rowConst.setPercentHeight(100.0 / NUM_ROWS);
            pane.getRowConstraints().add(rowConst);
        }
        return pane;
    }

    /**
     * Initializes and starts a daemon thread that reads messages from Standard Input.
     * This prevents the JavaFX Application Thread from blocking.
     */
    private void startInputListenerThread() {
        Thread inputThread = new Thread(() -> {
            try (Reader reader = new InputStreamReader(System.in)) {
                StringBuilder messageBuilder = new StringBuilder();
                int c;
                while ((c = reader.read()) != -1) {
                    messageBuilder.append((char) c);
                    // Check for the end-of-message delimiter "//"
                    if (messageBuilder.toString().endsWith("//")) {
                        String message = messageBuilder.toString();
                        // Schedule UI update on the JavaFX Application Thread
                        Platform.runLater(() -> processScreenMessage(message));
                        // Reset builder for the next message
                        messageBuilder.setLength(0);
                    }
                }
            } catch (Exception e) {
                // Using Platform.runLater to show error on UI in case of an exception.
                Platform.runLater(() -> {
                    Label errorLabel = new Label("Error reading input:\n" + e.getMessage());
                    errorLabel.setTextFill(Color.RED);
                    StackPane pane = new StackPane(errorLabel);
                    gridPane.getChildren().clear();
                    gridPane.add(pane, 0, 0, 4, NUM_ROWS);
                });
            }
        });
        inputThread.setDaemon(true); // Allows the application to exit when the main window is closed
        inputThread.start();
    }

    /**
     * Processes a complete screen layout message from the input stream.
     * It parses text fields and buttons separately and places them in the appropriate grid columns.
     * @param message The complete message string, ending with "//".
     */
    private void processScreenMessage(String message) {
        gridPane.getChildren().clear();
        Map<String, Node> textFields = new HashMap<>();
        Map<String, Node> buttons = new HashMap<>();

        String trimmedMessage = message.substring(0, message.length() - 2); // Remove trailing "//"
        String[] commands = trimmedMessage.split(";");

        // Parse all commands and populate the respective maps
        for (String command : commands) {
            String trimmedCmd = command.trim();
            if (trimmedCmd.startsWith("t:")) {
                parseAndCreateTextField(trimmedCmd, textFields);
            } else if (trimmedCmd.startsWith("b:")) {
                parseAndCreateButton(trimmedCmd, buttons);
            }
        }

        // Add all created text fields to the grid's center columns (1 and 2)
        for (Map.Entry<String, Node> entry : textFields.entrySet()) {
            String cellId = entry.getKey();
            Node node = entry.getValue();
            int cellNum = Integer.parseInt(cellId.substring(0, 1));
            int row = cellNum / NUM_COLS;
            int logicalCol = cellNum % NUM_COLS;

            if (cellId.length() > 1 && Character.isDigit(cellId.charAt(1))) { // Combined text field like "01"
                GridPane.setColumnSpan(node, 2);
                gridPane.add(node, 1, row);
            } else { // Single text field
                gridPane.add(node, logicalCol + 1, row); // Place in col 1 or 2
            }
        }

        // Add all created buttons to the grid's outer columns (0 and 3)
        for (Map.Entry<String, Node> entry : buttons.entrySet()) {
            String cellId = entry.getKey();
            Node node = entry.getValue();
            int cellNum = Integer.parseInt(cellId);
            int row = cellNum / NUM_COLS;
            int logicalCol = cellNum % NUM_COLS;

            int gridCol = (logicalCol == 0) ? 0 : 3; // Left button in col 0, right button in col 3
            gridPane.add(node, gridCol, row);
        }
    }

    /**
     * Parses a text field command and adds a styled StackPane with a Label to the provided map.
     * @param command The text field command string (e.g., "t:0/s:1/f:2/c:3/Hello").
     * @param textFields The map to store the created text field Node.
     */
    private void parseAndCreateTextField(String command, Map<String, Node> textFields) {
        try {
            String[] parts = command.substring(2).split("/", 5);
            if (parts.length < 5) return; // Malformed command

            String cellId = parts[0];
            int size = Integer.parseInt(parts[1].substring(2));
            String styleStr = parts[2].substring(2);
            int color = Integer.parseInt(parts[3].substring(2));
            String text = parts[4];

            Label label = new Label(text);
            applyFont(label, size, styleStr);

            StackPane cellPane = new StackPane(label);
            cellPane.setAlignment(Pos.CENTER);
            applyBackgroundColor(cellPane, color);

            textFields.put(cellId, cellPane);

        } catch (Exception e) {
            System.err.println("Failed to parse text field command: " + command);
        }
    }

    /**
     * Parses a button command and adds a styled, text-less Button to the provided map.
     * @param command The button command string (e.g., "b:8/m").
     * @param buttons The map to store the created button Node.
     */
    private void parseAndCreateButton(String command, Map<String, Node> buttons) {
        try {
            String[] parts = command.substring(2).split("/");
            if (parts.length < 2) return; // Malformed command

            String cellId = parts[0];
            // String buttonType = parts[1]; // 'm' or 'x', not used in this mockup's logic.

            Button button = new Button(); // Button has no text
            button.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE); // Make button fill the cell
            // Style to make it look like a distinct, clickable button
            button.setStyle("-fx-background-color: #E0E0E0; -fx-border-color: #808080; -fx-border-width: 2; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 5, 0, 0, 1);");


            button.setOnAction(event -> {
                // Per requirements, print the response to Standard Output
                System.out.print("b:" + cellId + ";//");
                System.out.flush(); // Ensure the output is sent immediately
            });

            buttons.put(cellId, button);

        } catch (Exception e) {
            System.err.println("Failed to parse button command: " + command);
        }
    }

    /**
     * Applies font size and style to a Label.
     * @param label The Label to style.
     * @param size The integer code for font size.
     * @param styleStr The string containing font style codes.
     */
    private void applyFont(Label label, int size, String styleStr) {
        double fontSize;
        switch (size) {
            case FONT_SIZE_SMALL:  fontSize = 14; break;
            case FONT_SIZE_LARGE:  fontSize = 30; break;
            case FONT_SIZE_MEDIUM:
            default:               fontSize = 20; break;
        }

        FontWeight weight = FontWeight.NORMAL;
        FontPosture posture = FontPosture.REGULAR;

        // Override rule: if '1' is present, style is always regular.
        if (styleStr.contains(String.valueOf(STYLE_REGULAR))) {
            // Defaults are already set to regular, so do nothing.
        } else {
            if (styleStr.contains(String.valueOf(STYLE_BOLD))) {
                weight = FontWeight.BOLD;
            }
            if (styleStr.contains(String.valueOf(STYLE_ITALIC))) {
                posture = FontPosture.ITALIC;
            }
        }
        label.setFont(Font.font("sans-serif", weight, posture, fontSize));
    }

    /**
     * Applies a background color to a StackPane.
     * @param pane The StackPane to color.
     * @param color The integer code for the background color.
     */
    private void applyBackgroundColor(StackPane pane, int color) {
        String style = "-fx-padding: 5;"; // Add some padding
        switch (color) {
            case COLOR_PURPLE: style += "-fx-background-color: #800080;"; break;
            case COLOR_RED:    style += "-fx-background-color: #FF0000;"; break;
            case COLOR_GREEN:  style += "-fx-background-color: #008000;"; break;
            case COLOR_BLUE:   style += "-fx-background-color: #0000FF;"; break;
            case COLOR_DEFAULT:
            default:           break; // No background color
        }
        pane.setStyle(style);
    }
}

