package no.nordicsemi.android.nrfthingy.ClusterHead;

import android.bluetooth.BluetoothDevice;

import androidx.annotation.Nullable;

import no.nordicsemi.android.support.v18.scanner.ScanResult;

public class ClusterLeaf {
    private String address;
    private int rssi;
    private boolean connected = false;

    public ClusterLeaf(String address, int rssi, boolean connected) {
        this.address = address;
        this.rssi = rssi;
        this.connected = connected;
    }

    public ClusterLeaf(ScanResult result) {
        this.address = result.getDevice().getAddress();
        this.rssi = result.getRssi();
    }

    public String getAddress() {
        return address;
    }

    public int getRssi() {
        return rssi;
    }

    public boolean isConnected() {
        return connected;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof ClusterLeaf) {
            boolean eq = super.equals(obj);
            if (eq) {
                ClusterLeaf c = (ClusterLeaf) obj;
                eq &= this.address == c.address;
                eq &= this.rssi == c.rssi;
                eq &= this.connected == c.connected;
                return eq;
            }
        }
        return false;
    }
}
