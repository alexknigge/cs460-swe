package Server;

import java.util.Map;

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

    // A map of port numbers to their corresponding device names.
    private static final Map<Integer, String> PORTMAP = Map.of(
            12345, "pump",
            12346, "card reader",
            12347, "flow meter",
            12348, "screen",
            20000, MAIN_HOST_NAME // The port that identifies an IOPort as the main host client.
    );

    /**
     * Returns the device name for a given port.
     *
     * @param port the port number
     * @return the device name as a String
     * @throws IllegalArgumentException if the port is not mapped to any device.
     */
    public static String getDeviceAddress(int port) {
        if (!PORTMAP.containsKey(port)) {
            throw new IllegalArgumentException("Port " + port + " is not mapped to any device.");
        }
        return PORTMAP.get(port);
    }
}
