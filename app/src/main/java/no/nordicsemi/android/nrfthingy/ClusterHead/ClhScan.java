package no.nordicsemi.android.nrfthingy.ClusterHead;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Handler;
import android.util.Log;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import no.nordicsemi.android.nrfthingy.ClusterHead.packet.BaseDataPacket;
import no.nordicsemi.android.nrfthingy.ClusterHead.packet.RoutingDataPacket;
import no.nordicsemi.android.nrfthingy.ClusterHead.packet.SoundDataPacket;
import no.nordicsemi.android.nrfthingy.ClusterHead.packet.SoundEventDataPacket;

public class ClhScan {
    private BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();
    private BluetoothLeScanner mCLHscanner ;
    private final String LOG_TAG="CLH Scanner:";

    private Handler handler = new Handler();
    private Handler handler2 = new Handler();
    private boolean mScanning;
    private byte mClhID=1;
    private boolean mIsSink=false;
    private ScanSettings mScanSettings;

    private SparseArray<Integer> ClhScanHistoryArray=new SparseArray();

    //private static final int MAX_PROCESS_LIST_ITEM=128;
    //private ClhAdvertisedData clhAdvData=new ClhAdvertisedData();
    private ClhAdvertise mClhAdvertiser;
    private ArrayList<BaseDataPacket> mClhProcDataList ;
    private ClhProcessData mClhProcessData;
    private ArrayList<BaseDataPacket> mClhAdvDataList;
    private static final int MAX_ADVERTISE_LIST_ITEM=128;

    public ClhScan()
    {

    }

    public ClhScan(ClhAdvertise clhAdvObj,ClhProcessData clhProcDataObj)
    {//constructor, set 2 alias to Clh advertiser and processor
        mClhAdvertiser=clhAdvObj;
        mClhAdvDataList=mClhAdvertiser.getAdvertiseList();
        mClhProcessData=clhProcDataObj;
        mClhProcDataList=clhProcDataObj.getProcessDataList();
    }

