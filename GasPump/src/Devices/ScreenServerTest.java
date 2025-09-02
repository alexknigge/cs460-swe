package Devices;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * A simple command-line server to test the GasPumpUI application.
 * It listens on a specified port, accepts one client, and then relays messages
 * between the server's console and the client application.
 */
public class ScreenServerTest {

    // Must match the configuration in ScreenCommunicationManager
    private static final int PORT = 1234;

    // Sender interface and implementation for sending messages to the UI
    private interface Sender { void send(String msg); }
    private static final class WriterSender implements Sender {
        private final PrintWriter out;
        WriterSender(PrintWriter out) { this.out = out; }
        @Override public void send(String msg) { out.print(msg + "//"); out.flush(); }
    }

    // Dynamic grade map discovered from incoming screen messages
    private static final java.util.Map<String, Grade> GRADES = new java.util.concurrent.ConcurrentHashMap<>();
    private record Grade(String name, double ppg) {}

    /** Parse a screen-definition message to discover grade options. Pairs t:<id>/<...>/<label>; b:<id>/m; */
    private static void parseGradeOptions(String msg) {
        if (msg == null) return;
        // Split by ';' to get commands
        String[] parts = msg.split(";");
        // First pass: collect labels by id
        java.util.Map<String, String> labels = new java.util.HashMap<>();
        for (String p : parts) {
            String s = p.trim();
            if (s.startsWith("t:")) {
                int idEnd = s.indexOf('/');
                if (idEnd > 2) {
                    String id = s.substring(2, idEnd); // may be "4" or "01"
                    int lastSlash = s.lastIndexOf('/');
                    if (lastSlash > idEnd && lastSlash + 1 < s.length()) {
                        String text = s.substring(lastSlash + 1);
                        labels.put(id, text);
                    }
                }
            }
        }
        // Second pass: for each button b:<id>/m; look up label and try extract price
        for (String p : parts) {
            String s = p.trim();
            if (s.startsWith("b:")) {
                // b:<id>/<type>
                int idEnd = s.indexOf('/');
                if (idEnd > 2) {
                    String id = s.substring(2, idEnd);
                    String label = labels.get(id);
                    if (label != null && !label.isEmpty()) {
                        // Extract $x.xxx if present in label text
                        double ppg = Double.NaN;
                        int dollar = label.indexOf('$');
                        if (dollar >= 0) {
                            int end = dollar + 1;
                            while (end < label.length() && (Character.isDigit(label.charAt(end)) || label.charAt(end) == '.')) end++;
                            try { ppg = Double.parseDouble(label.substring(dollar + 1, end)); } catch (Exception ignored) {}
                        }
                        String name = label.replaceAll("\\$[0-9]+(\\.[0-9]+)?", "").trim();
                        // Fallback default per id if no price found
                        if (Double.isNaN(ppg)) {
                            ppg = switch (id) {
                                case "4" -> 4.49;
                                case "5" -> 4.99;
                                case "6" -> 5.29;
                                default -> 4.59; // generic fallback
                            };
                        }
                        GRADES.put(id, new Grade(name, ppg));
                    }
                }
            }
        }
    }

    // Shift flow meter cells down one row in the screen message
    private static String shiftFlowCellsDownOneRow(String msg) {
        // Replace t:3/ with a temporary tag first to avoid overlap
        String tmp = msg.replace("t:3/", "t:_TMP5_/");
        // Replace t:1/ with t:3/
        tmp = tmp.replace("t:1/", "t:3/");
        // Replace temporary tag with t:5/
        return tmp.replace("t:_TMP5_/", "t:5/");
    }

