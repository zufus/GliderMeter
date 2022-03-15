package com.skylabmodels.glidermeter;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;


import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.skylabmodels.glidermeter.databinding.DihedralFragmentBinding;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DihedralFragment extends Fragment implements sklmBroadcastListener {

    private static final String TAG = "Main Activity";
    private static final int ELEV = 0;
    private static final int WING = 1;

    private static List<sklmBLTSensor> sensorList;
    private com.skylabmodels.glidermeter.databinding.DihedralFragmentBinding ui;

    private ActivityResultLauncher<String[]> requestPermissionsLauncher;
    private ActivityResultLauncher<Intent> enableBtResultLauncher;

    // The following code setup a receiver which detects if the BLUETOOTH sensor disconnects;
    BroadcastReceiver btReceiver = new sklmBluetoothReceiver();

    private interface setPreference {
        void set(sklmBLTSensor sensor, String value);
    }


    Map<String, setPreference> preferenceMap;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        preferenceMap = new HashMap<>();
        createPreferenceMap();
    }


    @RequiresApi(api = Build.VERSION_CODES.S)
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment with the ProductGrid theme
        //View view = inflater.inflate(R.layout.dihedral_fragment, container, false);
        ui = DihedralFragmentBinding.inflate(inflater, container, false);
        View view = ui.getRoot();
        Log.d(TAG, "Binding done");
        setUpToolbar(view);




        sensorList = createSensorList();

        // We can now setup the UI
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setOrientation(RecyclerView.VERTICAL);
        sklmSensorViewAdapter adapter = new sklmSensorViewAdapter(sensorList);

        // We now setup the UI
        ui.recyclerView.setLayoutManager(linearLayoutManager);
        ui.recyclerView.setAdapter(adapter);

        int spacing = getResources().getDimensionPixelSize(R.dimen.spacing_large);
        ui.recyclerView.addItemDecoration(new sklmSensorDecoration(spacing));
        // Calibration XY
        ui.buttonCalibrateXY.setOnClickListener(v -> {
            // TODO: add code to calibrate the sensors
            for (sklmBLTSensor s : sensorList){
                s.calibrateXY(getContext());
            }
        });

        // Calibration Z
        ui.buttonCalibrateZ.setOnClickListener(v -> {
            for (sklmBLTSensor s : sensorList){
                s.calibrateZ(getContext());
            }

        });

        //SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());


        //We can now start the handler to process data received by the sensors
        //Setup the queue that processes all the data received by the sensors
        // In each Connection Thread, when data are read, the data are sent to the following handler
        Handler queueMessageHandler = new Handler(Looper.getMainLooper(), msg -> {
            //TODO: Redefine processBuffer function to directly update the correct sensor value
            byte[] buffer= (byte[]) msg.obj;
            setSensorValue(msg.what, new String(buffer));

            //TODO: Import the getAngle function
            //ui.textViewResult.setText(String.format(Locale.ITALIAN, "%3.1f", getAngle(ELEVATOR) - getAngle(WING)));

            return true;
        });


        // We now associate the Handler to all the sensors
        adapter.setSensorHandler(queueMessageHandler, ELEV);
        adapter.setSensorHandler(queueMessageHandler, WING);

        // Register the permissions callback, which handles the user's response to the
        // system permissions dialog. Save the return value, an instance of
        // ActivityResultLauncher, as an instance variable.
        requestPermissionsLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                        permissionsMap -> {
            if (!permissionsMap.containsValue(false)) {
                // Permission is granted. Continue the action or workflow in your
                // app.
                Log.d(TAG, "BT permissions granted.");
            } else {
                // Explain to the user that the feature is unavailable because the
                // features requires a permission that the user has denied. At the
                // same time, respect the user's decision. Don't link to system
                // settings in an effort to convince the user to change their
                // decision.
                Log.d(TAG, "BT permissions **NOT* granted.");
            }
        });

        enableBtResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_CANCELED) {
                        Snackbar.make(ui.getRoot(), getString(R.string.snackPleaseEnableBT), BaseTransientBottomBar.LENGTH_LONG).show();
                    }
                }
        );

        checkBTState();
        return view;
    }

    @Override
    public void onPause(){
        super.onPause();
        LocalBroadcastManager bm = LocalBroadcastManager.getInstance(requireContext());
        bm.unregisterReceiver(btReceiver);
    }

    @Override
    public void onResume(){
        super.onResume();

        // Here we create the IntentFilter to detect Bluetooth connection state changes
        IntentFilter Connected= new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
        IntentFilter DisconnectRequest = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        IntentFilter Disconnect = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);

        //Register the intents to handle the device disconnections
        requireContext().registerReceiver(btReceiver, Connected);
        requireContext().registerReceiver(btReceiver, DisconnectRequest);
        requireContext().registerReceiver(btReceiver, Disconnect);


        /**
         *  Every time we get to this fragment, we check if preferences have been updated.
         *  Upon exiting {@link SettingsActivity} creates a list with all the preferences that have
         *  been updated.
         *
         *  In the following section, we process the list and apply the required changes.
         */


        // If the sensor address has been updated in the preferences, we disconnect the sensor
        // and set the new address accordingly.
        Log.d(TAG, "on Resume: checking changed preferences");

        for (sklmBLTSensor sensor : sensorList){
            for (HashMap.Entry<String, String> pref : sensor.pendingPrefsChange.entrySet()) {
                Log.d(TAG, "Found preference to be applied: " + pref.getKey());
                if (pref.getKey().equals(sensor.getPrefKey())) {
                    Log.d(TAG, "Preference indicates we need to set/change the sensor MAC address");
                    String address = pref.getValue();
                    sensor.disconnect();
                    sensor.setAddress(address);
                    Log.d(TAG, "Preference " + pref.getKey() + "updated. We mow remove it from the queue");
                    sensor.pendingPrefsChange.remove(pref.getKey());
                }

            }
        }


        String[] permissions = (Build.VERSION.SDK_INT > Build.VERSION_CODES.R) ?
                new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN} :
                new String[] {Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN};
        requestPermissionsLauncher.launch(permissions);



    }

    @Override
    public void onDestroyView(){
        super.onDestroyView();
        ui = null;
    }

    private List<sklmBLTSensor> createSensorList(){

        sklmBLTSensor wing, elevator;
        elevator = new sklmBLTSensor(ELEV, getResources().getString(R.string.elevator_device_title), "elevator");
        wing = new sklmBLTSensor(WING, getResources().getString(R.string.wing_device_title), "wing");

        return Arrays.asList(wing, elevator);
    }

    private void setSensorValue(int position, String value){
        sklmSensorViewHolder h = (sklmSensorViewHolder) ui.recyclerView.findViewHolderForLayoutPosition(position);
        if (h != null)
            h.sensorValue.setText(value);
        if (h == null)
            Log.d(TAG, "Holder pointing to null... :(");
    }

    private void setUpToolbar(View view) {
        Toolbar toolbar = view.findViewById(R.id.app_bar);
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity != null) {
            activity.setSupportActionBar(toolbar);
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater menuInflater) {
        menuInflater.inflate(R.menu.actions, menu);
        super.onCreateOptionsMenu(menu, menuInflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_preferences) {
            // User chose the "Settings" item, show the app settings UI...
            Intent intent = new Intent(getContext(), SettingsActivity.class);
            intent.putExtra("LIST", (Serializable) sensorList);
            startActivity(intent);
            Log.d(TAG, "Pressed Settings");
        }
        return super.onOptionsItemSelected(item);
    }


    private void checkBTState() {
        // Check for Bluetooth support and then check to make sure it is turned on
        // Emulator doesn't support Bluetooth and will return null
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!btAdapter.isEnabled()) {
            Log.d(TAG, "...Bluetooth off we request to enable it...");
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableBtResultLauncher.launch(enableBtIntent);
        }
    }

    public void updateSwitches(BluetoothDevice d) {
        String address = d.getAddress();
        Log.d(TAG, "in updateSwitches: called by " + d);
        Toast.makeText(getContext(), "Received by Sensor " + d, Toast.LENGTH_LONG).show();
        int position = 0;
        for (sklmBLTSensor s : sensorList ) {
            if (s.getAddress().equals(address)) {
                s.disconnect();
                sklmSensorViewHolder h = (sklmSensorViewHolder) ui.recyclerView.findViewHolderForLayoutPosition(position);
                if (h != null)
                    h.sensorSwitch.setChecked(false);
            }
            position++;
        }
    }

    public void createPreferenceMap(){

        preferenceMap.put("averages", (sensor, value) -> sensor.setAverages(value));

        preferenceMap.put("address", (sensor, address) -> {
            //TODO add method to read data from sensor with averages
            sensor.disconnect();
            sensor.setAddress(address);
        });

        preferenceMap.put("bandwidth", (sensor, bw) -> sensor.setBW(bw));

        preferenceMap.put("return_rate", (sensor, r_rate) -> sensor.setRRate(r_rate));

    }

}
