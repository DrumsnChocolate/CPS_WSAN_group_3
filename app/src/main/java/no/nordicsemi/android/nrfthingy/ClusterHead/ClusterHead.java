package no.nordicsemi.android.nrfthingy.ClusterHead;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Observable;

import no.nordicsemi.android.nrfthingy.ClusterHead.packet.BaseDataPacket;
import no.nordicsemi.android.nrfthingy.thingy.ThingyService;
import no.nordicsemi.android.support.v18.scanner.ScanResult;
import no.nordicsemi.android.nrfthingy.SoundFragment;
import no.nordicsemi.android.thingylib.ThingySdkManager;

public class ClusterHead extends Observable {
    private static final int MAX_ADVERTISE_LIST_ITEM = ClhConst.MAX_ADVERTISE_LIST_ITEM; //max items in waiting list for advertising
    private static final int MAX_PROCESS_LIST_ITEM = ClhConst.MAX_PROCESS_LIST_ITEM; //max items in waiting list for processing
    private static final String TAG = "Clusterhead";
    private static final int N_CLUSTERHEAD = 2; // TODO make sure this number is correct
    private static final int CLUSTER_MAX_SIZE = 4;
    private boolean mIsSink = false;
    private byte mClhID = 1;
    private final ArrayList<BaseDataPacket> mClhAdvDataList = new ArrayList<BaseDataPacket>(MAX_ADVERTISE_LIST_ITEM);
    private final ClhAdvertise mClhAdvertiser = new ClhAdvertise(mClhAdvDataList, MAX_ADVERTISE_LIST_ITEM);

    private final ClhScan mClhScanner = new ClhScan();

    private final ArrayList<BaseDataPacket> mClhProcDataList = new ArrayList<>(MAX_PROCESS_LIST_ITEM);
    private final ClhProcessData mClhProcessData = new ClhProcessData(mClhProcDataList, MAX_PROCESS_LIST_ITEM);

    private final ArrayList<BluetoothDevice> connecting = new ArrayList<>();
    private final ArrayList<BluetoothDevice> connected = new ArrayList<>();
    private final ArrayList<ClusterLeaf> visible = new ArrayList<>();
    private final ArrayList<ScanResult> results = new ArrayList<>();
    private boolean formedClusters = false;
    private boolean mClusterAdvertising = false;

    private ThingyService.ThingyBinder mBinder;
    private ThingySdkManager mThingySdkManager;


//    public ClusterHead() {
//        mClhScanner.setClusterHead(this);
//    }

    //construtor,
    //params: id: cluster head ID
    public ClusterHead(byte id) {
        if (id > 127) id -= 127;
        setClhID(id, false);
        mClhScanner.setClusterHead(this);
        mThingySdkManager = ThingySdkManager.getInstance();
        mBinder = (ThingyService.ThingyBinder) mThingySdkManager.getThingyBinder();
    }

    public ClusterHead(byte id, SoundFragment soundFragmentObject) {
        this(id);
        mClhScanner.setSoundFragmentObject(soundFragmentObject);
    }

    public ClhAdvertise getClhAdvertiser() {
        return mClhAdvertiser;
    }

    public ClhScan getClhScanner() {
        return mClhScanner;
    }

    // init Cluster Head BLE: include
    // init Advertiser
    // init Scanner
    // init ClusterScanner
    public int initClhBLE(long advertiseInterval) {
        int error;

        error = initClhBLEAdvertiser(advertiseInterval);
        if (error != ClhErrors.ERROR_CLH_NO) return error;

        error = initClhBLEScanner();
        if (error != ClhErrors.ERROR_CLH_NO) return error;
        return error;
    }

    public int initClhBLEAdvertiser(long advInterval) {
        int error;
        mClhAdvertiser.setAdvInterval(advInterval);
        mClhAdvertiser.setAdvClhID(mClhID, mIsSink);
        mClhAdvertiser.setAdvSettings(new byte[]{ClhAdvertise.ADV_SETTING_MODE_LOWLATENCY,
                ClhAdvertise.ADV_SETTING_SENDNAME_YES,
                ClhAdvertise.ADV_SETTING_SENDTXPOWER_NO});
        error = mClhAdvertiser.initCLHAdvertiser();

        return error;
    }

    public int initClhBLEScanner() {
        int error;
        mClhScanner.setAdvDataObject(mClhAdvertiser);
        mClhScanner.setProcDataObject(mClhProcessData);
        mClhScanner.setClhID(mClhID, mIsSink, false);
        error = mClhScanner.BLE_scan();

        return error;
    }

    public ClhProcessData getClhProcessor() {
        return mClhProcessData;
    }

    public ArrayList<BaseDataPacket> getAdvertiseList() {
        return mClhAdvDataList;
    }

    public final boolean setClhID(byte id, boolean startClicked) {
        mClhID = id;
        if (mClhID == 0) mIsSink = true;
        else mIsSink = false;
        if (mClhAdvertiser != null) mClhAdvertiser.setAdvClhID(mClhID, mIsSink);
        if (mClhScanner != null) mClhScanner.setClhID(mClhID, mIsSink, startClicked);
        return mIsSink;

    }