    // Parse gallons and price values from flow meter message
    private static String[] parseGallonsAndPrice(String flowMsg) {
        String gallons = "0.000";
        String price = "0.000";

        if (flowMsg == null) {
            return new String[]{gallons, price};
        }

        try {
            int idx3 = flowMsg.indexOf("t:3/");
            if (idx3 >= 0) {
                int end = flowMsg.indexOf(';', idx3);
                if (end > idx3) {
                    int lastSlash = flowMsg.lastIndexOf('/', end);
                    if (lastSlash > idx3) {
                        gallons = flowMsg.substring(lastSlash + 1, end).trim();
                        if (gallons.endsWith(" gal")) {
                            gallons = gallons.substring(0, gallons.length() - 4).trim();
                        }
                    }
                }
            }

            int idx5 = flowMsg.indexOf("t:5/");
            if (idx5 >= 0) {
                int end = flowMsg.indexOf(';', idx5);
                if (end > idx5) {
                    int lastSlash = flowMsg.lastIndexOf('/', end);
                    if (lastSlash > idx5) {
                        price = flowMsg.substring(lastSlash + 1, end).trim();
                        if (price.startsWith("$")) {
                            price = price.substring(1).trim();
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return new String[]{gallons, price};
    }

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Test Server started. Waiting for a client to connect on port " + PORT + "...");

            while (true) { // Loop to accept new clients after a disconnect
                try (Socket clientSocket = serverSocket.accept()) {
                    System.out.println("Client connected: " + clientSocket.getInetAddress().getHostAddress());
                    System.out.println("Type a screen message and press Enter to send it to the UI.");
                    System.out.println("Messages from the UI will be displayed here.\n");

                    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                    final String[] lastSentRaw = new String[1];

                    Sender sender = new WriterSender(out);
                    final FlowMeter[] flow = new FlowMeter[1];
                    final String[] baseScreen = new String[1];
                    final String[] lastFlowMsg = new String[1];

                    // Thread to listen for messages FROM the UI client
                    Thread clientListener = new Thread(() -> {
                        try {
                            StringBuilder messageBuilder = new StringBuilder();
                            int c;
                            while ((c = in.read()) != -1) {
                                messageBuilder.append((char) c);
                                if (messageBuilder.toString().endsWith("//")) {
                                    String full = messageBuilder.toString().trim();
                                    System.out.println("Received from UI: " + full);
                                    messageBuilder.setLength(0);

                                    if (full.startsWith("b:")) {
                                        String id = extractButtonId(full);
                                        if (id != null) {
                                            switch (id) {
                                                case "4", "5", "6" -> {
                                                    // Lookup grade info or fallback defaults
                                                    Grade g = GRADES.get(id);
                                                    if (g == null) {
                                                        if ("4".equals(id)) g = new Grade("87 Octane", 4.49);
                                                        else if ("5".equals(id)) g = new Grade("91 Octane", 4.99);
                                                        else g = new Grade("93 Octane", 5.29);
                                                    }
                                                    // Base screen layout for pumping display
                                                    baseScreen[0] =
                                                            "t:01/s:2/f:2/c:0/Pumping " + g.name() + ";" +
                                                            "t:2/s:2/f:1/c:0/Gallons:;" +
                                                            "t:4/s:2/f:1/c:0/Price:;" +
                                                            "t:8/s:1/f:3/c:2/Cancel;b:8/x;";
                                                    double ppg = g.ppg();
                                                    // Create FlowMeter to simulate flow, send updated screen messages
                                                    flow[0] = new FlowMeter(msg -> {
                                                        String shifted = shiftFlowCellsDownOneRow(msg);
                                                        lastFlowMsg[0] = shifted;
                                                        sender.send(baseScreen[0] + shifted);
                                                    }, 0.05, ppg);
                                                    // Show a Finished summary automatically when FlowMeter auto-stops
                                                    flow[0].setOnStop(auto -> {
                                                        String[] parsed = (lastFlowMsg[0] != null) ? parseGallonsAndPrice(lastFlowMsg[0]) : new String[]{"0.000", "0.000"};
                                                        String gallons = parsed[0];
                                                        String price   = parsed[1];
                                                        String title   = auto ? "Pumping Complete" : "Pumping Stopped";
                                                        String summary =
                                                            "t:01/s:2/f:2/c:0/" + title + ";" +
                                                            "t:2/s:2/f:1/c:0/Total Dispensed:;" +
                                                            "t:3/s:3/f:2/c:0/" + gallons + " gal;" +
                                                            "t:4/s:2/f:1/c:0/Total Price:;" +
                                                            "t:5/s:3/f:2/c:0/$" + price + ";" +
                                                            "t:8/s:1/f:3/c:2/Back;b:8/x;";
                                                        sender.send(summary);
                                                    });
                                                    flow[0].initLayout();
                                                    flow[0].start();
                                                }
                                                case "8" -> {
                                                    // Stop pumping when Cancel button pressed
                                                    if (flow[0] != null) flow[0].stop(false);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            // This will happen when the client disconnects.
                        } finally {
                            System.out.println("Client appears to have disconnected.");
                        }
                    });
                    clientListener.start();

                    // Main thread will read from console and send TO the UI client
                    BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
                    String userInput;
                    while ((userInput = stdIn.readLine()) != null && clientSocket.isConnected()) {
                        String trimmed = userInput.trim();
                        // Parse grade options from incoming screen messages to update prices
                        parseGradeOptions(trimmed);
                        lastSentRaw[0] = trimmed;
                        System.out.println("Sending to UI: " + trimmed);
                        out.print(trimmed);
                        out.flush();
                    }

                    // Wait for the listener thread to finish if the console input ends
                    clientListener.join();

                } catch (Exception e) {
                    System.err.println("Error during client session: " + e.getMessage());
                }
                System.out.println("\nWaiting for a new client to connect...");
            }
        } catch (Exception e) {
            System.err.println("Could not start server on port " + PORT);
            e.printStackTrace();
        }
    }
    // Helper to extract button id from message like "b:4//"
    private static String extractButtonId(String full) {
        int colon = full.indexOf(':');
        int end = full.lastIndexOf("//");
        if (colon >= 0 && end > colon + 1) {
            return full.substring(colon + 1, end).trim();
        }
        return null;
    }
}
