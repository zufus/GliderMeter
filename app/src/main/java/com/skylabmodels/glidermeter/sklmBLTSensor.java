package com.skylabmodels.glidermeter;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;


import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;





public class sklmBLTSensor implements Serializable {

    public int MESSAGE;
    public final String _TAG = "slmBLTSensor";

    /**
     * UUID String to initialize Serial Bluetooth communications
     */
    private final UUID SKLM_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private Communicator fabric;
    private boolean _isConnected, _isTransferring;
    private transient BluetoothDevice mmDevice;
    private BluetoothSocket mmSocket;
    private String address;
    private final String sensor_name;
    private final String prefKey;
    public HashMap<String, String> pendingPrefsChange;
    static Handler queue;

    private static final byte[] msgCalXY    = {(byte) 0xFF, (byte) 0xAA, (byte) 0x67};
    private static final byte[] msgCalZ     = {(byte) 0xFF, (byte) 0xAA, (byte) 0x52};
    private static final byte[] msgSetRate  = {(byte) 0xFF, (byte) 0xAA, (byte) 0x03, (byte) 0x07};
    private static final byte[] msgSendCmd  = {(byte) 0xFF, (byte) 0xAA, (byte) 0x69, (byte) 0x88, (byte) 0xb5};
    private static final byte[] prefixBW    = {(byte) 0xFF, (byte) 0xAA, (byte) 0x1f};
    private static final byte[] prefixRRate = {(byte) 0xFF, (byte) 0xAA, (byte) 0x03};
    private static final byte[] msgSave     = {(byte) 0xFF, (byte) 0xAA, (byte) 0x00, (byte) 0x00, (byte) 0x00};
    private static final byte suffix        = (byte) 0x00;

    public static final String DEFAULT_MAC_ADDRESS = "01:00:00:00:00:00";





    /**
     * Class constructor:
     * @param m: an integer to enumerate the sensors
     * @param name: a string indicating the sensor name
     * @param key: a string holding the key of the ListPreference resources, used to set/get the
     *           sensor mac-address
     */
    public sklmBLTSensor(int m, String name, String key) {
        mmDevice = null;
        mmSocket = null;
        queue = null;
        address = DEFAULT_MAC_ADDRESS;
        MESSAGE = m;
        prefKey = key;
        sensor_name = name;
        pendingPrefsChange = new HashMap<String, String>();
        _isTransferring = false;
        _isConnected = false;



        Log.d(_TAG, "Created " + sensor_name);
    }



    /**
     * Returns true if the sensor device has been verified and it is valid.
     * @return true if the sensor address is valid.
     */
    public boolean isAddressValid() {
        return mmDevice != null && BluetoothAdapter.checkBluetoothAddress(address);
    }

    /**
     * Returns true if connection has been established
     * @return true if sensor is connected
     */
    public boolean isConnected() {
        return _isConnected;
    }


    /**
     * Return the string containing the sensor name.
     * @return String
     */
    public String getSensorName() {
        return sensor_name;
    }


    public void addPendingPrefs(String key, String value){
        pendingPrefsChange.put(key, value);
    }

    /**
     * Set the {@link Handler} queue receiving the data read from the sensor
     * @param mainQueue the {@link Handler} that handles the received data
     */
    public void setQueue(Handler mainQueue) {
        queue = mainQueue;
    }

    /**
     * Set the sensor MAC address and link the sensor to the remote {@link BluetoothDevice}.
     * If the provided MAC address is not valid, or it is not possible to find a {@link BluetoothDevice}
     * at the given address, the MAC address is not updated.
     * @param deviceAddress -- a valid Bluetooth MAC address
     */
    public void setAddress(String deviceAddress) {
        if (!BluetoothAdapter.checkBluetoothAddress(deviceAddress)) {
            Log.d(_TAG, "Address " + deviceAddress + "is not valid");
            return;
        }

        Log.d(_TAG, "Setting address to: " + deviceAddress);
        address = deviceAddress;

        try {
            mmDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(address);
        } catch (IllegalArgumentException e) {
            Log.e(_TAG, "Illegal address");
        }
    }

