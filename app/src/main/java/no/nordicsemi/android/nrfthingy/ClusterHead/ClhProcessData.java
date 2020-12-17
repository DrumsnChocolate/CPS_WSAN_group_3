package no.nordicsemi.android.nrfthingy.ClusterHead;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;

import no.nordicsemi.android.nrfthingy.ClusterHead.packet.BaseDataPacket;
import no.nordicsemi.android.nrfthingy.ClusterHead.packet.SoundEventDataPacket;
import no.nordicsemi.android.nrfthingy.ClusterHead.packet.ActuateThingyPacket;

public class ClhProcessData {

    public static final int MAX_PROCESS_LIST_ITEM = ClhConst.MAX_PROCESS_LIST_ITEM;
    public static final int MICROPHONE_BUFFER_PROCESS_INTERVAL = ClhConst.MICROPHONE_BUFFER_PROCESS_INTERVAL;
    public static final int MICROPHONE_PROCESS_THRESHOLD = ClhConst.MICROPHONE_PROCESS_THRESHOLD;
    public static final double MICROPHONE_PROCESS_DECAY_FACTOR = ClhConst.MICROPHONE_PROCESS_DECAY_FACTOR;
    public static final int MICROPHONE_PROCESS_MIN_DATAPOINTS_ABOVE_THRESHOLD = ClhConst.MICROPHONE_PROCESS_MIN_DATAPOINTS_ABOVE_THRESHOLD;
    private final String LOG_TAG="ClH Processor:";

    private int mMaxProcAllowable = MAX_PROCESS_LIST_ITEM;
    private ArrayList<BaseDataPacket> mClhProcessDataList;
    private int[] mClhMicrophoneDataBuffer;

    public ClhProcessData() {
        mClhProcessDataList = new ArrayList<BaseDataPacket>(MAX_PROCESS_LIST_ITEM);
        clearMicrophoneDataBuffer();
    }

    public ClhProcessData(ArrayList<BaseDataPacket> ClhProcessDataList, int maxProcAllowable) {
        mMaxProcAllowable = maxProcAllowable;
        mClhProcessDataList = ClhProcessDataList;
        clearMicrophoneDataBuffer();
    }

    /**
     * Find the loudest thingy in the current buffer
     *
     * @return ActuateThingyPacket|null When a thingy was found, returns a packet to be transmitted, null otherwise
     */
    public ActuateThingyPacket getLoudestThingy() {
        ArrayList<BaseDataPacket> procList = getProcessDataList();

        SoundEventDataPacket greatestAmplitudePacket = new SoundEventDataPacket();
        greatestAmplitudePacket.setAmplitude(0);
        greatestAmplitudePacket.setThingyId((byte) -1);

        Log.i(LOG_TAG, "Processing event buffer with size "+ procList.size());

        // Loop through all packets in the buffer
        for(int i = 0; i < procList.size(); i++) {
            BaseDataPacket packet = procList.get(i);
            if (packet instanceof SoundEventDataPacket) {
                SoundEventDataPacket soundEventPacket = (SoundEventDataPacket) packet;

                if (soundEventPacket.getAmplitude() > greatestAmplitudePacket.getAmplitude()) {
                    greatestAmplitudePacket = soundEventPacket;
                }

                // Remove this packet, since it has fulfilled its purpose
                procList.remove(i);

                Log.i(LOG_TAG, "Processed a Sound Event packet with packet ID "+ packet.getPacketID());
            } else {
                Log.i(LOG_TAG, "There's a type "+ packet.getPacketType() +" packet in the buffer that I'm not sure how to process");
            }

        }

        ActuateThingyPacket actuateThingyPacket;
        if (greatestAmplitudePacket.getThingyId() >= 0) {
            // Populate Actuate packet with necessary data
            actuateThingyPacket = new ActuateThingyPacket();
            actuateThingyPacket.setThingyId(greatestAmplitudePacket.getThingyId());
            actuateThingyPacket.setSourceID((byte) 0);
            actuateThingyPacket.setDestId(greatestAmplitudePacket.getSourceID());

        } else {
            actuateThingyPacket = null;
        }

        return actuateThingyPacket;
    }

