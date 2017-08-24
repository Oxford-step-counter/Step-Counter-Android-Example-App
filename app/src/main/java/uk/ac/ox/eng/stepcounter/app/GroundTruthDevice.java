package uk.ac.ox.eng.stepcounter.app;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Created by Dario Salvi on 23/08/2017.
 */

public class GroundTruthDevice {

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner scanner;
    private BluetoothGatt mBluetoothGatt;

    private String deviceName = "RFduino";
    private final String serviceUUID = "00002220-0000-1000-8000-00805f9b34fb";
    private final String receiveUUID = "00002221-0000-1000-8000-00805f9b34fb";

    // max time in ms to find device
    private final int SCAN_PERIOD = 40000;

    // polling period, in ms
    private final int POLLING_PERIOD = 100;

    private boolean mConnected;
    private int lastValue = -1;

    private Thread pollingThread;


    public interface GTdeviceCallback{
        void connected();
        void disconnected();
        void stepDetected(boolean left, boolean right);
    }
    private GTdeviceCallback callback;
    private Context context;

    GroundTruthDevice(GTdeviceCallback callback, Context ctx){
        this.callback = callback;
        this.context = ctx;
        this.mConnected = false;
    }

    public void connect(){
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (!mConnected) {
            // Attempt to connect
            scanner = mBluetoothAdapter.getBluetoothLeScanner();
            final ScanFilter filter = new ScanFilter.Builder()//.setServiceUuid(ParcelUuid.fromString(serviceUUID))
                    .setDeviceName(deviceName).build();
            ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
            scanner.startScan (new ArrayList<ScanFilter>(Arrays.asList(filter)),
                    settings,
                    handleScan);

            // Handler to stop scan after a number of seconds
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!mConnected) {
                        scanner.stopScan(handleScan);
                        callback.disconnected();
                    }
                }

            }, SCAN_PERIOD);
        } else {
            callback.connected();
        }
    }

    public void disconnect(){
        mConnected = false;
        if(scanner != null) scanner.stopScan(handleScan);
        if(mBluetoothGatt != null) mBluetoothGatt.disconnect();
    }

    private ScanCallback handleScan = new ScanCallback(){
        @Override
        public void onBatchScanResults(List<ScanResult> results){
            scanner.stopScan(this);

        };

        @Override
        public void onScanFailed(int errorCode){
            if(errorCode != SCAN_FAILED_ALREADY_STARTED){
                mConnected = false;
                callback.disconnected();
            }
        };

        @Override
        public void onScanResult(final int callbackType, ScanResult result){
            scanner.stopScan(this);
            BluetoothDevice device = result.getDevice();
            mBluetoothGatt = device.connectGatt(context, false, new BluetoothGattCallback() {

                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                    super.onConnectionStateChange(gatt, status, newState);
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        Log.d(GroundTruthDevice.class.getSimpleName(), "Ground truth device connected");
                        mBluetoothGatt.discoverServices();
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        Log.d(GroundTruthDevice.class.getSimpleName(), "Ground truth device disconnected");
                        mConnected = false;
                        callback.disconnected();
                    }
                }

                @Override
                public void onServicesDiscovered(final BluetoothGatt gatt, int status) {
                    super.onServicesDiscovered(gatt, status);

                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        BluetoothGattService serv = gatt.getService(UUID.fromString(serviceUUID));
                        final BluetoothGattCharacteristic mCharacteristic = serv.getCharacteristic(UUID.fromString(receiveUUID));
                        if(mCharacteristic != null) {
                            //Now we assume that the device is fully connected
                            mConnected = true;
                            callback.connected();

                            // start a parallel thread that reads the characteristic
                            pollingThread = new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    while(mConnected){
                                        gatt.readCharacteristic(mCharacteristic);
                                        try {
                                            Thread.sleep(POLLING_PERIOD);
                                        } catch (InterruptedException e) {
                                        }
                                    }
                                }
                            });
                            pollingThread.start();
                        } else {
                            mConnected = false;
                            gatt.disconnect();
                        }
                    }
                }

                @Override
                public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    byte[] value = characteristic.getValue();
                    // Process bit array.
                    value = reverseArray(value);
                    ByteBuffer wrapped = ByteBuffer.wrap(value);
                    int val = wrapped.getInt();
                    // Log.d(this.getClass().getName(), "Received value "+ val);
                    if(val != lastValue){
                        switch(val) {
                            case 0:
                                callback.stepDetected(false, false);
                                break;
                            case 1:
                                callback.stepDetected(false, true);
                                break;
                            case 2:
                                callback.stepDetected(true, false);
                                break;
                            case 3:
                                callback.stepDetected(true, true);
                                break;
                        }
                        lastValue = val;
                    }
                }
            });
        }
    };


    private byte[] reverseArray(byte[] arr) {
        byte[] out = new byte[arr.length];
        for (int i = arr.length - 1; i > -1; i--) {
            out[arr.length - 1 - i] = arr[i];
        }
        return out;
    }
}
