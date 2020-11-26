package no.nordicsemi.android.nrfthingy.ClusterHead;

import java.util.ArrayList;

import no.nordicsemi.android.nrfthingy.ClusterHead.packet.BaseDataPacket;

public class ClhProcessData {

    public static final int MAX_PROCESS_LIST_ITEM = 128;
    private int mMaxProcAllowable = MAX_PROCESS_LIST_ITEM;
    private ArrayList<BaseDataPacket> mClhProcessDataList;


    public ClhProcessData() {
        mClhProcessDataList = new ArrayList<BaseDataPacket>(MAX_PROCESS_LIST_ITEM);

    }

    public ClhProcessData(ArrayList<BaseDataPacket> ClhProcessDataList, int maxProcAllowable) {
        mMaxProcAllowable = maxProcAllowable;
        mClhProcessDataList = ClhProcessDataList;
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
