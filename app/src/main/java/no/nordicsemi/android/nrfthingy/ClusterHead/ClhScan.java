package no.nordicsemi.android.nrfthingy.ClusterHead;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;
import android.util.SparseArray;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import no.nordicsemi.android.nrfthingy.ClusterHead.packet.ActuateThingyPacket;
import no.nordicsemi.android.nrfthingy.ClusterHead.packet.BaseDataPacket;
import no.nordicsemi.android.nrfthingy.ClusterHead.packet.ClusteringDataPacket;
import no.nordicsemi.android.nrfthingy.ClusterHead.packet.RoutingDataPacket;
import no.nordicsemi.android.nrfthingy.ClusterHead.packet.SoundDataPacket;
import no.nordicsemi.android.nrfthingy.ClusterHead.packet.SoundEventDataPacket;
import no.nordicsemi.android.nrfthingy.SoundFragment;
import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.thingylib.ThingySdkManager;
import no.nordicsemi.android.thingylib.utils.ThingyUtils;

public class ClhScan {
    private static final long THINGY_SCAN_DURATION = 4000;
    private static final int LED_BURN_TIME = 1000; // Number of ms for Thingy LED to burn

    private BluetoothAdapter mAdapter = BluetoothAdapter.getDefaultAdapter();
    private BluetoothLeScanner mCLHscanner;
    private final String LOG_TAG = "CLH Scanner:";

    private Handler handler = new Handler();
    private Handler handler2 = new Handler();
    private boolean mScanning;
    private byte mClhID = 1;
    private boolean mIsSink = false;
    private ScanSettings mScanSettings;

    private SparseArray<Integer> ClhScanHistoryArray = new SparseArray();

    //private static final int MAX_PROCESS_LIST_ITEM=128;
    //private ClhAdvertisedData clhAdvData=new ClhAdvertisedData();
    private ClhAdvertise mClhAdvertiser;
    private ArrayList<BaseDataPacket> mClhProcDataList;
    private ClhProcessData mClhProcessData;
    private ArrayList<BaseDataPacket> mClhAdvDataList;
    private SoundFragment mSoundFragment;

    // Hashmap of known routes. Key indicates the source of the route
    // We may also need to forward packets from the sink back to a clusterhead,
    // which is why we save all routes
    private HashMap<Byte, byte[]> mRoutes = new HashMap<>();
    private static final int MAX_ADVERTISE_LIST_ITEM = 128;


    //Clustering fields
    private ClusterHead clusterHead;
    private ThingySdkManager mThingySdkManager;
    private Handler mHandler = new Handler();
    private boolean mThingyScanning;


    public ClhScan() {
        mThingySdkManager = ThingySdkManager.getInstance();
        // Send routing packet to find sink every 2 seconds
    }

    public ClhScan(ClhAdvertise clhAdvObj, ClhProcessData clhProcDataObj) {//constructor, set 2 alias to Clh advertiser and processor
        mClhAdvertiser = clhAdvObj;
        mClhAdvDataList = mClhAdvertiser.getAdvertiseList();
        mClhProcessData = clhProcDataObj;
        mClhProcDataList = clhProcDataObj.getProcessDataList();
    }