    public SoundEventDataPacket findSoundEventsInMicrophoneBuffer() {
        final int[] data = getMicrophoneDataBuffer();

        // Empty buffer, we have our datapoints
        clearMicrophoneDataBuffer();

        // No need to do anything if the buffer is empty
        if (data.length < 1) {
            return null;
        }

        Log.i(LOG_TAG, "Processing a valid sound dataset of size "+ data.length);

        // This variable will keep track of the points where the data crosses the threshold
        ArrayList<Integer> borderPoints = new ArrayList<>();

        // Do the actual processing by looking at the envelope of the waveform
        // See also https://stackoverflow.com/a/32228352/13871462
        boolean isDataAboveThreshold = false;
        int envelope = data[0];
        for (int i = 1; i < data.length; i++) {
            if (Math.abs(data[i]) > envelope) {
                envelope = Math.abs(data[i]);
                data[i] = envelope;
            } else {
                envelope = (int) ( ((double) envelope) * MICROPHONE_PROCESS_DECAY_FACTOR );
                data[i] = envelope;
            }

            // If the data ever crosses the threshold, save those points
            if (data[i] < MICROPHONE_PROCESS_THRESHOLD && isDataAboveThreshold) {
                isDataAboveThreshold = false;
                borderPoints.add(i);
            } else if (data[i] > MICROPHONE_PROCESS_THRESHOLD && !isDataAboveThreshold) {
                isDataAboveThreshold = true;
                borderPoints.add(i);
            }
        }

        // If there is an odd number of borderPoints, add the last datapoint to the list to close it off nicely
        if (borderPoints.size() % 2 == 1) {
            borderPoints.add(data.length);
        }

        // Remove bordercrossings that are too small in range
        for (int i = 0; i < borderPoints.size() - 1; i += 2) {
            // If the number of datapoints above the border is below the threshold, remove those points
            if (borderPoints.get(i + 1) - borderPoints.get(i) < MICROPHONE_PROCESS_MIN_DATAPOINTS_ABOVE_THRESHOLD) {
                borderPoints.remove(i + 1);
                borderPoints.remove(i);
            }
        }

        if (borderPoints.size() < 1) {
            return null;
        }

        // Generate a single SoundEvent packet with highest amplitude of the remaining valid points
        int highestAmplitude = 0;
        int highestDuration = 0;
        double timePerDatapoint = ((double) MICROPHONE_BUFFER_PROCESS_INTERVAL) / ((double) data.length);
        int i;
        for (i = 0; i < borderPoints.size() / 2; i++) {
            int average = 0;
            int[] range = Arrays.copyOfRange(data, borderPoints.get(2 * i), borderPoints.get(2 * i + 1));
            for (int element : range) {
                average += element;
            }
            average /= range.length;

            if (average > highestAmplitude) {
                highestAmplitude = average;
                highestDuration = (int) ((borderPoints.get(2 * i + 1) - borderPoints.get(2 * i) * timePerDatapoint));
            }
        }

        SoundEventDataPacket packet = new SoundEventDataPacket();
        packet.setAmplitude(highestAmplitude);
        packet.setDuration(highestDuration);
        return packet;
    }

    public ArrayList<BaseDataPacket> getProcessDataList() {
        return mClhProcessDataList;
    }

    public void clearProcessDataList() {
        mClhProcessDataList.clear();
    }

    public int[] getMicrophoneDataBuffer() {
        return mClhMicrophoneDataBuffer;
    }

    public void clearMicrophoneDataBuffer() {
        mClhMicrophoneDataBuffer = new int[0];
    }

    public void addProcessPacketToBuffer(BaseDataPacket data) {
        if (mClhProcessDataList.size() < mMaxProcAllowable) {
            mClhProcessDataList.add(data);
        }
    }

    public void addMicrophoneDataToBuffer(int[] data) {
        // Create new buffer by copying old buffer and new data into one array
        int bufferLength = mClhMicrophoneDataBuffer.length;
        int[] newBuffer = new int[data.length + bufferLength];
        System.arraycopy(mClhMicrophoneDataBuffer, 0, newBuffer, 0, bufferLength);
        System.arraycopy(data, 0, newBuffer, bufferLength, data.length);
        mClhMicrophoneDataBuffer = newBuffer;
    }
}
