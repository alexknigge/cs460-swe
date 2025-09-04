package Server;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
// TODO: Rename vars and update javadoc comments
/**
 * Maps port numbers to device names and provides configuration constants for the system.
 */
public class DeviceMapper {

    public static final String MAIN_HOST_NAME = "main host";

    /**
     * The network address of the device server (for local testing).
     */
    public static final String DEVICE_SERVER_HOST = "localhost";
    /**
     * The designated port for the pump device, which will act as a server.
     */
    public static final int PUMP_SERVER_PORT = 12345;
    private static final List<String> deviceClientIDs = Arrays.asList("pumpToMain",
            "cardReaderToMain",
            "flowMeterToMain",
            "screenToMain");
    private static final List<String> mainServerIDs = Arrays.asList("MainToPump",
            "MainToCardReader",
            "MainToFlowMeter",
            "MainToScreen");
    public static boolean shouldIDBeAServer(String serverID) {
        return mainServerIDs.contains(serverID);
    }

    // A map of port numbers to their corresponding device names.
    private static final Map<String, Integer> idToPortMap = Map.of(
            // Device Mappings
            "pumpToMain", 1234,
            "cardReaderToMain", 1235,
            "flowMeterToMain", 1236,
            "screenToMain", 1237,
            // Main (Server) Mappings
            "MainToPump", 1234,
            "MainToCardReader", 1235,
            "MainToFlowMeter", 1246,
            "MainToScreen", 1247
    );

    /**
     * Returns the device name for a given port.
     *
     * @param deviceID the port number
     * @return the device name as a String
     * @throws IllegalArgumentException if the port is not mapped to any device.
     */
    public static int getDevicePort(String deviceID) {
        if (!idToPortMap.containsKey(deviceID)) {
            throw new IllegalArgumentException("Device " + deviceID + " is not mapped to any port.");
        }
        return idToPortMap.get(deviceID);
    }
}