    @SuppressLint("NewApi")
    public int BLE_scan() {
        boolean result = true;
        byte[] advsettings = new byte[16];
        byte[] advData = new byte[256];
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
            Log.i(LOG_TAG, "filters" + filters.toString());

            mScanSettings = ClhScanSettings;
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
                    }, ClhConst.REST_PERIOD);
                }
            }, ClhConst.SCAN_PERIOD);

            mScanning = true;
            mCLHscanner.startScan(filters, ClhScanSettings, CLHScanCallback);
            Log.i(LOG_TAG, "Start scan");
        } else {
            return ClhErrors.ERROR_CLH_SCAN_ALREADY_START;
        }

        return ClhErrors.ERROR_CLH_NO;
    }

    public void stopScanCLH() {
        mScanning = false;
        mCLHscanner.stopScan(CLHScanCallback);
        Log.i(LOG_TAG, "Stop scan");
    }


    private ScanCallback CLHScanCallback = new ScanCallback() {
        @Override
        public final void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            // Check RSSI to remove weak signal ones
            if (result.getRssi() < ClhConst.MIN_SCAN_RSSI_THRESHOLD) {
                Log.i(LOG_TAG, "Dropping packet, low RSSI");
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
            Log.e("BLE", "Discovery onScanFailed: " + errorCode);
            super.onScanFailed(errorCode);
        }
    };

    /**
     * Process received data of BLE Manufacturer field
     * This includes:
     * - Manufacturer Specification (in manufacturerData.key): "unique packet ID", include
     * 2 bytes: 0XAABB: AA: Source Cluster Head ID: 0-127
     * BB: Packet ID: 0-254 (unique for each packet)
     * - Manufacturer Data (in manufacturerData.value): remained n data bytes (manufacturerData.size())
     *
     * @param manufacturerData data received via bluetooth
     */
    public void processScanData(SparseArray<byte[]> manufacturerData) {

        if (manufacturerData == null) {
            Log.i(LOG_TAG, "No data received");
            return;

        }

        BaseDataPacket receivedPacket = manufacturerDataToPacket(manufacturerData);

        // For some reason it sometimes happens that receivedPacket is null, so check for that first
        if (receivedPacket == null) {
            Log.i(LOG_TAG, "Received an empty packet");
            return;
        }

        // Reflected data (we received a packet that we sent out)
        if (mClhID == receivedPacket.getSourceID()) {
            Log.i(LOG_TAG, "Reflected data, mClhID " + mClhID + ", recv:" + receivedPacket.getSourceID());
            return;
        }
        if (mClhID != receivedPacket.getReceiverId() && BaseDataPacket.BROADCAST_ID != receivedPacket.getReceiverId()) {
            // Packet is not for us
            return;
        }

        Log.i(LOG_TAG, "ID data " + receivedPacket.getSourceID() + "  " + receivedPacket.getPacketID());

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

            Log.i(LOG_TAG, "Handling packet");
            if (receivedPacket instanceof RoutingDataPacket) {
                // Routing packet
                handleRoutingPacket((RoutingDataPacket) receivedPacket);
            } else if (receivedPacket instanceof SoundEventDataPacket) {
                // Packet with sound event data
                handleSoundEventPacket((SoundEventDataPacket) receivedPacket);
            } else if (receivedPacket instanceof ClusteringDataPacket) {
                handleClusteringPacket((ClusteringDataPacket) receivedPacket);
                // Also forward the packet to other nodes.
                forwardPacket(receivedPacket);
            } else if (receivedPacket instanceof ActuateThingyPacket) {
                // Packet with thingy actuation data
                handleActuateThingyPacket((ActuateThingyPacket) receivedPacket);
            } else {
                // Normal packet

                if (mIsSink) {
                    // If this Cluster Head is the Sink node (ID=0), add data to waiting process list
                    mClhProcessData.addProcessPacketToBuffer(receivedPacket);

                    // Process the newly added data in the sink
                    mClhProcessData.process();

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
            case ClusteringDataPacket.PACKET_TYPE:
                packet = new ClusteringDataPacket();
                break;
            default:
                Log.i(LOG_TAG, "Received packet with unknown packet type " + packetType);
                return null;
        }
        packet.setDataFromBT(manufacturerData);
        return packet;
    }

    public void setClhID(byte clhID, boolean isSink, boolean startClicked) {
        mClhID = clhID;
        mIsSink = isSink;

        if (mClhID != BaseDataPacket.SINK_ID && startClicked) {
            // Send routing packet to find sink every 2 seconds
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (mSoundFragment.getStartButtonState() == true) {
                            Log.i(LOG_TAG, "Sending routing packet!");
                            RoutingDataPacket packet = new RoutingDataPacket();
                            packet.setDestId(BaseDataPacket.SINK_ID);
                            packet.setSourceID(mClhID);
                            packet.setRoute(new byte[]{mClhID});
                            packet.setRouteToId(BaseDataPacket.SINK_ID);
                            mClhAdvertiser.addAdvPacketToBuffer(packet, true);
                            Thread.sleep(2000);
                        }
                    } catch (InterruptedException e) {

                    }
                }
            }).start();
        }
        if (startClicked) {
            startClusterScan();
        }
    }


    //set alias to Clh advertiser
    public void setAdvDataObject(ClhAdvertise clhAdvObj) {
        mClhAdvertiser = clhAdvObj;
        mClhAdvDataList = mClhAdvertiser.getAdvertiseList();
    }

    //set alias to Clh processor
    public void setProcDataObject(ClhProcessData clhProObj) {
        mClhProcessData = clhProObj;
        mClhProcDataList = mClhProcessData.getProcessDataList();
    }

    public void setSoundFragmentObject(SoundFragment soundFragmentObject) {
        mSoundFragment = soundFragmentObject;
    }

    /**
     * Handle a received Routing Packet
     *
     * @param routingPacket The packet
     */
    private void handleRoutingPacket(RoutingDataPacket routingPacket) {
        Log.i(LOG_TAG, "Handling routing packet");
        Log.i(LOG_TAG, "Data: " + Arrays.toString(routingPacket.getData()));
        Log.i(LOG_TAG, "Route: " + Arrays.toString(routingPacket.getRoute()));

        if (routingPacket.routeResolved()) {
            Log.i(LOG_TAG, "Route is resolved");
            // The route to the destination has been found, sending result back
            if (routingPacket.getDestinationID() == mClhID) {
                Log.i(LOG_TAG, "We requested the route");
                // A route that we requested was found
                // Save the route
                saveRoute(routingPacket.getRoute());
            } else {
                // Forward the routing packet to the device that requested the route
                if (routingPacket.routeContains(mClhID)) {
                    Log.i(LOG_TAG, "Forwarding routing packet");
                    // We are in the route, forward the packet back to the source
                    // We should probably also save this route in case a future 'normal' packet
                    // has to be forwarded
                    // If shorter route arrives later we should exchange packet
                    saveRoute(routingPacket.getRoute());
                    mClhAdvertiser.addAdvPacketToBuffer(routingPacket, false);
                } else {
                    // We are not the best route to the source. Ignore the packet
                    Log.i(LOG_TAG, "Not forwarding routing packet");
                    return;
                }
            }
        } else if (routingPacket.routeContains(mClhID)) {
            // We are already in the route list so this packet has already been through
            // this node. We can ignore it.
            Log.i(LOG_TAG, "Ignoring routing packet");
            return;
        } else {
            Log.i(LOG_TAG, "Forwarding incomplete routing packet");
            // Destination not found yet, add our address to the route and forward it4
            Log.i(LOG_TAG, "Adding " + mClhID + " to " + Arrays.toString(routingPacket.getRoute()));
            routingPacket.addToRoute(mClhID);
            Log.i(LOG_TAG, "New route: " + Arrays.toString(routingPacket.getRoute()));

            // Save the route so we have a route to the source node
            saveRoute(routingPacket.getRoute());

            if (routingPacket.routeResolved()) {
                Log.i(LOG_TAG, "Route is now resolved");
                // The route is now resolved, send it back to the source
                routingPacket.setDestId(routingPacket.getSourceID());
                routingPacket.setSourceID(mClhID);
            }

            Log.i(LOG_TAG, "Sending routing packet");
            // Forward, with new address so a second packet with a different route won't be ignored
            mClhAdvertiser.addAdvPacketToBuffer(routingPacket, true);
        }
    }

    /**
     * Handle a received Sound Event package
     *
     * @param soundEventPacket The packet
     */
    private void handleSoundEventPacket(SoundEventDataPacket soundEventPacket) {
        if (mIsSink) {
            // If this Cluster Head is the Sink node (ID=0), add the data to the buffer
            // There it is compared to other incoming data
            mClhProcessData.addProcessPacketToBuffer(soundEventPacket);
            Log.i(LOG_TAG, "Received a Sound Event with amplitude " + soundEventPacket.getAmplitude() + " and duration " + soundEventPacket.getDuration());
            Log.i(LOG_TAG, "Add event to process list, new lenght:" + mClhProcDataList.size());
        } else {
            forwardPacket(soundEventPacket);
        }
    }

    private void handleClusteringPacket(ClusteringDataPacket clusteringDataPacket) {
        Log.i(LOG_TAG, "Handling clustering data packet");
        Log.i(LOG_TAG, clusteringDataPacket.toString());
        clusterHead.addExternalClusteringDataPacket(clusteringDataPacket);

        // TODO implement
    }

    private void handleActuateThingyPacket(ActuateThingyPacket actuateThingyPacket) {
        if (mClhID == actuateThingyPacket.getDestinationID()) {
            // If this clusterhead is the intended recipient, process the packet
            byte thingyID = actuateThingyPacket.getThingyId();

            //TODO alter this code to actually set the Thingy LED color
            // Must first wait for Clusterhead-Thingies connection to be implemented
            Log.i("SoundFragment", "Received packet to turn on LED for Thingy "+ thingyID);

            // Schedule for LED to turn off after timeout
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    // TODO alter this code to actually turn off Thingy LED color
                    Log.i("SoundFragment", "Timeout reached to turn off LED");
                }
            }, LED_BURN_TIME); //the time you want to delay in milliseconds

        } else {
            // If packet is not meant for this clusterhead, forward it
            forwardPacket(actuateThingyPacket);
        }
    }

    private void forwardPacket(BaseDataPacket packet) {
        // [source, 1, 2, 3, 0]
        byte[] route = null;
        Log.i(LOG_TAG, "Forwarding packet with destination " + packet.getDestinationID());
        if (packet.getDestinationID() == BaseDataPacket.SINK_ID) {
            route = mRoutes.get(mClhID);
        } else {
            route = mRoutes.get(packet.getDestinationID());
        }

        if (route == null) {
            // No route to the destination known, broadcast to all neighbors
            Log.i(LOG_TAG, "No route found, flooding network");
            packet.setReceiverId(BaseDataPacket.BROADCAST_ID);
        } else {
            Log.i(LOG_TAG, "Route found: " + Arrays.toString(route));

            // Route known, find next node
            int indexOfNode = -1;
            int indexOfDest = -1;
            for (int i = 0; i < route.length; i++) {
                if (route[i] == mClhID) {
                    indexOfNode = i;
                } else if (route[i] == packet.getDestinationID()) {
                    indexOfDest = i;
                }
            }

            // The route to the destination is in our route to sink map
            // Forward the packet to the destination via the next
            // hop according to our route to the sink
            byte nextStep = -1;
            if (indexOfNode < indexOfDest) {
                nextStep = route[indexOfNode + 1];
            } else if (indexOfNode > indexOfDest) {
                nextStep = route[indexOfNode - 1];
            }
            Log.i(LOG_TAG, "Next node is " + nextStep);
            packet.setReceiverId(nextStep);
        }

        mClhAdvertiser.addAdvPacketToBuffer(packet, false);
        Log.i(LOG_TAG, "Add data to advertised list, len:" + mClhAdvDataList.size());
        Log.i(LOG_TAG, "Advertise list at " + (mClhAdvDataList.size() - 1) + ":"
                + Arrays.toString(mClhAdvDataList.get(mClhAdvDataList.size() - 1).getData()));
    }


    private void saveRoute(byte[] route) {
        if (route.length == 0) return;
        Log.i(LOG_TAG, "Saving route" + Arrays.toString(route));

        byte routeSource = route[0];
        if (!mRoutes.containsKey(routeSource)) {
            // We do not have a route from this source yet, save it
            mRoutes.put(routeSource, route);
            Log.i(LOG_TAG, "Saving route for first time");
            return;
        }

        // Since we are also saving unresolved routes (routes that don't have the sink node inside)
        // we should also save the route if the new route is resolved, even though that route
        // may be longer in list length than the current route
        byte[] currentRoute = mRoutes.get(routeSource);
        boolean currentRouteHasSink = false;
        for (byte item : currentRoute) {
            if (item == BaseDataPacket.SINK_ID) currentRouteHasSink = true;
        }
        boolean newRouteHasSink = false;
        for (byte item : route) {
            if (item == BaseDataPacket.SINK_ID) newRouteHasSink = true;
        }

        if (route.length < currentRoute.length || !currentRouteHasSink && newRouteHasSink) {
            // Save the route if it is shorter or if it is a resolved route
            Log.i(LOG_TAG, "Replacing route " + Arrays.toString(currentRoute) + " with " + Arrays.toString(route));
            mRoutes.put(routeSource, route);
        }
    }

    public int startClusterScan() {
        if (!mThingyScanning) {
            final BluetoothLeScannerCompat scanner = BluetoothLeScannerCompat.getScanner();
            final no.nordicsemi.android.support.v18.scanner.ScanSettings settings =
                    new no.nordicsemi.android.support.v18.scanner.ScanSettings.Builder().setScanMode(no.nordicsemi.android.support.v18.scanner.ScanSettings.SCAN_MODE_LOW_LATENCY).setReportDelay(750)
                            .setUseHardwareBatchingIfSupported(false).setUseHardwareFilteringIfSupported(false).build();
            final List<no.nordicsemi.android.support.v18.scanner.ScanFilter> filters = new ArrayList<>();
            filters.add(new no.nordicsemi.android.support.v18.scanner.ScanFilter.Builder().setServiceUuid(new ParcelUuid(ThingyUtils.THINGY_BASE_UUID)).build());
            scanner.startScan(filters, settings, scanCallBack);
            mThingyScanning = true;
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mThingyScanning) {
                        scanner.stopScan(scanCallBack);
                        clusterHead.startAdvertisingCluster();
                    }
                }
            }, THINGY_SCAN_DURATION);
        } else {
            return ClhErrors.ERROR_CLH_THINGY_SCAN_ALREADY_START;
        }
        return ClhErrors.ERROR_CLH_NO;
    }

    private void stopScan() {
    }

    public void setClusterHead(ClusterHead clusterHead) {
        this.clusterHead = clusterHead;
    }


    private no.nordicsemi.android.support.v18.scanner.ScanCallback scanCallBack = new no.nordicsemi.android.support.v18.scanner.ScanCallback() {
        @Override
        public void onScanResult(int callbackType, @NonNull no.nordicsemi.android.support.v18.scanner.ScanResult result) {
            clusterHead.addToCluster(result);
        }

        @Override
        public void onBatchScanResults(@NonNull List<no.nordicsemi.android.support.v18.scanner.ScanResult> results) {
            super.onBatchScanResults(results);
            Log.i(LOG_TAG, "Got " + results.size() + " results in batch");
            for (no.nordicsemi.android.support.v18.scanner.ScanResult result : results) {
                clusterHead.addToCluster(result);
            }
//            clusterHead.startAdvertisingCluster();
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }

        @Override
        protected void finalize() throws Throwable {
            super.finalize();
        }
    };
}
