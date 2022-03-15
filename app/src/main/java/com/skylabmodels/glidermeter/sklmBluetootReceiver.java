package com.skylabmodels.glidermeter;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.fragment.app.FragmentActivity;

class sklmBluetoothReceiver extends BroadcastReceiver {
    private static final String TAG = "GliderMeter Broadcast receiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

        Log.d(TAG, "Broadcast received from " + context.toString());
        Log.d(TAG, action + device.toString());

        sklmBroadcastListener listener = (sklmBroadcastListener) context;

        //TODO Consider to add an action also for other intents.
        if (action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED)) {
            Log.d(TAG, "Action Disconnected. Calling updateSwitches");
            listener.updateSwitches(device);
        }
    }
}