    @SuppressLint("NewApi")
    public int BLE_scan() {
        boolean result=true;
        byte[] advsettings=new byte[16];
        byte[] advData= new byte[256];
        int length;
        final List<ScanFilter> filters = new ArrayList<>();

        if (!mScanning) {
            //verify BLE available
            mCLHscanner = mAdapter.getBluetoothLeScanner();
            if (mCLHscanner == null) {
                Log.i(LOG_TAG, "BLE not supported");
                return ClhErrors.ERROR_CLH_BLE_NOT_ENABLE;
            }

            //setting
            ScanSettings ClhScanSettings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                    .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                    .setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
                    .build();

            //set filter: filter name
            ScanFilter filter = new ScanFilter.Builder()
                    .setDeviceName(ClhConst.clusterHeadName)
                    .build();
            filters.add(filter);
            Log.i(LOG_TAG, "filters"+ filters.toString());

            mScanSettings =ClhScanSettings;
// Stops scanning after 60 seconds.

            // Create a timer to stop scanning after a pre-defined scan period.
            //rest, then restart to avoid auto disable from Android
           handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mCLHscanner.stopScan(CLHScanCallback);
                    Log.i(LOG_TAG, "Stop scan");
                    //start another timer for resting in 1s
                    handler2.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mScanning = true;
                            mCLHscanner.startScan(filters, mScanSettings, CLHScanCallback);
                        }
                    },ClhConst.REST_PERIOD);
                }
            }, ClhConst.SCAN_PERIOD);

            mScanning = true;
            mCLHscanner.startScan(filters, ClhScanSettings, CLHScanCallback);
            Log.i(LOG_TAG, "Start scan");
        }
        else
        {
            return ClhErrors.ERROR_CLH_SCAN_ALREADY_START;
        }

        return ClhErrors.ERROR_CLH_NO;
    }

    public void stopScanCLH()
    {
        mScanning = false;
        mCLHscanner.stopScan(CLHScanCallback);
        Log.i(LOG_TAG, "Stop scan");
    }


    private ScanCallback CLHScanCallback = new ScanCallback() {
        @Override
        public final void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            // Check RSSI to remove weak signal ones
            if (result.getRssi()<ClhConst.MIN_SCAN_RSSI_THRESHOLD) {
                Log.i(LOG_TAG,"Dropping packet, low RSSI");
                return;
            }

            // Get data
            SparseArray<byte[]> manufacturerData = result.getScanRecord().getManufacturerSpecificData();
            processScanData(manufacturerData);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e( "BLE", "Discovery onScanFailed: " + errorCode );
            super.onScanFailed(errorCode);
        }
    };

    /**
     * Process received data of BLE Manufacturer field
     * This includes:
     * - Manufacturer Specification (in manufacturerData.key): "unique packet ID", include
     *             2 bytes: 0XAABB: AA: Source Cluster Head ID: 0-127
     *                              BB: Packet ID: 0-254 (unique for each packet)
     * - Manufacturer Data (in manufacturerData.value): remained n data bytes (manufacturerData.size())
     * @param manufacturerData data received via bluetooth
     */
    public void processScanData(SparseArray<byte[]> manufacturerData) {

        if (manufacturerData == null) {
            Log.i(LOG_TAG, "No data received");
            return;

        }

        int receiverID = manufacturerData.keyAt(0);
        byte sourceDeviceId = (byte) (receiverID >> 8);
        byte packetId = (byte) (receiverID & 0xFF);

        // Reflected data (we received a packet that we sent out)
        if (mClhID == sourceDeviceId) {
            Log.i(LOG_TAG, "Reflected data, mClhID " + mClhID + ", recv:" + sourceDeviceId);
            return;
        }
        Log.i(LOG_TAG, "ID data " + sourceDeviceId + "  " + packetId);

        /* check packet has been yet received by searching the "unique packet ID" history list
        - history list include:
           Key: unique packet ID
           Life counter: time of the packet lived in history list
         */

        if (ClhScanHistoryArray.indexOfKey(manufacturerData.keyAt(0)) < 0) {//not yet received
            // History not yet full, update new "unique packet ID" to history list, reset life counter
            if (ClhScanHistoryArray.size() < ClhConst.SCAN_HISTORY_LIST_SIZE) {
                ClhScanHistoryArray.append(manufacturerData.keyAt(0), 0);
            }

            byte packetType = BaseDataPacket.getPacketTypeFromBT(manufacturerData);
            BaseDataPacket receivedPacket;
            switch (packetType) {
                case RoutingDataPacket.PACKET_TYPE:
                    receivedPacket = new RoutingDataPacket();
                    break;
                case SoundDataPacket.PACKET_TYPE:
                    receivedPacket = new SoundDataPacket();
                    break;
                case SoundEventDataPacket.PACKET_TYPE:
                    receivedPacket = new SoundEventDataPacket();
                    break;
                default:
                    Log.i(LOG_TAG, "Received packet with unknown packet type "+packetType);
                    return;
            }
            receivedPacket.setDataFromBT(manufacturerData);


            if (receivedPacket instanceof RoutingDataPacket) {
                // Routing packet
                handleRoutingPacket((RoutingDataPacket) receivedPacket);
            } else if (receivedPacket instanceof SoundEventDataPacket) {
                // Packet with sound event data
                handleSoundEventPacket((SoundEventDataPacket) receivedPacket);
            } else {
                // Normal packet

                if (mIsSink) {
                    // If this Cluster Head is the Sink node (ID=0), add data to waiting process list
                    mClhProcessData.addProcessPacketToBuffer(receivedPacket);
                    Log.i(LOG_TAG, "Add data to process list, len:" + mClhProcDataList.size());
                } else {
                    // Normal Cluster Head (ID 1..127), forward data
                    forwardPacket(receivedPacket);
                }
            }
        }
    }

    public void setClhID(byte clhID, boolean isSink){
        mClhID=clhID;
        mIsSink=isSink;
    }

    //set alias to Clh advertiser
    public void setAdvDataObject(ClhAdvertise clhAdvObj){
        mClhAdvertiser=clhAdvObj;
        mClhAdvDataList=mClhAdvertiser.getAdvertiseList();

    }

    //set alias to Clh processor
    public void setProcDataObject(ClhProcessData clhProObj){
        mClhProcessData=clhProObj;
        mClhProcDataList=mClhProcessData.getProcessDataList();
    }

    /**
     * Handle a received Routing Packet
     *
     * @param routingPacket The packet
     */
    private void handleRoutingPacket(RoutingDataPacket routingPacket) {
        if (routingPacket.routeResolved()) {
            // The route to the destination has been found, sending result back
            if (routingPacket.getDestinationID() == mClhID) {
                // A route that we requested was found
                // TODO Save the route
            } else {
                // Forward the routing packet to the device that requested the route
                if (routingPacket.routeContains(mClhID)) {
                    // We are in the route, forward the packet back to the source
                    // TODO We should probably also save this route in case a future 'normal' packet
                    // has to be forwarded
                    mClhAdvertiser.addAdvPacketToBuffer(routingPacket, false);
                } else {
                    // We are not the best route to the source. Ignore the packet
                    return;
                }
            }
        } else if (routingPacket.routeContains(mClhID)) {
            // We are already in the route list so this packet has already been through
            // this node. We can ignore it.
            return;
        } else {
            // Destination not found yet, add our address to the route and forward it
            routingPacket.addToRoute(mClhID);

            if (routingPacket.routeResolved()) {
                // The route is now resolved, send it back to the source
                routingPacket.setDestId(routingPacket.getSourceID());
                routingPacket.setSourceID(mClhID);
            }

            // Forward, with new address so a second packet with a different route won't be ignored
            mClhAdvertiser.addAdvPacketToBuffer(routingPacket, true);
        }
    }

    /**
     * Handle a received Sound Event package
     *
     * @param soundEventPacket The package
     */
    private void handleSoundEventPacket(SoundEventDataPacket soundEventPacket) {
        if (mIsSink) {
            // If this Cluster Head is the Sink node (ID=0), add data to waiting process list
            mClhProcessData.addProcessPacketToBuffer(soundEventPacket);
            Log.i(LOG_TAG, "Add event to process list, new lenght:" + mClhProcDataList.size());
        } else {
            forwardPacket(soundEventPacket);
        }
    }

    private void forwardPacket(BaseDataPacket packet) {
        // TODO Use route to forward
        mClhAdvertiser.addAdvPacketToBuffer(packet, false);
        Log.i(LOG_TAG, "Add data to advertised list, len:" + mClhAdvDataList.size());
        Log.i(LOG_TAG, "Advertise list at " + (mClhAdvDataList.size() - 1) + ":"
                + Arrays.toString(mClhAdvDataList.get(mClhAdvDataList.size() - 1).getData()));
    }

}


