package no.nordicsemi.android.nrfthingy.ClusterHead.packet;

import android.util.SparseArray;

public abstract class BaseDataPacket implements Cloneable {
    protected static final int SOURCE_CLH_ID_POS = 0;
    protected static final int PACKET_CLH_ID_POS = 1;
    protected static final int DEST_CLH_ID_POS = 2;
    protected static final int HOP_COUNT_POS = 3;

    byte[] data = new byte[getDataSize()];

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
     * @param destID receiver
     */
    public void setDestId(byte destID) {
        data[DEST_CLH_ID_POS] = (byte) (destID & 0x7F);
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
    public void setData(SparseArray<byte[]> data, int index) {
        this.data[SOURCE_CLH_ID_POS] = (byte) (data.keyAt(index) >> 8);
        this.data[PACKET_CLH_ID_POS] = (byte) (data.keyAt(index) & 0x00FF);
        if (data.valueAt(index) != null) {
            System.arraycopy(data.valueAt(index), 0, this.data, PACKET_CLH_ID_POS + 1, data.valueAt(index).length);
        }
    }

    /**
     * Set the data from the bluetooth received data
     * @param data data received via bluetooth
     */
    public void setData(SparseArray<byte[]> data) {
        setData(data, 0);
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
     * @return receiver
     */
    public byte getDestinationID() {
        return data[DEST_CLH_ID_POS];
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
