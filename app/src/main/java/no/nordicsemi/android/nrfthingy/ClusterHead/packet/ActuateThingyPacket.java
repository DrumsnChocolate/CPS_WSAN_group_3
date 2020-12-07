package no.nordicsemi.android.nrfthingy.ClusterHead.packet;

import java.util.Arrays;

public class ActuateThingyPacket extends BaseDataPacket {
    public static final byte PACKET_TYPE = 3;

    private static final int THINGY_ID_POS = 5;

    public ActuateThingyPacket() {
        setPacketType(PACKET_TYPE);
    }

    @Override
    protected int getDataSize() {
        return THINGY_ID_POS + 1;
    }

    @Override
    public ActuateThingyPacket clone() {
        ActuateThingyPacket clone = new ActuateThingyPacket();
        clone.data = Arrays.copyOf(data, data.length);
        return clone;
    }

    public void setThingyId(byte id) {
        data[THINGY_ID_POS] = (byte) (id & 0x00FF);
    }

    public byte getThingyId() {
        return data[THINGY_ID_POS];
    }

}