    public final byte getClhID() { //return 16 bit from byte 2 and 3 in 128 UUID
        return mClhID;
    }

    public void clearClhAdvList() {
        mClhAdvertiser.clearAdvList();
    }

    public void addToVisible(ScanResult result) {
        ClusterLeaf leaf = new ClusterLeaf(result);
        if (!visible.contains(leaf)) {
            for (int i = 0; i < visible.size(); i++) {
                ClusterLeaf c = visible.get(i);
                if (c.getAddress().equals(leaf.getAddress())) {
                    visible.remove(c);
                    i--;
                }
            }
            visible.add(leaf);
        }
        if (!results.contains(result)) {
            for (int i = 0; i < results.size(); i++) {
                ScanResult sr = results.get(i);
                if (sr.getDevice().getAddress().equals(result.getDevice().getAddress())) {
                    results.remove(sr);
                    i--;
                }
            }
            results.add(result);
        }

    }

    public void removeFromVisible(BluetoothDevice device) {
        for (int i = 0; i < results.size(); i++) {
            if (results.get(i).getDevice().equals(device)) {
                results.remove(i);
                i--;
            }
        }
    }



    public List<BluetoothDevice> getClosestUnconnectedThingies() {
        Comparator<ScanResult> cp = new Comparator<ScanResult>() {
            @Override
            public int compare(ScanResult o1, ScanResult o2) {
                int rssi1 = o1.getRssi();
                int rssi2 = o2.getRssi();
                return Integer.compare(rssi1, rssi2);
            }
        };
        List<ScanResult> sortedResults = (List<ScanResult>) results.clone();
        List<BluetoothDevice> closest = new ArrayList<>();
        if (results.size() > CLUSTER_MAX_SIZE) {
            Collections.sort(sortedResults, cp);
        }
        for (int i = 0; i < sortedResults.size(); i++) {
            closest.add(sortedResults.get(i).getDevice());
        }
        for (int i = CLUSTER_MAX_SIZE - 1; i < closest.size(); i++) {
            closest.remove(i);
            i--;
        }
        Log.i(TAG,closest.toString());
        return closest;
    }

    public void connectClosestThingies() {
        setChanged();
        notifyObservers();
    }

    public void removeConnectedDevice(BluetoothDevice device) {
        for (int i = 0; i < results.size(); i++) {
            if (results.get(i).getDevice().equals(device)) {
                results.remove(i);
                i--;
            }
        }
        for (int i = 0; i < connected.size(); i++) {
            if (connected.get(i).equals(device)) {
                connected.remove(i);
                i--;
            }
        }
    }

    public void addConnectedDevice(BluetoothDevice device) {
        connected.add(device);
    }

    public void addConnectingDevice(BluetoothDevice device) {
        connecting.add(device);
    }

    public void removeConnectingDevice(BluetoothDevice device) {
        for (int i = 0; i < connecting.size(); i++) {
            if (connecting.get(i).equals(device)) {
                connecting.remove(i);
                i--;
            }
        }
    }

    public void removeResult(BluetoothDevice device) {
        for (int i = 0; i < results.size(); i++) {
            if (results.get(i).getDevice().equals(device)) {
                results.remove(i);
                i--;
            }
        }
    }

    public boolean clusterIsFull() {
        return connected.size() + connecting.size() >= CLUSTER_MAX_SIZE;
    }


//    public void startAdvertisingCluster() {
//        Log.i(TAG, "Started advertising the cluster: " + cluster.toString());
//        mClusterAdvertising = true;
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                while (mClusterAdvertising) {
//                    try {
//                        Log.i(TAG, "Sending clustering packet!");
//                        ClusteringDataPacket clusteringDataPacket = new ClusteringDataPacket();
//                        clusteringDataPacket.setSourceID(mClhID);
//                        clusteringDataPacket.setCluster(cluster);
//                        mClhAdvertiser.addAdvPacketToBuffer(clusteringDataPacket, true);
//                        Thread.sleep(2000);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//
//                }
//            }
//        }).start();
//
////        mClhAdvertiser.stopAdvertiseClhData();
//    }

//    public void addExternalClusteringDataPacket(ClusteringDataPacket clusteringDataPacket) {
//        Log.i(TAG, "Adding external clustering data packet");
//        for (int i = 0; i < externalClusteringDataPackets.size(); i++) {
//            ClusteringDataPacket externalClusteringDataPacket = externalClusteringDataPackets.get(i);
//            if (clusteringDataPacket.getSourceID() == externalClusteringDataPacket.getSourceID()) {
//                externalClusteringDataPackets.remove(externalClusteringDataPacket);
//            }
//        }
//        externalClusteringDataPackets.add(clusteringDataPacket);
//        if (externalClusteringDataPackets.size() == N_CLUSTERHEAD - 1) {
//            // TODO: form clusters.
//            if (!formedClusters) {
//                formedClusters = true;
//                Log.i(TAG, "Received clustering info from all clusterheads! forming clusters should be possible!");
//                for (ClusteringDataPacket p : externalClusteringDataPackets) {
//                    Log.i(TAG, p.getCluster().toString());
//                }
//            }
//        }
//    }

}
