package no.nordicsemi.android.nrfthingy.ClusterHead;
/*important constants*/
public class ClhConst {
    //for scanner
    public static final String clusterHeadName="CH";
    public static final int MAX_ADVERTISE_LIST_ITEM=512; //max items in waiting list for advertising
    public static final int ADVERTISING_INTERVAL=200; //default 200 ms interval for each advertising packet
    //----------
    //for scanner
    public static final int MIN_SCAN_RSSI_THRESHOLD=-120;    //min RSSI of receive packet from other clusterheads
    public static final long SCAN_PERIOD = 60000*5;   //scan 10 minutes
    public static final long REST_PERIOD=1000; //rest in 1 sec
    public static final int SCAN_HISTORY_LIST_SIZE=512; //max item in history list
    public static final int MAX_HOP_COUNT=10;

    //for processor
    public static final int MAX_PROCESS_LIST_ITEM=128; //max items in waiting list for processing
    public static final int MICROPHONE_BUFFER_PROCESS_INTERVAL = 200; // Number of ms between microphone buffer processing
    public static final int MICROPHONE_PROCESS_THRESHOLD = 37000;
    public static final double MICROPHONE_PROCESS_DECAY_FACTOR = 0.99; // Decay factor for envelope function
    public static final int MICROPHONE_PROCESS_MIN_DATAPOINTS_ABOVE_THRESHOLD = 500;
    public static final int SINK_PROCESS_INTERVAL = 750; // Number of ms between packet processing

}
