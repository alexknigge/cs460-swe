import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;

import java.util.HashMap;
import java.util.Map;

/**
 * A helper class to encapsulate all message parsing and UI node creation logic.
 */
public class ScreenParser {
    // --- Style and Layout Constants ---
    private static final int FONT_SIZE_SMALL = 1;
    private static final int FONT_SIZE_MEDIUM = 2;
    private static final int FONT_SIZE_LARGE = 3;
    private static final int STYLE_REGULAR = 1;
    private static final int STYLE_BOLD = 2;
    private static final int STYLE_ITALIC = 3;
    private static final int COLOR_DEFAULT = 0;
    private static final int COLOR_PURPLE = 1;
    private static final int COLOR_RED = 2;
    private static final int COLOR_GREEN = 3;
    private static final int COLOR_BLUE = 4;

    /**
     * A record to hold the results of parsing a screen message.
     */
    public record ScreenLayout(Map<String, Node> textFields, Map<String, Node> buttons) {}

    /**
     * Parses a full message string and returns a ScreenLayout object containing the generated UI nodes.
     * @param message The raw message string from the input.
     * @return A ScreenLayout object.
     */
    public ScreenLayout parse(String message) {
        Map<String, Node> textFields = new HashMap<>();
        Map<String, Node> buttons = new HashMap<>();
        String trimmedMessage = message.substring(0, message.length() - 2);
        String[] commands = trimmedMessage.split(";");

        for (String command : commands) {
            String trimmedCmd = command.trim();
            if (trimmedCmd.startsWith("t:")) {
                parseAndCreateTextField(trimmedCmd, textFields);
            } else if (trimmedCmd.startsWith("b:")) {
                parseAndCreateButton(trimmedCmd, buttons);
            }
        }
        return new ScreenLayout(textFields, buttons);
    }

    private void parseAndCreateTextField(String command, Map<String, Node> textFields) {
        try {
            String[] parts = command.substring(2).split("/", 5);
            if (parts.length < 5) return;
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

    private void parseAndCreateButton(String command, Map<String, Node> buttons) {
        try {
            String[] parts = command.substring(2).split("/");
            if (parts.length < 2) return;
            String cellId = parts[0];
            Button button = new Button();
            button.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            button.setStyle("-fx-background-color: #E0E0E0; -fx-border-color: #808080; -fx-border-width: 2; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 5, 0, 0, 1);");
            button.setOnAction(event -> {
                System.out.print("b:" + cellId + ";//");
                System.out.flush();
            });
            buttons.put(cellId, button);
        } catch (Exception e) {
            System.err.println("Failed to parse button command: " + command);
        }
    }

    private void applyFont(Label label, int size, String styleStr) {
        double fontSize;
        switch (size) {
            case FONT_SIZE_SMALL:  fontSize = 14; break;
            case FONT_SIZE_LARGE:  fontSize = 30; break;
            case FONT_SIZE_MEDIUM: default: fontSize = 20; break;
        }
        FontWeight weight = FontWeight.NORMAL;
        FontPosture posture = FontPosture.REGULAR;
        if (!styleStr.contains(String.valueOf(STYLE_REGULAR))) {
            if (styleStr.contains(String.valueOf(STYLE_BOLD))) {
                weight = FontWeight.BOLD;
            }
            if (styleStr.contains(String.valueOf(STYLE_ITALIC))) {
                posture = FontPosture.ITALIC;
            }
        }
        label.setFont(Font.font("sans-serif", weight, posture, fontSize));
    }

    private void applyBackgroundColor(StackPane pane, int color) {
        String style = "-fx-padding: 5;";
        switch (color) {
            case COLOR_PURPLE: style += "-fx-background-color: #800080;"; break;
            case COLOR_RED:    style += "-fx-background-color: #FF0000;"; break;
            case COLOR_GREEN:  style += "-fx-background-color: #008000;"; break;
            case COLOR_BLUE:   style += "-fx-background-color: #0000FF;"; break;
            case COLOR_DEFAULT: default: break;
        }
        pane.setStyle(style);
    }
}
