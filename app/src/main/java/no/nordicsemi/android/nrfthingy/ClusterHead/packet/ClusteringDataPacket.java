package no.nordicsemi.android.nrfthingy.ClusterHead.packet;

import android.bluetooth.BluetoothDevice;
import android.util.SparseArray;

public class ClusteringDataPacket extends BaseDataPacket {
    public static final int PACKET_TYPE = 3;

    public ClusteringDataPacket(byte[] data) {
        setData(data);
    }

    public ClusteringDataPacket() {

    }


    public void addThingyToData(BluetoothDevice device) {
        //TODO implement a byte representation of the bluetoothdevice
    }

    @Override
    public BaseDataPacket clone() {
        return null;
    }

    @Override
    protected int getDataSize() {
        return -1;
        //TODO implement
    }

    @Override
    public void setDataFromBT(SparseArray<byte[]> data) {
        super.setDataFromBT(data);

    }
}
