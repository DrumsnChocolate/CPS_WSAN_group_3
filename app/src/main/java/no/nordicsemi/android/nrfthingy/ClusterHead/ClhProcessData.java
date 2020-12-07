package no.nordicsemi.android.nrfthingy.ClusterHead;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;

import no.nordicsemi.android.nrfthingy.ClusterHead.packet.ActuateThingyPacket;
import no.nordicsemi.android.nrfthingy.ClusterHead.packet.BaseDataPacket;
import no.nordicsemi.android.nrfthingy.ClusterHead.packet.SoundEventDataPacket;

public class ClhProcessData {

    public static final int MAX_PROCESS_LIST_ITEM = ClhConst.MAX_PROCESS_LIST_ITEM;
    private final String LOG_TAG="ClH Processor:";

    private int mMaxProcAllowable = MAX_PROCESS_LIST_ITEM;
    private ArrayList<BaseDataPacket> mClhProcessDataList;

    private int NextToProcess = 0;
    private int[] FilteredData;
    private int audioThreshold = 0; // Set threshold for the sound
    int smoothingSetting = 2;       // Set how smooth the filter should make the data
    private boolean threshold = false;


    public ClhProcessData() {
        mClhProcessDataList = new ArrayList<BaseDataPacket>(MAX_PROCESS_LIST_ITEM);

    }

    public ClhProcessData(ArrayList<BaseDataPacket> ClhProcessDataList, int maxProcAllowable) {
        mMaxProcAllowable = maxProcAllowable;
        mClhProcessDataList = ClhProcessDataList;
    }

    /**
     * Find the loudest thingy in the current buffer
     *
     * @return ActuateThingyPacket|null When a thingy was found, returns a packet to be transmitted, null otherwise
     */
    public ActuateThingyPacket getLoudestThingy() {
        ArrayList<BaseDataPacket> procList = getProcessDataList();

        byte thingyIdToActuate = -1;
        int greatestAmplitude = 0;

        // Loop through all packets in the buffer
        for(int i = 0; i < procList.size(); i++) {
            BaseDataPacket packet = procList.get(i);
            if (packet instanceof SoundEventDataPacket) {
                SoundEventDataPacket soundEventPacket = (SoundEventDataPacket) packet;

                if (soundEventPacket.getAmplitude() > greatestAmplitude) {
                    greatestAmplitude = soundEventPacket.getAmplitude();
                    thingyIdToActuate = soundEventPacket.getThingyId();
                }

                // Remove this packet, since it has fulfilled its purpose
                procList.remove(i);

            } else {
                Log.i(LOG_TAG, "There's a type "+ packet.getPacketType() +" packet in the buffer that I'm not sure how to process");
            }

        }

        ActuateThingyPacket actuateThingyPacket;
        if (thingyIdToActuate >= 0) {
            actuateThingyPacket = new ActuateThingyPacket();
            actuateThingyPacket.setThingyId(thingyIdToActuate);
        } else {
            actuateThingyPacket = null;
        }

        return actuateThingyPacket;
    }


    // A method that analyses the data in the sink:
    public void process(byte[] data) {/*
        ArrayList<BaseDataPacket> processDataList = getProcessDataList();


        //  - Apply a low pass filter to remove the noise:
        int value = processDataList.get(0).getSoundPower();

        // Check if the first data point surpasses the audioThreshold
        if (value >= audioThreshold) {
            threshold = true;
        }

        for (int i = 1; i < processDataList.size(); ++i) {
            int currentValue = processDataList.get(i).getSoundPower();

            // Check if the next data point surpasses the audioThreshold
            if (currentValue >= audioThreshold) {
                threshold = true;
            }

            value += (currentValue - value) / smoothingSetting;
            FilteredData[i] = value;
        }

        //  - detecting a change in statistics (mean, variance, etc.)
        //  - outlier detection and machine learning (need data clustering for this, see: http://java-ml.sourceforge.net/)
        //  - pattern recognition (in time and/or frequency spectrum)
        //      * cross-correlation?
        //      * Fourier Transform?

        // Based on the data processing above, check if an event has happened:
        if (threshold) { // Add all other specifications with && and ||
            //      - send correct data to be visualized
            //      - notify the right thingy to light the LED
        }
        else {
            //      - remove useless data and reset everything
            threshold = false;
        }
        ++NextToProcess;
        */
    }

    public ArrayList<BaseDataPacket> getProcessDataList() {
        return mClhProcessDataList;
    }

    public void clearProcessDataList() {
        mClhProcessDataList.clear();
    }

    public void addProcessPacketToBuffer(BaseDataPacket data) {
        if (mClhProcessDataList.size() < mMaxProcAllowable) {
            mClhProcessDataList.add(data);
        }
    }
}
