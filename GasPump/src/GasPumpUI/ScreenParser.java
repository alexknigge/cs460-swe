package GasPumpUI;

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
    // --- Centralized Style Constants ---
    public static final String STYLE_BUTTON_DEFAULT = "-fx-background-color: #E0E0E0; -fx-border-color: #808080; -fx-border-width: 2; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 5, 0, 0, 1);";
    public static final String STYLE_BUTTON_SELECTED = "-fx-background-color: #BEBEBE; -fx-border-color: #606060; -fx-border-width: 2; -fx-background-insets: 1; -fx-effect: innershadow(three-pass-box, rgba(0,0,0,0.4), 5, 0, 0, 1);";
    public static final char BUTTON_TYPE_MUTUALLY_EXCLUSIVE = 'm';
    private static final String STYLE_BG_PURPLE = "-fx-background-color: #800080;";
    private static final String STYLE_BG_RED = "-fx-background-color: #FF0000;";
    private static final String STYLE_BG_GREEN = "-fx-background-color: #008000;";
    private static final String STYLE_BG_BLUE = "-fx-background-color: #0000FF;";
    // --- Style and Layout Constants ---
    private static final int FONT_SIZE_SMALL = 1, FONT_SIZE_MEDIUM = 2, FONT_SIZE_LARGE = 3;
    private static final int STYLE_REGULAR = 1, STYLE_BOLD = 2, STYLE_ITALIC = 3;
    private static final int COLOR_DEFAULT = 0, COLOR_PURPLE = 1, COLOR_RED = 2, COLOR_GREEN = 3, COLOR_BLUE = 4;

    public ScreenLayout parse(String message) {
        Map<String, Node> textFields = new HashMap<>();
        Map<String, ButtonInfo> buttons = new HashMap<>();
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

    private void parseAndCreateButton(String command, Map<String, ButtonInfo> buttons) {
        try {
            String[] parts = command.substring(2).split("/");
            if (parts.length < 2) return;
            String cellId = parts[0];
            char type = parts[1].charAt(0);
            Button button = new Button();
            button.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            button.setStyle(STYLE_BUTTON_DEFAULT);
            buttons.put(cellId, new ButtonInfo(button, type));
        } catch (Exception e) {
            System.err.println("Failed to parse button command: " + command);
        }
    }

    private void applyFont(Label label, int size, String styleStr) {
        double fontSize = switch (size) {
            case FONT_SIZE_SMALL -> 14;
            case FONT_SIZE_LARGE -> 30;
            default -> 20;
        };
        FontWeight weight = FontWeight.NORMAL;
        FontPosture posture = FontPosture.REGULAR;
        if (!styleStr.contains(String.valueOf(STYLE_REGULAR))) {
            if (styleStr.contains(String.valueOf(STYLE_BOLD)))
                weight = FontWeight.BOLD;
            if (styleStr.contains(String.valueOf(STYLE_ITALIC)))
                posture = FontPosture.ITALIC;
        }
        label.setFont(Font.font("sans-serif", weight, posture, fontSize));
    }

    private void applyBackgroundColor(StackPane pane, int color) {
        String style = "-fx-padding: 5;";
        switch (color) {
            case COLOR_PURPLE:
                style += STYLE_BG_PURPLE;
                break;
            case COLOR_RED:
                style += STYLE_BG_RED;
                break;
            case COLOR_GREEN:
                style += STYLE_BG_GREEN;
                break;
            case COLOR_BLUE:
                style += STYLE_BG_BLUE;
                break;
            case COLOR_DEFAULT:
            default:
                break;
        }
        pane.setStyle(style);
    }

    /**
     * A record to hold a created button and its protocol-defined type.
     */
    public record ButtonInfo(Button button, char type) {
    }

    /**
     * A record to hold the results of parsing a screen message.
     */
    public record ScreenLayout(Map<String, Node> textFields,
                               Map<String, ButtonInfo> buttons) {
    }
}