    /**
     * Send command to calibrate the sensor on X and Y axis.
     * @param c - {@link Context} required to display the {@link Toast} message in case of errors.
     */
    public void calibrateXY(Context c){
        if (!_isConnected && !_isTransferring) {
            Log.d (_TAG, String.valueOf(R.string.onErrorNotConnected));
            Toast.makeText(c, R.string.onErrorNotConnected, Toast.LENGTH_LONG).show();
            return;
        }
        try {
            fabric.write(msgCalXY);
        } catch (IOException e) {
            Log.d(_TAG, "Error sending calibration XY: " + e.getMessage());
            Toast.makeText(c, R.string.onErrorWritingMsgCal, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Send command to calibrate the sensor on Z axis.
     * @param c - {@link Context} required to display the {@link Toast} message in case of errors.
     */
    public void calibrateZ(Context c) {
        if (!_isConnected && !_isTransferring) {
            Log.d (_TAG, String.valueOf(R.string.onErrorNotConnected));
            Toast.makeText(c, R.string.onErrorNotConnected, Toast.LENGTH_LONG).show();
            return;
        }
        try {
            fabric.write(msgCalZ);
        } catch (IOException e) {
            Log.d(_TAG, "Error sending calibration XY: " + e.getMessage());
            Toast.makeText(c, R.string.onErrorWritingMsgCal, Toast.LENGTH_LONG).show();
        }
    }

    public void setRRate(String rrate){

    }

    public void setAverages(String a){

    }

    public void setBW(String bw) {


    }

    public void saveConfiguration() {

    }

    /**
     * Return MAC address associated to the sensor.
     * @return String -- current MAC address.
     */
    public String getAddress() {
        return address;
    }

    /**
     * Returns the preference key to be queried in order to set/get values of MAC address in
     * the {@link androidx.preference.PreferenceManager}
     * @return String - PreferenceManager key
     */
    public String getPrefKey() {
        return prefKey;
    }



    public void connect(Context context) throws Exception {
        Log.d(_TAG, "in Connector: check permissions.");

        Log.d(_TAG, "Context: " + context.toString());
        int btPermission = (Build.VERSION.SDK_INT > Build.VERSION_CODES.R) ?
                ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) :
                ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH);

        if (btPermission == PackageManager.PERMISSION_GRANTED)
            try {
                if (mmSocket == null)
                    mmSocket = mmDevice.createRfcommSocketToServiceRecord(SKLM_UUID);
                mmSocket.connect();
                Log.d(_TAG, ".... BT Connection to " + sensor_name + " ok...");
                Toast.makeText(context, "Connected to" + sensor_name, Toast.LENGTH_LONG).show();
                _isConnected = true;
            } catch (IOException e) {
                this.disconnect();
                throw new IOException("Connection Failed");
            }


    }

    public void disconnect() {
        if (_isTransferring)
            fabric.cancel();
        if (_isConnected)
            try {
                mmSocket.close();
                Log.d(_TAG, ".... BT Connection " + sensor_name + " failed...");
            } catch (IOException e2) {
                Log.d(_TAG, "In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
            }
            _isConnected = false;
    }

    public void readData() {
        fabric = new Communicator();
        fabric.start();
    }

    private void setReturnRate() {
        //TODO Add string to set return rate to the device
    }



    private class Communicator extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        boolean stop;


        public Communicator() {

            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            _isTransferring = false;
            stop = false;

            // Get the input and output streams; using temp objects because
            // member streams are final.
            try {
                tmpIn = mmSocket.getInputStream();
            } catch (IOException e) {
                stop = true;
                Log.e(_TAG, "Error occurred when creating input stream", e);
            }
            try {
                tmpOut = mmSocket.getOutputStream();
            } catch (IOException e) {
                stop = true;
                Log.e(_TAG, "Error occurred when creating output stream", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;

        }

        public void run() {
            int bytes_available, bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (!stop) {

                try {

                    //TODO: test the code with and without the call to sleep(50);
                    //sleep(50);

                    bytes_available = mmInStream.available();
                    if (bytes_available > 0) {
                        byte[] buffer = new byte[bytes_available];  // buffer store for the stream
                        //bytes = mmInStream.read(buffer, 0, 200);        // Get number of bytes and message in "buffer"
                        bytes = mmInStream.read(buffer);
                        //Log.d(_TAG, "Bytes Read: " + bytes);

                        queue.obtainMessage(MESSAGE, bytes, -1, buffer).sendToTarget();     // Send to message queue Handler in main thread
                    }

                } catch (IOException e) {
                    Log.d(_TAG, "Reading from BT exception: " + e.getMessage());
                    break;
                }
            }

            _isTransferring = false;
        }

        public void write(byte[] data) throws IOException {
            mmOutStream.write(data);

        }

        public void cancel() {
            stop = true;
        }

    }
}
