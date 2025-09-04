package Server;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Maps device IDS to port numbers and provides configuration constants for the system.
 */
public class DeviceMapper {

    public static final String PUMP_TO_MAIN = "pumpToMain";
    public static final String MAIN_TO_PUMP = "MainToPump";

    public static final String CARD_READER_TO_MAIN = "cardReaderToMain";
    public static final String MAIN_TO_CARD_READER = "MainToCardReader";

    public static final String FLOW_METER_TO_MAIN = "flowMeterToMain";
    public static final String MAIN_TO_FLOW_METER = "MainToFlowMeter";

    public static final String SCREEN_TO_MAIN = "screenToMain";
    public static final String MAIN_TO_SCREEN = "MainToScreen";

    private static final List<String> deviceClientIDs = Arrays.asList(
            PUMP_TO_MAIN,
            CARD_READER_TO_MAIN,
            FLOW_METER_TO_MAIN,
            SCREEN_TO_MAIN);
    private static final List<String> mainServerIDs = Arrays.asList(
            MAIN_TO_PUMP,
            MAIN_TO_CARD_READER,
            MAIN_TO_FLOW_METER,
            MAIN_TO_SCREEN);
    // A map of port numbers to their corresponding device names.
    private static final Map<String, Integer> idToPortMap = Map.of(
            // Device Mappings
            PUMP_TO_MAIN, 1234,
            CARD_READER_TO_MAIN, 1235,
            FLOW_METER_TO_MAIN, 1236,
            SCREEN_TO_MAIN, 1237,
            // Main (Server) Mappings
            MAIN_TO_PUMP, 1234,
            MAIN_TO_CARD_READER, 1235,
            MAIN_TO_FLOW_METER, 1236,
            MAIN_TO_SCREEN, 1237
    );

    /**
     * Returns whether a device should be a server.
     *
     * @param deviceID The ID of the device
     * @return True if the device should be a server, false if it should be a client (or the deviceID does not exist)
     */
    public static boolean shouldIDBeAServer(String deviceID) {
        return mainServerIDs.contains(deviceID);
    }

    /**
     * Returns a port for a given deviceID.
     *
     * @param deviceID The string ID of the device (defined as constants in DeviceMapper)
     * @return the port number.
     * @throws IllegalArgumentException if the deviceID is not mapped to any port.
     */
    public static int getDevicePort(String deviceID) {
        if (!idToPortMap.containsKey(deviceID)) {
            throw new IllegalArgumentException("Device " + deviceID + " is not mapped to any port.");
        }
        return idToPortMap.get(deviceID);
    }
}
