package com.skylabmodels.glidermeter;

import androidx.appcompat.app.AppCompatActivity;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends AppCompatActivity implements sklmBroadcastListener {

    private static final String TAG = "MainActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.main_activity_container, new DihedralFragment())
                    .commit();
        }
    }


    /**
     * This method executes the updateSwitches status on the DihedralFragment.
     * @param d - the {@link BluetoothDevice} that has been disconnected
     */
    @Override
    public void updateSwitches(BluetoothDevice d) {
        //TODO Test what happens if the BLT sensor is disconnected when the fragment is not visible
        DihedralFragment f;

        f = (DihedralFragment) getSupportFragmentManager().findFragmentById(R.id.main_activity_container);
        if (f != null) {
            f.updateSwitches(d);
            return;
        }
        Log.d(TAG, "Fragment not found");
    }
}