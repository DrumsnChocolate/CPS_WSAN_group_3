package no.nordicsemi.android.nrfthingy.ClusterHead;

import android.bluetooth.BluetoothDevice;

import java.util.ArrayList;

import no.nordicsemi.android.nrfthingy.ClusterHead.packet.BaseDataPacket;
import no.nordicsemi.android.nrfthingy.ClusterHead.packet.ClusteringDataPacket;
import no.nordicsemi.android.nrfthingy.thingy.Thingy;
import no.nordicsemi.android.thingylib.BaseThingyService;
import no.nordicsemi.android.thingylib.ThingySdkManager;

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

    private final ArrayList<BluetoothDevice> cluster = new ArrayList<>();

    public ClusterHead(){}

    //construtor,
    //params: id: cluster head ID
    public ClusterHead(byte id)
    {
        if(id>127) id-=127;
        setClhID(id);

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
    // init ClusterScanner
    public int initClhBLE(long advertiseInterval)
    {
        int error;

        error=initThingyScanner();
        if(error!=ClhErrors.ERROR_CLH_NO) return error;

        error=initClhBLEAdvertiser(advertiseInterval);
        if(error!=ClhErrors.ERROR_CLH_NO) return error;

        error= initClhBLEScanner();
        if(error!=ClhErrors.ERROR_CLH_NO) return error;
        return error;
    }

    private int initThingyScanner() {
        int error;
        mClhScanner.setClusterHead(this);
        error=mClhScanner.thingy_scan();
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

    public int initClhBLEScanner() {
        int error;
        mClhScanner.setAdvDataObject(mClhAdvertiser);
        mClhScanner.setProcDataObject(mClhProcessData);
        mClhScanner.setClhID(mClhID,mIsSink);
        error=mClhScanner.BLE_scan();

        return error;
    }

    public ClhProcessData getClhProcessor(){
        return mClhProcessData;
    }

    public ArrayList<BaseDataPacket> getAdvertiseList() {return mClhAdvDataList;}
    public final boolean setClhID(byte id){
        mClhID=id;
        if (mClhID == 0) mIsSink = true;
        else mIsSink = false;
        if(mClhAdvertiser!=null)    mClhAdvertiser.setAdvClhID(mClhID,mIsSink);
        if(mClhScanner!=null) mClhScanner.setClhID(mClhID,mIsSink);
        return mIsSink;

    }
    public final byte getClhID(){ //return 16 bit from byte 2 and 3 in 128 UUID
        return mClhID;
    }

    public void clearClhAdvList()
    {
        mClhAdvertiser.clearAdvList();
    }

    public void addToCluster(BluetoothDevice device) {
        cluster.add(device);
    }

    public void startAdvertisingCluster() {
        ClusteringDataPacket clusteringDataPacket = new ClusteringDataPacket();
        for (BluetoothDevice device : cluster) {
            clusteringDataPacket.addThingyToData(device);
        }
        mClhAdvertiser.addAdvPacketToBuffer(clusteringDataPacket,true);
        mClhAdvertiser.stopAdvertiseClhData();
    }
}
