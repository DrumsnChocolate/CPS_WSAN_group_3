package no.nordicsemi.android.nrfthingy.ClusterHead.packet;

import android.util.Log;
import android.util.SparseArray;

import java.util.Arrays;

public class SoundDataPacket extends BaseDataPacket {
    public static final byte PACKET_TYPE = 0;

    private static final int THINGY_ID_POS = LAST_BASE_PACKET_BYTE_POS + 1;
    private static final int THINGY_DATA_TYPE_POS = LAST_BASE_PACKET_BYTE_POS + 2;
    private static final int SOUND_POWER_POSH = LAST_BASE_PACKET_BYTE_POS + 3;
    private static final int SOUND_POWER_POSL = LAST_BASE_PACKET_BYTE_POS + 4;

    public SoundDataPacket() {
        setPacketType(PACKET_TYPE);
    }

    @Override
    protected int getDataSize() {
        return SOUND_POWER_POSL + 1;
    }

    @Override
    public SoundDataPacket clone() {
        SoundDataPacket clone = new SoundDataPacket();
        clone.data = Arrays.copyOf(data, data.length);
        return clone;
    }

    public void setSoundPower(int soundPower) {
        data[SOUND_POWER_POSH] = (byte) (soundPower >> 8);
        data[SOUND_POWER_POSL] = (byte) (soundPower & 0x00FF);
        Log.i("Sound power:", "Sound power:" + soundPower);
        Log.i("Sound power:", "Sound power:" + data[SOUND_POWER_POSH]);
        Log.i("Sound power:", "Sound power:" + data[SOUND_POWER_POSL]);
    }

    public void setThingyId(byte id) {
        data[THINGY_ID_POS] = (byte) (id & 0x00FF);
    }

    public void setThingyDataType(byte typeData) {
        data[THINGY_DATA_TYPE_POS] = (byte) (typeData & 0x00FF);
    }

    public byte getThingyId() {
        return data[THINGY_ID_POS];
    }

    public byte getThingyDataType() {
        return data[THINGY_DATA_TYPE_POS];
    }

    public int getSoundPower() {
        return (data[SOUND_POWER_POSH] << 8) + ((int) (data[SOUND_POWER_POSL]) & 0x00FF);
    }

}
