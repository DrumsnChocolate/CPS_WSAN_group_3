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
import java.util.HashMap;
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


    // The best route to the sink, every clusterhead has it except of sink
    byte[] mBestRouteToSink = null;

    // Hashmap of known routes. Key indicates the source of the route
    // We may also need to forward packets from the sink back to a clusterhead,
    // which is why we save all routes
    private HashMap<Byte, byte[]> mRoutes = new HashMap<>();
    private static final int MAX_ADVERTISE_LIST_ITEM = 128;

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

        BaseDataPacket packet = manufacturerDataToPacket(manufacturerData);

        // Reflected data (we received a packet that we sent out)
        if (mClhID == packet.getSourceID()) {
            Log.i(LOG_TAG, "Reflected data, mClhID " + mClhID + ", recv:" + packet.getSourceID());
            return;
        }
        if (mClhID != packet.getReceiverId() && BaseDataPacket.BROADCAST_ID != packet.getReceiverId()) {
            // Packet is not for us
            return;
        }

        Log.i(LOG_TAG, "ID data " + packet.getSourceID() + "  " + packet.getPacketID());

        /* check packet has been yet received by searching the "unique packet ID" history list
        - history list include:
           Key: unique packet ID
           Life counter: time of the packet lived in history list
         */

        int sourceAndPacketId = manufacturerData.keyAt(0);

        if (ClhScanHistoryArray.indexOfKey(sourceAndPacketId) < 0) {//not yet received
            // History not yet full, update new "unique packet ID" to history list, reset life counter
            if (ClhScanHistoryArray.size() < ClhConst.SCAN_HISTORY_LIST_SIZE) {
                ClhScanHistoryArray.append(sourceAndPacketId, 0);
            }

            BaseDataPacket receivedPacket = manufacturerDataToPacket(manufacturerData);

            Log.i(LOG_TAG, "Handling packet");
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
                    Log.i(LOG_TAG, "Forwarding packet");
                }
            }
        }
    }

    public BaseDataPacket manufacturerDataToPacket(SparseArray<byte[]> manufacturerData) {
        byte packetType = BaseDataPacket.getPacketTypeFromBT(manufacturerData);
        BaseDataPacket packet;
        switch (packetType) {
            case RoutingDataPacket.PACKET_TYPE:
                packet = new RoutingDataPacket();
                break;
            case SoundDataPacket.PACKET_TYPE:
                packet = new SoundDataPacket();
                break;
            case SoundEventDataPacket.PACKET_TYPE:
                packet = new SoundEventDataPacket();
                break;
            default:
                Log.i(LOG_TAG, "Received packet with unknown packet type "+packetType);
                return null;
        }
        packet.setDataFromBT(manufacturerData);
        return packet;
    }

    public void setClhID(byte clhID, boolean isSink, boolean startClicked){
        mClhID=clhID;
        mIsSink=isSink;

        if (mClhID == BaseDataPacket.SINK_ID && startClicked) {
            // Send routing packet to find sink every 2 seconds
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (true) {
                            Log.i(LOG_TAG, "Sending routing packet!");
                            RoutingDataPacket packet = new RoutingDataPacket();
                            packet.setDestId(BaseDataPacket.SINK_ID);
                            packet.setSourceID(mClhID);
                            packet.setRoute(new byte[]{mClhID});
                            mClhAdvertiser.addAdvPacketToBuffer(packet, true);
                            Thread.sleep(2000);
                        }
                    } catch (InterruptedException e) {

                    }
                }
            }).start();
        }
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
        Log.i(LOG_TAG, "Handling routing packet");
        Log.i(LOG_TAG, "Data: "+Arrays.toString(routingPacket.getData()));
        Log.i(LOG_TAG, "Route: "+Arrays.toString(routingPacket.getRoute()));

        // If packet reached end of life it can be discarded
        if (routingPacket.getHopCounts() < ClhConst.MAX_HOP_COUNT) {
            return;
        } else {
            // always rebroadcast packet which hopped less times than MAX hop count
            routingPacket.addToRoute(mClhID);
            forwardPacket(routingPacket);
        }

        if(mIsSink) {
            if (routingPacket.getReceiverId() == routingPacket.getDestinationID() && mClhID == routingPacket.getDestinationID()) {
                // current cluster head is sink and sink was the destination of the packet...
                byte[] routeToClh = routingPacket.getRoute();
                // we can save the route in the map
                mRoutes.put(routingPacket.getSourceID(), routeToClh);
            } else {
                Log.i(LOG_TAG, "packet not meant for sink... Ignoring routing packet");
            }
        } else {
            if(mBestRouteToSink == null) {
                routingPacket.addToRoute(mClhID);
                saveRoute(routingPacket.getRoute());
//                send packet back to sink so it knows the fastest route
                routingPacket.setHopCount((byte) 0); // set hop count to zero to prevent from discarding of the packet before it reaches the sink
                sendRouteBackToSink(routingPacket);
                Log.i(LOG_TAG, "Route is empty, adding route" + Arrays.toString(mBestRouteToSink));
            } else if (routingPacket.routeContains(mClhID)) {
                // check if we are recipient of the packet
                if (mClhID == routingPacket.getReceiverId()) {
                    // This cluster head was supposed to receive the packet
                    Log.i(LOG_TAG, "Cluster on the route.. sending packet to next node on the route: " + Arrays.toString(routingPacket.getRoute()));
                    sendRouteBackToSink(routingPacket);
                } else {
                    forwardPacket(routingPacket);
                    // This cluster head was not meant to get this packet. We can ignore it.
                    Log.i(LOG_TAG, "Not on the route.. sending packet further");
                }
            }
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

        // this func should:
        // TODO: broadcast packet to all cluster heads in sight, maybe there is a clh that does not have route yet

        // [source, 1, 2, 3, 0]
        byte[] route = null;

        packet.setReceiverId(BaseDataPacket.BROADCAST_ID);
        mClhAdvertiser.addAdvPacketToBuffer(packet, false);

        Log.i(LOG_TAG, "Packet  broadcasted at: " + packet.getData()[packet.getData().length - 1]);

//        Log.i(LOG_TAG, "Forwarding packet with destination "+packet.getDestinationID());
//        if (packet.getDestinationID() == BaseDataPacket.SINK_ID) {
//            route = mRoutes.get(mClhID);
//        } else {
//            route = mRoutes.get(packet.getDestinationID());
//        }
//
//        if (route == null) {
//            // No route to the destination known, broadcast to all neighbors
//            Log.i(LOG_TAG, "No route found, flooding network");
//            packet.setReceiverId(BaseDataPacket.BROADCAST_ID);
//        } else {
//            Log.i(LOG_TAG, "Route found: "+Arrays.toString(route));
//
//            // Route known, find next node
//            int indexOfNode = -1;
//            int indexOfDest = -1;
//            for (int i = 0; i < route.length; i++) {
//                if (route[i] == mClhID) {
//                    indexOfNode = i;
//                } else if (route[i] == packet.getDestinationID()) {
//                    indexOfDest = i;
//                }
//            }
//
//            // The route to the destination is in our route to sink map
//            // Forward the packet to the destination via the next
//            // hop according to our route to the sink
//            byte nextStep = -1;
//            if (indexOfNode < indexOfDest) {
//                nextStep = route[indexOfNode + 1];
//            } else if (indexOfNode > indexOfDest) {
//                nextStep = route[indexOfNode - 1];
//            }
//            Log.i(LOG_TAG, "Next node is "+nextStep);
//            packet.setReceiverId(nextStep);
//        }
//
//        mClhAdvertiser.addAdvPacketToBuffer(packet, false);
//        Log.i(LOG_TAG, "Add data to advertised list, len:" + mClhAdvDataList.size());
//        Log.i(LOG_TAG, "Advertise list at " + (mClhAdvDataList.size() - 1) + ":"
//                + Arrays.toString(mClhAdvDataList.get(mClhAdvDataList.size() - 1).getData()));
    }

    /**
     * Sends found route back to sink along that route.
     * @param routingDataPacket
     */
    private void sendRouteBackToSink(RoutingDataPacket routingDataPacket) {
//        TODO: set receiverID as next on the path and send routingPacket
        final int nextStep = -1;
        byte[] routeToSink = routingDataPacket.getRoute();
        int currentHopCount = routingDataPacket.getHopCounts();
        int currentRouteNode = routeToSink.length - 1 - currentHopCount;

        routingDataPacket.setDestId(BaseDataPacket.SINK_ID);
        routingDataPacket.setReceiverId(routeToSink[currentRouteNode + nextStep]);
        Log.i(LOG_TAG, "Sending route: " + Arrays.toString(routeToSink) + " from source: " + routingDataPacket.getSourceID()
                + ", Through: " + routingDataPacket.getReceiverId()
                + ", To destination: " + routingDataPacket.getDestinationID());
        if(currentHopCount == 0) {
            // we are in source, we need to assign new Id to packet
            mClhAdvertiser.addAdvPacketToBuffer(routingDataPacket, true);
        } else {
            // we are not in source anymore, forward same packet to next node
            mClhAdvertiser.addAdvPacketToBuffer(routingDataPacket, false);
        }
    }


    private void saveRoute(byte[] route) {
        if (route.length == 0) return;
        Log.i(LOG_TAG, "Saving route"+Arrays.toString(route));
        mBestRouteToSink = route.clone();
    }
}


