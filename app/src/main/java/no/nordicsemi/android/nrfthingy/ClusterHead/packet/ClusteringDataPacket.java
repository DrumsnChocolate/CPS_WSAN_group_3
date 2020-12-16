package no.nordicsemi.android.nrfthingy.ClusterHead.packet;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import no.nordicsemi.android.nrfthingy.ClusterHead.ClusterLeaf;

public class ClusteringDataPacket extends BaseDataPacket {
    public static final byte PACKET_TYPE = 3;
    public static final int CLUSTER_POS = LAST_BASE_PACKET_BYTE_POS + 1;

    public static final int ELEMENT_ADDRESS_RELATIVE_POS = 0;
    public static final int ELEMENT_ADDRESS_SIZE = 42 / Byte.SIZE; // 42 bit address for bluetooth. // "00:11:22:AA:BB:CC".getBytes().length; //  Bluetooth addresses look like this: "00:11:22:AA:BB:CC" and chars need 2 bytes.
    public static final int ELEMENT_RSSI_RELATIVE_POS = ELEMENT_ADDRESS_RELATIVE_POS + ELEMENT_ADDRESS_SIZE;
    public static final int ELEMENT_RSSI_SIZE = 4;
    public static final int ELEMENT_CONNECTED_RELATIVE_POS = ELEMENT_RSSI_RELATIVE_POS + ELEMENT_RSSI_SIZE;
    public static final int ELEMENT_CONNECTED_SIZE = 1; // booleans 'only' need one byte..

    public static final int CLUSTER_ELEMENT_SIZE = ELEMENT_CONNECTED_RELATIVE_POS + ELEMENT_CONNECTED_SIZE;

    public ClusteringDataPacket() {
        setPacketType(PACKET_TYPE);
    }

    public void setCluster(List<ClusterLeaf> cluster) {
        byte[] newData = Arrays.copyOf(data, getDataSize() + cluster.size() * CLUSTER_ELEMENT_SIZE);
        for (int i = 0; i < cluster.size(); i++) {
            ClusterLeaf leaf = cluster.get(i);
            byte[] leaf_bytes = leafToBytes(leaf);
            System.arraycopy(leaf_bytes, 0, newData, getDataSize() + i * CLUSTER_ELEMENT_SIZE, CLUSTER_ELEMENT_SIZE);
        }
    }


    public void addThingyToData(ClusterLeaf leaf) {
        // Bluetooth addresses look like this: "00:11:22:AA:BB:CC"
        byte[] leafBytes = leafToBytes(leaf);
        byte[] newData = Arrays.copyOf(data, getDataSize() + CLUSTER_ELEMENT_SIZE);
        System.arraycopy(leafBytes, 0, newData, getDataSize(), CLUSTER_ELEMENT_SIZE);
        data = newData;

    }

    private byte[] leafToBytes(ClusterLeaf leaf) {
        byte[] result = new byte[CLUSTER_ELEMENT_SIZE];
        byte[] addressBytes = addressToBytes(leaf.getAddress());
        byte[] rssi_bytes = ByteBuffer.allocate(4).putInt(leaf.getRssi()).array();
        byte[] connected_bytes = {(byte) (leaf.isConnected() ? 1 : 0)};
        System.arraycopy(addressBytes, 0, result, ELEMENT_ADDRESS_RELATIVE_POS, ELEMENT_ADDRESS_SIZE);
        System.arraycopy(rssi_bytes, 0, result, ELEMENT_RSSI_RELATIVE_POS, ELEMENT_RSSI_SIZE);
        System.arraycopy(connected_bytes, 0, result, ELEMENT_CONNECTED_RELATIVE_POS, ELEMENT_CONNECTED_SIZE);
        return result;
    }


    private List<ClusterLeaf> getCluster() {
        ArrayList<ClusterLeaf> result = new ArrayList<>();
        int clusterSize = (getDataSize() - CLUSTER_POS) / CLUSTER_ELEMENT_SIZE;
        for (int i = 0; i < clusterSize; i++) {
            byte[] leafBytes = new byte[CLUSTER_ELEMENT_SIZE];
            System.arraycopy(data, CLUSTER_POS + i * CLUSTER_ELEMENT_SIZE, leafBytes, 0, CLUSTER_ELEMENT_SIZE);
            result.add(leafFromBytes(leafBytes));
        }
        return result;
    }

    private ClusterLeaf leafFromBytes(byte[] leafBytes) {
        byte[] addressBytes = new byte[ELEMENT_ADDRESS_SIZE];
        byte[] rssiBytes = new byte[ELEMENT_RSSI_SIZE];
        byte[] connectedBytes = new byte[ELEMENT_CONNECTED_SIZE];
        System.arraycopy(leafBytes, ELEMENT_ADDRESS_RELATIVE_POS, addressBytes, 0, ELEMENT_ADDRESS_SIZE);
        System.arraycopy(leafBytes, ELEMENT_RSSI_RELATIVE_POS, rssiBytes, 0, ELEMENT_RSSI_SIZE);
        System.arraycopy(leafBytes, ELEMENT_CONNECTED_RELATIVE_POS, connectedBytes, 0, ELEMENT_CONNECTED_SIZE);
        String address = addressFromBytes(addressBytes);
        int rssi = ByteBuffer.wrap(rssiBytes).getInt();
        boolean connected = connectedBytes[0] == (byte) 1;
        return new ClusterLeaf(address, rssi, connected);
    }


    @Override
    public BaseDataPacket clone() {
        ClusteringDataPacket clone = new ClusteringDataPacket();
        clone.data = Arrays.copyOf(data, data.length);
        return clone;
    }

    @Override
    protected int getDataSize() {
        if (data == null) {
            return CLUSTER_POS;
        } else {
            return data.length;
        }
    }


    private byte[] addressToBytes(String address) {
        String[] parts = address.split(":");
        byte[] result = new byte[ELEMENT_ADDRESS_SIZE];
        int r = 0;
        for (int i = 0; i < parts.length; i++) {
            for (int j = 0; j < parts[i].length(); j++) {
                byte b;
                char c = parts[i].charAt(j);
                b = Byte.parseByte("" + c, 16);
                result[r] = b;
                r++;
            }
        }
        return result;
    }

    private String addressFromBytes(byte[] addressBytes) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (byte b : addressBytes) {
            if(i!=0 && (i%2) == 0) {
                sb.append(":");
            }
            sb.append(String.format("%1X", b));
            i++;
        }
        return sb.toString();
    }
}
