package no.nordicsemi.android.nrfthingy.ClusterHead.packet;

import android.util.Log;
import android.util.SparseArray;

import java.util.Arrays;

public class SoundDataPacket extends BaseDataPacket {
    private static final int THINGY_ID_POS = 4;
    private static final int THINGY_DATA_TYPE_POS = 5;
    private static final int SOUND_POWER_POSH = 6;
    private static final int SOUND_POWER_POSL = 7;

    protected static final int DATA_SIZE = SOUND_POWER_POSL + 1;

    @Override
    protected int getDataSize() {
        return DATA_SIZE;
    }

    @Override
    public BaseDataPacket clone() {
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
