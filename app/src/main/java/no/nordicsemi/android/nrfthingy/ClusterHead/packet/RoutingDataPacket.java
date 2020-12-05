package no.nordicsemi.android.nrfthingy.ClusterHead.packet;

import java.util.Arrays;

public class RoutingDataPacket extends BaseDataPacket {
    public static final byte PACKET_TYPE = 1;

    /**
     * Position in the data array of the device address ID
     * to which the route should be found.
     */
    private static final int ROUTE_TO_ID_POS = LAST_BASE_PACKET_BYTE_POS + 1;

    /**
     * Position in the data array of the route data.
     * The route field is the last data field and has a dynamic length.
     */
    private static final int ROUTE_POS = LAST_BASE_PACKET_BYTE_POS + 2;

    public RoutingDataPacket() {
        setPacketType(PACKET_TYPE);
    }

    @Override
    protected int getDataSize() {
        // Account for dynamic length of route field
        if (data == null) {
            return ROUTE_POS + 1;
        } else {
            return data.length;
        }
    }

    @Override
    public RoutingDataPacket clone() {
        RoutingDataPacket clone = new RoutingDataPacket();
        clone.data = Arrays.copyOf(data, data.length);
        return clone;
    }

    /**
     * Get the route that this packet has taken
     * @return list of addresses the packet has travelled through
     */
    public byte[] getRoute() {
        return Arrays.copyOfRange(data, ROUTE_POS, getDataSize() - ROUTE_POS);
    }

    /**
     * Check if an address is in the route
     * @param addressToFind address to find in the route
     * @return true if the address is in the route, false otherwise
     */
    public boolean routeContains(byte addressToFind) {
        for (byte address : getRoute()) {
            if (address == addressToFind) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if the route has been resolved
     * @return
     */
    public boolean routeResolved() {
        return routeContains(getRouteToId());
    }

    /**
     * Set the route that this packet has taken
     * @param route list of addresses the packet has travelled through
     */
    public void setRoute(byte[] route) {
        // Extend length of data
        byte[] newData = Arrays.copyOf(data, ROUTE_POS + route.length);

        // Copy new route into data
        System.arraycopy(route, 0, newData, ROUTE_POS, route.length);

        data = newData;
    }

    /**
     * Add one address to the route
     * @param address address to add
     */
    public void addToRoute(byte address) {
        // Extend length of data
        byte[] newData = Arrays.copyOf(data, getDataSize() + 1);

        // Set add the address to the route
        newData[getDataSize() - 1] = address;

        data = newData;
    }

    /**
     * Set the address of the device to which a route should be found
     * @param address address of the device to find a route to
     */
    public void setRouteToId(byte address) {
        data[ROUTE_TO_ID_POS] = address;
    }

    /**
     * Get the address of the device to which a route should be found
     * @return address address of the device to find a route to
     */
    public byte getRouteToId() {
        return data[ROUTE_TO_ID_POS];
    }
}
