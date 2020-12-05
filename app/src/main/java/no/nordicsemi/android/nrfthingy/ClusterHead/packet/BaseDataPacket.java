package no.nordicsemi.android.nrfthingy.ClusterHead.packet;

import android.util.SparseArray;

public abstract class BaseDataPacket implements Cloneable {
    protected static final int SOURCE_CLH_ID_POS = 0;
    protected static final int PACKET_CLH_ID_POS = 1;
    protected static final int DEST_CLH_ID_POS = 2;
    protected static final int RECV_CLH_ID_POS = 3;
    protected static final int PACKET_TYPE_POS = 4;
    protected static final int HOP_COUNT_POS = 5;

    public static final byte SINK_ID = 0;
    public static final byte BROADCAST_ID = 127;

    protected static final int LAST_BASE_PACKET_BYTE_POS = HOP_COUNT_POS;

    byte[] data = new byte[getDataSize()];

    public BaseDataPacket() {
        // Broadcast packet by default
        setReceiverId(BROADCAST_ID);
    }

    /**
     * Set packet type
     * @param packetType packet type
     */
    public void setPacketType(byte packetType) {
        data[PACKET_TYPE_POS] = packetType;
    }

    /**
     * Set the ID of the source that sent the packet
     * @param sourceID sender
     */
    public void setSourceID(byte sourceID) {
        data[SOURCE_CLH_ID_POS] = (byte) (sourceID & 0x7F);
    }

    /**
     * Set the unique packet ID
     * @param packetID packet id
     */
    public void setPacketID(byte packetID) {
        data[PACKET_CLH_ID_POS] = packetID;
    }

    /**
     * Set the ID of the destination to which the packet should be sent
     * @param destID destination
     */
    public void setDestId(byte destID) {
        data[DEST_CLH_ID_POS] = (byte) (destID & 0x7F);
    }

    /**
     * Set the ID of the receiver who should forward the packet
     * @param receiverId receiver
     */
    public void setReceiverId(byte receiverId) {
        data[RECV_CLH_ID_POS] = (byte) (receiverId & 0x7F);
    }

    /**
     * Set the number of hops this packet has taken
     * @param hop number of hops
     */
    public void setHopCount(byte hop) {
        data[HOP_COUNT_POS] = hop;
    }

    /**
     * Set the data from the bluetooth received data
     * @param data data received via bluetooth
     * @param index index of the data
     */
    public void setDataFromBT(SparseArray<byte[]> data, int index) {
        this.data = getDataArrayFromBTSparseArray(data, index);
    }

    private static byte[] getDataArrayFromBTSparseArray(SparseArray<byte[]> btData, int index) {
        byte[] data;
        if (btData.valueAt(index) == null) {
           data = new byte[2];
        } else {
            data = new byte[btData.valueAt(index).length + 2];
            System.arraycopy(btData.valueAt(index), 0, data, PACKET_CLH_ID_POS + 1, btData.valueAt(index).length);
        }
        data[SOURCE_CLH_ID_POS] = (byte) (btData.keyAt(index) >> 8);
        data[PACKET_CLH_ID_POS] = (byte) (btData.keyAt(index) & 0xFF);
        return data;
    }

    /**
     * Set the data from the bluetooth received data
     * @param data data received via bluetooth
     */
    public void setDataFromBT(SparseArray<byte[]> data) {
        setDataFromBT(data, 0);
    }

    /**
     * Set the data bytes
     * @param data data bytes
     */
    public void setData(byte[] data) {
        this.data = data;
    }

    /**
     * Get the type of the packet
     * @return packet type
     */
    public byte getPacketType() {
        return data[PACKET_TYPE_POS];
    }

    /**
     * Get the type of the packet based on bluetooth received data
     * @param data data received via bluetooth
     * @return packet type
     */
    public static byte getPacketTypeFromBT(SparseArray<byte[]> data) {
        return getDataArrayFromBTSparseArray(data, 0)[PACKET_TYPE_POS];
    }

    /**
     * Get the ID of the source that sent the packet as integer
     * @return sender
     */
    public int getSourcePacketID() {
        return (data[SOURCE_CLH_ID_POS] << 8) + ((int) (data[PACKET_CLH_ID_POS]) & 0x00FF);
    }

    /**
     * Get the ID of the source that sent the packet
     * @return sender
     */
    public byte getSourceID() {
        return data[SOURCE_CLH_ID_POS];
    }

    /**
     * Get the unique packet ID
     * @return packet id
     */
    public byte getPacketID() {
        return data[PACKET_CLH_ID_POS];
    }

    /**
     * Get the ID of the destination to which the packet should be sent
     * @return destination
     */
    public byte getDestinationID() {
        return data[DEST_CLH_ID_POS];
    }

    /**
     * Get the ID of the receiver who should forward the packet
     * @return receiver
     */
    public byte getReceiverId() {
        return data[RECV_CLH_ID_POS];
    }

    /**
     * Get the number of hops this packet has taken
     * @return number of hops
     */
    public byte getHopCounts() {
        return data[HOP_COUNT_POS];
    }

    /**
     * Return the packet data as a byte array
     * @return byte array that contains all the data
     */
    public byte[] getData() {
        return data;
    }

    /**
     * Create a clone of the packet
     * @return packet clone
     */
    public abstract BaseDataPacket clone();

    /**
     * Get the total size of the packet
     *
     * @return packet size in bytes
     */
    protected abstract int getDataSize();
}
