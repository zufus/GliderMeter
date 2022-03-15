package com.skylabmodels.glidermeter;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;



public class SettingsActivity extends AppCompatActivity {
    private static final String TAG = "settings";

    List<sklmBLTSensor> sensorList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent i = getIntent();
        sensorList = (List<sklmBLTSensor>) i.getSerializableExtra("LIST");
        Log.d(TAG, "list of sensor size, " + sensorList.size());
        for (sklmBLTSensor s: sensorList) {
            Log.d(TAG, "Received by the intent: " + s.getSensorName());
        }
        setContentView(R.layout.settings_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    SharedPreferences.OnSharedPreferenceChangeListener listener = (sharedPreferences, key) -> {
        //TODO add code to configure the sensors based on the selected preferences...
        Log.d(TAG, key + " preference changed");

        //TODO Decide how to apply the changes:
        // Option 1: if sensors are not connected, the preferences are not selectable.
        //           otherwise, if the sensors are connected, apply the preference on the fly
        // Option 2: create a "queue" of new preferences values and apply them as soon as the Sensor
        //           is connected, or when going back to the measurement fragments

        for (sklmBLTSensor s : sensorList){
            s.addPendingPrefs(key, sharedPreferences.getString(key, ""));
        }
    };


    @Override
    public void onStart() {
        super.onStart();
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(listener);
    }

    @Override
    public void onStop() {
        super.onStop();
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(listener);
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {

        @RequiresApi(api = Build.VERSION_CODES.R)
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

            ActivityResultLauncher<String[]> requestPermissionsLauncher =
                    registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                    permissionsMap -> {
                        if (!permissionsMap.containsValue(false)) {
                            Log.d(TAG, "BT permissions granted.");
                        } else {

                            Log.d(TAG, "BT permissions **NOT* granted.");
                        }
                    });

            setPreferencesFromResource(R.xml.root_preferences, rootKey);


            //Query for paired devices
            //TODO try to move the authorization code to a reusable class
            int btPermission = ActivityCompat.checkSelfPermission(requireContext(),
                    (Build.VERSION.SDK_INT > Build.VERSION_CODES.R) ?
                            Manifest.permission.BLUETOOTH_CONNECT :
                            Manifest.permission.BLUETOOTH);
            BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();

            if ( btPermission != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "in onCreatePreferences(): requesting permission");
                requestPermissionsLauncher.launch((Build.VERSION.SDK_INT > Build.VERSION_CODES.R) ?
                        new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN} :
                        new String[] {Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN});
            }
            Log.d(TAG, "in onCreatePreferences(): BT Permission Requested, we now query the list of paired devices");

            // TODO Exit if permissions are not granted.


            Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();

            Log.d(TAG, "in onCreatePreferences(): Found " + pairedDevices.size() + " devices");

            ListPreference device_wing_list = (ListPreference) findPreference("wing");
            ListPreference device_elevator_list = (ListPreference) findPreference("elevator");

            if (pairedDevices.size() > 0) {
                PreferenceCategory category = (PreferenceCategory) findPreference("btSensorCategory");
                if (category == null){
                    return;
                }
                category.setTitle(R.string.sensor_summary);

                List<String> deviceNames = new ArrayList<>();
                List<String> deviceHardwareAddresses = new ArrayList<>();
                if(device_elevator_list == null || device_wing_list == null)
                    return;
                device_elevator_list.setEnabled(true);
                device_wing_list.setEnabled(true);

                for(BluetoothDevice bt : pairedDevices) {

                    String alias = bt.getAlias();

                    if (alias == null) {
                        alias = bt.getName();
                    }

                    if (alias == null)
                        break;
                    deviceNames.add(alias);
                    deviceHardwareAddresses.add(bt.getAddress());

                }
                CharSequence[] device_list_names = deviceNames.toArray(new CharSequence[0]);
                CharSequence[] device_list_mac = deviceHardwareAddresses.toArray(new CharSequence[0]);

                device_wing_list.setEntries(device_list_names);
                device_wing_list.setEntryValues(device_list_mac);

                device_elevator_list.setEntries(device_list_names);
                device_elevator_list.setEntryValues(device_list_mac);
            }
        }
    }
}
