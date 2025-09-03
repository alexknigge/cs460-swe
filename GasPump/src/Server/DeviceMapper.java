package Server;

import java.util.Map;

public class DeviceMapper {

    // map ports to devices
    private static final Map<Integer, String> PORTMAP = Map.of(
            12345, "pump",
            12346, "card reader",
            12347, "flow meter",
            12348, "screen"
    );

    /**
     * Returns the device address for a given port.
     * @param port the port number
     * @return the device address
     * @throws IllegalArgumentException if port is not mapped
     */
    public static String getDeviceAddress(int port) {
        if (!PORTMAP.containsKey(port)) {
            throw new IllegalArgumentException("Port " + port + " is not mapped to any device.");
        }
        return PORTMAP.get(port);
    }
}
