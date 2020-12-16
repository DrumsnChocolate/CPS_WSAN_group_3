package no.nordicsemi.android.nrfthingy.ClusterHead;

import java.util.ArrayList;

import no.nordicsemi.android.nrfthingy.ClusterHead.packet.BaseDataPacket;
import no.nordicsemi.android.nrfthingy.ClusterHead.packet.SoundDataPacket;
import no.nordicsemi.android.nrfthingy.SoundFragment;

public class ClusterHead {
    private static final int MAX_ADVERTISE_LIST_ITEM=ClhConst.MAX_ADVERTISE_LIST_ITEM; //max items in waiting list for advertising
    private static final int MAX_PROCESS_LIST_ITEM=ClhConst.MAX_PROCESS_LIST_ITEM; //max items in waiting list for processing
    private boolean mIsSink=false;
    private byte mClhID=1;
    private final ArrayList<BaseDataPacket> mClhAdvDataList =new ArrayList<BaseDataPacket>(MAX_ADVERTISE_LIST_ITEM);
    private final ClhAdvertise mClhAdvertiser=new ClhAdvertise(mClhAdvDataList,MAX_ADVERTISE_LIST_ITEM);

    private final ClhScan mClhScanner=new ClhScan();

    private final ArrayList<BaseDataPacket> mClhProcDataList =new ArrayList<>(MAX_PROCESS_LIST_ITEM);
    private final ClhProcessData mClhProcessData=new ClhProcessData(mClhProcDataList,MAX_PROCESS_LIST_ITEM);

    public ClusterHead(){}

    //construtor,
    //params: id: cluster head ID
    public ClusterHead(byte id)
    {
        if(id>127) id-=127;
        setClhID(id, false);
    }

    public ClusterHead(byte id, SoundFragment soundFragmentObject) {
        this(id);
        mClhScanner.setSoundFragmentObject(soundFragmentObject);
    }

    public ClhAdvertise getClhAdvertiser()
    {
        return mClhAdvertiser;
    }
    public ClhScan getClhScanner()
    {
        return mClhScanner;
    }

    // init Cluster Head BLE: include
    // init Advertiser
    // init Scanner
    public int initClhBLE(long advertiseInterval)
    {
        int error;
        error=initClhBLEAdvertiser(advertiseInterval);
        if(error!=ClhErrors.ERROR_CLH_NO) return error;

        error=initClhBLEScaner();
        if(error!=ClhErrors.ERROR_CLH_NO) return error;
        return error;
    }

    public int initClhBLEAdvertiser(long advInterval) {
        int error;
        mClhAdvertiser.setAdvInterval(advInterval);
        mClhAdvertiser.setAdvClhID(mClhID,mIsSink);
        mClhAdvertiser.setAdvSettings(new byte[]{ClhAdvertise.ADV_SETTING_MODE_LOWLATENCY,
                ClhAdvertise.ADV_SETTING_SENDNAME_YES,
                ClhAdvertise.ADV_SETTING_SENDTXPOWER_NO});
        error=mClhAdvertiser.initCLHAdvertiser();

        return error;
    }

    public int initClhBLEScaner() {
        int error;
        mClhScanner.setAdvDataObject(mClhAdvertiser);
        mClhScanner.setProcDataObject(mClhProcessData);
        mClhScanner.setClhID(mClhID, mIsSink, false);
        error=mClhScanner.BLE_scan();

        return error;
    }

    public ClhProcessData getClhProcessor(){
        return mClhProcessData;
    }

    public ArrayList<BaseDataPacket> getAdvertiseList() {return mClhAdvDataList;}
    public final boolean setClhID(byte id, boolean startClicked){
        mClhID=id;
        if (mClhID == 0) mIsSink = true;
        else mIsSink = false;
        if(mClhAdvertiser!=null)    mClhAdvertiser.setAdvClhID(mClhID,mIsSink);
        if(mClhScanner!=null) mClhScanner.setClhID(mClhID,mIsSink, startClicked);
        return mIsSink;

    }
    public final byte getClhID(){ //return 16 bit from byte 2 and 3 in 128 UUID
        return mClhID;
    }

    public void clearClhAdvList()
    {
        mClhAdvertiser.clearAdvList();
    }

}
