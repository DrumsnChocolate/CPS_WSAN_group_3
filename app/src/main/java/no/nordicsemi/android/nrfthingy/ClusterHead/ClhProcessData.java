package no.nordicsemi.android.nrfthingy.ClusterHead;

import java.util.ArrayList;

import no.nordicsemi.android.nrfthingy.ClusterHead.packet.BaseDataPacket;

public class ClhProcessData {

    public static final int MAX_PROCESS_LIST_ITEM = ClhConst.MAX_PROCESS_LIST_ITEM;
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
    
    // A method that analyses the data in the sink:
    public void process(byte[] data) {
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
    }

    public ArrayList<BaseDataPacket> getProcessDataList() {
        return mClhProcessDataList;
    }

    public void addProcessPacketToBuffer(BaseDataPacket data) {
        if (mClhProcessDataList.size() < mMaxProcAllowable) {
            mClhProcessDataList.add(data);
        }
    }
}
