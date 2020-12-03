package no.nordicsemi.android.nrfthingy.ClusterHead.packet;

import android.util.Log;

import java.util.Arrays;

public class SoundEventDataPacket extends BaseDataPacket {
    public static final byte PACKET_TYPE = 2;

    private static final int THINGY_ID_POS = 5;
    private static final int THINGY_DATA_TYPE_POS = 6;
    private static final int EVENT_AMPLITUDE_POSH = 7; // Amplitude of sound event, high part of int
    private static final int EVENT_AMPLITUDE_POSL = 8; // Amplitude of sound event, low part of int
    private static final int EVENT_DURATION_POSH = 9; // Duration of sound event in milliseconds, high half of int
    private static final int EVENT_DURATION_POSL = 10; // Duration of sound event in milliseconds, low half of int

    public SoundEventDataPacket() {
        setPacketType(PACKET_TYPE);
    }

    @Override
    protected int getDataSize() {
        return EVENT_DURATION_POSL + 1;
    }

    @Override
    public SoundEventDataPacket clone() {
        SoundEventDataPacket clone = new SoundEventDataPacket();
        clone.data = Arrays.copyOf(data, data.length);
        return clone;
    }

    public void setAmplitude(int amplitude) {
        data[EVENT_AMPLITUDE_POSH] = (byte) (amplitude >> 8);
        data[EVENT_AMPLITUDE_POSL] = (byte) (amplitude & 0x00FF);
        Log.i("Sound power:", "Sound event amplitude:" + amplitude);
    }

    public int getAmplitude() {
        return (data[EVENT_AMPLITUDE_POSH] << 8) + ((int) (data[EVENT_AMPLITUDE_POSL]) & 0x00FF);
    }

    public void setDuration(int duration) {
        data[EVENT_DURATION_POSH] = (byte) (duration >> 8);
        data[EVENT_DURATION_POSL] = (byte) (duration & 0x00FF);
        Log.i("Sound power:", "Sound event duration:" + duration);
    }

    public int getDuration() {
        return (data[EVENT_DURATION_POSH] << 8) + ((int) (data[EVENT_DURATION_POSL]) & 0x00FF);
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

}
